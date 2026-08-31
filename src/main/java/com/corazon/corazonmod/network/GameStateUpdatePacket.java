package com.corazon.corazonmod.network;

import com.corazon.corazonmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: Sync game state info for HUD overlay (phase, role, timer, alive count).
 */
public class GameStateUpdatePacket {
    private final String phaseName;
    private final int phaseColorRGB;
    private final String roleName;
    private final int roleColorRGB;
    private final int timeRemaining;
    private final int aliveCount;
    private final int totalPlayers;
    private final boolean isAlive;

    public GameStateUpdatePacket(String phaseName, int phaseColorRGB, String roleName, int roleColorRGB,
                                  int timeRemaining, int aliveCount, int totalPlayers, boolean isAlive) {
        this.phaseName = phaseName;
        this.phaseColorRGB = phaseColorRGB;
        this.roleName = roleName;
        this.roleColorRGB = roleColorRGB;
        this.timeRemaining = timeRemaining;
        this.aliveCount = aliveCount;
        this.totalPlayers = totalPlayers;
        this.isAlive = isAlive;
    }

    public GameStateUpdatePacket(FriendlyByteBuf buf) {
        this.phaseName = buf.readUtf(64);
        this.phaseColorRGB = buf.readInt();
        this.roleName = buf.readUtf(64);
        this.roleColorRGB = buf.readInt();
        this.timeRemaining = buf.readInt();
        this.aliveCount = buf.readInt();
        this.totalPlayers = buf.readInt();
        this.isAlive = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(phaseName, 64);
        buf.writeInt(phaseColorRGB);
        buf.writeUtf(roleName, 64);
        buf.writeInt(roleColorRGB);
        buf.writeInt(timeRemaining);
        buf.writeInt(aliveCount);
        buf.writeInt(totalPlayers);
        buf.writeBoolean(isAlive);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleGameStateUpdate(phaseName, phaseColorRGB, roleName, roleColorRGB,
                    timeRemaining, aliveCount, totalPlayers, isAlive);
        });
        return true;
    }
}
