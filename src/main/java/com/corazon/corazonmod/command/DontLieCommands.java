package com.corazon.corazonmod.command;

import com.corazon.corazonmod.config.ArenaConfigManager;
import com.corazon.corazonmod.game.ArenaBuilder;
import com.corazon.corazonmod.game.DontLieGame;
import com.corazon.corazonmod.game.PlayerRole;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
                        DontLieGame.getInstance().startNewGame(ctx.getSource().getServer(), null);
                        return 1;
                    })
                )

                // === REGISTER / JOIN (ADMIN ONLY) ===
                .then(Commands.literal("register")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        DontLieGame.getInstance().registerPlayer(player);
                        return 1;
                    })
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DontLieGame.getInstance().registerPlayer(target);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("join")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        DontLieGame.getInstance().registerPlayer(player);
                        return 1;
                    })
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DontLieGame.getInstance().registerPlayer(target);
                            return 1;
                        })
                    )
                )

                // === UNREGISTER / LEAVE (ADMIN ONLY) ===
                .then(Commands.literal("unregister")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        DontLieGame.getInstance().unregisterPlayer(player);
                        return 1;
                    })
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DontLieGame.getInstance().unregisterPlayer(target);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("leave")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        DontLieGame.getInstance().unregisterPlayer(player);
                        return 1;
                    })
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DontLieGame.getInstance().unregisterPlayer(target);
                            return 1;
                        })
                    )
                )

                // === ROSTER LIST ===
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        var server = ctx.getSource().getServer();
                        List<ServerPlayer> registered = DontLieGame.getInstance().getRegisteredOnlinePlayers(server);
                        int totalReg = DontLieGame.getInstance().getRegisteredPlayersCount();

                        ctx.getSource().sendSuccess(() -> Component.literal("════ DAFTAR PESERTA TERDAFTAR (" + totalReg + ") ════").withStyle(net.minecraft.ChatFormatting.GOLD), false);
                        if (registered.isEmpty()) {
                            ctx.getSource().sendSuccess(() -> Component.literal("Belum ada pemain yang terdaftar. Ketik /dontlie join untuk mendaftar!").withStyle(net.minecraft.ChatFormatting.GRAY), false);
                        } else {
                            for (ServerPlayer p : registered) {
                                ctx.getSource().sendSuccess(() -> Component.literal(" • " + p.getScoreboardName()).withStyle(net.minecraft.ChatFormatting.GREEN), false);
                            }
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal("════════════════════════════════════════").withStyle(net.minecraft.ChatFormatting.GOLD), false);
                        return 1;
                    })
                )

                // === CLEAR REGISTRATION QUEUE ===
                .then(Commands.literal("clear")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        DontLieGame.getInstance().clearRegisteredPlayers(ctx.getSource().getServer());
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

                // === SKIP DISCUSSION VOTE (ALL PLAYERS) ===
                .then(Commands.literal("skip")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        DontLieGame.getInstance().voteSkipDiscussion(player);
                        return 1;
                    })
                )

                // === FORCE SKIP PHASE (ADMIN ONLY) ===
                .then(Commands.literal("forceskip")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        DontLieGame.getInstance().skipCurrentPhase(ctx.getSource().getServer());
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

                // === SET PLAYER ROLE (ADMIN ONLY) ===
                .then(Commands.literal("setrole")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("role", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                String roleStr = StringArgumentType.getString(ctx, "role").toUpperCase();
                                DontLieGame game = DontLieGame.getInstance();
                                if (roleStr.equals("AUTO") || roleStr.equals("CLEAR") || roleStr.equals("NONE")) {
                                    game.setPlayerForcedRole(target.getUUID(), null);
                                    ctx.getSource().sendSuccess(() -> Component.literal("[Don't Lie] 🔄 Role manual untuk " + target.getScoreboardName() + " dihapus (Mode Auto)."), true);
                                } else {
                                    try {
                                        PlayerRole role = PlayerRole.valueOf(roleStr);
                                        game.setPlayerForcedRole(target.getUUID(), role);
                                        ctx.getSource().sendSuccess(() -> Component.literal("[Don't Lie] 🎭 Role manual untuk " + target.getScoreboardName() + " diset ke: " + role.getDisplayName()), true);
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().sendFailure(Component.literal("[Don't Lie] ❌ Role tidak valid! Pilih: MAFIA, DOCTOR, POLICE, CITIZEN, atau AUTO"));
                                    }
                                }
                                return 1;
                            })
                        )
                    )
                )

                // === CLEAR ALL FORCED ROLES (ADMIN ONLY) ===
                .then(Commands.literal("clearroles")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        DontLieGame.getInstance().clearForcedRoles();
                        ctx.getSource().sendSuccess(() -> Component.literal("[Don't Lie] 🧹 Semua settingan role manual telah dibersihkan!"), true);
                        return 1;
                    })
                )

                // === SET ROLE COUNTS (ADMIN ONLY) ===
                .then(Commands.literal("rolecount")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("mafia", IntegerArgumentType.integer(-1, 50))
                        .then(Commands.argument("doctor", IntegerArgumentType.integer(-1, 50))
                            .then(Commands.argument("police", IntegerArgumentType.integer(-1, 50))
                                .executes(ctx -> {
                                    int mafia = IntegerArgumentType.getInteger(ctx, "mafia");
                                    int doctor = IntegerArgumentType.getInteger(ctx, "doctor");
                                    int police = IntegerArgumentType.getInteger(ctx, "police");
                                    DontLieGame.getInstance().setCustomRoleCounts(mafia, doctor, police);
                                    ctx.getSource().sendSuccess(() -> Component.literal(String.format("[Don't Lie] 📊 Jumlah role diperbarui! (Mafia: %s, Doctor: %s, Police: %s)",
                                        mafia == -1 ? "Auto" : mafia,
                                        doctor == -1 ? "Auto" : doctor,
                                        police == -1 ? "Auto" : police
                                    )), true);
                                    return 1;
                                })
                            )
                        )
                    )
                )

                // === VOTE ===
                .then(Commands.literal("vote")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer voter = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DontLieGame.getInstance().votePlayer(voter, target.getUUID());
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
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie register <player>").withStyle(net.minecraft.ChatFormatting.GREEN).append(Component.literal(" - Daftarkan peserta (Admin)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie unregister <player>").withStyle(net.minecraft.ChatFormatting.RED).append(Component.literal(" - Hapus pendaftaran peserta (Admin)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie list").withStyle(net.minecraft.ChatFormatting.AQUA).append(Component.literal(" - Lihat daftar pemain terdaftar").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie clear").withStyle(net.minecraft.ChatFormatting.DARK_RED).append(Component.literal(" - Hapus daftar pendaftaran (Admin)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie start").withStyle(net.minecraft.ChatFormatting.GREEN).append(Component.literal(" - Mulai game (Admin)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie stop").withStyle(net.minecraft.ChatFormatting.RED).append(Component.literal(" - Hentikan game (Admin)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie skip").withStyle(net.minecraft.ChatFormatting.YELLOW).append(Component.literal(" - Setuju lewati fase Diskusi (Butuh persetujuan semua pemain)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie forceskip").withStyle(net.minecraft.ChatFormatting.RED).append(Component.literal(" - Paksa lewati fase saat ini (Admin)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie arena").withStyle(net.minecraft.ChatFormatting.AQUA).append(Component.literal(" - Pasang & teleport ke arena world MAP-DONT-LIE").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie tp").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE).append(Component.literal(" - Teleport ke world dontlie_world").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie spawn").withStyle(net.minecraft.ChatFormatting.YELLOW).append(Component.literal(" - Teleport kembali ke Overworld spawn").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie setspawn").withStyle(net.minecraft.ChatFormatting.GOLD).append(Component.literal(" - Simpan posisi player saat ini ke config JSON").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie reloadconfig").withStyle(net.minecraft.ChatFormatting.GOLD).append(Component.literal(" - Reload file config JSON dari disk").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie setrole <player> <role>").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE).append(Component.literal(" - Set role manual pemain (Admin)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie clearroles").withStyle(net.minecraft.ChatFormatting.DARK_RED).append(Component.literal(" - Hapus semua role manual (Admin)").withStyle(net.minecraft.ChatFormatting.GRAY)));
                        ctx.getSource().sendSystemMessage(Component.literal("/dontlie rolecount <m> <d> <p>").withStyle(net.minecraft.ChatFormatting.GOLD).append(Component.literal(" - Set jumlah role Mafia/Doctor/Police (Admin)").withStyle(net.minecraft.ChatFormatting.GRAY)));
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
