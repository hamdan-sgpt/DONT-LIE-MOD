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
 * building arenas, starting/stopping games, setting custom phase durations and role configurations via text input & buttons.
 */
public class AdminControlScreen extends Screen {

    private final OpenAdminMenuPacket packetData;
    private EditBox hidingDurationBox;
    private EditBox minigameDurationBox;
    private EditBox searchDurationBox;
    private EditBox discussionDurationBox;
    private EditBox votingDurationBox;

    private EditBox mafiaCountBox;
    private EditBox doctorCountBox;
    private EditBox policeCountBox;

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
        int leftColX = centerX - 195;
        int rightColX = centerX + 5;
        int btnWidth = 190;
        int btnHeight = 16;

        // --- Custom Phase Duration Inputs ---
        this.hidingDurationBox = new EditBox(this.font, leftColX + 115, 45, 65, 12, Component.literal("Hiding (s)"));
        this.hidingDurationBox.setFilter(s -> s.matches("\\d*"));
        this.hidingDurationBox.setValue("60");
        this.addRenderableWidget(this.hidingDurationBox);

        this.minigameDurationBox = new EditBox(this.font, leftColX + 115, 59, 65, 12, Component.literal("Minigame (s)"));
        this.minigameDurationBox.setFilter(s -> s.matches("\\d*"));
        this.minigameDurationBox.setValue(packetData.minigameDuration > 0 ? String.valueOf(packetData.minigameDuration) : "45");
        this.addRenderableWidget(this.minigameDurationBox);

        this.searchDurationBox = new EditBox(this.font, leftColX + 115, 73, 65, 12, Component.literal("Search (s)"));
        this.searchDurationBox.setFilter(s -> s.matches("\\d*"));
        this.searchDurationBox.setValue("300");
        this.addRenderableWidget(this.searchDurationBox);

        this.discussionDurationBox = new EditBox(this.font, leftColX + 115, 87, 65, 12, Component.literal("Discussion (s)"));
        this.discussionDurationBox.setFilter(s -> s.matches("\\d*"));
        this.discussionDurationBox.setValue("90");
        this.addRenderableWidget(this.discussionDurationBox);

        this.votingDurationBox = new EditBox(this.font, leftColX + 115, 101, 65, 12, Component.literal("Voting (s)"));
        this.votingDurationBox.setFilter(s -> s.matches("\\d*"));
        this.votingDurationBox.setValue("30");
        this.addRenderableWidget(this.votingDurationBox);

        // --- Custom Role Count Inputs ---
        this.mafiaCountBox = new EditBox(this.font, leftColX + 115, 124, 65, 12, Component.literal("Mafia Count"));
        this.mafiaCountBox.setValue(packetData.mafiaCount == -1 ? "Auto" : String.valueOf(packetData.mafiaCount));
        this.addRenderableWidget(this.mafiaCountBox);

        this.doctorCountBox = new EditBox(this.font, leftColX + 115, 138, 65, 12, Component.literal("Doctor Count"));
        this.doctorCountBox.setValue(packetData.doctorCount == -1 ? "Auto" : String.valueOf(packetData.doctorCount));
        this.addRenderableWidget(this.doctorCountBox);

        this.policeCountBox = new EditBox(this.font, leftColX + 115, 152, 65, 12, Component.literal("Police Count"));
        this.policeCountBox.setValue(packetData.policeCount == -1 ? "Auto" : String.valueOf(packetData.policeCount));
        this.addRenderableWidget(this.policeCountBox);

        // --- ADMIN ACTION BUTTONS ---
        int actionY = 180;

        // 1. START GAME Button
        this.addRenderableWidget(Button.builder(
                Component.literal("🚀 MULAI GAME (START)").withStyle(s -> s.withBold(true)),
                b -> {
                    int hidingSec = parseDurationOrDefault(this.hidingDurationBox.getValue(), 60);
                    int minigameSec = parseDurationOrDefault(this.minigameDurationBox.getValue(), 45);
                    int searchSec = parseDurationOrDefault(this.searchDurationBox.getValue(), 300);
                    int discussionSec = parseDurationOrDefault(this.discussionDurationBox.getValue(), 90);
                    int votingSec = parseDurationOrDefault(this.votingDurationBox.getValue(), 30);

                    int mafiaCount = parseRoleCount(this.mafiaCountBox.getValue(), -1);
                    int doctorCount = parseRoleCount(this.doctorCountBox.getValue(), 1);
                    int policeCount = parseRoleCount(this.policeCountBox.getValue(), 1);

                    ModMessages.sendToServer(new AdminActionPacket("START", hidingSec, minigameSec, searchSec, discussionSec, votingSec, mafiaCount, doctorCount, policeCount));
                    this.onClose();
                })
                .bounds(leftColX, actionY, btnWidth, btnHeight)
                .build()
        );

        // 2. STOP GAME Button
        this.addRenderableWidget(Button.builder(
                Component.literal("🛑 HENTIKAN GAME (STOP)").withStyle(s -> s.withBold(true)),
                b -> {
                    ModMessages.sendToServer(new AdminActionPacket("STOP"));
                    this.onClose();
                })
                .bounds(leftColX, actionY + 18, btnWidth, btnHeight)
                .build()
        );

        // 3. GENERATE ARENA & SKIP PHASE Buttons
        this.addRenderableWidget(Button.builder(
                Component.literal("🏗️ ARENA"),
                b -> {
                    ModMessages.sendToServer(new AdminActionPacket("BUILD_ARENA"));
                    this.onClose();
                })
                .bounds(leftColX, actionY + 36, btnWidth / 2 - 2, btnHeight)
                .build()
        );

        this.addRenderableWidget(Button.builder(
                Component.literal("⏩ SKIP FASE"),
                b -> ModMessages.sendToServer(new AdminActionPacket("SKIP_PHASE")))
                .bounds(leftColX + btnWidth / 2 + 2, actionY + 36, btnWidth / 2 - 2, btnHeight)
                .build()
        );

        // 4. ADD +30s / +60s TIME Buttons
        this.addRenderableWidget(Button.builder(
                Component.literal("⏱️ +30s"),
                b -> ModMessages.sendToServer(new AdminActionPacket("ADD_TIME_30")))
                .bounds(leftColX, actionY + 54, btnWidth / 2 - 2, btnHeight)
                .build()
        );

        this.addRenderableWidget(Button.builder(
                Component.literal("⏱️ +60s"),
                b -> ModMessages.sendToServer(new AdminActionPacket("ADD_TIME_60")))
                .bounds(leftColX + btnWidth / 2 + 2, actionY + 54, btnWidth / 2 - 2, btnHeight)
                .build()
        );

        // 5. CLEAR MANUAL ROLES & EXIT Buttons
        this.addRenderableWidget(Button.builder(
                Component.literal("🧹 RESET ROLE"),
                b -> ModMessages.sendToServer(new AdminActionPacket("CLEAR_FORCED_ROLES")))
                .bounds(leftColX, actionY + 72, btnWidth / 2 - 2, btnHeight)
                .build()
        );

        this.addRenderableWidget(Button.builder(
                Component.literal("❌ TUTUP"),
                b -> this.onClose())
                .bounds(leftColX + btnWidth / 2 + 2, actionY + 72, btnWidth / 2 - 2, btnHeight)
                .build()
        );

        // --- PLAYER ROSTER ROLE SELECTION BUTTONS (Right Column) ---
        List<OpenAdminMenuPacket.AdminPlayerEntry> players = packetData.playerEntries;
        int rosterTop = 88;
        for (int i = 0; i < Math.min(6, players.size()); i++) {
            OpenAdminMenuPacket.AdminPlayerEntry entry = players.get(i);
            int rowY = rosterTop + 18 + (i * 24);

            if (!packetData.isGameRunning) {
                String nextRole = getNextRoleName(entry.forcedRoleName);
                String buttonText = "🎭 " + entry.forcedRoleName;

                this.addRenderableWidget(Button.builder(
                        Component.literal(buttonText),
                        b -> ModMessages.sendToServer(new AdminActionPacket("SET_PLAYER_ROLE", entry.uuid.toString(), nextRole)))
                        .bounds(rightColX + 115, rowY, 70, 18)
                        .build()
                );
            }
        }
    }

    private String getNextRoleName(String current) {
        if (current == null) return "MAFIA";
        return switch (current.toUpperCase()) {
            case "AUTO" -> "MAFIA";
            case "MAFIA" -> "DOCTOR";
            case "DOCTOR" -> "POLICE";
            case "POLICE" -> "CITIZEN";
            default -> "AUTO";
        };
    }

    private int parseDurationOrDefault(String input, int fallback) {
        try {
            int val = Integer.parseInt(input.trim());
            return val > 0 ? val : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int parseRoleCount(String input, int fallback) {
        if (input == null || input.trim().isEmpty() || input.equalsIgnoreCase("auto")) {
            return -1;
        }
        try {
            int val = Integer.parseInt(input.trim());
            return val >= -1 ? val : fallback;
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
        guiGraphics.fill(centerX - 200, 4, centerX + 200, 36, 0xEE1A1A1A);
        guiGraphics.fill(centerX - 200, 36, centerX + 200, 38, 0xFFFFD700); // Gold accent border

        // Header Title
        guiGraphics.drawCenteredString(this.font, "👑 DON'T LIE - ADMIN CONTROL PANEL", centerX, 8, 0xFFFFD700);
        guiGraphics.drawCenteredString(this.font, "Kelola durasi, kuota role, dan setting role pemain secara langsung", centerX, 22, 0xFFAAAAAA);

        // --- Left Panel Boxes ---
        int leftColX = centerX - 195;
        int panelWidth = 190;

        // 1. Phase Durations Box
        guiGraphics.fill(leftColX, 40, leftColX + panelWidth, 118, 0xAA111111);
        guiGraphics.fill(leftColX, 40, leftColX + panelWidth, 41, 0x55FFFFFF);
        guiGraphics.drawString(this.font, "⏱️ DURASI FASE (DETIK)", leftColX + 6, 43, 0xFFFFD700);

        guiGraphics.drawString(this.font, "Hiding:", leftColX + 8, 47, 0xFFFFAA00);
        guiGraphics.drawString(this.font, "Minigame:", leftColX + 8, 61, 0xFFFF77FF);
        guiGraphics.drawString(this.font, "Search:", leftColX + 8, 75, 0xFFFFFF55);
        guiGraphics.drawString(this.font, "Diskusi:", leftColX + 8, 89, 0xFF55FFFF);
        guiGraphics.drawString(this.font, "Voting:", leftColX + 8, 103, 0xFFFF55FF);

        // 2. Role Counts Box
        guiGraphics.fill(leftColX, 116, leftColX + panelWidth, 172, 0xAA111111);
        guiGraphics.fill(leftColX, 116, leftColX + panelWidth, 117, 0x55FFFFFF);
        guiGraphics.drawString(this.font, "📊 JUMLAH ROLE (COUNT)", leftColX + 6, 120, 0xFFFFD700);

        guiGraphics.drawString(this.font, "Mafia:", leftColX + 8, 125, 0xFFFF5555);
        guiGraphics.drawString(this.font, "Doctor:", leftColX + 8, 141, 0xFF5555FF);
        guiGraphics.drawString(this.font, "Police:", leftColX + 8, 157, 0xFFFFAA00);

        // --- Right Panel Boxes ---
        int rightColX = centerX + 5;

        // 3. Status Card Box
        guiGraphics.fill(rightColX, 42, rightColX + panelWidth, 84, 0xAA111111);
        guiGraphics.fill(rightColX, 42, rightColX + panelWidth, 43, 0x55FFFFFF);

        String statusStr = packetData.isGameRunning ? "BERJALAN" : "TIDAK AKTIF";
        int statusColor = packetData.isGameRunning ? 0xFF55FF55 : 0xFFFF5555;
        guiGraphics.drawString(this.font, "Status: " + statusStr, rightColX + 8, 48, statusColor);
        guiGraphics.drawString(this.font, "Fase: " + packetData.phaseName, rightColX + 8, 60, 0xFFFFFF55);
        guiGraphics.drawString(this.font, "Sisa Waktu: " + packetData.timeRemaining + " s", rightColX + 8, 72, 0xFF55FFFF);

        // 4. Player Roster & Role Override Box
        guiGraphics.fill(rightColX, 88, rightColX + panelWidth, 270, 0xAA111111);
        guiGraphics.fill(rightColX, 88, rightColX + panelWidth, 89, 0x55FFFFFF);

        guiGraphics.drawString(this.font, "👥 ROLE PEMAIN (" + packetData.playerEntries.size() + ")", rightColX + 8, 93, 0xFFFFAA00);
        guiGraphics.fill(rightColX + 6, 103, rightColX + panelWidth - 6, 104, 0x55FFFFFF);

        // Render Player List Entries
        List<OpenAdminMenuPacket.AdminPlayerEntry> players = packetData.playerEntries;
        int pY = 108;
        for (int i = 0; i < Math.min(6, players.size()); i++) {
            OpenAdminMenuPacket.AdminPlayerEntry entry = players.get(i);
            int color;
            String statusText;
            if (packetData.isGameRunning) {
                color = entry.isAlive ? 0xFF55FF55 : 0xFFFF5555;
                statusText = entry.roleDisplayName + " " + (entry.isAlive ? "[HIDUP]" : "[ELIM]");
            } else {
                color = entry.forcedRoleName.equalsIgnoreCase("AUTO") ? 0xFFAAAAAA : 0xFFFFD700;
                statusText = entry.forcedRoleName.equalsIgnoreCase("AUTO") ? "Mode: AUTO" : "Set: " + entry.forcedRoleName;
            }
            guiGraphics.drawString(this.font, entry.name, rightColX + 8, pY, 0xFFFFFFFF);
            guiGraphics.drawString(this.font, statusText, rightColX + 8, pY + 10, color);
            pY += 24;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
