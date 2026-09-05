package com.corazon.corazonmod.network;

import com.corazon.corazonmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client packet to open the Parkour Mode Voting Screen (BARENG-BARENG vs PERWAKILAN).
 */
public class OpenParkourModeVotePacket {

    public final int durationSeconds;

    public OpenParkourModeVotePacket(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public OpenParkourModeVotePacket(FriendlyByteBuf buf) {
        this.durationSeconds = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(durationSeconds);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenParkourModeVote(this));
        });
        ctx.setPacketHandled(true);
    }
}
