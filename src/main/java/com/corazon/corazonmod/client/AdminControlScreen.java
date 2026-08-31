package com.corazon.corazonmod.client;

import com.corazon.corazonmod.network.AdminActionPacket;
import com.corazon.corazonmod.network.ModMessages;
import com.corazon.corazonmod.network.OpenAdminMenuPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Admin Control Screen GUI — Full interactive dashboard for managing Don't Lie game matches,
 * building arenas, starting/stopping games, setting custom phase durations via text input, and monitoring status.
 */
public class AdminControlScreen extends Screen {

    private final OpenAdminMenuPacket packetData;
    private EditBox hidingDurationBox;
    private EditBox searchDurationBox;

    public AdminControlScreen(OpenAdminMenuPacket packetData) {
        super(Component.literal("Don't Lie Admin Control Panel"));
        this.packetData = packetData;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int leftColX = centerX - 185;
        int btnWidth = 170;
        int btnHeight = 20;
        int startY = 120;

        // Custom Phase Duration Inputs (Typed in seconds)
        this.hidingDurationBox = new EditBox(this.font, leftColX + 110, 58, 55, 16, Component.literal("Hiding Duration (s)"));
        this.hidingDurationBox.setFilter(s -> s.matches("\\d*"));
        this.hidingDurationBox.setValue("60");
        this.addRenderableWidget(this.hidingDurationBox);

        this.searchDurationBox = new EditBox(this.font, leftColX + 110, 78, 55, 16, Component.literal("Search Duration (s)"));
        this.searchDurationBox.setFilter(s -> s.matches("\\d*"));
        this.searchDurationBox.setValue("300");
        this.addRenderableWidget(this.searchDurationBox);

        // 1. START GAME Button (Reads typed durations)
        this.addRenderableWidget(Button.builder(
                Component.literal("🚀 MULAI GAME (START)").withStyle(s -> s.withBold(true)),
                b -> {
                    int hidingSec = parseDurationOrDefault(this.hidingDurationBox.getValue(), 60);
                    int searchSec = parseDurationOrDefault(this.searchDurationBox.getValue(), 300);
                    ModMessages.sendToServer(new AdminActionPacket("START", hidingSec, searchSec));
                    this.onClose();
                })
                .bounds(leftColX, startY, btnWidth, btnHeight)
                .build()
        );

        // 2. STOP GAME Button
        this.addRenderableWidget(Button.builder(
                Component.literal("🛑 HENTIKAN GAME (STOP)").withStyle(s -> s.withBold(true)),
                b -> {
                    ModMessages.sendToServer(new AdminActionPacket("STOP"));
                    this.onClose();
                })
                .bounds(leftColX, startY + 24, btnWidth, btnHeight)
                .build()
        );

        // 3. GENERATE ARENA Button
        this.addRenderableWidget(Button.builder(
                Component.literal("🏗️ GENERATE MAP ARENA").withStyle(s -> s.withBold(true)),
                b -> {
                    ModMessages.sendToServer(new AdminActionPacket("BUILD_ARENA"));
                    this.onClose();
                })
                .bounds(leftColX, startY + 48, btnWidth, btnHeight)
                .build()
        );

        // 4. SKIP HIDING PHASE Button
        this.addRenderableWidget(Button.builder(
                Component.literal("⏩ LEWATI HIDING PHASE"),
                b -> ModMessages.sendToServer(new AdminActionPacket("SKIP_HIDING")))
                .bounds(leftColX, startY + 72, btnWidth, btnHeight)
                .build()
        );

        // 5. ADD +30s / +60s TIME Buttons
        this.addRenderableWidget(Button.builder(
                Component.literal("⏱️ +30s"),
                b -> ModMessages.sendToServer(new AdminActionPacket("ADD_TIME_30")))
                .bounds(leftColX, startY + 96, btnWidth / 2 - 2, btnHeight)
                .build()
        );

        this.addRenderableWidget(Button.builder(
                Component.literal("⏱️ +60s"),
                b -> ModMessages.sendToServer(new AdminActionPacket("ADD_TIME_60")))
                .bounds(leftColX + btnWidth / 2 + 2, startY + 96, btnWidth / 2 - 2, btnHeight)
                .build()
        );

        // Close / Exit Button
        this.addRenderableWidget(Button.builder(
                Component.literal("❌ TUTUP DASHBOARD"),
                b -> this.onClose())
                .bounds(leftColX, startY + 120, btnWidth, 18)
                .build()
        );
    }

    private int parseDurationOrDefault(String input, int fallback) {
        try {
            int val = Integer.parseInt(input);
            return val > 0 ? val : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark background overlay
        guiGraphics.fill(0, 0, this.width, this.height, 0xDD000000);

        int centerX = this.width / 2;

        // Header Background Bar
        guiGraphics.fill(centerX - 200, 8, centerX + 200, 44, 0xEE1A1A1A);
        guiGraphics.fill(centerX - 200, 44, centerX + 200, 46, 0xFFFFD700); // Gold accent border

        // Header Title
        guiGraphics.drawCenteredString(this.font, "👑 DON'T LIE - ADMIN CONTROL PANEL", centerX, 14, 0xFFFFD700);
        guiGraphics.drawCenteredString(this.font, "Kelola durasi game, arena, dan status pemain", centerX, 28, 0xFFAAAAAA);

        // Settings Box (Left side upper)
        int settingsLeft = centerX - 185;
        int settingsTop = 52;
        int settingsWidth = 170;
        int settingsHeight = 46;
        guiGraphics.fill(settingsLeft, settingsTop, settingsLeft + settingsWidth, settingsTop + settingsHeight, 0xAA111111);
        guiGraphics.fill(settingsLeft, settingsTop, settingsLeft + settingsWidth, settingsTop + 1, 0x55FFFFFF);

        // Input Labels
        guiGraphics.drawString(this.font, "Hiding (detik):", settingsLeft + 6, settingsTop + 8, 0xFFFFAA00);
        guiGraphics.drawString(this.font, "Search (detik):", settingsLeft + 6, settingsTop + 28, 0xFFFFFF55);

        // Status Card Box (Right side upper)
        int statusLeft = centerX + 10;
        int statusTop = 52;
        int statusWidth = 175;
        int statusHeight = 46;
        guiGraphics.fill(statusLeft, statusTop, statusLeft + statusWidth, statusTop + statusHeight, 0xAA111111);
        guiGraphics.fill(statusLeft, statusTop, statusLeft + statusWidth, statusTop + 1, 0x55FFFFFF);

        // Render Game Status Info
        String statusStr = packetData.isGameRunning ? "BERJALAN" : "TIDAK AKTIF";
        int statusColor = packetData.isGameRunning ? 0xFF55FF55 : 0xFFFF5555;
        guiGraphics.drawString(this.font, "Status: " + statusStr, statusLeft + 8, statusTop + 6, statusColor);
        guiGraphics.drawString(this.font, "Fase: " + packetData.phaseName, statusLeft + 8, statusTop + 18, 0xFFFFFF55);
        guiGraphics.drawString(this.font, "Sisa Waktu: " + packetData.timeRemaining + " s", statusLeft + 8, statusTop + 30, 0xFF55FFFF);

        // Player Roster Box (Right side lower)
        int rosterLeft = centerX + 10;
        int rosterTop = 104;
        int rosterWidth = 175;
        int rosterHeight = 114;
        guiGraphics.fill(rosterLeft, rosterTop, rosterLeft + rosterWidth, rosterTop + rosterHeight, 0xAA111111);
        guiGraphics.fill(rosterLeft, rosterTop, rosterLeft + rosterWidth, rosterTop + 1, 0x55FFFFFF);

        // Render Player Roster Title
        guiGraphics.drawString(this.font, "👥 DAFTAR PEMAIN (" + packetData.playerEntries.size() + ")", rosterLeft + 8, rosterTop + 6, 0xFFFFAA00);
        guiGraphics.fill(rosterLeft + 8, rosterTop + 17, rosterLeft + rosterWidth - 8, rosterTop + 18, 0x55FFFFFF);

        // Render Player List Entries
        List<OpenAdminMenuPacket.AdminPlayerEntry> players = packetData.playerEntries;
        int pY = rosterTop + 22;
        for (int i = 0; i < Math.min(5, players.size()); i++) {
            OpenAdminMenuPacket.AdminPlayerEntry entry = players.get(i);
            int color = entry.isAlive ? 0xFF55FF55 : 0xFFFF5555;
            String statusText = entry.isAlive ? "[HIDUP]" : "[ELIM]";
            guiGraphics.drawString(this.font, entry.name, rosterLeft + 8, pY, 0xFFFFFFFF);
            guiGraphics.drawString(this.font, entry.roleDisplayName + " " + statusText, rosterLeft + 8, pY + 9, color);
            pY += 18;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
