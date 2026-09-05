package com.corazon.corazonmod.client;

import com.corazon.corazonmod.network.ModMessages;
import com.corazon.corazonmod.network.VoteParkourModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Interactive Voting Screen GUI for players to choose the Parkour Minigame Mode:
 * Option 1: 👥 BARENG-BARENG (Group Parkour - All players play together)
 * Option 2: 👤 PERWAKILAN (1 Runner represents the group)
 */
public class ParkourModeVoteScreen extends Screen {

    private final int durationSeconds;
    private String selectedMode = "BARENG_BARENG"; // Default choice
    private Button confirmButton;

    public ParkourModeVoteScreen(int durationSeconds) {
        super(Component.literal("Voting Mode Parkour"));
        this.durationSeconds = durationSeconds;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Card X coordinates
        int cardWidth = 140;
        int cardHeight = 85;
        int cardY = centerY - 30;

        int leftCardX = centerX - 150;
        int rightCardX = centerX + 10;

        // 1. Button Select BARENG-BARENG
        this.addRenderableWidget(Button.builder(
                Component.literal("👥 PILIH BARENG-BARENG"),
                b -> {
                    this.selectedMode = "BARENG_BARENG";
                    updateButtonState();
                })
                .bounds(leftCardX + 5, cardY + 60, cardWidth - 10, 20)
                .build()
        );

        // 2. Button Select PERWAKILAN
        this.addRenderableWidget(Button.builder(
                Component.literal("👤 PILIH PERWAKILAN"),
                b -> {
                    this.selectedMode = "PERWAKILAN";
                    updateButtonState();
                })
                .bounds(rightCardX + 5, cardY + 60, cardWidth - 10, 20)
                .build()
        );

        // 3. Confirm Vote Button
        this.confirmButton = Button.builder(
                Component.literal("✅ VOTE MODE PARKOUR!").withStyle(s -> s.withBold(true)),
                b -> submitVote())
                .bounds(centerX - 90, cardY + 98, 180, 22)
                .build();
        this.addRenderableWidget(this.confirmButton);
    }

    private void updateButtonState() {
        // Keeps confirm button enabled
        if (confirmButton != null) {
            confirmButton.active = true;
        }
    }

    private void submitVote() {
        ModMessages.sendToServer(new VoteParkourModePacket(selectedMode));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark translucent overlay background
        guiGraphics.fill(0, 0, this.width, this.height, 0xEE09090D);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Outer Container Box
        int boxWidth = 330;
        int boxHeight = 210;
        int boxLeft = centerX - boxWidth / 2;
        int boxTop = centerY - 105;

        guiGraphics.fill(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, 0xEE14141E);
        guiGraphics.fill(boxLeft, boxTop, boxLeft + boxWidth, boxTop + 2, 0xFFFFD700); // Top Accent Line

        // Header Title
        guiGraphics.drawCenteredString(this.font, "🏃 VOTING MODE MINIGAME PARKOUR", centerX, boxTop + 10, 0xFFFFD700);
        guiGraphics.drawCenteredString(this.font, "Pilih bagaimana minigame parkour ronde ini dimainkan:", centerX, boxTop + 24, 0xFFAAAAAA);

        // Cards layout
        int cardY = centerY - 30;
        int cardWidth = 140;
        int cardHeight = 85;
        int leftCardX = centerX - 150;
        int rightCardX = centerX + 10;

        // --- Card 1: BARENG-BARENG ---
        boolean isBarengSelected = "BARENG_BARENG".equals(selectedMode);
        int leftBgColor = isBarengSelected ? 0xFF1E3A20 : 0xAA22222B;
        int leftBorderColor = isBarengSelected ? 0xFF55FF55 : 0x55FFFFFF;

        guiGraphics.fill(leftCardX, cardY, leftCardX + cardWidth, cardY + cardHeight, leftBgColor);
        guiGraphics.fill(leftCardX, cardY, leftCardX + cardWidth, cardY + 1, leftBorderColor);
        guiGraphics.fill(leftCardX, cardY, leftCardX + 1, cardY + cardHeight, leftBorderColor);
        guiGraphics.fill(leftCardX + cardWidth - 1, cardY, leftCardX + cardWidth, cardY + cardHeight, leftBorderColor);
        guiGraphics.fill(leftCardX, cardY + cardHeight - 1, leftCardX + cardWidth, cardY + cardHeight, leftBorderColor);

        guiGraphics.drawCenteredString(this.font, "👥 BARENG-BARENG", leftCardX + cardWidth / 2, cardY + 8, isBarengSelected ? 0xFF55FF55 : 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Semua pemain hidup", leftCardX + cardWidth / 2, cardY + 24, 0xFFAAAAAA);
        guiGraphics.drawCenteredString(this.font, "bermain parkour", leftCardX + cardWidth / 2, cardY + 34, 0xFFAAAAAA);
        guiGraphics.drawCenteredString(this.font, "bersama-sama!", leftCardX + cardWidth / 2, cardY + 44, 0xFFAAAAAA);

        // --- Card 2: PERWAKILAN ---
        boolean isPerwakilanSelected = "PERWAKILAN".equals(selectedMode);
        int rightBgColor = isPerwakilanSelected ? 0xFF3A2A1E : 0xAA22222B;
        int rightBorderColor = isPerwakilanSelected ? 0xFFFFAA00 : 0x55FFFFFF;

        guiGraphics.fill(rightCardX, cardY, rightCardX + cardWidth, cardY + cardHeight, rightBgColor);
        guiGraphics.fill(rightCardX, cardY, rightCardX + cardWidth, cardY + 1, rightBorderColor);
        guiGraphics.fill(rightCardX, cardY, rightCardX + 1, cardY + cardHeight, rightBorderColor);
        guiGraphics.fill(rightCardX + cardWidth - 1, cardY, rightCardX + cardWidth, cardY + cardHeight, rightBorderColor);
        guiGraphics.fill(rightCardX, cardY + cardHeight - 1, rightCardX + cardWidth, cardY + cardHeight, rightBorderColor);

        guiGraphics.drawCenteredString(this.font, "👤 PERWAKILAN", rightCardX + cardWidth / 2, cardY + 8, isPerwakilanSelected ? 0xFFFFAA00 : 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Pilih 1 pemain", rightCardX + cardWidth / 2, cardY + 24, 0xFFAAAAAA);
        guiGraphics.drawCenteredString(this.font, "sebagai Runner mewakili", rightCardX + cardWidth / 2, cardY + 34, 0xFFAAAAAA);
        guiGraphics.drawCenteredString(this.font, "seluruh kelompok!", rightCardX + cardWidth / 2, cardY + 44, 0xFFAAAAAA);

        // Selection feedback
        String selectedName = isBarengSelected ? "👥 BARENG-BARENG" : "👤 PERWAKILAN (1 Runner)";
        guiGraphics.drawCenteredString(this.font, "Pilihanmu: " + selectedName, centerX, cardY + 87, 0xFF55FFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
