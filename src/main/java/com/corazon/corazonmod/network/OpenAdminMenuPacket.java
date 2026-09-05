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
        public final UUID uuid;
        public final String name;
        public final String roleDisplayName;
        public final String forcedRoleName;
        public final boolean isAlive;
        public final boolean isRegistered;

        public AdminPlayerEntry(UUID uuid, String name, String roleDisplayName, String forcedRoleName, boolean isAlive, boolean isRegistered) {
            this.uuid = uuid;
            this.name = name;
            this.roleDisplayName = roleDisplayName;
            this.forcedRoleName = forcedRoleName;
            this.isAlive = isAlive;
            this.isRegistered = isRegistered;
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUUID(uuid);
            buf.writeUtf(name);
            buf.writeUtf(roleDisplayName);
            buf.writeUtf(forcedRoleName);
            buf.writeBoolean(isAlive);
            buf.writeBoolean(isRegistered);
        }

        public static AdminPlayerEntry fromBytes(FriendlyByteBuf buf) {
            return new AdminPlayerEntry(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readBoolean());
        }
    }

    public final boolean isGameRunning;
    public final String phaseName;
    public final int timeRemaining;
    public final int aliveCount;
    public final int totalCount;
    public final int minigameDuration;
    public final int mafiaCount;
    public final int doctorCount;
    public final int policeCount;
    public final boolean parkourGroupMode;
    public final List<AdminPlayerEntry> playerEntries;

    public OpenAdminMenuPacket(boolean isGameRunning, String phaseName, int timeRemaining, int aliveCount, int totalCount,
                                int minigameDuration, int mafiaCount, int doctorCount, int policeCount,
                                boolean parkourGroupMode, List<AdminPlayerEntry> playerEntries) {
        this.isGameRunning = isGameRunning;
        this.phaseName = phaseName;
        this.timeRemaining = timeRemaining;
        this.aliveCount = aliveCount;
        this.totalCount = totalCount;
        this.minigameDuration = minigameDuration;
        this.mafiaCount = mafiaCount;
        this.doctorCount = doctorCount;
        this.policeCount = policeCount;
        this.parkourGroupMode = parkourGroupMode;
        this.playerEntries = playerEntries;
    }

    public OpenAdminMenuPacket(FriendlyByteBuf buf) {
        this.isGameRunning = buf.readBoolean();
        this.phaseName = buf.readUtf();
        this.timeRemaining = buf.readInt();
        this.aliveCount = buf.readInt();
        this.totalCount = buf.readInt();
        this.minigameDuration = buf.readInt();
        this.mafiaCount = buf.readInt();
        this.doctorCount = buf.readInt();
        this.policeCount = buf.readInt();
        this.parkourGroupMode = buf.readBoolean();
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
        buf.writeInt(minigameDuration);
        buf.writeInt(mafiaCount);
        buf.writeInt(doctorCount);
        buf.writeInt(policeCount);
        buf.writeBoolean(parkourGroupMode);
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
                PlayerRole forced = game.getForcedRole(uuid);
                String forcedName = (forced != null) ? forced.name() : "AUTO";
                boolean alive = game.isAlive(uuid);
                boolean isReg = game.isRegistered(uuid);
                entries.add(new AdminPlayerEntry(uuid, p.getScoreboardName(), role.getDisplayName(), forcedName, alive, isReg));
            }
        }

        OpenAdminMenuPacket packet = new OpenAdminMenuPacket(
                game.isGameRunning(),
                game.getCurrentPhase().getDisplayName(),
                game.getTimeRemaining(),
                game.getAliveCount(),
                game.getTotalPlayers(),
                game.getCustomMinigameDuration(),
                game.getCustomMafiaCount(),
                game.getCustomDoctorCount(),
                game.getCustomPoliceCount(),
                game.isParkourGroupMode(),
                entries
        );

        ModMessages.sendToPlayer(packet, admin);
    }
}
