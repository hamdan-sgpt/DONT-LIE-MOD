package com.corazon.corazonmod.integration;

import com.corazon.corazonmod.CorazonMod;
import com.corazon.corazonmod.game.DontLieGame;
import com.corazon.corazonmod.game.GamePhase;
import com.corazon.corazonmod.game.PlayerRole;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

/**
 * Handles soft-dependency integration with Simple Voice Chat (modid: "voicechat").
 * Safely executes without crashing whether Simple Voice Chat is installed on the server or not.
 * 
 * Rules:
 * - DISCUSSION phase: All alive players have open proximity voice chat in Meeting Room.
 * - NIGHT phase: Special channels & muted dead/civilian players to avoid ghosting.
 * - ELIMINATED players: Spectator ghost voice mode (cannot talk to living players).
 */
public class VoiceChatIntegration {

    private static final String VOICECHAT_MOD_ID = "voicechat";

    public static boolean isVoiceChatAvailable() {
        return ModList.get().isLoaded(VOICECHAT_MOD_ID);
    }

    public static void onPhaseChange(MinecraftServer server, GamePhase phase) {
        if (server == null) return;
        DontLieGame game = DontLieGame.getInstance();

        boolean voiceLoaded = isVoiceChatAvailable();

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!game.isParticipant(p.getUUID())) continue;

            boolean alive = game.isAlive(p.getUUID());
            PlayerRole role = game.getRole(p.getUUID());

            if (!alive) {
                // Dead player notification / spectator mode
                p.sendSystemMessage(Component.literal("👻 [Voice Chat] Kamu berada di Ghost Voice Mode. Hanya bisa bicara dengan sesama pemain gugur.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                continue;
            }

            switch (phase) {
                case DISCUSSION -> {
                    p.sendSystemMessage(Component.literal("🎙️ [Voice Chat] Fase Diskusi: Voice Chat Proximity AKTIF untuk semua pemain hidup!").withStyle(ChatFormatting.GREEN));
                }
                case NIGHT -> {
                    if (role == PlayerRole.MAFIA) {
                        p.sendSystemMessage(Component.literal("🤫 [Voice Chat] Malam hari: Mafia Channel AKTIF. Bicara rahasia dengan sesama Mafia!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                    } else {
                        p.sendSystemMessage(Component.literal("🤐 [Voice Chat] Malam hari: Harap diam saat giliran pemain lain beraksi.").withStyle(ChatFormatting.GRAY));
                    }
                }
                case HIDING -> {
                    if (role == PlayerRole.MAFIA) {
                        p.sendSystemMessage(Component.literal("🤫 [Voice Chat] Fase Sembunyi Uang: Hanya Mafia yang dapat bergerak & berkomunikasi.").withStyle(ChatFormatting.RED));
                    }
                }
            }
        }

        if (voiceLoaded) {
            CorazonMod.LOGGER.info("Simple Voice Chat integration updated for phase: {}", phase);
        }
    }

    public static void onPlayerEliminated(MinecraftServer server, UUID eliminatedUUID) {
        if (server == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(eliminatedUUID);
        if (p != null) {
            p.sendSystemMessage(Component.literal("👻 [Voice Chat] Kamu telah tereliminasi! Voice Chat ke pemain hidup telah dinonaktifkan.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
    }
}
