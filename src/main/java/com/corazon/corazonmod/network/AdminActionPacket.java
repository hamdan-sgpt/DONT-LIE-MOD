package com.corazon.corazonmod.network;

import com.corazon.corazonmod.game.ArenaBuilder;
import com.corazon.corazonmod.game.DontLieGame;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: Sends admin action triggered from AdminControlScreen GUI.
 * Actions: "START", "STOP", "BUILD_ARENA", "ADD_TIME_30", "ADD_TIME_60", "REQUEST_OPEN_MENU"
 */
public class AdminActionPacket {
    private final String action;
    private final int hidingDurationSeconds;
    private final int searchDurationSeconds;

    public AdminActionPacket(String action) {
        this(action, 60, 300);
    }

    public AdminActionPacket(String action, int hidingDurationSeconds, int searchDurationSeconds) {
        this.action = action;
        this.hidingDurationSeconds = hidingDurationSeconds;
        this.searchDurationSeconds = searchDurationSeconds;
    }

    public AdminActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf();
        this.hidingDurationSeconds = buf.readInt();
        this.searchDurationSeconds = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(action);
        buf.writeInt(hidingDurationSeconds);
        buf.writeInt(searchDurationSeconds);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Permission check: player must be OP / permission level 2 or higher
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("❌ Kamu tidak memiliki izin Admin untuk fitur ini!").withStyle(net.minecraft.ChatFormatting.RED));
                return;
            }

            DontLieGame game = DontLieGame.getInstance();
            ServerLevel level = player.serverLevel();

            switch (action) {
                case "START" -> {
                    if (game.isGameRunning()) {
                        player.sendSystemMessage(Component.literal("⚠️ Game sudah sedang berjalan!").withStyle(net.minecraft.ChatFormatting.YELLOW));
                    } else {
                        game.setCustomDurations(hidingDurationSeconds, searchDurationSeconds);
                        game.startNewGame(player.getServer(), new java.util.ArrayList<>(player.getServer().getPlayerList().getPlayers()));
                    }
                }
                case "STOP" -> {
                    if (!game.isGameRunning()) {
                        player.sendSystemMessage(Component.literal("⚠️ Tidak ada game yang sedang berjalan.").withStyle(net.minecraft.ChatFormatting.YELLOW));
                    } else {
                        game.stopGame(player.getServer());
                    }
                }
                case "BUILD_ARENA" -> {
                    BlockPos pos = player.blockPosition();
                    player.sendSystemMessage(Component.literal("[Don't Lie] 🏗️ Membangun Arena Don't Lie di posisi kamu...").withStyle(net.minecraft.ChatFormatting.GOLD));
                    ArenaBuilder.buildArena(level, pos, player);
                }
                case "SKIP_HIDING" -> {
                    if (game.isGameRunning() && game.getCurrentPhase() == com.corazon.corazonmod.game.GamePhase.HIDING) {
                        player.sendSystemMessage(Component.literal("⏩ Hiding Phase dilewati oleh Admin!").withStyle(net.minecraft.ChatFormatting.GREEN));
                        game.setPhase(player.getServer(), com.corazon.corazonmod.game.GamePhase.SEARCH);
                    }
                }
                case "ADD_TIME_30" -> {
                    if (game.isGameRunning()) {
                        game.addTime(30);
                        player.sendSystemMessage(Component.literal("⏱️ Waktu ditambah +30 detik!").withStyle(net.minecraft.ChatFormatting.GREEN));
                    }
                }
                case "ADD_TIME_60" -> {
                    if (game.isGameRunning()) {
                        game.addTime(60);
                        player.sendSystemMessage(Component.literal("⏱️ Waktu ditambah +60 detik!").withStyle(net.minecraft.ChatFormatting.GREEN));
                    }
                }
                case "REQUEST_OPEN_MENU" -> {
                    OpenAdminMenuPacket.sendToAdmin(player);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
