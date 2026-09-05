package com.corazon.corazonmod.config;

import com.corazon.corazonmod.CorazonMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ArenaConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILENAME = "corazon_dontlie_coords.json";
    private static ArenaConfigManager INSTANCE;

    private ArenaData data;
    private File configFile;

    public static class PosData {
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;

        public PosData() {}

        public PosData(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public PosData(double x, double y, double z) {
            this(x, y, z, 0.0f, 0.0f);
        }
    }

    public static class TaskPosData {
        public String name;
        public double x;
        public double y;
        public double z;

        public TaskPosData() {}

        public TaskPosData(String name, double x, double y, double z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static class ArenaData {
        public String mapName = "MAP-DONT-LIE";
        public PosData mainSpawn = new PosData(-56.065, 31.000, 132.809, 0.0f, 0.0f);
        public PosData meetingTable = new PosData(-56.065, 31.000, 132.809, 0.0f, 0.0f);
        public PosData mafiaSpawn = new PosData(-56.065, 31.000, 132.809, 0.0f, 0.0f);
        public PosData doctorSpawn = new PosData(-56.065, 31.000, 132.809, 0.0f, 0.0f);
        public PosData policeSpawn = new PosData(-56.065, 31.000, 132.809, 0.0f, 0.0f);
        public PosData parkourStart = new PosData(-113.671, -5.000, 40.424, 91.8f, -0.3f);
        public PosData parkourCheckpoint = new PosData(-160.500, 10.000, 32.400, 0.0f, 0.0f);
        public PosData parkourFinish = new PosData(-138.434, 26.000, 61.381, 175.4f, 16.7f);
        public List<PosData> discussionSeats = new ArrayList<>();
        public List<TaskPosData> taskLocations = new ArrayList<>();

        public ArenaData() {
            // Default 10 Discussion Seats around the meeting table
            discussionSeats.add(new PosData(-13.698, 33.000, 190.382));
            discussionSeats.add(new PosData(-12.392, 33.000, 200.329));
            discussionSeats.add(new PosData(-22.277, 33.000, 191.436));
            discussionSeats.add(new PosData(-19.400, 33.000, 197.413));
            discussionSeats.add(new PosData(-25.466, 33.000, 198.425));
            discussionSeats.add(new PosData(-7.534, 33.000, 188.411));
            discussionSeats.add(new PosData(-10.661, 33.000, 184.513));
            discussionSeats.add(new PosData(-25.647, 33.000, 185.721));
            discussionSeats.add(new PosData(-18.402, 33.000, 188.262));
            discussionSeats.add(new PosData(-19.566, 33.000, 197.406));

            // Default sample tasks
            taskLocations.add(new TaskPosData("Fix Wiring", -50.0, 31.0, 130.0));
            taskLocations.add(new TaskPosData("Download Data", -62.0, 31.0, 135.0));
        }
    }

    private ArenaConfigManager() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        this.configFile = configDir.resolve(CONFIG_FILENAME).toFile();
        load();
    }

    public static synchronized ArenaConfigManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ArenaConfigManager();
        }
        return INSTANCE;
    }

    public void load() {
        if (!configFile.exists()) {
            CorazonMod.LOGGER.info("Config file {} not found. Creating default...", CONFIG_FILENAME);
            this.data = new ArenaData();
            save();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            this.data = GSON.fromJson(reader, ArenaData.class);
            if (this.data == null) {
                this.data = new ArenaData();
                save();
            }
            CorazonMod.LOGGER.info("Successfully loaded arena coordinates from {}", CONFIG_FILENAME);
        } catch (IOException e) {
            CorazonMod.LOGGER.error("Failed to load arena config {}", CONFIG_FILENAME, e);
            this.data = new ArenaData();
        }
    }

    public void save() {
        try {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(this.data, writer);
            }
            CorazonMod.LOGGER.info("Successfully saved arena coordinates to {}", CONFIG_FILENAME);
        } catch (IOException e) {
            CorazonMod.LOGGER.error("Failed to save arena config {}", CONFIG_FILENAME, e);
        }
    }

    public ArenaData getData() {
        if (data == null) data = new ArenaData();
        return data;
    }

    public void setMainSpawn(double x, double y, double z, float yaw, float pitch) {
        getData().mainSpawn = new PosData(x, y, z, yaw, pitch);
        save();
    }

    public PosData getMainSpawn() {
        return getData().mainSpawn != null ? getData().mainSpawn : new PosData(-56.065, 31.000, 132.809);
    }

    public PosData getMeetingTable() {
        return getData().meetingTable != null ? getData().meetingTable : getMainSpawn();
    }

    public List<PosData> getDiscussionSeats() {
        if (getData().discussionSeats == null || getData().discussionSeats.isEmpty()) {
            List<PosData> fallback = new ArrayList<>();
            fallback.add(getMainSpawn());
            return fallback;
        }
        return getData().discussionSeats;
    }

    public void setParkourStart(double x, double y, double z, float yaw, float pitch) {
        getData().parkourStart = new PosData(x, y, z, yaw, pitch);
        save();
    }

    public void setParkourFinish(double x, double y, double z) {
        getData().parkourFinish = new PosData(x, y, z, 0.0f, 0.0f);
        save();
    }

    public void setParkourCheckpoint(double x, double y, double z) {
        getData().parkourCheckpoint = new PosData(x, y, z, 0.0f, 0.0f);
        save();
    }

    public PosData getParkourStart() {
        return getData().parkourStart != null ? getData().parkourStart : getMainSpawn();
    }

    public PosData getParkourCheckpoint() {
        return getData().parkourCheckpoint != null ? getData().parkourCheckpoint : new PosData(-160.5, 10.0, 32.4);
    }

    public PosData getParkourFinish() {
        return getData().parkourFinish != null ? getData().parkourFinish : getMainSpawn();
    }
}
