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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Listens for interactions with containers (chests, barrels, etc.).
 * Automatically accelerates Hiding Phase as soon as Mafia hides the Money Pouch in a chest.
 */
@Mod.EventBusSubscriber(modid = CorazonMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChestInteractionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning()) return;

        // Check if game is in HIDING phase
        if (game.getCurrentPhase() != GamePhase.HIDING) return;

        // Check if player is Mafia
        if (game.getRole(player.getUUID()) != PlayerRole.MAFIA) return;

        // Check if player is holding the Money Pouch
        ItemStack heldItem = event.getItemStack();
        if (!heldItem.is(ModItems.MONEY_POUCH.get())) return;

        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);

        // Check if block interacted with is a Container (Chest, Barrel, Shulker Box, etc.)
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
                heldItem.setCount(0);

                // Play lock / chest sound
                level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0f, 0.9f);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.0f, 1.2f);

                player.sendSystemMessage(Component.literal("💰 Uang berhasil kamu sembunyikan di dalam kontainer!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                // Broadcast to all players and immediately skip to SEARCH phase
                if (player.getServer() != null) {
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

                // Cancel default placement/open event to prevent dupe or glitch
                event.setCanceled(true);
            }
        }
    }
}
