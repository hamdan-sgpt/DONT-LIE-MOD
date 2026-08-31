package com.corazon.corazonmod.network;

import com.corazon.corazonmod.game.DontLieGame;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client -> Server: Submit a night action (Mafia kill / Doctor save / Police check).
 */
public class NightActionPacket {
    private final String actionType; // "MAFIA_KILL", "DOCTOR_SAVE", "POLICE_CHECK"
    private final UUID targetUUID;

    public NightActionPacket(String actionType, UUID targetUUID) {
        this.actionType = actionType;
        this.targetUUID = targetUUID;
    }

    public NightActionPacket(FriendlyByteBuf buf) {
        this.actionType = buf.readUtf(32);
        this.targetUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(actionType, 32);
        buf.writeUUID(targetUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            ServerPlayer target = sender.server.getPlayerList().getPlayer(targetUUID);
            if (target == null) return;

            switch (actionType) {
                case "MAFIA_KILL" -> DontLieGame.getInstance().setMafiaTarget(sender, target);
                case "DOCTOR_SAVE" -> DontLieGame.getInstance().setDoctorTarget(sender, target);
                case "POLICE_CHECK" -> DontLieGame.getInstance().policeInspect(sender, target);
            }
        });
        return true;
    }
}
