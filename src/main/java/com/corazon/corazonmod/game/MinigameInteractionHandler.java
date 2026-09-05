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

    @SubscribeEvent
    public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning() || game.getCurrentPhase() != GamePhase.MINIGAME) return;
        if (!game.isParticipant(player.getUUID()) || !game.isAlive(player.getUUID())) return;

        // Prevent dying during Parkour Minigame — Cancel damage & reset back to Parkour Start!
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.fallDistance = 0.0f;
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        var parkourStart = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getParkourStart();
        net.minecraft.server.level.ServerLevel dontLieLevel = player.getServer() != null ? player.getServer().getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY) : null;
        net.minecraft.server.level.ServerLevel level = dontLieLevel != null ? dontLieLevel : (net.minecraft.server.level.ServerLevel) player.level();

        player.teleportTo(level, parkourStart.x, parkourStart.y, parkourStart.z, parkourStart.yaw, parkourStart.pitch);
        player.connection.teleport(parkourStart.x, parkourStart.y, parkourStart.z, parkourStart.yaw, parkourStart.pitch);

        player.sendSystemMessage(Component.literal("⚠️ Kamu jatuh dari Parkour! Reset ke Garis Start.").withStyle(ChatFormatting.YELLOW));
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_HURT, net.minecraft.sounds.SoundSource.MASTER, 0.6f, 1.2f);
    }
}
