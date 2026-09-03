package com.corazon.corazonmod.client;

import com.corazon.corazonmod.network.ModMessages;
import com.corazon.corazonmod.network.NightActionPacket;
import com.corazon.corazonmod.network.OpenVotingScreenPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

/**
 * Night Action Screen — Used by Mafia, Doctor, and Police during Night Phase.
 * Each role gets a themed action screen:
 * - MAFIA: Dark red "Choose your victim" 
 * - DOCTOR: Blue "Choose who to protect"
 * - POLICE: Gold "Choose who to investigate"
 */
public class NightActionScreen extends Screen {
    private final String actionType;
    private final List<OpenVotingScreenPacket.PlayerEntry> targetPlayers;
    private UUID selectedTarget = null;
    private int ticksOpen = 0;

    public NightActionScreen(String actionType, List<OpenVotingScreenPacket.PlayerEntry> targetPlayers) {
        super(Component.literal("Night Action"));
        this.actionType = actionType;
        this.targetPlayers = targetPlayers;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int cardWidth = 110;
        int cardHeight = 24;
        int columns = Math.min(3, targetPlayers.size());
        int rows = (int) Math.ceil((double) targetPlayers.size() / columns);

        int totalWidth = columns * (cardWidth + 8);
        int totalHeight = rows * (cardHeight + 6);
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - totalHeight) / 2 + 25;

        for (int i = 0; i < targetPlayers.size(); i++) {
            final OpenVotingScreenPacket.PlayerEntry entry = targetPlayers.get(i);
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

        // Confirm action button
        String confirmText = switch (actionType) {
            case "MAFIA_KILL" -> "EKSEKUSI!";
            case "DOCTOR_SAVE" -> "LINDUNGI!";
            case "POLICE_CHECK" -> "PERIKSA!";
            default -> "KONFIRMASI";
        };

        this.addRenderableWidget(Button.builder(
                Component.literal(confirmText).withStyle(style -> style.withBold(true)),
                button -> {
                    if (selectedTarget != null) {
                        ModMessages.sendToServer(new NightActionPacket(actionType, selectedTarget));
                        this.onClose();
                    }
                })
                .bounds(this.width / 2 - 60, startY + totalHeight + 16, 120, 24)
                .build()
        );
    }

    @Override
    public void tick() {
        super.tick();
        ticksOpen++;

        // Auto-close Night Screen if phase has changed or night phase ended
        if (!"Night Elimination".equalsIgnoreCase(ClientPacketHandler.currentPhaseName)) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float totalTicks = ticksOpen + partialTick;

        // Background color based on action type
        int bgColor = switch (actionType) {
            case "MAFIA_KILL" -> 0xDD1A0000;    // Dark red
            case "DOCTOR_SAVE" -> 0xDD000D1A;   // Dark blue
            case "POLICE_CHECK" -> 0xDD1A1500;  // Dark gold
            default -> 0xDD000000;
        };
        guiGraphics.fill(0, 0, this.width, this.height, bgColor);

        int centerX = this.width / 2;

        // Header styling based on role
        int headerColor;
        String titleText;
        String subtitleText;

        switch (actionType) {
            case "MAFIA_KILL" -> {
                headerColor = 0xFFFF4444;
                titleText = "MALAM HARI - MAFIA";
                subtitleText = "Pilih korban untuk dieksekusi malam ini";
            }
            case "DOCTOR_SAVE" -> {
                headerColor = 0xFF4488FF;
                titleText = "MALAM HARI - DOCTOR";
                subtitleText = "Pilih pemain yang ingin kamu lindungi";
            }
            case "POLICE_CHECK" -> {
                headerColor = 0xFFFFAA00;
                titleText = "MALAM HARI - POLICE";
                subtitleText = "Pilih pemain yang ingin kamu selidiki";
            }
            default -> {
                headerColor = 0xFFAAAAAA;
                titleText = "MALAM HARI";
                subtitleText = "Pilih target aksi malam";
            }
        }

        // Animated decorative lines
        float lineGrow = Math.min(1.0f, totalTicks / 20.0f);
        int lineWidth = (int)(lineGrow * 90);
        guiGraphics.fill(centerX - lineWidth, 18, centerX + lineWidth, 19, headerColor);

        // Pulsating moon icon
        float pulse = (float)(Math.sin(totalTicks * 0.08) * 0.2 + 0.8);
        int moonAlpha = (int)(pulse * 255);
        guiGraphics.drawCenteredString(this.font, titleText, centerX, 24, (moonAlpha << 24) | (headerColor & 0x00FFFFFF));
        guiGraphics.drawCenteredString(this.font, subtitleText, centerX, 38, 0xFFAAAAAA);

        guiGraphics.fill(centerX - lineWidth, 50, centerX + lineWidth, 51, headerColor);

        // Selected target indicator with glow
        if (selectedTarget != null) {
            String targetName = "";
            for (OpenVotingScreenPacket.PlayerEntry e : targetPlayers) {
                if (e.uuid.equals(selectedTarget)) {
                    targetName = e.name;
                    break;
                }
            }
            float glowPulse = (float)(Math.sin(totalTicks * 0.15) * 0.3 + 0.7);
            int glowAlpha = (int)(glowPulse * 255);
            guiGraphics.drawCenteredString(this.font, "Target: " + targetName, centerX, 56, (glowAlpha << 24) | (headerColor & 0x00FFFFFF));
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
