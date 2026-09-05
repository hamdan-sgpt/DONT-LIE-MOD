package com.corazon.corazonmod.network;

import com.corazon.corazonmod.CorazonMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(CorazonMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Server -> Client packets
        net.messageBuilder(OpenRoleRevealPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenRoleRevealPacket::new)
                .encoder(OpenRoleRevealPacket::toBytes)
                .consumerMainThread(OpenRoleRevealPacket::handle)
                .add();

        net.messageBuilder(OpenVotingScreenPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenVotingScreenPacket::new)
                .encoder(OpenVotingScreenPacket::toBytes)
                .consumerMainThread(OpenVotingScreenPacket::handle)
                .add();

        net.messageBuilder(OpenNightActionPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenNightActionPacket::new)
                .encoder(OpenNightActionPacket::toBytes)
                .consumerMainThread(OpenNightActionPacket::handle)
                .add();

        net.messageBuilder(GameStateUpdatePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(GameStateUpdatePacket::new)
                .encoder(GameStateUpdatePacket::toBytes)
                .consumerMainThread(GameStateUpdatePacket::handle)
                .add();

        net.messageBuilder(OpenAdminMenuPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenAdminMenuPacket::new)
                .encoder(OpenAdminMenuPacket::toBytes)
                .consumerMainThread(OpenAdminMenuPacket::handle)
                .add();

        net.messageBuilder(OpenRunnerSelectionPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenRunnerSelectionPacket::new)
                .encoder(OpenRunnerSelectionPacket::toBytes)
                .consumerMainThread(OpenRunnerSelectionPacket::handle)
                .add();

        net.messageBuilder(OpenParkourModeVotePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenParkourModeVotePacket::new)
                .encoder(OpenParkourModeVotePacket::toBytes)
                .consumerMainThread(OpenParkourModeVotePacket::handle)
                .add();

        // Client -> Server packets
        net.messageBuilder(VotePlayerPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(VotePlayerPacket::new)
                .encoder(VotePlayerPacket::toBytes)
                .consumerMainThread(VotePlayerPacket::handle)
                .add();

        net.messageBuilder(VoteRunnerPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(VoteRunnerPacket::new)
                .encoder(VoteRunnerPacket::toBytes)
                .consumerMainThread(VoteRunnerPacket::handle)
                .add();

        net.messageBuilder(VoteParkourModePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(VoteParkourModePacket::new)
                .encoder(VoteParkourModePacket::toBytes)
                .consumerMainThread(VoteParkourModePacket::handle)
                .add();

        net.messageBuilder(NightActionPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(NightActionPacket::new)
                .encoder(NightActionPacket::toBytes)
                .consumerMainThread(NightActionPacket::handle)
                .add();

        net.messageBuilder(AdminActionPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AdminActionPacket::new)
                .encoder(AdminActionPacket::toBytes)
                .consumerMainThread(AdminActionPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
