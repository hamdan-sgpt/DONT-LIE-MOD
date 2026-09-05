package com.corazon.corazonmod.network;

import com.corazon.corazonmod.game.ArenaBuilder;
import com.corazon.corazonmod.game.DontLieGame;
import com.corazon.corazonmod.game.PlayerRole;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client -> Server: Sends admin action triggered from AdminControlScreen GUI.
 */
public class AdminActionPacket {
    private final String action;
    private final int hidingDurationSeconds;
    private final int minigameDurationSeconds;
    private final int searchDurationSeconds;
    private final int discussionDurationSeconds;
    private final int votingDurationSeconds;
    private final int mafiaCount;
    private final int doctorCount;
    private final int policeCount;
    private final String targetUUIDStr;
    private final String roleName;

    public AdminActionPacket(String action) {
        this(action, 60, 45, 300, 90, 30, -1, 1, 1, "", "");
    }

    public AdminActionPacket(String action, String targetUUIDStr, String roleName) {
        this(action, 60, 45, 300, 90, 30, -1, 1, 1, targetUUIDStr, roleName);
    }

    public AdminActionPacket(String action, int hidingDurationSeconds, int minigameDurationSeconds, int searchDurationSeconds, int discussionDurationSeconds, int votingDurationSeconds,
                            int mafiaCount, int doctorCount, int policeCount) {
        this(action, hidingDurationSeconds, minigameDurationSeconds, searchDurationSeconds, discussionDurationSeconds, votingDurationSeconds,
             mafiaCount, doctorCount, policeCount, "", "");
    }

    public AdminActionPacket(String action, int hidingDurationSeconds, int minigameDurationSeconds, int searchDurationSeconds, int discussionDurationSeconds, int votingDurationSeconds,
                            int mafiaCount, int doctorCount, int policeCount,
                            String targetUUIDStr, String roleName) {
        this.action = action;
        this.hidingDurationSeconds = hidingDurationSeconds;
        this.minigameDurationSeconds = minigameDurationSeconds;
        this.searchDurationSeconds = searchDurationSeconds;
        this.discussionDurationSeconds = discussionDurationSeconds;
        this.votingDurationSeconds = votingDurationSeconds;
        this.mafiaCount = mafiaCount;
        this.doctorCount = doctorCount;
        this.policeCount = policeCount;
        this.targetUUIDStr = targetUUIDStr != null ? targetUUIDStr : "";
        this.roleName = roleName != null ? roleName : "";
    }

    public AdminActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf();
        this.hidingDurationSeconds = buf.readInt();
        this.minigameDurationSeconds = buf.readInt();
        this.searchDurationSeconds = buf.readInt();
        this.discussionDurationSeconds = buf.readInt();
        this.votingDurationSeconds = buf.readInt();
        this.mafiaCount = buf.readInt();
        this.doctorCount = buf.readInt();
        this.policeCount = buf.readInt();
        this.targetUUIDStr = buf.readUtf();
        this.roleName = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(action);
        buf.writeInt(hidingDurationSeconds);
        buf.writeInt(minigameDurationSeconds);
        buf.writeInt(searchDurationSeconds);
        buf.writeInt(discussionDurationSeconds);
        buf.writeInt(votingDurationSeconds);
        buf.writeInt(mafiaCount);
        buf.writeInt(doctorCount);
        buf.writeInt(policeCount);
        buf.writeUtf(targetUUIDStr);
        buf.writeUtf(roleName);
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
            ServerLevel level = (ServerLevel) player.level();

            switch (action) {
                case "START" -> {
                    if (game.isGameRunning()) {
                        player.sendSystemMessage(Component.literal("⚠️ Game sudah sedang berjalan!").withStyle(net.minecraft.ChatFormatting.YELLOW));
                    } else {
                        game.setCustomDurations(hidingDurationSeconds, minigameDurationSeconds, searchDurationSeconds, discussionDurationSeconds, votingDurationSeconds);
                        game.setCustomRoleCounts(mafiaCount, doctorCount, policeCount);
                        game.startNewGame(player.getServer(), null);
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
                case "SKIP_PHASE", "SKIP_HIDING" -> {
                    if (game.isGameRunning()) {
                        game.skipCurrentPhase(player.getServer());
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
                case "SET_PLAYER_ROLE" -> {
                    try {
                        UUID targetUUID = UUID.fromString(targetUUIDStr);
                        if ("AUTO".equalsIgnoreCase(roleName) || "CLEAR".equalsIgnoreCase(roleName) || roleName.isEmpty()) {
                            game.setPlayerForcedRole(targetUUID, null);
                        } else {
                            PlayerRole role = PlayerRole.valueOf(roleName.toUpperCase());
                            game.setPlayerForcedRole(targetUUID, role);
                        }
                        OpenAdminMenuPacket.sendToAdmin(player);
                    } catch (Exception ignored) {}
                }
                case "CLEAR_FORCED_ROLES" -> {
                    game.clearForcedRoles();
                    OpenAdminMenuPacket.sendToAdmin(player);
                }
                case "TOGGLE_REGISTER" -> {
                    try {
                        UUID targetUUID = UUID.fromString(targetUUIDStr);
                        ServerPlayer target = player.getServer().getPlayerList().getPlayer(targetUUID);
                        if (target != null) {
                            if (game.isRegistered(targetUUID)) {
                                game.unregisterPlayer(target);
                            } else {
                                game.registerPlayer(target);
                            }
                        }
                        OpenAdminMenuPacket.sendToAdmin(player);
                    } catch (Exception ignored) {}
                }
                case "REGISTER_ALL" -> {
                    int added = 0;
                    for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
                        if (game.registerPlayer(p)) added++;
                    }
                    player.sendSystemMessage(Component.literal("[Don't Lie] ✅ Berhasil mendaftarkan " + added + " pemain online!").withStyle(net.minecraft.ChatFormatting.GREEN));
                    OpenAdminMenuPacket.sendToAdmin(player);
                }
                case "UNREGISTER_ALL" -> {
                    game.clearRegisteredPlayers(player.getServer());
                    OpenAdminMenuPacket.sendToAdmin(player);
                }
                case "TOGGLE_PARKOUR_MODE" -> {
                    game.toggleParkourGroupMode();
                    String modeName = game.isParkourGroupMode() ? "BARENG-BARENG (SEMUA PEMAIN)" : "PERWAKILAN (1 PEMAIN)";
                    player.sendSystemMessage(Component.literal("[Don't Lie] 🏃 Mode Parkour Minigame diubah ke: " + modeName).withStyle(net.minecraft.ChatFormatting.GOLD));
                    OpenAdminMenuPacket.sendToAdmin(player);
                }
                case "REQUEST_OPEN_MENU" -> {
                    OpenAdminMenuPacket.sendToAdmin(player);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
