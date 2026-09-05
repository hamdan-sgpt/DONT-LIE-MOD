package com.corazon.corazonmod.network;

import com.corazon.corazonmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> Client: Tells client to open Night Action GUI for Mafia/Doctor/Police.
 */
public class OpenNightActionPacket {
    private final String actionType; // "MAFIA_KILL", "DOCTOR_SAVE", "POLICE_CHECK"
    private final List<OpenVotingScreenPacket.PlayerEntry> targetPlayers;

    public OpenNightActionPacket(String actionType, List<OpenVotingScreenPacket.PlayerEntry> targetPlayers) {
        this.actionType = actionType;
        this.targetPlayers = targetPlayers;
    }

    public OpenNightActionPacket(FriendlyByteBuf buf) {
        this.actionType = buf.readUtf(32);
        int count = buf.readInt();
        this.targetPlayers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf(64);
            targetPlayers.add(new OpenVotingScreenPacket.PlayerEntry(uuid, name));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(actionType, 32);
        buf.writeInt(targetPlayers.size());
        for (OpenVotingScreenPacket.PlayerEntry entry : targetPlayers) {
            buf.writeUUID(entry.uuid);
            buf.writeUtf(entry.name, 64);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleOpenNightAction(actionType, targetPlayers);
        });
        ctx.setPacketHandled(true);
        return true;
    }

    public String getActionType() { return actionType; }
    public List<OpenVotingScreenPacket.PlayerEntry> getTargetPlayers() { return targetPlayers; }
}
