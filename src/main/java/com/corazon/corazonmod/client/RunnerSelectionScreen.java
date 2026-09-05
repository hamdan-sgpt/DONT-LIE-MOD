package com.corazon.corazonmod.client;

import com.corazon.corazonmod.network.ModMessages;
import com.corazon.corazonmod.network.OpenRunnerSelectionPacket;
import com.corazon.corazonmod.network.VoteRunnerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

/**
 * Runner Selection Screen GUI — Pop-up screen displayed before Parkour Minigame.
 * All alive players vote for the representative player who will run the Parkour course.
 */
public class RunnerSelectionScreen extends Screen {

    private final List<OpenRunnerSelectionPacket.RunnerCandidateEntry> candidates;
    private UUID selectedTarget = null;
    private int ticksOpen = 0;

    public RunnerSelectionScreen(List<OpenRunnerSelectionPacket.RunnerCandidateEntry> candidates) {
        super(Component.literal("Voting Parkour Runner"));
        this.candidates = candidates;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int cardWidth = 120;
        int cardHeight = 26;
        int columns = Math.min(3, Math.max(1, candidates.size()));
        int rows = (int) Math.ceil((double) candidates.size() / columns);

        int totalWidth = columns * (cardWidth + 8);
        int totalHeight = rows * (cardHeight + 6);
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - totalHeight) / 2 + 10;

        for (int i = 0; i < candidates.size(); i++) {
            final OpenRunnerSelectionPacket.RunnerCandidateEntry entry = candidates.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (cardWidth + 8);
            int y = startY + row * (cardHeight + 6);

            this.addRenderableWidget(Button.builder(
                    Component.literal("🏃 " + entry.name),
                    button -> {
                        selectedTarget = entry.uuid;
                    })
                    .bounds(x, y, cardWidth, cardHeight)
                    .build()
            );
        }

        int btnY = startY + totalHeight + 12;

        // Submit Vote button
        this.addRenderableWidget(Button.builder(
                Component.literal("✅ VOTE RUNNER!").withStyle(style -> style.withBold(true)),
                button -> {
                    if (selectedTarget != null) {
                        ModMessages.sendToServer(new VoteRunnerPacket(selectedTarget));
                        this.onClose();
                    }
                })
                .bounds(this.width / 2 - 80, btnY, 160, 22)
                .build()
        );
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;

        // Auto-close if phase changes from Runner Selection
        if (ticksOpen > 10 && !"Voting Runner".equalsIgnoreCase(ClientPacketHandler.currentPhaseName)) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Dark translucent overlay
        guiGraphics.fill(0, 0, this.width, this.height, 0xEE000000);

        int centerX = this.width / 2;

        // Header Background Box
        guiGraphics.fill(centerX - 180, 15, centerX + 180, 55, 0xEE1E1E1E);
        guiGraphics.fill(centerX - 180, 55, centerX + 180, 57, 0xFFFFD700); // Gold accent line

        // Title and Subtitle
        guiGraphics.drawCenteredString(this.font, "🏃 VOTING PARKOUR RUNNER KELOMPOK", centerX, 20, 0xFFFF77FF);
        guiGraphics.drawCenteredString(this.font, "Pilih 1 pemain yang menurutmu paling jago parkour!", centerX, 36, 0xFFFFFF55);

        // Highlight selected target
        if (selectedTarget != null) {
            String selectedName = "";
            for (OpenRunnerSelectionPacket.RunnerCandidateEntry e : candidates) {
                if (e.uuid.equals(selectedTarget)) {
                    selectedName = e.name;
                    break;
                }
            }
            guiGraphics.drawCenteredString(this.font, "Pilihan Kamu: " + selectedName, centerX, this.height - 30, 0xFF55FF55);
        } else {
            guiGraphics.drawCenteredString(this.font, "Klik nama pemain di atas untuk memilih", centerX, this.height - 30, 0xFFAAAAAA);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
