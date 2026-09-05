package com.corazon.corazonmod.game;

import com.corazon.corazonmod.CorazonMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event listener for interactive player actions during the Minigame Phase.
 * Any punch/hit or block click during MINIGAME phase grants points towards group Extra Search Time!
 */
@Mod.EventBusSubscriber(modid = CorazonMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MinigameInteractionHandler {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning() || game.getCurrentPhase() != GamePhase.MINIGAME) return;
        if (!game.isParticipant(player.getUUID()) || !game.isAlive(player.getUUID())) return;

        game.addMinigameScore(player, 1);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning() || game.getCurrentPhase() != GamePhase.MINIGAME) return;
        if (!game.isParticipant(player.getUUID()) || !game.isAlive(player.getUUID())) return;

        game.addMinigameScore(player, 1);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning() || game.getCurrentPhase() != GamePhase.MINIGAME) return;
        if (!game.isParticipant(player.getUUID()) || !game.isAlive(player.getUUID())) return;

        game.addMinigameScore(player, 1);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning() || game.getCurrentPhase() != GamePhase.MINIGAME) return;
        if (!game.isParticipant(player.getUUID()) || !game.isAlive(player.getUUID())) return;

        game.addMinigameScore(player, 1);
    }
}
