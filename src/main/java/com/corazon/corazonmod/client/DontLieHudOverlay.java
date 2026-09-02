package com.corazon.corazonmod.client;

import com.corazon.corazonmod.CorazonMod;
import com.corazon.corazonmod.client.ClientPacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HUD Overlay that renders persistent game info on the player's screen:
 * - Role indicator (top-left corner with colored badge)
 * - Phase name & timer (top-center)
 * - Alive player count (top-right)
 * - Status indicator (ALIVE / ELIMINATED)
 *
 * Styled with semi-transparent background panels for readability.
 */
@Mod.EventBusSubscriber(modid = CorazonMod.MOD_ID, value = Dist.CLIENT)
public class DontLieHudOverlay {

    private static float closedProgress = 0.0f; // 0.0 = open, 1.0 = fully closed
    private static long lastAnimTime = System.currentTimeMillis();

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!ClientPacketHandler.isInGame) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        // ==========================================
        // === CUSTOM EYE CLOSING ANIMATION OVERLAY ===
        // ==========================================
        boolean isHidingPhase = "Money Hiding Phase".equalsIgnoreCase(ClientPacketHandler.currentPhaseName);
        boolean isNightMafia = "Night: Mafia Turn".equalsIgnoreCase(ClientPacketHandler.currentPhaseName);
        boolean isNightDoctor = "Night: Doctor Turn".equalsIgnoreCase(ClientPacketHandler.currentPhaseName);
        boolean isNightPolice = "Night: Police Turn".equalsIgnoreCase(ClientPacketHandler.currentPhaseName);
        boolean isNightGeneric = "Night Elimination".equalsIgnoreCase(ClientPacketHandler.currentPhaseName);

        boolean isMafia = "Mafia".equalsIgnoreCase(ClientPacketHandler.currentRoleName);
        boolean isDoctor = "Doctor".equalsIgnoreCase(ClientPacketHandler.currentRoleName);
        boolean isPolice = "Police".equalsIgnoreCase(ClientPacketHandler.currentRoleName);

        boolean shouldCloseEyes = false;
        String subText = "";

        if (ClientPacketHandler.isInGame && ClientPacketHandler.isPlayerAlive) {
            if (isHidingPhase) {
                shouldCloseEyes = !isMafia;
                subText = "Mafia sedang menyembunyikan Money Pouch...";
            } else if (isNightMafia) {
                shouldCloseEyes = !isMafia;
                subText = "Malam hari... Mafia sedang mencari sasaran!";
            } else if (isNightDoctor) {
                shouldCloseEyes = !isDoctor;
                subText = "Malam hari... Doctor sedang memilih pemain yang dilindungi!";
            } else if (isNightPolice) {
                shouldCloseEyes = !isPolice;
                subText = "Malam hari... Police sedang menyelidiki identitas pemain!";
            } else if (isNightGeneric) {
                shouldCloseEyes = !isMafia;
                subText = "Malam hari... Harap tunggu pergiliran!";
            }
        }

        long now = System.currentTimeMillis();
        float dt = Math.max(0.001f, Math.min(0.1f, (now - lastAnimTime) / 1000.0f));
        lastAnimTime = now;

        if (shouldCloseEyes) {
            closedProgress = Math.min(1.0f, closedProgress + dt * 2.5f); // Smooth closing transition (~0.4s)
        } else {
            closedProgress = Math.max(0.0f, closedProgress - dt * 2.5f); // Smooth opening transition (~0.4s)
        }

        if (closedProgress > 0.0f) {
            int halfH = screenHeight / 2 + 10;
            int topEyelidY = (int) (halfH * closedProgress);
            int bottomEyelidY = screenHeight - (int) (halfH * closedProgress);

            // Render Top & Bottom Eyelids (Cinematic Dark Solid)
            guiGraphics.fill(0, 0, screenWidth, topEyelidY, 0xFF0B0C10);
            guiGraphics.fill(0, bottomEyelidY, screenWidth, screenHeight, 0xFF0B0C10);

            // Render Eyelid Edge Gold Accent Lines
            if (topEyelidY > 2) {
                guiGraphics.fill(0, topEyelidY - 3, screenWidth, topEyelidY, 0xFFD4AF37);
            }
            if (bottomEyelidY < screenHeight - 2) {
                guiGraphics.fill(0, bottomEyelidY, screenWidth, bottomEyelidY + 3, 0xFFD4AF37);
            }

            // Render Center Closed-Eyes Banner when fully closed
            if (closedProgress >= 0.85f) {
                int cx = screenWidth / 2;
                int cy = screenHeight / 2;

                // Outer Card Box
                guiGraphics.fill(cx - 150, cy - 40, cx + 150, cy + 40, 0xEE1A1A24);
                
                // Gold Border
                guiGraphics.fill(cx - 150, cy - 40, cx + 150, cy - 38, 0xFFE5C158);
                guiGraphics.fill(cx - 150, cy + 38, cx + 150, cy + 40, 0xFFE5C158);
                guiGraphics.fill(cx - 150, cy - 40, cx - 148, cy + 40, 0xFFE5C158);
                guiGraphics.fill(cx + 148, cy - 40, cx + 150, cy + 40, 0xFFE5C158);

                // Title: CLOSED EYES
                String title = "🙈 MATAMU SEDANG DITUTUP";
                int titleW = font.width(title);
                guiGraphics.drawString(font, title, cx - titleW / 2, cy - 24, 0xFFFFAA00, true);

                // Subtitle
                int subW = font.width(subText);
                guiGraphics.drawString(font, subText, cx - subW / 2, cy - 4, 0xFFCCCCCC, true);

                // Pulsing Status Text
                float pulse = (float) (Math.sin(now * 0.005) * 0.5 + 0.5);
                int pulseAlpha = (int) (150 + pulse * 105);
                int pulseColor = (pulseAlpha << 24) | 0x00FF88;
                String hint = "Dilarang mengintip! Harap tunggu fase selesai...";
                int hintW = font.width(hint);
                guiGraphics.drawString(font, hint, cx - hintW / 2, cy + 16, pulseColor, true);
            }
        }

        // ==========================================
        // === TOP LEFT: Role Badge ===
        // ==========================================
        int roleBadgeX = 4;
        int roleBadgeY = 4;
        int roleBadgeWidth = font.width(ClientPacketHandler.currentRoleName) + 16;

        // Background panel
        guiGraphics.fill(roleBadgeX, roleBadgeY, roleBadgeX + roleBadgeWidth + 8, roleBadgeY + 16, 0xAA000000);

        // Colored accent bar on left edge
        guiGraphics.fill(roleBadgeX, roleBadgeY, roleBadgeX + 3, roleBadgeY + 16, 0xFF000000 | ClientPacketHandler.currentRoleColor);

        // Role icon (colored dot)
        guiGraphics.fill(roleBadgeX + 6, roleBadgeY + 5, roleBadgeX + 10, roleBadgeY + 11, 0xFF000000 | ClientPacketHandler.currentRoleColor);

        // Role name text
        guiGraphics.drawString(font, ClientPacketHandler.currentRoleName, roleBadgeX + 14, roleBadgeY + 4, 0xFF000000 | ClientPacketHandler.currentRoleColor, true);

        // ==========================================
        // === TOP CENTER: Phase & Timer ===
        // ==========================================
        String phaseText = ClientPacketHandler.currentPhaseName;
        String timerText = formatTime(ClientPacketHandler.currentTimeRemaining);
        String combinedText = phaseText + " | " + timerText;
        int combinedWidth = font.width(combinedText);
        int phaseCenterX = screenWidth / 2 - combinedWidth / 2;

        // Background panel with colored top accent line
        int panelY = 4;
        guiGraphics.fill(phaseCenterX - 8, panelY, phaseCenterX + combinedWidth + 8, panelY + 16, 0xEE111118);
        guiGraphics.fill(phaseCenterX - 8, panelY, phaseCenterX + combinedWidth + 8, panelY + 2, 0xFF000000 | ClientPacketHandler.currentPhaseColor);

        // Phase name
        guiGraphics.drawString(font, phaseText, phaseCenterX, panelY + 4, 0xFF000000 | ClientPacketHandler.currentPhaseColor, true);

        // Separator
        int sepX = phaseCenterX + font.width(phaseText);
        guiGraphics.drawString(font, " | ", sepX, panelY + 4, 0xFF888888, true);

        // Timer (flash red if <= 10 seconds)
        int timerX = sepX + font.width(" | ");
        int timerColor;
        if (ClientPacketHandler.currentTimeRemaining <= 10) {
            float flash = (float)(Math.sin(System.currentTimeMillis() * 0.01) * 0.5 + 0.5);
            timerColor = flash > 0.5f ? 0xFFFF4444 : 0xFFFF8888;
        } else if (ClientPacketHandler.currentTimeRemaining <= 30) {
            timerColor = 0xFFFFAA00;
        } else {
            timerColor = 0xFFFFFFFF;
        }
        guiGraphics.drawString(font, timerText, timerX, panelY + 4, timerColor, true);

        // Minigame Banner Overlay (When Minigame Phase is active)
        if ("Minigame Extra Time".equalsIgnoreCase(ClientPacketHandler.currentPhaseName)) {
            String miniHint = "🎯 MINIGAME PHASE: Pukul/klik blok/target untuk kumpulkan Poin Extra Time!";
            int miniW = font.width(miniHint);
            int miniX = screenWidth / 2 - miniW / 2;
            guiGraphics.fill(miniX - 6, panelY + 18, miniX + miniW + 6, panelY + 30, 0xDD220033);
            guiGraphics.drawString(font, miniHint, miniX, panelY + 20, 0xFFFF88FF, true);
        }

        // ==========================================
        // === TOP RIGHT: Alive Count & Status ===
        // ==========================================
        String aliveText = "Hidup: " + ClientPacketHandler.currentAliveCount + "/" + ClientPacketHandler.currentTotalPlayers;
        int aliveTextWidth = font.width(aliveText);
        int aliveX = screenWidth - aliveTextWidth - 12;

        // Background panel
        guiGraphics.fill(aliveX - 6, 4, screenWidth - 2, 20, 0xAA000000);
        guiGraphics.drawString(font, aliveText, aliveX, 8, 0xFF55FF55, true);

        // ==========================================
        // === BELOW ROLE: Status indicator ===
        // ==========================================
        if (!ClientPacketHandler.isPlayerAlive) {
            String statusText = "ELIMINATED";
            int statusWidth = font.width(statusText);
            guiGraphics.fill(roleBadgeX, roleBadgeY + 18, roleBadgeX + statusWidth + 12, roleBadgeY + 32, 0xCC440000);
            guiGraphics.drawString(font, statusText, roleBadgeX + 6, roleBadgeY + 22, 0xFFFF4444, true);
        }
    }

    private static String formatTime(int seconds) {
        if (seconds <= 0) return "00:00";
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
