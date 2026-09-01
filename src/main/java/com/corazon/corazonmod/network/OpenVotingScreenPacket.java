package com.corazon.corazonmod.network;

import com.corazon.corazonmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> Client: Tells client to open the Voting GUI with list of alive players.
 */
public class OpenVotingScreenPacket {
    private final List<PlayerEntry> alivePlayers;

    public static class PlayerEntry {
        public final UUID uuid;
        public final String name;

        public PlayerEntry(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    public OpenVotingScreenPacket(List<PlayerEntry> alivePlayers) {
        this.alivePlayers = alivePlayers;
    }

    public OpenVotingScreenPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.alivePlayers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf(64);
            alivePlayers.add(new PlayerEntry(uuid, name));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(alivePlayers.size());
        for (PlayerEntry entry : alivePlayers) {
            buf.writeUUID(entry.uuid);
            buf.writeUtf(entry.name, 64);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleOpenVoting(alivePlayers);
        });
        return true;
    }

    public List<PlayerEntry> getAlivePlayers() { return alivePlayers; }
}