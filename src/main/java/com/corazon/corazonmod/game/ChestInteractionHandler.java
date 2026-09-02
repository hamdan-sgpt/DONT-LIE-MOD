package com.corazon.corazonmod.game;

import com.corazon.corazonmod.CorazonMod;
import com.corazon.corazonmod.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Listens for interactions with containers and physical Money Pouch placement/pickup.
 * Allows slipping Money Pouch into any tiny gap, under furniture, or inside chests.
 */
@Mod.EventBusSubscriber(modid = CorazonMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChestInteractionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DontLieGame game = DontLieGame.getInstance();
        boolean isGameHidingPhase = game.isGameRunning() 
            && game.getCurrentPhase() == GamePhase.HIDING 
            && game.getRole(player.getUUID()) == PlayerRole.MAFIA;

        boolean isTestingMode = !game.isGameRunning() || player.isCreative();

        // Only allow hiding if game is in HIDING phase (Mafia) OR player is testing outside game / in Creative mode
        if (!isGameHidingPhase && !isTestingMode) return;

        // Check if player is holding the Money Pouch
        ItemStack heldItem = event.getItemStack();
        if (!heldItem.is(ModItems.MONEY_POUCH.get())) return;

        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);

        // 1. Check if block interacted with is a Container (Chest, Barrel, Shulker Box, etc.)
        if (be instanceof Container container) {
            // Attempt to insert Money Pouch into container
            boolean inserted = false;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (container.getItem(slot).isEmpty()) {
                    container.setItem(slot, heldItem.copy());
                    inserted = true;
                    break;
                }
            }

            if (inserted) {
                // Remove item from player's hand
                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }

                // Play lock / chest sound
                level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0f, 0.9f);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.0f, 1.2f);

                player.sendSystemMessage(Component.literal("💰 Uang berhasil kamu sembunyikan di dalam kontainer!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                if (isGameHidingPhase) {
                    triggerHidingComplete(game, player);
                }
                event.setCanceled(true);
                return;
            }
        }

        // 2. Physical Placement anywhere in world (Kolong meja, sela-sela, bawah tangga/kasur/lantai)
        Vec3 hitLoc = event.getHitVec().getLocation();
        float playerYaw = player.getYRot();
        com.corazon.corazonmod.entity.MoneyPouchEntity pouchEntity = new com.corazon.corazonmod.entity.MoneyPouchEntity(
            com.corazon.corazonmod.init.ModEntities.MONEY_POUCH.get(),
            level
        );
        pouchEntity.moveTo(hitLoc.x, hitLoc.y, hitLoc.z, playerYaw, 0.0F);
        pouchEntity.setYRot(playerYaw);
        pouchEntity.setAttachFace(event.getFace());

        level.addFreshEntity(pouchEntity);

        // Remove item from hand
        if (!player.isCreative()) {
            heldItem.shrink(1);
        }

        // Play sounds
        level.playSound(null, hitLoc.x, hitLoc.y, hitLoc.z, SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, hitLoc.x, hitLoc.y, hitLoc.z, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0f, 0.9f);

        player.sendSystemMessage(Component.literal("💰 Uang berhasil kamu selipkan di lokasi ini!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        if (isGameHidingPhase) {
            triggerHidingComplete(game, player);
        }
        event.setCanceled(true);
    }



    private static void triggerHidingComplete(DontLieGame game, ServerPlayer player) {
        if (player.getServer() != null) {
            // Return Mafia back to initial spawn position so nobody suspects Mafia!
            game.returnMafiaToStartPos(player);

            game.broadcastToAll(
                player.getServer(),
                Component.literal("========================================").withStyle(ChatFormatting.GOLD)
            );
            game.broadcastToAll(
                player.getServer(),
                Component.literal("  💰 UANG TELAH DISEMBUNYIKAN OLEH MAFIA!  ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
            );
            game.broadcastToAll(
                player.getServer(),
                Component.literal("  Hiding Phase diselesaikan lebih cepat. Treasure Hunt dimulai!  ").withStyle(ChatFormatting.GREEN)
            );
            game.broadcastToAll(
                player.getServer(),
                Component.literal("========================================").withStyle(ChatFormatting.GOLD)
            );

            // Skip straight to SEARCH phase!
            game.setPhase(player.getServer(), GamePhase.SEARCH);
        }
    }
}
