package com.corazon.corazonmod.game;

import com.corazon.corazonmod.CorazonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ArenaBuilder — Built-in Map Generator for "MAP-DONT-LIE".
 * Dynamically reads exact NBT spawn/player coordinates from level.dat & playerdata
 * matching the exact architecture of the AMOGUS project.
 */
public class ArenaBuilder {

    private static final String TARGET_MAP_NAME = "MAP-DONT-LIE";
    private static final AtomicBoolean IS_GENERATING = new AtomicBoolean(false);

    public static final ResourceKey<Level> DONTLIE_DIMENSION_KEY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(CorazonMod.MOD_ID, "dontlie_world")
    );

    // Map Spawn Coordinates (Read dynamically from JSON config)
    public static double getSpawnX() { return com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getMainSpawn().x; }
    public static double getSpawnY() { return com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getMainSpawn().y; }
    public static double getSpawnZ() { return com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getMainSpawn().z; }
    public static float getSpawnYaw() { return com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getMainSpawn().yaw; }
    public static float getSpawnPitch() { return com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getMainSpawn().pitch; }

    public static final double SPAWN_X = -56.065;
    public static final double SPAWN_Y = 31.000;
    public static final double SPAWN_Z = 132.809;
    public static final BlockPos DEFAULT_SPAWN_POS = new BlockPos(-56, 31, 133);

    public static void buildArena(ServerLevel level, BlockPos origin, ServerPlayer builder) {
        safeLoadMap(builder, TARGET_MAP_NAME);
    }

    public static int safeLoadMap(ServerPlayer player, String mapName) {
        MinecraftServer server = player.getServer();
        if (server == null) return 0;

        if (!IS_GENERATING.compareAndSet(false, true)) {
            player.sendSystemMessage(Component.literal("⚠️ [Don't Lie] Proses ekstraksi map sedang berjalan di background, harap tunggu!").withStyle(net.minecraft.ChatFormatting.YELLOW));
            return 0;
        }

        player.sendSystemMessage(Component.literal("⏳ [Don't Lie] Memulai generasi map built-in '" + mapName + "' dari file JAR... Server tetap lancar!").withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD));

        CompletableFuture.runAsync(() -> {
            try {
                loadMapFilesAsync(server, player, mapName);
            } catch (Exception e) {
                IS_GENERATING.set(false);
                player.sendSystemMessage(Component.literal("❌ Error loading map '" + mapName + "': " + e.getMessage()).withStyle(net.minecraft.ChatFormatting.RED));
            }
        });

        return 1;
    }

    private static void loadMapFilesAsync(MinecraftServer server, ServerPlayer triggerPlayer, String mapName) {
        final Path[] sourceDir = {null};

        // 1. Search directly inside the Mod .JAR assets/resources
        ModList.get().getModContainerById(CorazonMod.MOD_ID).ifPresent(container -> {
            try {
                Path p = container.getModInfo().getOwningFile().getFile().findResource("maps/" + mapName);
                if (Files.exists(p)) {
                    sourceDir[0] = p;
                }
            } catch (Exception ignored) {}
        });

        // 2. Search local dev environment paths
        Path baseDir = Paths.get(".").toAbsolutePath().normalize();
        if (baseDir.getFileName() != null && baseDir.getFileName().toString().startsWith("run")) {
            baseDir = baseDir.getParent();
        }

        List<Path> candidates = List.of(
            baseDir.resolve("src/main/resources/maps/" + mapName),
            baseDir.resolve(mapName),
            Paths.get("src/main/resources/maps/" + mapName),
            Paths.get(mapName),
            Paths.get("../" + mapName)
        );

        if (sourceDir[0] == null || !Files.exists(sourceDir[0])) {
            for (Path candidate : candidates) {
                if (Files.exists(candidate)) {
                    sourceDir[0] = candidate;
                    break;
                }
            }
        }

        if (sourceDir[0] == null || !Files.exists(sourceDir[0])) {
            IS_GENERATING.set(false);
            triggerPlayer.sendSystemMessage(Component.literal("❌ Folder map built-in '" + mapName + "' tidak ditemukan di JAR!").withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }

        // Default to exact Don't Lie spawn coordinates
        BlockPos nbtDetectedPos = DEFAULT_SPAWN_POS;
        try {
            Path levelDatPath = sourceDir[0].resolve("level.dat");
            if (Files.exists(levelDatPath)) {
                CompoundTag nbt = NbtIo.readCompressed(levelDatPath.toFile());
                if (nbt.contains("Data", 10)) {
                    CompoundTag data = nbt.getCompound("Data");
                    int sx = data.getInt("SpawnX");
                    int sy = data.getInt("SpawnY");
                    int sz = data.getInt("SpawnZ");
                    if (sx != 0 || sy != 0 || sz != 0) {
                        nbtDetectedPos = new BlockPos(sx, Math.max(64, sy), sz);
                    }
                }
            }

            Path playerDataDir = sourceDir[0].resolve("playerdata");
            if (Files.exists(playerDataDir) && Files.isDirectory(playerDataDir)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(playerDataDir, "*.dat")) {
                    for (Path datFile : ds) {
                        CompoundTag pNbt = NbtIo.readCompressed(datFile.toFile());
                        if (pNbt.contains("Pos", 9)) {
                            ListTag posList = pNbt.getList("Pos", 6);
                            double px = posList.getDouble(0);
                            double py = posList.getDouble(1);
                            double pz = posList.getDouble(2);
                            nbtDetectedPos = new BlockPos((int) Math.round(px), (int) Math.round(py), (int) Math.round(pz));
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            CorazonMod.LOGGER.error("Failed to read NBT spawn coordinates", e);
        }

        final BlockPos spawnPos = nbtDetectedPos;

        // Target custom dimension save directory (like Multiverse-Core)
        Path worldDir = server.getWorldPath(LevelResource.LEVEL_DATA_FILE).getParent();
        Path dimensionTargetDir = worldDir.resolve("dimensions").resolve(CorazonMod.MOD_ID).resolve("dontlie_world");

        // Copy map files strictly into custom dimension target directory (do NOT overwrite Overworld!)
        copyDirectory(sourceDir[0], dimensionTargetDir);

        triggerPlayer.sendSystemMessage(Component.literal("✔ Ekstraksi map built-in '" + mapName + "' ke world 'corazonmod:dontlie_world' selesai! Preloading chunk di X: " + spawnPos.getX() + ", Y: " + spawnPos.getY() + ", Z: " + spawnPos.getZ() + "...").withStyle(net.minecraft.ChatFormatting.GREEN));

        // Return to main server thread for chunk preloading & dimension teleportation
        server.execute(() -> {
            try {
                ServerLevel overworld = server.overworld();
                ServerLevel targetLevel = server.getLevel(DONTLIE_DIMENSION_KEY);
                if (targetLevel == null) {
                    CorazonMod.LOGGER.warn("Custom dimension corazonmod:dontlie_world is null! Falling back to Overworld.");
                    targetLevel = overworld;
                }

                final ServerLevel finalLevel = targetLevel;

                List<BlockPos> keyPositions = List.of(
                    spawnPos,
                    spawnPos.offset(30, 0, 30),
                    spawnPos.offset(-30, 0, -30),
                    spawnPos.offset(60, 0, 60),
                    spawnPos.offset(-60, 0, -60)
                );

                Set<Long> chunkCoords = new HashSet<>();
                for (BlockPos p : keyPositions) {
                    chunkCoords.add(ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
                }

                List<CompletableFuture<?>> chunkFutures = new ArrayList<>();
                for (long cp : chunkCoords) {
                    int cx = ChunkPos.getX(cp);
                    int cz = ChunkPos.getZ(cp);
                    chunkFutures.add(finalLevel.getChunkSource().getChunkFuture(cx, cz, ChunkStatus.FULL, true));
                }

                CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0])).thenRunAsync(() -> {
                    server.execute(() -> {
                        try {
                            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                                p.teleportTo(finalLevel, SPAWN_X, SPAWN_Y, SPAWN_Z, p.getYRot(), p.getXRot());
                            }

                            // Set gamerules for minigame dimension
                            finalLevel.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                            finalLevel.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                            finalLevel.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);

                            server.sendSystemMessage(Component.literal("════════════════════════════════════════").withStyle(net.minecraft.ChatFormatting.GOLD));
                            server.sendSystemMessage(Component.literal("✅ MAP BUILT-IN '" + mapName + "' BERHASIL DIPASANG DI WORLD SEPARATE!").withStyle(net.minecraft.ChatFormatting.GREEN, net.minecraft.ChatFormatting.BOLD));
                            server.sendSystemMessage(Component.literal("🏰 Dimensi: " + finalLevel.dimension().location() + " | Posisi Spawn (" + SPAWN_X + ", " + SPAWN_Y + ", " + SPAWN_Z + ")").withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD));
                            server.sendSystemMessage(Component.literal("════════════════════════════════════════").withStyle(net.minecraft.ChatFormatting.GOLD));
                        } finally {
                            IS_GENERATING.set(false);
                        }
                    });
                });

            } catch (Exception e) {
                IS_GENERATING.set(false);
                triggerPlayer.sendSystemMessage(Component.literal("❌ Error saat setup world: " + e.getMessage()).withStyle(net.minecraft.ChatFormatting.RED));
            }
        });
    }

    private static boolean copyDirectory(Path source, Path target) {
        final boolean[] success = {true};
        try {
            Files.walk(source).forEach(sourcePath -> {
                try {
                    String name = sourcePath.getFileName() != null ? sourcePath.getFileName().toString() : "";
                    if (name.equalsIgnoreCase("stats") || name.equalsIgnoreCase("playerdata") || name.equalsIgnoreCase("advancements") || name.equalsIgnoreCase("session.lock") || name.equalsIgnoreCase("uid.dat")) {
                        return;
                    }
                    String pathStr = sourcePath.toString();
                    if (pathStr.contains("playerdata") || pathStr.contains("stats") || pathStr.contains("advancements")) {
                        return;
                    }

                    Path targetPath = target.resolve(source.relativize(sourcePath).toString());
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                    } else {
                        try (InputStream is = Files.newInputStream(sourcePath)) {
                            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (IOException e) {
                    success[0] = false;
                }
            });
        } catch (IOException e) {
            success[0] = false;
        }
        return success[0];
    }
}
