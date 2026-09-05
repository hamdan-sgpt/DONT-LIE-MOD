package com.corazon.corazonmod.client;

import com.corazon.corazonmod.network.OpenVotingScreenPacket;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Client-side handler that receives packets from the server and opens appropriate GUI screens.
 * This class MUST only be called from the client distribution.
 */
public class ClientPacketHandler {

    // Cached game state for HUD overlay
    public static String currentPhaseName = "";
    public static int currentPhaseColor = 0xFFFFFF;
    public static String currentRoleName = "";
    public static int currentRoleColor = 0xFFFFFF;
    public static int currentTimeRemaining = 0;
    public static int currentAliveCount = 0;
    public static int currentTotalPlayers = 0;
    public static boolean isPlayerAlive = true;
    public static boolean isInGame = false;

    public static void handleRoleReveal(String roleName, String roleDescription, int roleColorRGB) {
        Minecraft.getInstance().execute(() -> {
            isInGame = true;
            currentRoleName = roleName;
            currentRoleColor = roleColorRGB;
            Minecraft.getInstance().setScreen(new RoleRevealScreen(roleName, roleDescription, roleColorRGB));
        });
    }

    public static void handleOpenVoting(List<OpenVotingScreenPacket.PlayerEntry> alivePlayers) {
        Minecraft.getInstance().execute(() -> {
            currentPhaseName = "Voting Phase";
            Minecraft.getInstance().setScreen(new VotingScreen(alivePlayers));
        });
    }

    public static void handleOpenNightAction(String actionType, List<OpenVotingScreenPacket.PlayerEntry> targetPlayers) {
        Minecraft.getInstance().execute(() -> {
            currentPhaseName = "Night Elimination";
            Minecraft.getInstance().setScreen(new NightActionScreen(actionType, targetPlayers));
        });
    }

    public static void handleGameStateUpdate(String phaseName, int phaseColorRGB, String roleName, int roleColorRGB,
                                              int timeRemaining, int aliveCount, int totalPlayers, boolean isAlive) {
        currentPhaseName = phaseName;
        currentPhaseColor = phaseColorRGB;
        currentRoleName = roleName;
        currentRoleColor = roleColorRGB;
        currentTimeRemaining = timeRemaining;
        currentAliveCount = aliveCount;
        currentTotalPlayers = totalPlayers;
        isPlayerAlive = isAlive;
        isInGame = true;

        // Auto-close open GUI screens if phase changes or game ends
        Minecraft.getInstance().execute(() -> {
            net.minecraft.client.gui.screens.Screen currentScreen = Minecraft.getInstance().screen;
            if (currentScreen instanceof VotingScreen && !"Voting Phase".equalsIgnoreCase(phaseName)) {
                Minecraft.getInstance().setScreen(null);
            }
            if (currentScreen instanceof NightActionScreen && !"Night Elimination".equalsIgnoreCase(phaseName)) {
                Minecraft.getInstance().setScreen(null);
            }
            if (currentScreen instanceof RunnerSelectionScreen && !"Voting Runner".equalsIgnoreCase(phaseName)) {
                Minecraft.getInstance().setScreen(null);
            }
        });

        // Reset when game ends
        if ("Game Ended".equals(phaseName) || "Lobby".equals(phaseName)) {
            isInGame = false;
        }
    }

    public static void handleOpenAdminMenu(com.corazon.corazonmod.network.OpenAdminMenuPacket packet) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new AdminControlScreen(packet));
        });
    }

    public static void handleOpenRunnerSelection(com.corazon.corazonmod.network.OpenRunnerSelectionPacket packet) {
        Minecraft.getInstance().execute(() -> {
            currentPhaseName = "Voting Runner";
            List<com.corazon.corazonmod.network.OpenRunnerSelectionPacket.RunnerCandidateEntry> candidates = packet.candidates;
            Minecraft.getInstance().setScreen(new RunnerSelectionScreen(candidates));
        });
    }

    public static void handleOpenParkourModeVote(com.corazon.corazonmod.network.OpenParkourModeVotePacket packet) {
        Minecraft.getInstance().execute(() -> {
            currentPhaseName = "Voting Mode Parkour";
            Minecraft.getInstance().setScreen(new ParkourModeVoteScreen(packet.durationSeconds));
        });
    }
}

