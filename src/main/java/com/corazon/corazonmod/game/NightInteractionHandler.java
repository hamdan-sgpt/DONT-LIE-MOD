package com.corazon.corazonmod.game;

import com.corazon.corazonmod.CorazonMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles physical in-world interactions during Night Phase sequential turns:
 * - Mafia attacks/interacts with player -> Selects execution target & advances to Doctor turn.
 * - Doctor interacts with player (or self) -> Protects target & advances to Police turn.
 * - Police interacts with player -> Investigates target identity & advances to Morning.
 */
@Mod.EventBusSubscriber(modid = CorazonMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NightInteractionHandler {

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;

        if (handleNightSelection(player, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof ServerPlayer target)) return;

        if (handleNightSelection(player, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning() || game.getCurrentPhase() != GamePhase.NIGHT) return;

        // If Doctor right-clicks item/air, Doctor can protect himself/herself!
        if (game.getNightSubPhase() == 1 && game.getRole(player.getUUID()) == PlayerRole.DOCTOR) {
            handleDoctorSelfProtect(player);
            event.setCanceled(true);
        }
    }

    private static boolean handleNightSelection(ServerPlayer player, ServerPlayer target) {
        DontLieGame game = DontLieGame.getInstance();
        if (!game.isGameRunning() || game.getCurrentPhase() != GamePhase.NIGHT) return false;
        if (!game.isParticipant(player.getUUID()) || !game.isAlive(player.getUUID())) return false;
        if (!game.isParticipant(target.getUUID()) || !game.isAlive(target.getUUID())) return false;

        PlayerRole role = game.getRole(player.getUUID());
        int nightTurn = game.getNightSubPhase();

        // 1. MAFIA TURN (nightTurn == 0)
        if (nightTurn == 0 && role == PlayerRole.MAFIA) {
            if (game.getRole(target.getUUID()) == PlayerRole.MAFIA) {
                player.sendSystemMessage(Component.literal("⚠️ Kamu tidak bisa mengeksekusi sesama Mafia!").withStyle(ChatFormatting.RED));
                return true;
            }
            game.setMafiaTarget(player, target);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
            player.sendSystemMessage(Component.literal("🔪 Target eksekusi telah dipilih: " + target.getScoreboardName() + "!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            game.advanceNightTurn(player.getServer());
            return true;
        }

        // 2. DOCTOR TURN (nightTurn == 1)
        if (nightTurn == 1 && role == PlayerRole.DOCTOR) {
            game.setDoctorTarget(player, target);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_DIAMOND, SoundSource.PLAYERS, 1.0f, 1.2f);
            player.sendSystemMessage(Component.literal("🛡️ Kamu memilih untuk melindungi " + target.getScoreboardName() + " malam ini!").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
            game.advanceNightTurn(player.getServer());
            return true;
        }

        // 3. POLICE TURN (nightTurn == 2)
        if (nightTurn == 2 && role == PlayerRole.POLICE) {
            game.setPoliceCheck(player, target);
            player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.5f);
            game.advanceNightTurn(player.getServer());
            return true;
        }

        return false;
    }

    private static void handleDoctorSelfProtect(ServerPlayer doctor) {
        DontLieGame game = DontLieGame.getInstance();
        game.setDoctorTarget(doctor, doctor);
        doctor.level().playSound(null, doctor.blockPosition(), SoundEvents.ARMOR_EQUIP_DIAMOND, SoundSource.PLAYERS, 1.0f, 1.2f);
        doctor.sendSystemMessage(Component.literal("🛡️ Kamu memilih untuk melindungi DIRI SENDIRI malam ini!").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
        game.advanceNightTurn(doctor.getServer());
    }
}
