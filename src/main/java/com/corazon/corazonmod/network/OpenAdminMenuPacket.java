package com.corazon.corazonmod.network;

import com.corazon.corazonmod.client.ClientPacketHandler;
import com.corazon.corazonmod.game.DontLieGame;
import com.corazon.corazonmod.game.PlayerRole;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> Client: Sends current server game state and player info to open AdminControlScreen.
 */
public class OpenAdminMenuPacket {

    public static class AdminPlayerEntry {
        public final String name;
        public final String roleDisplayName;
        public final boolean isAlive;

        public AdminPlayerEntry(String name, String roleDisplayName, boolean isAlive) {
            this.name = name;
            this.roleDisplayName = roleDisplayName;
            this.isAlive = isAlive;
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeUtf(roleDisplayName);
            buf.writeBoolean(isAlive);
        }

        public static AdminPlayerEntry fromBytes(FriendlyByteBuf buf) {
            return new AdminPlayerEntry(buf.readUtf(), buf.readUtf(), buf.readBoolean());
        }
    }

    public final boolean isGameRunning;
    public final String phaseName;
    public final int timeRemaining;
    public final int aliveCount;
    public final int totalCount;
    public final List<AdminPlayerEntry> playerEntries;

    public OpenAdminMenuPacket(boolean isGameRunning, String phaseName, int timeRemaining, int aliveCount, int totalCount, List<AdminPlayerEntry> playerEntries) {
        this.isGameRunning = isGameRunning;
        this.phaseName = phaseName;
        this.timeRemaining = timeRemaining;
        this.aliveCount = aliveCount;
        this.totalCount = totalCount;
        this.playerEntries = playerEntries;
    }

    public OpenAdminMenuPacket(FriendlyByteBuf buf) {
        this.isGameRunning = buf.readBoolean();
        this.phaseName = buf.readUtf();
        this.timeRemaining = buf.readInt();
        this.aliveCount = buf.readInt();
        this.totalCount = buf.readInt();
        int size = buf.readInt();
        this.playerEntries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.playerEntries.add(AdminPlayerEntry.fromBytes(buf));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isGameRunning);
        buf.writeUtf(phaseName);
        buf.writeInt(timeRemaining);
        buf.writeInt(aliveCount);
        buf.writeInt(totalCount);
        buf.writeInt(playerEntries.size());
        for (AdminPlayerEntry entry : playerEntries) {
            entry.toBytes(buf);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenAdminMenu(this));
        });
        ctx.setPacketHandled(true);
    }

    public static void sendToAdmin(ServerPlayer admin) {
        DontLieGame game = DontLieGame.getInstance();
        List<AdminPlayerEntry> entries = new ArrayList<>();

        if (admin.getServer() != null) {
            for (ServerPlayer p : admin.getServer().getPlayerList().getPlayers()) {
                UUID uuid = p.getUUID();
                PlayerRole role = game.getRole(uuid);
                boolean alive = game.isAlive(uuid);
                entries.add(new AdminPlayerEntry(p.getScoreboardName(), role.getDisplayName(), alive));
            }
        }

        OpenAdminMenuPacket packet = new OpenAdminMenuPacket(
                game.isGameRunning(),
                game.getCurrentPhase().getDisplayName(),
                game.getTimeRemaining(),
                game.getAliveCount(),
                game.getTotalPlayers(),
                entries
        );

        ModMessages.sendToPlayer(packet, admin);
    }
}
