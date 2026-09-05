package com.corazon.corazonmod.network;

import com.corazon.corazonmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> Client: Opens the Runner Selection Voting GUI screen for all alive players.
 */
public class OpenRunnerSelectionPacket {

    public static class RunnerCandidateEntry {
        public final UUID uuid;
        public final String name;

        public RunnerCandidateEntry(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUUID(uuid);
            buf.writeUtf(name);
        }

        public static RunnerCandidateEntry fromBytes(FriendlyByteBuf buf) {
            return new RunnerCandidateEntry(buf.readUUID(), buf.readUtf());
        }
    }

    public final List<RunnerCandidateEntry> candidates;

    public OpenRunnerSelectionPacket(List<RunnerCandidateEntry> candidates) {
        this.candidates = candidates;
    }

    public OpenRunnerSelectionPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        this.candidates = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.candidates.add(RunnerCandidateEntry.fromBytes(buf));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(candidates.size());
        for (RunnerCandidateEntry entry : candidates) {
            entry.toBytes(buf);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenRunnerSelection(this));
        });
        ctx.setPacketHandled(true);
    }
}
