package com.corazon.corazonmod.network;

import com.corazon.corazonmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: Tells client to open the Role Reveal GUI screen.
 */
public class OpenRoleRevealPacket {
    private final String roleName;
    private final String roleDescription;
    private final int roleColorRGB;

    public OpenRoleRevealPacket(String roleName, String roleDescription, int roleColorRGB) {
        this.roleName = roleName;
        this.roleDescription = roleDescription;
        this.roleColorRGB = roleColorRGB;
    }

    public OpenRoleRevealPacket(FriendlyByteBuf buf) {
        this.roleName = buf.readUtf(64);
        this.roleDescription = buf.readUtf(256);
        this.roleColorRGB = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(roleName, 64);
        buf.writeUtf(roleDescription, 256);
        buf.writeInt(roleColorRGB);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleRoleReveal(roleName, roleDescription, roleColorRGB);
        });
        return true;
    }

    public String getRoleName() { return roleName; }
    public String getRoleDescription() { return roleDescription; }
    public int getRoleColorRGB() { return roleColorRGB; }
}
