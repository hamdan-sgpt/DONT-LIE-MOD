package com.corazon.corazonmod.network;

import com.corazon.corazonmod.game.DontLieGame;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server packet to send player's chosen Parkour Mode ("BARENG_BARENG" or "PERWAKILAN").
 */
public class VoteParkourModePacket {

    private final String modeChoice;

    public VoteParkourModePacket(String modeChoice) {
        this.modeChoice = modeChoice;
    }

    public VoteParkourModePacket(FriendlyByteBuf buf) {
        this.modeChoice = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(modeChoice);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                DontLieGame.getInstance().voteParkourMode(sender, modeChoice);
            }
        });
        ctx.setPacketHandled(true);
    }
}
