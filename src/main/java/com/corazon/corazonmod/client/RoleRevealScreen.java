package com.corazon.corazonmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dramatic cinematic Role Reveal Screen.
 * Shows your secret role with a dramatic fade-in animation, color-coded role name,
 * and a pulsating glow effect — like opening a secret envelope in Going Seventeen.
 */
public class RoleRevealScreen extends Screen {
    private final String roleName;
    private final String roleDescription;
    private final int roleColor;
    private int ticksOpen = 0;

    private static final int FADE_IN_TICKS = 30;       // 1.5 seconds fade in
    private static final int REVEAL_DELAY_TICKS = 40;   // 2 seconds before role shows
    private static final int FULL_REVEAL_TICKS = 60;    // 3 seconds for full text
    private static final int AUTO_CLOSE_TICKS = 160;    // 8 seconds total

    public RoleRevealScreen(String roleName, String roleDescription, int roleColor) {
        super(Component.literal("Role Reveal"));
        this.roleName = roleName;
        this.roleDescription = roleDescription;
        this.roleColor = roleColor;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;
        if (ticksOpen >= AUTO_CLOSE_TICKS) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float totalTicks = ticksOpen + partialTick;

        // === BACKGROUND: Dark fade-in overlay ===
        float bgAlpha = Math.min(1.0f, totalTicks / FADE_IN_TICKS);
        int bgAlphaInt = (int)(bgAlpha * 200);
        guiGraphics.fill(0, 0, this.width, this.height, (bgAlphaInt << 24));

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // === PHASE 1: "GOING SEVENTEEN" title with gold shimmer ===
        if (totalTicks > 10) {
            float titleAlpha = Math.min(1.0f, (totalTicks - 10) / 20.0f);
            int titleAlphaInt = (int)(titleAlpha * 255);

            // Decorative top line
            int lineWidth = (int)(Math.min(1.0f, (totalTicks - 10) / 30.0f) * 120);
            guiGraphics.fill(centerX - lineWidth, centerY - 55, centerX + lineWidth, centerY - 54, (titleAlphaInt << 24) | 0xFFD700);

            // Title text
            String titleText = "GOING SEVENTEEN";
            int titleColor = (titleAlphaInt << 24) | 0xFFD700;
            guiGraphics.drawCenteredString(this.font, titleText, centerX, centerY - 48, titleColor);

            // Subtitle
            String subtitleText = "DON'T LIE";
            int subtitleColor = (titleAlphaInt << 24) | 0xFF6B6B;
            guiGraphics.drawCenteredString(this.font, subtitleText, centerX, centerY - 36, subtitleColor);

            // Decorative bottom line under subtitle
            guiGraphics.fill(centerX - lineWidth, centerY - 28, centerX + lineWidth, centerY - 27, (titleAlphaInt << 24) | 0xFFD700);
        }

        // === PHASE 2: "Your Role Is..." teaser text ===
        if (totalTicks > REVEAL_DELAY_TICKS) {
            float teaserAlpha = Math.min(1.0f, (totalTicks - REVEAL_DELAY_TICKS) / 15.0f);
            int teaserAlphaInt = (int)(teaserAlpha * 255);
            int teaserColor = (teaserAlphaInt << 24) | 0xAAAAAA;
            guiGraphics.drawCenteredString(this.font, "Peran rahasia kamu adalah...", centerX, centerY - 12, teaserColor);
        }

        // === PHASE 3: ROLE NAME with dramatic reveal ===
        if (totalTicks > FULL_REVEAL_TICKS) {
            float roleAlpha = Math.min(1.0f, (totalTicks - FULL_REVEAL_TICKS) / 20.0f);
            int roleAlphaInt = (int)(roleAlpha * 255);

            // Pulsating glow effect behind role name
            float pulse = (float)(Math.sin(totalTicks * 0.1) * 0.3 + 0.7);
            int glowAlpha = (int)(roleAlphaInt * pulse * 0.3f);
            int glowColor = (glowAlpha << 24) | (roleColor & 0x00FFFFFF);

            // Glow rectangle behind role name
            int glowPad = 30 + (int)(pulse * 5);
            guiGraphics.fill(centerX - glowPad - 20, centerY + 2, centerX + glowPad + 20, centerY + 22, glowColor);

            // Role name (large, bold, colored)
            int roleTextColor = (roleAlphaInt << 24) | (roleColor & 0x00FFFFFF);

            // Draw role name with shadow for emphasis (draw multiple times for "bold" effect)
            String roleDisplay = "★ " + roleName.toUpperCase() + " ★";
            guiGraphics.drawCenteredString(this.font, roleDisplay, centerX, centerY + 8, roleTextColor);
            guiGraphics.drawCenteredString(this.font, roleDisplay, centerX + 1, centerY + 8, roleTextColor);

            // Role description below
            if (totalTicks > FULL_REVEAL_TICKS + 20) {
                float descAlpha = Math.min(1.0f, (totalTicks - FULL_REVEAL_TICKS - 20) / 15.0f);
                int descAlphaInt = (int)(descAlpha * 255);
                int descColor = (descAlphaInt << 24) | 0xCCCCCC;
                guiGraphics.drawCenteredString(this.font, roleDescription, centerX, centerY + 28, descColor);
            }

            // Role-specific emoji/icon hint
            if (totalTicks > FULL_REVEAL_TICKS + 30) {
                float hintAlpha = Math.min(1.0f, (totalTicks - FULL_REVEAL_TICKS - 30) / 15.0f);
                int hintAlphaInt = (int)(hintAlpha * 255);
                String hint = getRoleHint();
                int hintColor = (hintAlphaInt << 24) | 0x888888;
                guiGraphics.drawCenteredString(this.font, hint, centerX, centerY + 46, hintColor);
            }
        }

        // === Bottom decorative bar ===
        if (totalTicks > 20) {
            float barAlpha = Math.min(1.0f, (totalTicks - 20) / 30.0f);
            int barAlphaInt = (int)(barAlpha * 180);
            int barWidth = (int)(Math.min(1.0f, (totalTicks - 20) / 40.0f) * 100);
            guiGraphics.fill(centerX - barWidth, centerY + 62, centerX + barWidth, centerY + 63, (barAlphaInt << 24) | 0xFFD700);

            // "Press ESC to close" hint
            if (totalTicks > AUTO_CLOSE_TICKS - 60) {
                guiGraphics.drawCenteredString(this.font, "Tekan ESC untuk menutup", centerX, centerY + 70, 0x55FFFFFF);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private String getRoleHint() {
        return switch (roleName.toUpperCase()) {
            case "MAFIA" -> "[Sembunyikan uang & eliminasi warga di malam hari]";
            case "DOCTOR" -> "[Pilih 1 pemain untuk diselamatkan setiap malam]";
            case "POLICE" -> "[Periksa identitas 1 pemain setiap malam]";
            default -> "[Cari uang & voting untuk menemukan Mafia!]";
        };
    }
}
