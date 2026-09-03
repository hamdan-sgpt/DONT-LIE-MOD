package com.corazon.corazonmod.client;

import com.corazon.corazonmod.network.ModMessages;
import com.corazon.corazonmod.network.OpenVotingScreenPacket;
import com.corazon.corazonmod.network.VotePlayerPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/**
 * Voting Screen GUI — displays alive players as clickable cards.
 * Players click on the person they suspect is Mafia.
 * Styled with dark theme and red accents like a crime board.
 */
public class VotingScreen extends Screen {
    public static final UUID ABSTAIN_UUID = new UUID(0L, 0L);
    private final List<OpenVotingScreenPacket.PlayerEntry> alivePlayers;
    private UUID selectedTarget = null;
    private int ticksOpen = 0;

    public VotingScreen(List<OpenVotingScreenPacket.PlayerEntry> alivePlayers) {
        super(Component.literal("Voting Phase"));
        this.alivePlayers = alivePlayers;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int cardWidth = 110;
        int cardHeight = 26;
        int columns = Math.min(3, alivePlayers.size());
        int rows = (int) Math.ceil((double) alivePlayers.size() / columns);

        int totalWidth = columns * (cardWidth + 8);
        int totalHeight = rows * (cardHeight + 6);
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - totalHeight) / 2 + 10;

        for (int i = 0; i < alivePlayers.size(); i++) {
            final OpenVotingScreenPacket.PlayerEntry entry = alivePlayers.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (cardWidth + 8);
            int y = startY + row * (cardHeight + 6);

            this.addRenderableWidget(Button.builder(
                    Component.literal(entry.name),
                    button -> {
                        selectedTarget = entry.uuid;
                    })
                    .bounds(x, y, cardWidth, cardHeight)
                    .build()
            );
        }

        int btnY = startY + totalHeight + 10;

        // Skip Vote (Golput/Abstain) button
        this.addRenderableWidget(Button.builder(
                Component.literal("⏩ SKIP VOTE (Golput)").withStyle(style -> style.withColor(0xFFFFFF55)),
                button -> {
                    selectedTarget = ABSTAIN_UUID;
                })
                .bounds(this.width / 2 - 110, btnY, 105, 22)
                .build()
        );

        // Submit Vote button
        this.addRenderableWidget(Button.builder(
                Component.literal("✅ VOTE!").withStyle(style -> style.withBold(true)),
                button -> {
                    if (selectedTarget != null) {
                        ModMessages.sendToServer(new VotePlayerPacket(selectedTarget));
                        this.onClose();
                    }
                })
                .bounds(this.width / 2 + 5, btnY, 105, 22)
                .build()
        );
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;

        // Auto-close Voting Screen if phase has changed or voting time ended
        if (!"Voting Phase".equalsIgnoreCase(ClientPacketHandler.currentPhaseName)) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent dark background
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC000000);

        int centerX = this.width / 2;

        // === Header ===
        guiGraphics.fill(centerX - 120, 12, centerX + 120, 13, 0xFFFF4444);
        guiGraphics.drawCenteredString(this.font, "VOTING PHASE", centerX, 18, 0xFFFF4444);
        guiGraphics.drawCenteredString(this.font, "Siapa yang kamu curigai sebagai MAFIA?", centerX, 30, 0xFFAAAAAA);
        guiGraphics.fill(centerX - 120, 42, centerX + 120, 43, 0xFFFF4444);

        // === Selected target indicator ===
        if (selectedTarget != null) {
            String targetName = "SKIP VOTE (Golput)";
            if (!ABSTAIN_UUID.equals(selectedTarget)) {
                for (OpenVotingScreenPacket.PlayerEntry e : alivePlayers) {
                    if (e.uuid.equals(selectedTarget)) {
                        targetName = e.name;
                        break;
                    }
                }
            }
            float pulse = (float)(Math.sin((ticksOpen + partialTick) * 0.15) * 0.3 + 0.7);
            int alpha = (int)(pulse * 255);
            int color = ABSTAIN_UUID.equals(selectedTarget) ? ((alpha << 24) | 0xFFFF55) : ((alpha << 24) | 0xFF6B6B);
            guiGraphics.drawCenteredString(this.font, "Pilihan Kamu: " + targetName, centerX, 46, color);
        }

        // === Player count info ===
        guiGraphics.drawCenteredString(this.font, "Pemain Hidup: " + alivePlayers.size(), centerX, this.height - 18, 0xFF888888);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
