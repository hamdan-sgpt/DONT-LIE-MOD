package com.corazon.corazonmod.network;

import com.corazon.corazonmod.game.DontLieGame;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client -> Server: Sends player's vote choice for Parkour Runner.
 */
public class VoteRunnerPacket {
    private final UUID targetUUID;

    public VoteRunnerPacket(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public VoteRunnerPacket(FriendlyByteBuf buf) {
        this.targetUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(targetUUID);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer voter = ctx.getSender();
            if (voter != null) {
                DontLieGame.getInstance().voteRunner(voter, targetUUID);
            }
        });
        ctx.setPacketHandled(true);
    }
}
