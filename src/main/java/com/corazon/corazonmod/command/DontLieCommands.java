package com.corazon.corazonmod.command;

import com.corazon.corazonmod.config.ArenaConfigManager;
import com.corazon.corazonmod.game.ArenaBuilder;
import com.corazon.corazonmod.game.DontLieGame;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class DontLieCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main /dontlie Command
        dispatcher.register(
            Commands.literal("dontlie")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    com.corazon.corazonmod.network.OpenAdminMenuPacket.sendToAdmin(player);
                    return 1;
                })
                // === START GAME ===
                .then(Commands.literal("start")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        List<ServerPlayer> players = new ArrayList<>(ctx.getSource().getServer().getPlayerList().getPlayers());
                        DontLieGame.getInstance().startNewGame(ctx.getSource().getServer(), players);
                        return 1;
                    })
                )

                // === STOP GAME ===
                .then(Commands.literal("stop")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        DontLieGame.getInstance().stopGame(ctx.getSource().getServer());
                        return 1;
                    })
                )

                // === ADD TIME ===
                .then(Commands.literal("addtime")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                        .executes(ctx -> {
                            int sec = IntegerArgumentType.getInteger(ctx, "seconds");
                            DontLieGame.getInstance().addTime(sec);
                            ctx.getSource().sendSuccess(() -> Component.literal("[Don't Lie] ⏱️ Menambah waktu " + sec + " detik!"), true);
                            return 1;
                        })
                    )
                )

                // === VOTE ===
                .then(Commands.literal("vote")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer voter = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DontLieGame.getInstance().votePlayer(voter, target);
                            return 1;
                        })
                    )
                )

                // === BUILD ARENA ===
                .then(Commands.literal("arena")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ServerLevel level = ctx.getSource().getLevel();
                        BlockPos pos = player.blockPosition();
                        ctx.getSource().sendSuccess(() -> Component.literal("[Don't Lie] 🏗️ Membangun & Memasang Arena Don't Lie di world tersendiri..."), true);
                        ArenaBuilder.buildArena(level, pos, player);
                        return 1;
                    })
                )

                // === TP TO DONTLIE WORLD ===
                .then(Commands.literal("tp")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ServerLevel targetLevel = ctx.getSource().getServer().getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);
                        if (targetLevel != null) {
                            double sx = ArenaBuilder.getSpawnX();
                            double sy = ArenaBuilder.getSpawnY();
                            double sz = ArenaBuilder.getSpawnZ();
                            player.teleportTo(targetLevel, sx, sy, sz, ArenaBuilder.getSpawnYaw(), ArenaBuilder.getSpawnPitch());
                            ctx.getSource().sendSuccess(() -> Component.literal("[Don't Lie] 🌀 Diteleportasi ke spawn 'corazonmod:dontlie_world'! (" + String.format("%.2f", sx) + ", " + String.format("%.2f", sy) + ", " + String.format("%.2f", sz) + ")"), true);
                        } else {
                            ctx.getSource().sendFailure(Component.literal("[Don't Lie] ❌ Dimensi corazonmod:dontlie_world belum dimuat. Jalankan /dontlie arena dulu."));
                        }
                        return 1;
                    })
                )

                // === SET SPAWN COORDS IN JSON ===
                .then(Commands.literal("setspawn")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        double x = player.getX();
                        double y = player.getY();
                        double z = player.getZ();
                        float yaw = player.getYRot();
                        float pitch = player.getXRot();

                        ArenaConfigManager.getInstance().setMainSpawn(x, y, z, yaw, pitch);
                        ctx.getSource().sendSuccess(() -> Component.literal(String.format("[Don't Lie] 📍 Spawn arena baru disimpan ke JSON! (X: %.3f, Y: %.3f, Z: %.3f, Yaw: %.1f, Pitch: %.1f)", x, y, z, yaw, pitch)), true);
                        return 1;
                    })
                )

                // === RELOAD CONFIG JSON ===
                .then(Commands.literal("reloadconfig")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ArenaConfigManager.getInstance().load();
                        ctx.getSource().sendSuccess(() -> Component.literal("[Don't Lie] 🔄 Config koordinat arena (corazon_dontlie_coords.json) berhasil di-reload!"), true);
                        return 1;
                    })
                )

                // === TP BACK TO OVERWORLD ===
                .then(Commands.literal("spawn")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ServerLevel overworld = ctx.getSource().getServer().overworld();
                        BlockPos spawn = overworld.getSharedSpawnPos();
                        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
                        ctx.getSource().sendSuccess(() -> Component.literal("[Don't Lie] 🏠 Kembali ke Overworld Spawn!"), true);
                        return 1;
                    })
                )

                // === INFO / HELP ===
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        ctx.getSource().sendSystemMessage(Component.literal("════════ DON'T LIE COMMANDS ════════").withStyle(net.minecraft.ChatFormatting.GOLD));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie start").withStyle(net.minecraft.ChatFormatting.GREEN).append(Component.literal(" - Mulai game baru").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie stop").withStyle(net.minecraft.ChatFormatting.RED).append(Component.literal(" - Hentikan game").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie arena").withStyle(net.minecraft.ChatFormatting.AQUA).append(Component.literal(" - Pasang & teleport ke arena world MAP-DONT-LIE").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie tp").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE).append(Component.literal(" - Teleport ke world dontlie_world").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie spawn").withStyle(net.minecraft.ChatFormatting.YELLOW).append(Component.literal(" - Teleport kembali ke Overworld spawn").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie setspawn").withStyle(net.minecraft.ChatFormatting.GOLD).append(Component.literal(" - Simpan posisi player saat ini ke config JSON").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie reloadconfig").withStyle(net.minecraft.ChatFormatting.GOLD).append(Component.literal(" - Reload file config JSON dari disk").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie addtime <detik>").withStyle(net.minecraft.ChatFormatting.YELLOW).append(Component.literal(" - Tambah waktu").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie vote <player>").withStyle(net.minecraft.ChatFormatting.WHITE).append(Component.literal(" - Vote pemain").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/mafia kill <player>").withStyle(net.minecraft.ChatFormatting.DARK_RED).append(Component.literal(" - Eksekusi malam (Mafia)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/doctor save <player>").withStyle(net.minecraft.ChatFormatting.BLUE).append(Component.literal(" - Lindungi (Doctor)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/police check <player>").withStyle(net.minecraft.ChatFormatting.GOLD).append(Component.literal(" - Selidiki (Police)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("════════════════════════════════").withStyle(net.minecraft.ChatFormatting.GOLD));
                        return 1;
                    })
                )
        );

        // /mafia kill <target>
        dispatcher.register(
            Commands.literal("mafia")
                .then(Commands.literal("kill")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer mafia = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DontLieGame.getInstance().setMafiaTarget(mafia, target);
                            return 1;
                        })
                    )
                )
        );

        // /doctor save <target>
        dispatcher.register(
            Commands.literal("doctor")
                .then(Commands.literal("save")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer doctor = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DontLieGame.getInstance().setDoctorTarget(doctor, target);
                            return 1;
                        })
                    )
                )
        );

        // /police check <target>
        dispatcher.register(
            Commands.literal("police")
                .then(Commands.literal("check")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer police = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DontLieGame.getInstance().policeInspect(police, target);
                            return 1;
                        })
                    )
                )
        );
    }
}
