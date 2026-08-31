package com.corazon.corazonmod.network;

import com.corazon.corazonmod.game.DontLieGame;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client -> Server: Submit a vote for a target player.
 */
public class VotePlayerPacket {
    private final UUID targetUUID;

    public VotePlayerPacket(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public VotePlayerPacket(FriendlyByteBuf buf) {
        this.targetUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(targetUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                DontLieGame.getInstance().votePlayer(sender, targetUUID);
            }
        });
        return true;
    }
}
