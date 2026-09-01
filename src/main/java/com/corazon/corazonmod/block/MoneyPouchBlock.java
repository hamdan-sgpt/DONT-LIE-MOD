package com.corazon.corazonmod.block;

import com.corazon.corazonmod.game.DontLieGame;
import com.corazon.corazonmod.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoneyPouchBlock extends Block {

    // Small 6x8x6 pixel box centered at the bottom of the block (fits in 0.5 block gaps / kolong)
    private static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 8.0D, 11.0D);

    public MoneyPouchBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Give Money Pouch item to player
        ItemStack pouchStack = new ItemStack(ModItems.MONEY_POUCH.get());
        if (!player.getInventory().add(pouchStack)) {
            player.drop(pouchStack, false);
        }

        // Play pickup sound
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Remove block from world
        level.removeBlock(pos, false);

        player.sendSystemMessage(Component.literal("💰 KAMU MENEMUKAN MONEY POUCH!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        DontLieGame game = DontLieGame.getInstance();
        if (game.isGameRunning() && player.getServer() != null) {
            game.broadcastToAll(
                player.getServer(),
                Component.literal("🔍 Seseorang telah menemukan Money Pouch yang disembunyikan!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
            );
        }

        return InteractionResult.SUCCESS;
    }
}
