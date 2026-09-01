package com.corazon.corazonmod.entity;

import com.corazon.corazonmod.game.DontLieGame;
import com.corazon.corazonmod.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class MoneyPouchEntity extends Entity {

    private static final EntityDataAccessor<Integer> ATTACH_FACE = SynchedEntityData.defineId(MoneyPouchEntity.class, EntityDataSerializers.INT);

    public MoneyPouchEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ATTACH_FACE, Direction.UP.get3DDataValue());
    }

    public void setAttachFace(Direction face) {
        this.entityData.set(ATTACH_FACE, face.get3DDataValue());
    }

    public Direction getAttachFace() {
        return Direction.from3DDataValue(this.entityData.get(ATTACH_FACE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("AttachFace")) {
            setAttachFace(Direction.from3DDataValue(tag.getInt("AttachFace")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("AttachFace", getAttachFace().get3DDataValue());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack pouchStack = new ItemStack(ModItems.MONEY_POUCH.get());
            if (!serverPlayer.getInventory().add(pouchStack)) {
                serverPlayer.drop(pouchStack, false);
            }

            this.level().playSound(null, serverPlayer.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            this.level().playSound(null, serverPlayer.blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.0f, 1.2f);

            this.discard();

            serverPlayer.sendSystemMessage(Component.literal("💰 KAMU MENEMUKAN MONEY POUCH!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            DontLieGame game = DontLieGame.getInstance();
            if (game.isGameRunning() && serverPlayer.getServer() != null) {
                game.broadcastToAll(
                    serverPlayer.getServer(),
                    Component.literal("🔍 Seseorang telah menemukan Money Pouch yang disembunyikan!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                );
            }

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
