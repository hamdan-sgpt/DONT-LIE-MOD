package com.corazon.corazonmod.game;

import com.corazon.corazonmod.CorazonMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler to completely disable PvP (Player vs Player punches, attacks, and damage)
 * whenever a Don't Lie game match is active.
 */
@Mod.EventBusSubscriber(modid = CorazonMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PvPProtectionHandler {

    /**
     * Prevents players from swinging attacks / punching each other when game is running.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer attacker)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (game.isGameRunning()) {
            // During NIGHT phase, allow NightInteractionHandler to handle role target selection
            if (game.getCurrentPhase() == GamePhase.NIGHT) return;

            event.setCanceled(true);
        }
    }

    /**
     * Prevents player vs player attack events when game is running.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning()) return;

        // Cancel damage if the source is another player
        if (event.getSource().getEntity() instanceof ServerPlayer) {
            event.setCanceled(true);
        }
    }

    /**
     * Secondary safety check: Cancels any PvP damage calculations when game is running.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning()) return;

        // Cancel hurt damage if caused by another player
        if (event.getSource().getEntity() instanceof ServerPlayer) {
            event.setCanceled(true);
        }
    }
}
