package com.corazon.corazonmod.game;

import com.corazon.corazonmod.init.ModItems;
import com.corazon.corazonmod.network.GameStateUpdatePacket;
import com.corazon.corazonmod.network.ModMessages;
import com.corazon.corazonmod.network.NightActionPacket;
import com.corazon.corazonmod.network.OpenAdminMenuPacket;
import com.corazon.corazonmod.network.OpenNightActionPacket;
import com.corazon.corazonmod.network.OpenParkourModeVotePacket;
import com.corazon.corazonmod.network.OpenRoleRevealPacket;
import com.corazon.corazonmod.network.OpenRunnerSelectionPacket;
import com.corazon.corazonmod.network.OpenVotingScreenPacket;
import com.corazon.corazonmod.network.OpenVotingScreenPacket.PlayerEntry;
import com.corazon.corazonmod.network.VotePlayerPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.*;

public class DontLieGame {
    private static final DontLieGame INSTANCE = new DontLieGame();

    private final Map<UUID, PlayerRole> playerRoles = new HashMap<>();
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Map<UUID, UUID> votes = new HashMap<>();
    private final Set<UUID> discussionSkipVotes = new HashSet<>();

    private GamePhase currentPhase = GamePhase.LOBBY;
    private int phaseTimeRemaining = 0;
    private int tickCounter = 0;
    private int hudSyncCounter = 0;

    private int customHidingDuration = 60;
    private int customMinigameDuration = 120;
    private int customSearchDuration = 300;
    private int customDiscussionDuration = 90;
    private int customVotingDuration = 30;
    private int customRunnerVoteDuration = 15;

    private int minigameScore = 0;

    private int customMafiaCount = -1;  // -1 means auto
    private int customDoctorCount = 1;
    private int customPoliceCount = 1;

    private final Map<UUID, PlayerRole> forcedRoles = new HashMap<>();

    private UUID moneyHolder = null;
    private UUID mafiaTarget = null;
    private UUID doctorTarget = null;
    private UUID parkourRunnerUUID = null;
    private boolean isParkourFinished = false;
    private boolean isParkourEnabled = true; // Admin toggle ON/OFF for Parkour minigame
    private boolean parkourGroupMode = false; // false = Perwakilan (1 Runner), true = Bareng-bareng (Semua Pemain)
    private boolean isParkourModeVotingPhase = false;
    private final Map<UUID, String> parkourModeVotes = new HashMap<>();
    private boolean isRunnerVotingPhase = false;
    private final Map<UUID, UUID> runnerVotes = new HashMap<>();

    public boolean isParkourEnabled() {
        return isParkourEnabled;
    }

    public void setParkourEnabled(boolean enabled) {
        this.isParkourEnabled = enabled;
    }

    public void toggleParkourEnabled() {
        this.isParkourEnabled = !this.isParkourEnabled;
    }

    public boolean isParkourGroupMode() {
        return parkourGroupMode;
    }

    public void setParkourGroupMode(boolean groupMode) {
        this.parkourGroupMode = groupMode;
    }

    public void toggleParkourGroupMode() {
        this.parkourGroupMode = !this.parkourGroupMode;
    }

    public void setParkourRunner(UUID playerUUID) {
        this.parkourRunnerUUID = playerUUID;
    }

    public UUID getParkourRunner() {
        return parkourRunnerUUID;
    }

    public int getCustomRunnerVoteDuration() {
        return customRunnerVoteDuration;
    }

    public void setCustomRunnerVoteDuration(int duration) {
        if (duration > 0) this.customRunnerVoteDuration = duration;
    }

    public void setCustomDurations(int hidingSeconds, int searchSeconds, int discussionSeconds, int votingSeconds) {
        setCustomDurations(hidingSeconds, 120, searchSeconds, discussionSeconds, votingSeconds, 15);
    }

    public void setCustomDurations(int hidingSeconds, int minigameSeconds, int searchSeconds, int discussionSeconds, int votingSeconds) {
        setCustomDurations(hidingSeconds, minigameSeconds, searchSeconds, discussionSeconds, votingSeconds, 15);
    }

    public void setCustomDurations(int hidingSeconds, int minigameSeconds, int searchSeconds, int discussionSeconds, int votingSeconds, int runnerVoteSeconds) {
        if (hidingSeconds > 0) this.customHidingDuration = hidingSeconds;
        if (minigameSeconds > 0) this.customMinigameDuration = minigameSeconds;
        if (searchSeconds > 0) this.customSearchDuration = searchSeconds;
        if (discussionSeconds > 0) this.customDiscussionDuration = discussionSeconds;
        if (votingSeconds > 0) this.customVotingDuration = votingSeconds;
        if (runnerVoteSeconds > 0) this.customRunnerVoteDuration = runnerVoteSeconds;
    }

    public int getCustomMinigameDuration() {
        return customMinigameDuration;
    }

    public int getMinigameScore() {
        return minigameScore;
    }

    public void addMinigameScore(ServerPlayer player, int points) {
        if (currentPhase != GamePhase.MINIGAME) return;
        minigameScore += points;
        player.sendSystemMessage(Component.literal("🎯 + " + points + " Poin Minigame! Total Skor Kelompok: " + minigameScore).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 0.8f, 1.2f);
    }

    public void setCustomRoleCounts(int mafiaCount, int doctorCount, int policeCount) {
        this.customMafiaCount = mafiaCount;
        this.customDoctorCount = doctorCount;
        this.customPoliceCount = policeCount;
    }

    public int getCustomMafiaCount() { return customMafiaCount; }
    public int getCustomDoctorCount() { return customDoctorCount; }
    public int getCustomPoliceCount() { return customPoliceCount; }

    public void setPlayerForcedRole(UUID playerUUID, PlayerRole role) {
        if (role == null) {
            forcedRoles.remove(playerUUID);
        } else {
            forcedRoles.put(playerUUID, role);
        }
    }

    public PlayerRole getForcedRole(UUID playerUUID) {
        return forcedRoles.get(playerUUID);
    }

    public Map<UUID, PlayerRole> getForcedRoles() {
        return Collections.unmodifiableMap(forcedRoles);
    }

    public void clearForcedRoles() {
        forcedRoles.clear();
    }

    public int getCustomHidingDuration() {
        return customHidingDuration;
    }

    public int getCustomSearchDuration() {
        return customSearchDuration;
    }

    public int getCustomDiscussionDuration() {
        return customDiscussionDuration;
    }

    public int getCustomVotingDuration() {
        return customVotingDuration;
    }

    private final Set<UUID> registeredPlayers = new LinkedHashSet<>();
    private ServerBossEvent bossBar;
    private final Set<UUID> allPlayerUUIDs = new HashSet<>();

    public boolean registerPlayer(ServerPlayer player) {
        if (isGameRunning()) {
            player.sendSystemMessage(Component.literal("[Don't Lie] ❌ Game sedang berjalan! Tidak bisa mendaftar sekarang.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (registeredPlayers.contains(player.getUUID())) {
            player.sendSystemMessage(Component.literal("[Don't Lie] ℹ️ Kamu sudah terdaftar sebagai peserta!").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        registeredPlayers.add(player.getUUID());
        player.sendSystemMessage(Component.literal("[Don't Lie] ✅ Kamu berhasil mendaftar sebagai peserta game! (Total terdaftar: " + registeredPlayers.size() + ")").withStyle(ChatFormatting.GREEN));
        broadcastToAll(player.server, Component.literal("📝 " + player.getScoreboardName() + " telah mendaftar sebagai peserta! (" + registeredPlayers.size() + " pemain)").withStyle(ChatFormatting.AQUA));
        return true;
    }

    public boolean unregisterPlayer(ServerPlayer player) {
        if (isGameRunning()) {
            player.sendSystemMessage(Component.literal("[Don't Lie] ❌ Game sedang berjalan! Tidak bisa membatalkan pendaftaran.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!registeredPlayers.contains(player.getUUID())) {
            player.sendSystemMessage(Component.literal("[Don't Lie] ℹ️ Kamu belum terdaftar.").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        registeredPlayers.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("[Don't Lie] ❌ Kamu telah membatalkan pendaftaran peserta.").withStyle(ChatFormatting.RED));
        broadcastToAll(player.server, Component.literal("📝 " + player.getScoreboardName() + " keluar dari pendaftaran peserta. (" + registeredPlayers.size() + " pemain)").withStyle(ChatFormatting.GRAY));
        return true;
    }

    public boolean isRegistered(UUID uuid) {
        return registeredPlayers.contains(uuid);
    }

    public int getRegisteredPlayersCount() {
        return registeredPlayers.size();
    }

    public void clearRegisteredPlayers(MinecraftServer server) {
        registeredPlayers.clear();
        broadcastToAll(server, Component.literal("[Don't Lie] 🧹 Daftar pendaftaran peserta telah dikosongkan oleh Admin.").withStyle(ChatFormatting.YELLOW));
    }

    public List<ServerPlayer> getRegisteredOnlinePlayers(MinecraftServer server) {
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID uuid : registeredPlayers) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                list.add(p);
            }
        }
        return list;
    }

    public static DontLieGame getInstance() {
        return INSTANCE;
    }

    public boolean isGameRunning() {
        return currentPhase != GamePhase.LOBBY && currentPhase != GamePhase.ENDED;
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public PlayerRole getRole(UUID playerUUID) {
        return playerRoles.getOrDefault(playerUUID, PlayerRole.CITIZEN);
    }

    public boolean isAlive(UUID playerUUID) {
        return alivePlayers.contains(playerUUID);
    }

    public boolean isParticipant(UUID playerUUID) {
        return allPlayerUUIDs.contains(playerUUID);
    }

    public int getAliveCount() {
        return alivePlayers.size();
    }

    public int getTotalPlayers() {
        return allPlayerUUIDs.size();
    }

    public int getTimeRemaining() {
        return phaseTimeRemaining;
    }

    public void startNewGame(MinecraftServer server, List<ServerPlayer> inputPlayers) {
        List<ServerPlayer> players = inputPlayers;
        if (players == null || players.isEmpty()) {
            if (!registeredPlayers.isEmpty()) {
                players = getRegisteredOnlinePlayers(server);
                broadcastToAll(server, Component.literal("[Don't Lie] 📝 Memulai game dengan " + players.size() + " pemain terdaftar!").withStyle(ChatFormatting.GOLD));
            } else {
                broadcastToAll(server, Component.literal("[Don't Lie] ❌ Tidak ada pemain yang terdaftar! Pemain harus mendaftar dulu (/dontlie join) atau Admin menaftarkan semua pemain (/dontlie registerall).").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                return;
            }
        }

        if (players.size() < 2) {
            broadcastToAll(server, Component.literal("[Don't Lie] ❌ Minimal 2 pemain terdaftar yang online untuk memulai game!").withStyle(ChatFormatting.RED));
            return;
        }

        // Teleport all participants to Don't Lie dimension & discussion seats and set to Adventure Mode
        ServerLevel dontLieLevel = server.getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);
        List<com.corazon.corazonmod.config.ArenaConfigManager.PosData> seats = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getDiscussionSeats();
        int seatIdx = 0;
        for (ServerPlayer p : players) {
            p.setGameMode(GameType.ADVENTURE);
            if (dontLieLevel != null) {
                if (!seats.isEmpty()) {
                    var seat = seats.get(seatIdx % seats.size());
                    p.teleportTo(dontLieLevel, seat.x, seat.y, seat.z, seat.yaw, seat.pitch);
                    seatIdx++;
                } else {
                    p.teleportTo(dontLieLevel, ArenaBuilder.getSpawnX(), ArenaBuilder.getSpawnY(), ArenaBuilder.getSpawnZ(), ArenaBuilder.getSpawnYaw(), ArenaBuilder.getSpawnPitch());
                }
            }
        }

        playerRoles.clear();
        alivePlayers.clear();
        allPlayerUUIDs.clear();
        votes.clear();
        moneyHolder = null;
        mafiaTarget = null;
        doctorTarget = null;

        // Determine targeted role counts
        int totalCount = players.size();
        int targetMafia = (customMafiaCount >= 0) ? customMafiaCount : Math.max(1, Math.min(3, totalCount / 3));
        int targetDoctor = (customDoctorCount >= 0) ? customDoctorCount : 1;
        int targetPolice = (customPoliceCount >= 0) ? customPoliceCount : 1;

        List<ServerPlayer> unassignedPlayers = new ArrayList<>();

        // First pass: assign forced roles for players who have a forced role configured
        for (ServerPlayer p : players) {
            UUID uuid = p.getUUID();
            alivePlayers.add(uuid);
            allPlayerUUIDs.add(uuid);

            if (forcedRoles.containsKey(uuid)) {
                PlayerRole forcedRole = forcedRoles.get(uuid);
                playerRoles.put(uuid, forcedRole);

                // Reduce needed count for this role
                switch (forcedRole) {
                    case MAFIA -> targetMafia = Math.max(0, targetMafia - 1);
                    case DOCTOR -> targetDoctor = Math.max(0, targetDoctor - 1);
                    case POLICE -> targetPolice = Math.max(0, targetPolice - 1);
                    default -> {}
                }
            } else {
                unassignedPlayers.add(p);
            }
        }

        // Shuffle remaining unassigned players
        Collections.shuffle(unassignedPlayers);
        int unassignedIdx = 0;

        // Fill remaining MAFIA quota
        for (int i = 0; i < targetMafia && unassignedIdx < unassignedPlayers.size(); i++) {
            ServerPlayer p = unassignedPlayers.get(unassignedIdx++);
            playerRoles.put(p.getUUID(), PlayerRole.MAFIA);
        }

        // Fill remaining DOCTOR quota
        for (int i = 0; i < targetDoctor && unassignedIdx < unassignedPlayers.size(); i++) {
            ServerPlayer p = unassignedPlayers.get(unassignedIdx++);
            playerRoles.put(p.getUUID(), PlayerRole.DOCTOR);
        }

        // Fill remaining POLICE quota
        for (int i = 0; i < targetPolice && unassignedIdx < unassignedPlayers.size(); i++) {
            ServerPlayer p = unassignedPlayers.get(unassignedIdx++);
            playerRoles.put(p.getUUID(), PlayerRole.POLICE);
        }

        // Assign CITIZEN for all remaining unassigned players
        while (unassignedIdx < unassignedPlayers.size()) {
            ServerPlayer p = unassignedPlayers.get(unassignedIdx++);
            playerRoles.put(p.getUUID(), PlayerRole.CITIZEN);
        }

        // Give Money Pouch to one of the Mafias to hide
        ServerPlayer moneyMafia = null;
        for (ServerPlayer p : players) {
            if (getRole(p.getUUID()) == PlayerRole.MAFIA) {
                moneyMafia = p;
                break;
            }
        }
        if (moneyMafia != null) {
            moneyMafia.getInventory().add(new ItemStack(ModItems.MONEY_POUCH.get()));
            moneyMafia.sendSystemMessage(Component.literal("💰 Uang (Money Pouch) ada di inventaris kamu! Sembunyikan di dalam chest selama Hiding Phase!").withStyle(ChatFormatting.GOLD));
        }

        // *** Remove any Daggers from all players at game start ***
        removeMafiaDaggers(server);

        // *** Send Role Reveal GUI packet to each player ***
        for (ServerPlayer p : players) {
            PlayerRole role = getRole(p.getUUID());
            ModMessages.sendToPlayer(
                new OpenRoleRevealPacket(role.getDisplayName(), role.getDescription(), role.getColor().getColor()),
                p
            );
            // Also play dramatic sound
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 0.8f);
        }

        // Start with Hiding Phase after 8 seconds (let role reveal finish)
        currentPhase = GamePhase.LOBBY;
        phaseTimeRemaining = 8; // 8 second delay before game truly starts

        broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.GOLD));
        broadcastToAll(server, Component.literal("  GAME GOING SEVENTEEN: DON'T LIE DIMULAI!  ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        broadcastToAll(server, Component.literal("  Role rahasia telah dibagikan!  ").withStyle(ChatFormatting.AQUA));
        broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.GOLD));

        // Set server difficulty to Peaceful & start Hiding Phase
        server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
        setPhase(server, GamePhase.HIDING);
    }

    public void stopGame(MinecraftServer server) {
        currentPhase = GamePhase.ENDED;
        removeMafiaDaggers(server);

        // Remove effects and reset gamemode to Survival for all players
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.removeEffect(MobEffects.BLINDNESS);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (isParticipant(p.getUUID())) {
                p.setGameMode(GameType.SURVIVAL);
            }
        }

        // Send game ended state to clients
        syncGameStateToAll(server);

        broadcastToAll(server, Component.literal("[Don't Lie] Game dihentikan oleh Admin.").withStyle(ChatFormatting.RED));
    }

    public void addTime(int seconds) {
        phaseTimeRemaining += seconds;
    }

    public void voteSkipDiscussion(ServerPlayer player) {
        if (!isGameRunning() || currentPhase != GamePhase.DISCUSSION) {
            player.sendSystemMessage(Component.literal("[Don't Lie] ⚠️ Usulan lewati diskusi hanya bisa dilakukan saat Fase Diskusi!").withStyle(ChatFormatting.RED));
            return;
        }
        if (!isParticipant(player.getUUID())) {
            player.sendSystemMessage(Component.literal("[Don't Lie] ⚠️ Hanya peserta game yang bisa memilih lewati diskusi.").withStyle(ChatFormatting.RED));
            return;
        }
        if (!isAlive(player.getUUID())) {
            player.sendSystemMessage(Component.literal("[Don't Lie] ⚠️ Pemain yang gugur tidak bisa memilih.").withStyle(ChatFormatting.RED));
            return;
        }
        if (discussionSkipVotes.contains(player.getUUID())) {
            player.sendSystemMessage(Component.literal("[Don't Lie] ℹ️ Kamu sudah menyetujui untuk melewati fase diskusi.").withStyle(ChatFormatting.YELLOW));
            return;
        }

        discussionSkipVotes.add(player.getUUID());
        int count = discussionSkipVotes.size();
        int needed = getAliveCount();

        broadcastToAll(player.server, Component.literal("⏩ " + player.getScoreboardName() + " ingin melewati fase Diskusi! (" + count + "/" + needed + " pemain setuju)").withStyle(ChatFormatting.GOLD));

        if (count >= needed) {
            broadcastToAll(player.server, Component.literal("⏩ SEMUA PEMAIN SETUJU! Fase Diskusi dilewati.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            advancePhase(player.server);
        }
    }

    public boolean isParkourModeVotingPhase() {
        return isParkourModeVotingPhase;
    }

    public void startParkourModeVoting(MinecraftServer server) {
        this.isParkourModeVotingPhase = true;
        this.isRunnerVotingPhase = false;
        this.parkourModeVotes.clear();
        this.phaseTimeRemaining = customRunnerVoteDuration;

        OpenParkourModeVotePacket packet = new OpenParkourModeVotePacket(customRunnerVoteDuration);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID())) {
                ModMessages.sendToPlayer(packet, p);
                p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.MASTER, 1.0f, 1.2f);
            }
        }

        broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.LIGHT_PURPLE));
        broadcastToAll(server, Component.literal("🏃 VOTING MODE PARKOUR (" + customRunnerVoteDuration + " DETIK) DIMULAI!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        broadcastToAll(server, Component.literal("🗳️ Buka layar pop-up dan pilih: BARENG-BARENG atau PERWAKILAN!").withStyle(ChatFormatting.YELLOW));
        broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public void voteParkourMode(ServerPlayer voter, String choice) {
        if (!isParkourModeVotingPhase) return;
        if (!isAlive(voter.getUUID())) return;

        if (!"BARENG_BARENG".equals(choice) && !"PERWAKILAN".equals(choice)) return;

        parkourModeVotes.put(voter.getUUID(), choice);
        String choiceName = "BARENG_BARENG".equals(choice) ? "👥 BARENG-BARENG" : "👤 PERWAKILAN";
        voter.sendSystemMessage(Component.literal("✓ Kamu memilih mode Parkour: " + choiceName).withStyle(ChatFormatting.GREEN));
        broadcastToAll(voter.server, Component.literal("🗳️ " + voter.getScoreboardName() + " telah memilih mode parkour. (" + parkourModeVotes.size() + "/" + getAliveCount() + ")").withStyle(ChatFormatting.YELLOW));

        if (parkourModeVotes.size() >= getAliveCount()) {
            resolveParkourModeVoting(voter.server);
        }
    }

    public void resolveParkourModeVoting(MinecraftServer server) {
        if (!isParkourModeVotingPhase) return;
        isParkourModeVotingPhase = false;

        int barengCount = 0;
        int perwakilanCount = 0;
        for (String c : parkourModeVotes.values()) {
            if ("PERWAKILAN".equals(c)) {
                perwakilanCount++;
            } else {
                barengCount++;
            }
        }

        broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.GOLD));
        broadcastToAll(server, Component.literal("📊 HASIL VOTING MODE PARKOUR:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        broadcastToAll(server, Component.literal("👥 Bareng-bareng: " + barengCount + " Vote | 👤 Perwakilan: " + perwakilanCount + " Vote").withStyle(ChatFormatting.YELLOW));

        if (perwakilanCount > barengCount) {
            this.parkourGroupMode = false;
            broadcastToAll(server, Component.literal("🎉 Mode PERWAKILAN (1 Runner) Terpilih!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.GOLD));
            startRunnerSelection(server);
        } else {
            this.parkourGroupMode = true;
            broadcastToAll(server, Component.literal("🎉 Mode BARENG-BARENG (Semua Pemain) Terpilih!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.GOLD));
            setPhase(server, GamePhase.MINIGAME);
        }
    }

    public boolean isRunnerVotingPhase() {
        return isRunnerVotingPhase;
    }

    public void startRunnerSelection(MinecraftServer server) {
        this.isRunnerVotingPhase = true;
        this.runnerVotes.clear();
        this.phaseTimeRemaining = customRunnerVoteDuration;

        List<OpenRunnerSelectionPacket.RunnerCandidateEntry> candidates = new ArrayList<>();
        for (UUID uuid : alivePlayers) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                candidates.add(new OpenRunnerSelectionPacket.RunnerCandidateEntry(uuid, p.getScoreboardName()));
            }
        }

        OpenRunnerSelectionPacket packet = new OpenRunnerSelectionPacket(candidates);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID())) {
                ModMessages.sendToPlayer(packet, p);
                p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.MASTER, 1.0f, 1.2f);
            }
        }

        broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.LIGHT_PURPLE));
        broadcastToAll(server, Component.literal("🏃 VOTING PARKOUR RUNNER (" + customRunnerVoteDuration + " DETIK) DIMULAI!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        broadcastToAll(server, Component.literal("🗳️ Buka layar pop-up dan pilih pemain yang mewakili kelompok!").withStyle(ChatFormatting.YELLOW));
        broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public void voteRunner(ServerPlayer voter, UUID targetUUID) {
        if (!isRunnerVotingPhase) return;
        if (!isAlive(voter.getUUID())) return;

        ServerPlayer target = voter.server.getPlayerList().getPlayer(targetUUID);
        if (target == null || !isAlive(target.getUUID())) return;

        runnerVotes.put(voter.getUUID(), target.getUUID());
        voter.sendSystemMessage(Component.literal("✓ Kamu memilih " + target.getScoreboardName() + " sebagai Parkour Runner.").withStyle(ChatFormatting.GREEN));
        broadcastToAll(voter.server, Component.literal("🗳️ " + voter.getScoreboardName() + " telah memberikan suara runner. (" + runnerVotes.size() + "/" + getAliveCount() + ")").withStyle(ChatFormatting.YELLOW));

        if (runnerVotes.size() >= getAliveCount()) {
            resolveRunnerVoting(voter.server);
        }
    }

    public void resolveRunnerVoting(MinecraftServer server) {
        if (!isRunnerVotingPhase) return;
        isRunnerVotingPhase = false;

        Map<UUID, Integer> voteCounts = new HashMap<>();
        for (UUID targetUUID : runnerVotes.values()) {
            voteCounts.put(targetUUID, voteCounts.getOrDefault(targetUUID, 0) + 1);
        }

        UUID highestVoted = null;
        int maxVotes = 0;
        for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                highestVoted = entry.getKey();
            }
        }

        if (highestVoted == null) {
            // Fallback to random alive player if no votes were placed
            for (UUID uuid : alivePlayers) {
                if (server.getPlayerList().getPlayer(uuid) != null) {
                    highestVoted = uuid;
                    break;
                }
            }
        }

        if (highestVoted != null) {
            setParkourRunner(highestVoted);
            ServerPlayer winner = server.getPlayerList().getPlayer(highestVoted);
            String winnerName = winner != null ? winner.getScoreboardName() : "Runner";

            broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.GOLD));
            broadcastToAll(server, Component.literal("🎉 " + winnerName + " TERPILIH SEBAGAI PARKOUR RUNNER KELOMPOK!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            broadcastToAll(server, Component.literal("🗳️ Total Suara: " + maxVotes + " Vote").withStyle(ChatFormatting.YELLOW));
            broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.GOLD));
        }

        setPhase(server, GamePhase.MINIGAME);
    }

    public void skipCurrentPhase(MinecraftServer server) {
        if (!isGameRunning()) return;
        if (isParkourModeVotingPhase) {
            resolveParkourModeVoting(server);
            return;
        }
        if (isRunnerVotingPhase) {
            resolveRunnerVoting(server);
            return;
        }
        broadcastToAll(server, Component.literal("[Don't Lie] ⏩ Fase " + currentPhase.getDisplayName() + " dipaksa lewat oleh Admin!").withStyle(ChatFormatting.YELLOW));
        advancePhase(server);
    }

    public void setPhase(MinecraftServer server, GamePhase phase) {
        this.currentPhase = phase;
        this.isParkourModeVotingPhase = false;
        this.isRunnerVotingPhase = false;
        removeMafiaDaggers(server);
        if (phase == GamePhase.HIDING && customHidingDuration > 0) {
            this.phaseTimeRemaining = customHidingDuration;
        } else if (phase == GamePhase.MINIGAME && customMinigameDuration > 0) {
            this.phaseTimeRemaining = customMinigameDuration;
        } else if (phase == GamePhase.SEARCH && customSearchDuration > 0) {
            this.phaseTimeRemaining = customSearchDuration;
        } else if (phase == GamePhase.DISCUSSION && customDiscussionDuration > 0) {
            this.phaseTimeRemaining = customDiscussionDuration;
        } else if (phase == GamePhase.VOTING && customVotingDuration > 0) {
            this.phaseTimeRemaining = customVotingDuration;
        } else {
            this.phaseTimeRemaining = phase.getDefaultDurationSeconds();
        }
        this.votes.clear();
        this.discussionSkipVotes.clear();
        this.mafiaTarget = null;
        this.doctorTarget = null;

        // Play phase transition sounds
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID())) {
                switch (phase) {
                    case MINIGAME -> p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 1.0f);
                    case SEARCH -> p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.MASTER, 0.8f, 1.2f);
                    case DISCUSSION -> p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.MASTER, 1.0f, 1.0f);
                    case VOTING -> p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.MASTER, 1.0f, 1.2f);
                    case NIGHT -> p.level().playSound(null, p.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.MASTER, 1.0f, 0.6f);
                }
            }
        }

        // Broadcast phase change with title
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID())) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
                p.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(phase.getDisplayName()).withStyle(phase.getColor(), ChatFormatting.BOLD)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal(getPhaseHint(phase)).withStyle(ChatFormatting.GRAY)));
            }
        }

        // Notify Simple Voice Chat Integration
        com.corazon.corazonmod.integration.VoiceChatIntegration.onPhaseChange(server, phase);

        // Handle Phase-specific behaviors & effects
        if (phase == GamePhase.MINIGAME) {
            this.minigameScore = 0;
            this.isParkourFinished = false;

            var parkourStart = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getParkourStart();
            ServerLevel dontLieLevel = server.getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);

            if (parkourGroupMode) {
                // Mode Bareng-bareng (Semua Pemain Bertanding)
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                        p.removeEffect(MobEffects.BLINDNESS);
                        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        p.removeEffect(MobEffects.MOVEMENT_SPEED);

                        ServerLevel level = dontLieLevel != null ? dontLieLevel : (ServerLevel) p.level();
                        p.teleportTo(level, parkourStart.x, parkourStart.y, parkourStart.z, parkourStart.yaw, parkourStart.pitch);
                        p.connection.teleport(parkourStart.x, parkourStart.y, parkourStart.z, parkourStart.yaw, parkourStart.pitch);

                        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 10));
                        p.connection.send(new ClientboundSetTitleTextPacket(
                            Component.literal("🏃 PARKOUR BARENG-BARENG!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
                        p.connection.send(new ClientboundSetSubtitleTextPacket(
                            Component.literal("Lari ke Garis Finish Parkour secepatnya! Siapa saja yang finish berikan bonus waktu!").withStyle(ChatFormatting.YELLOW)));
                    }
                }

                broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.LIGHT_PURPLE));
                broadcastToAll(server, Component.literal("🎯 FASE PARKOUR MINIGAME (BARENG-BARENG - 2 MENIT) DIMULAI!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                broadcastToAll(server, Component.literal("🏃 Semua pemain berlari bersama di Arena Parkour!").withStyle(ChatFormatting.YELLOW));
                broadcastToAll(server, Component.literal("🎁 Capai garis Finish dalam 2 menit = BONUS +3 MENIT Waktu Pencarian Uang!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                // Mode Perwakilan (1 Runner Utama)
                if (parkourRunnerUUID == null || !isAlive(parkourRunnerUUID) || server.getPlayerList().getPlayer(parkourRunnerUUID) == null) {
                    for (UUID uuid : alivePlayers) {
                        if (server.getPlayerList().getPlayer(uuid) != null) {
                            this.parkourRunnerUUID = uuid;
                            break;
                        }
                    }
                }

                ServerPlayer runner = (parkourRunnerUUID != null) ? server.getPlayerList().getPlayer(parkourRunnerUUID) : null;
                String runnerName = runner != null ? runner.getScoreboardName() : "Perwakilan Pemain";

                if (runner != null) {
                    ServerLevel level = dontLieLevel != null ? dontLieLevel : (ServerLevel) runner.level();
                    runner.teleportTo(level, parkourStart.x, parkourStart.y, parkourStart.z, parkourStart.yaw, parkourStart.pitch);
                    runner.connection.teleport(parkourStart.x, parkourStart.y, parkourStart.z, parkourStart.yaw, parkourStart.pitch);
                }

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                        p.removeEffect(MobEffects.BLINDNESS);
                        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        p.removeEffect(MobEffects.MOVEMENT_SPEED);

                        if (runner != null && p.getUUID().equals(runner.getUUID())) {
                            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 10));
                            p.connection.send(new ClientboundSetTitleTextPacket(
                                Component.literal("🏃 KAMU PARKOUR RUNNER!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
                            p.connection.send(new ClientboundSetSubtitleTextPacket(
                                Component.literal("Lari ke Garis Finish Parkour secepatnya untuk bonus waktu!").withStyle(ChatFormatting.YELLOW)));
                        } else {
                            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 10));
                            p.connection.send(new ClientboundSetTitleTextPacket(
                                Component.literal("🎯 PARKOUR MINIGAME!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
                            p.connection.send(new ClientboundSetSubtitleTextPacket(
                                Component.literal("🏃 " + runnerName + " sedang berlari di Arena Parkour! Berikan dukungan!").withStyle(ChatFormatting.YELLOW)));
                        }
                    }
                }

                broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.LIGHT_PURPLE));
                broadcastToAll(server, Component.literal("🎯 FASE PARKOUR MINIGAME (PERWAKILAN - 2 MENIT) DIMULAI!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                broadcastToAll(server, Component.literal("🏃 Runner: " + runnerName + " | Capai garis Finish dalam 2 menit!").withStyle(ChatFormatting.YELLOW));
                broadcastToAll(server, Component.literal("🎁 Berhasil Finish = BONUS +3 MENIT Waktu Pencarian Uang!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else if (phase == GamePhase.HIDING) {
            hidingStartPositions.clear();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                    hidingStartPositions.put(p.getUUID(), new PlayerPos(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()));
                    PlayerRole role = getRole(p.getUUID());
                    if (role != PlayerRole.MAFIA) {
                        // Citizens/Doctor/Police close their eyes (Blindness) & frozen in place
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, phase.getDefaultDurationSeconds() * 20, 2, false, false));
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, phase.getDefaultDurationSeconds() * 20, 255, false, false));
                    } else {
                        // Mafia gets Speed boost to roam around and hide money pouch
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, phase.getDefaultDurationSeconds() * 20, 1, false, false));
                    }
                }
            }
        } else if (phase == GamePhase.NIGHT) {
            this.nightSubPhase = 0;
            startNightSubPhase(server);
        } else if (phase == GamePhase.DISCUSSION) {
            // Teleport all alive players to their seats at Discussion table & freeze them in place
            List<com.corazon.corazonmod.config.ArenaConfigManager.PosData> seats = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getDiscussionSeats();
            ServerLevel dontLieLevel = server.getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);
            
            int seatIndex = 0;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.MOVEMENT_SPEED);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, phaseTimeRemaining * 20, 255, false, false));

                    if (dontLieLevel != null && !seats.isEmpty()) {
                        com.corazon.corazonmod.config.ArenaConfigManager.PosData seat = seats.get(seatIndex % seats.size());
                        p.teleportTo(dontLieLevel, seat.x, seat.y, seat.z, seat.yaw, seat.pitch);
                        seatIndex++;
                    }
                }
            }
        } else if (phase == GamePhase.VOTING) {
            // Freeze players in place & Open Voting GUI for all alive players
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.MOVEMENT_SPEED);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, phaseTimeRemaining * 20, 255, false, false));

                    List<OpenVotingScreenPacket.PlayerEntry> targets = getAlivePlayerEntries(server, p.getUUID(), true);
                    ModMessages.sendToPlayer(new OpenVotingScreenPacket(targets), p);
                }
            }
        } else if (phase == GamePhase.SEARCH) {
            // Teleport ALL alive players to the Discussion Seats / Starting Point, and unfreeze them so everyone starts searching from the same spot!
            List<com.corazon.corazonmod.config.ArenaConfigManager.PosData> seats = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getDiscussionSeats();
            ServerLevel dontLieLevel = server.getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);

            int seatIndex = 0;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    p.removeEffect(MobEffects.MOVEMENT_SPEED);
                    p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

                    if (dontLieLevel != null && !seats.isEmpty()) {
                        com.corazon.corazonmod.config.ArenaConfigManager.PosData seat = seats.get(seatIndex % seats.size());
                        p.teleportTo(dontLieLevel, seat.x, seat.y, seat.z, seat.yaw, seat.pitch);
                        p.connection.teleport(seat.x, seat.y, seat.z, seat.yaw, seat.pitch);
                        seatIndex++;
                    }
                }
            }
        } else {
            // Remove night/hiding effects for other phases
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.removeEffect(MobEffects.BLINDNESS);
                p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                p.removeEffect(MobEffects.MOVEMENT_SPEED);
            }
        }

        // Sync updated game state & phase to all client HUDs immediately
        syncGameStateToAll(server);
    }

    public static class PlayerPos {
        public double x, y, z;
        public float yaw, pitch;
        public PlayerPos(double x, double y, double z, float yaw, float pitch) {
            this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
    }

    private final Map<UUID, PlayerPos> hidingStartPositions = new HashMap<>();

    public void returnPlayerToStartPos(ServerPlayer player) {
        if (player == null) return;
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        PlayerPos startPos = hidingStartPositions.get(player.getUUID());
        if (startPos != null) {
            ServerLevel level = (ServerLevel) player.level();
            player.teleportTo(level, startPos.x, startPos.y, startPos.z, startPos.yaw, startPos.pitch);
            player.connection.teleport(startPos.x, startPos.y, startPos.z, startPos.yaw, startPos.pitch);
            player.sendSystemMessage(Component.literal("↩️ Kamu telah dikembalikan ke posisi awal agar tidak dicurigai pemain lain!").withStyle(ChatFormatting.GREEN));
        } else {
            // Fallback to arena spawn position if startPos was not recorded
            com.corazon.corazonmod.config.ArenaConfigManager.PosData spawn = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getMainSpawn();
            ServerLevel dontLieLevel = player.getServer() != null ? player.getServer().getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY) : null;
            ServerLevel targetLevel = dontLieLevel != null ? dontLieLevel : (ServerLevel) player.level();

            player.teleportTo(targetLevel, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch);
            player.connection.teleport(spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch);
            player.sendSystemMessage(Component.literal("↩️ Kamu telah dikembalikan ke posisi spawn arena agar tidak dicurigai!").withStyle(ChatFormatting.GREEN));
        }
    }

    private int nightSubPhase = 0; // 0: Mafia, 1: Doctor, 2: Police

    public int getNightSubPhase() {
        return nightSubPhase;
    }

    public void advanceNightTurn(MinecraftServer server) {
        if (currentPhase != GamePhase.NIGHT) return;

        // Return current active role player back to starting position immediately after making choice
        PlayerRole activeRole = switch (nightSubPhase) {
            case 0 -> PlayerRole.MAFIA;
            case 1 -> PlayerRole.DOCTOR;
            case 2 -> PlayerRole.POLICE;
            default -> PlayerRole.MAFIA;
        };

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID()) && isAlive(p.getUUID()) && getRole(p.getUUID()) == activeRole) {
                returnPlayerToStartPos(p);
            }
        }

        if (nightSubPhase < 2) {
            nightSubPhase++;
            startNightSubPhase(server);
        } else {
            nightSubPhase = 0;
            resolveNightActions(server);
            if (checkWinConditions(server)) return;
            setPhase(server, GamePhase.SEARCH);
        }
    }

    public void startNightSubPhase(MinecraftServer server) {
        removeMafiaDaggers(server);
        this.phaseTimeRemaining = 15;

        PlayerRole activeRole = switch (nightSubPhase) {
            case 0 -> PlayerRole.MAFIA;
            case 1 -> PlayerRole.DOCTOR;
            case 2 -> PlayerRole.POLICE;
            default -> PlayerRole.MAFIA;
        };

        String titleText = switch (nightSubPhase) {
            case 0 -> "🔪 MALAM HARI: MAFIA TURN";
            case 1 -> "🛡️ MALAM HARI: DOCTOR TURN";
            case 2 -> "🔍 MALAM HARI: POLICE TURN";
            default -> "🌙 MALAM HARI";
        };

        String subtitleText = switch (nightSubPhase) {
            case 0 -> "Mafia tentukan target eksekusi...";
            case 1 -> "Doctor pilih pemain yang dilindungi...";
            case 2 -> "Police selidiki identitas pemain...";
            default -> "";
        };

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID())) {
                PlayerRole role = getRole(p.getUUID());
                p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 5));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(titleText).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitleText).withStyle(ChatFormatting.GRAY)));

                if (isAlive(p.getUUID())) {
                    if (role == activeRole) {
                        // Eyes open & FREE MOVEMENT for active turn role
                        p.removeEffect(MobEffects.BLINDNESS);
                        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                        if (role == PlayerRole.MAFIA) {
                            p.getInventory().add(new ItemStack(ModItems.MAFIA_DAGGER.get()));
                            p.sendSystemMessage(Component.literal("🔪 [MALAM HARI] Dagger Mafia diberikan! Jalan & pukul/klik pemain yang ingin kamu eksekusi!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                        } else if (role == PlayerRole.DOCTOR) {
                            p.sendSystemMessage(Component.literal("🛡️ [MALAM HARI] Giliran Doctor! Jalan & pukul/klik pemain (atau klik kanan untuk diri sendiri) untuk melindungi!").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
                        } else if (role == PlayerRole.POLICE) {
                            p.sendSystemMessage(Component.literal("🔍 [MALAM HARI] Giliran Police! Jalan & pukul/klik pemain untuk menyelidiki identitasnya!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                        }
                    } else {
                        // Eyes closed & frozen for non-active roles
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 15 * 20, 1, false, false));
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15 * 20, 255, false, false));
                    }
                }
            }
        }
    }

    public void tick(MinecraftServer server) {
        if (!isGameRunning()) return;

        tickCounter++;

        if (isParkourModeVotingPhase) {
            if (tickCounter % 20 == 0) {
                if (phaseTimeRemaining > 0) {
                    phaseTimeRemaining--;
                }
                if (phaseTimeRemaining <= 0) {
                    resolveParkourModeVoting(server);
                    return;
                }
            }
            hudSyncCounter++;
            if (hudSyncCounter >= 10) {
                hudSyncCounter = 0;
                syncGameStateToAll(server);
            }
            return;
        }

        if (isRunnerVotingPhase) {
            if (tickCounter % 20 == 0) {
                if (phaseTimeRemaining > 0) {
                    phaseTimeRemaining--;
                }
                if (phaseTimeRemaining <= 0) {
                    resolveRunnerVoting(server);
                    return;
                }
            }
            hudSyncCounter++;
            if (hudSyncCounter >= 10) {
                hudSyncCounter = 0;
                syncGameStateToAll(server);
            }
            return;
        }

        // Parkour Fall Y-Level Detection during MINIGAME Phase
        if (currentPhase == GamePhase.MINIGAME && !isParkourFinished) {
            var start = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getParkourStart();
            double dropLimitY = start.y - 7.0;

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                    if (parkourGroupMode || (parkourRunnerUUID != null && p.getUUID().equals(parkourRunnerUUID))) {
                        if (p.getY() < dropLimitY) {
                            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                            p.fallDistance = 0.0f;
                            p.setHealth(p.getMaxHealth());

                            ServerLevel dontLieLevel = server.getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);
                            ServerLevel level = dontLieLevel != null ? dontLieLevel : (ServerLevel) p.level();
                            p.teleportTo(level, start.x, start.y, start.z, start.yaw, start.pitch);
                            p.connection.teleport(start.x, start.y, start.z, start.yaw, start.pitch);

                            p.sendSystemMessage(Component.literal("⚠️ Kamu jatuh dari Parkour! Reset ke Garis Start.").withStyle(ChatFormatting.YELLOW));
                            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.MASTER, 0.6f, 1.2f);
                        }
                    }
                }
            }
        }
        if (currentPhase == GamePhase.MINIGAME && !isParkourFinished) {
            var finish = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getParkourFinish();
            if (parkourGroupMode) {
                // Check all alive players in Bareng-bareng mode
                for (UUID uuid : alivePlayers) {
                    ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                    if (p != null) {
                        double dx = p.getX() - finish.x;
                        double dy = p.getY() - finish.y;
                        double dz = p.getZ() - finish.z;
                        if ((dx * dx + dy * dy + dz * dz) <= 4.0) {
                            finishParkour(server, p);
                            break;
                        }
                    }
                }
            } else if (parkourRunnerUUID != null) {
                // Check designated runner in Perwakilan mode
                ServerPlayer runner = server.getPlayerList().getPlayer(parkourRunnerUUID);
                if (runner != null && isAlive(runner.getUUID())) {
                    double dx = runner.getX() - finish.x;
                    double dy = runner.getY() - finish.y;
                    double dz = runner.getZ() - finish.z;
                    if ((dx * dx + dy * dy + dz * dz) <= 4.0) {
                        finishParkour(server, runner);
                    }
                }
            }
        }

        // Sync game state to all clients every 10 ticks (0.5 sec) for HUD overlay
        hudSyncCounter++;
        if (hudSyncCounter >= 10) {
            hudSyncCounter = 0;
            syncGameStateToAll(server);
        }

        if (tickCounter % 20 == 0) { // Every 1 second
            if (phaseTimeRemaining > 0) {
                phaseTimeRemaining--;
            }

            // Sound warnings for low time
            if (phaseTimeRemaining == 10 || phaseTimeRemaining == 5 || phaseTimeRemaining == 3 || phaseTimeRemaining == 2 || phaseTimeRemaining == 1) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (isParticipant(p.getUUID())) {
                        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 0.8f, 1.5f);
                    }
                }
            }



            // Phase Timer Expired
            if (phaseTimeRemaining <= 0) {
                advancePhase(server);
            }
        }
    }

    public void finishParkour(MinecraftServer server, ServerPlayer runner) {
        if (isParkourFinished) return;
        isParkourFinished = true;

        int timeTaken = customMinigameDuration - phaseTimeRemaining;
        if (timeTaken <= 0) timeTaken = 1;

        int extraSeconds = 180; // +3 minutes (180 seconds) bonus search time!
        String modeName = parkourGroupMode ? "BARENG-BARENG" : "PERWAKILAN";

        broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.GOLD));
        broadcastToAll(server, Component.literal("🎉 PARKOUR BERHASIL DISLESAIKAN! 🎉").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        broadcastToAll(server, Component.literal("🏃 Runner: " + runner.getScoreboardName() + " (" + modeName + ")").withStyle(ChatFormatting.YELLOW));
        broadcastToAll(server, Component.literal("⏱️ Waktu Tempuh: " + timeTaken + " detik (Target: < 2 Menit)").withStyle(ChatFormatting.AQUA));
        broadcastToAll(server, Component.literal("🎁 BONUS WAKTU PENCARIAN: +3 MENIT (+180 Detik)!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.GOLD));

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID())) {
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
                if (parkourGroupMode && isAlive(p.getUUID())) {
                    returnPlayerToStartPos(p);
                }
            }
        }

        if (!parkourGroupMode) {
            returnPlayerToStartPos(runner);
        }

        setPhase(server, GamePhase.SEARCH);
        addTime(extraSeconds);
    }

    private void syncGameStateToAll(MinecraftServer server) {
        String phaseDisplayName = currentPhase.getDisplayName();
        if (isParkourModeVotingPhase) {
            phaseDisplayName = "Vote Mode Parkour";
        } else if (isRunnerVotingPhase) {
            phaseDisplayName = "Vote Parkour Runner";
        } else if (currentPhase == GamePhase.NIGHT) {
            phaseDisplayName = switch (nightSubPhase) {
                case 0 -> "Night: Mafia Turn";
                case 1 -> "Night: Doctor Turn";
                case 2 -> "Night: Police Turn";
                default -> "Night Elimination";
            };
        }

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID())) {
                PlayerRole role = getRole(p.getUUID());
                ModMessages.sendToPlayer(new GameStateUpdatePacket(
                    phaseDisplayName,
                    currentPhase.getColor().getColor(),
                    role.getDisplayName(),
                    role.getColor().getColor(),
                    phaseTimeRemaining,
                    getAliveCount(),
                    getTotalPlayers(),
                    isAlive(p.getUUID())
                ), p);
            }
        }
    }

    private List<PlayerEntry> getAlivePlayerEntries(MinecraftServer server, UUID selfUUID, boolean includeSelf) {
        List<PlayerEntry> entries = new ArrayList<>();
        for (UUID uuid : alivePlayers) {
            if (includeSelf || !uuid.equals(selfUUID)) {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p != null) {
                    String displayName = p.getScoreboardName();
                    if (uuid.equals(selfUUID)) {
                        displayName += " (Kamu)";
                    }
                    entries.add(new PlayerEntry(uuid, displayName));
                }
            }
        }
        return entries;
    }

    private String getPhaseHint(GamePhase phase) {
        return switch (phase) {
            case HIDING -> "Mafia sedang menyembunyikan uang di dalam arena!";
            case MINIGAME -> "MINIGAME PARKOUR! Capai Finish dalam 2 menit untuk +3 menit bonus waktu pencarian!";
            case SEARCH -> "CARI UANG! Periksa semua chest dan sudut ruangan!";
            case DISCUSSION -> "Kumpul di Meeting Room! Diskusi & Berinterogasi!";
            case VOTING -> "SAATNYA VOTING! Pilih pemain yang dicurigai sebagai Mafia!";
            case NIGHT -> "Malam tiba. Special roles beraksi secara bergantian...";
            default -> "";
        };
    }

    private void advancePhase(MinecraftServer server) {
        switch (currentPhase) {
            case HIDING -> {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (isParticipant(p.getUUID()) && getRole(p.getUUID()) == PlayerRole.MAFIA) {
                        returnPlayerToStartPos(p);
                    }
                }
                if (isParkourEnabled) {
                    startParkourModeVoting(server);
                } else {
                    broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.YELLOW));
                    broadcastToAll(server, Component.literal("ℹ️ Minigame Parkour [OFF]. Langsung menuju Fase Pencarian Uang!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                    broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.YELLOW));
                    setPhase(server, GamePhase.SEARCH);
                }
            }
            case MINIGAME -> {
                if (parkourGroupMode) {
                    for (UUID uuid : alivePlayers) {
                        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                        if (p != null) returnPlayerToStartPos(p);
                    }
                } else {
                    ServerPlayer runner = (parkourRunnerUUID != null) ? server.getPlayerList().getPlayer(parkourRunnerUUID) : null;
                    if (runner != null) {
                        returnPlayerToStartPos(runner);
                    }
                }

                broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.RED));
                broadcastToAll(server, Component.literal("  ⏰ WAKTU PARKOUR 2 MENIT HABIS!  ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                broadcastToAll(server, Component.literal("  ⚠️ " + (parkourGroupMode ? "Tidak ada pemain" : "Runner") + " yang berhasil mencapai garis Finish dalam 2 menit.  ").withStyle(ChatFormatting.YELLOW));
                broadcastToAll(server, Component.literal("  ❌ Tidak mendapatkan bonus waktu pencarian (0 bonus).  ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                broadcastToAll(server, Component.literal("========================================").withStyle(ChatFormatting.RED));

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (isParticipant(p.getUUID())) {
                        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.MASTER, 1.0f, 1.0f);
                    }
                }

                setPhase(server, GamePhase.SEARCH);
            }
            case SEARCH -> setPhase(server, GamePhase.DISCUSSION);
            case DISCUSSION -> setPhase(server, GamePhase.VOTING);
            case VOTING -> {
                resolveVoting(server);
                if (checkWinConditions(server)) return;
                setPhase(server, GamePhase.NIGHT);
            }
            case NIGHT -> {
                if (nightSubPhase < 2) {
                    nightSubPhase++;
                    startNightSubPhase(server);
                } else {
                    nightSubPhase = 0;
                    resolveNightActions(server);
                    if (checkWinConditions(server)) return;
                    setPhase(server, GamePhase.MINIGAME);
                }
            }
        }
    }

    public static final UUID ABSTAIN_UUID = new UUID(0L, 0L);

    public void votePlayer(ServerPlayer voter, UUID targetUUID) {
        if (currentPhase != GamePhase.VOTING) {
            voter.sendSystemMessage(Component.literal("Voting hanya bisa dilakukan pada fase Voting!").withStyle(ChatFormatting.RED));
            return;
        }
        if (!isAlive(voter.getUUID())) {
            voter.sendSystemMessage(Component.literal("Pemain yang gugur tidak bisa memilih.").withStyle(ChatFormatting.RED));
            return;
        }

        if (ABSTAIN_UUID.equals(targetUUID)) {
            votes.put(voter.getUUID(), ABSTAIN_UUID);
            voter.sendSystemMessage(Component.literal("✓ Kamu memilih SKIP VOTE (Golput).").withStyle(ChatFormatting.YELLOW));
            broadcastToAll(voter.server, Component.literal("🗳️ " + voter.getScoreboardName() + " memilih SKIP VOTE. (" + votes.size() + "/" + getAliveCount() + ")").withStyle(ChatFormatting.GRAY));
        } else {
            ServerPlayer target = voter.server.getPlayerList().getPlayer(targetUUID);
            if (target == null || !isAlive(target.getUUID())) {
                voter.sendSystemMessage(Component.literal("Pemain target tidak ditemukan atau sudah gugur.").withStyle(ChatFormatting.RED));
                return;
            }
            votes.put(voter.getUUID(), target.getUUID());
            voter.sendSystemMessage(Component.literal("✓ Kamu memilih " + target.getScoreboardName() + " untuk dieliminasi.").withStyle(ChatFormatting.GREEN));
            broadcastToAll(voter.server, Component.literal("🗳️ " + voter.getScoreboardName() + " telah memberikan suara. (" + votes.size() + "/" + getAliveCount() + ")").withStyle(ChatFormatting.YELLOW));
        }

        // Auto-finish voting if all alive players have voted!
        if (votes.size() >= getAliveCount()) {
            broadcastToAll(voter.server, Component.literal("🗳️ Semua pemain telah memberikan suara! Sisa timer Voting dilewati.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            resolveVoting(voter.server);
            if (!checkWinConditions(voter.server)) {
                setPhase(voter.server, GamePhase.NIGHT);
            }
        }
    }

    private void resolveVoting(MinecraftServer server) {
        if (votes.isEmpty()) {
            broadcastToAll(server, Component.literal("Tidak ada suara voting yang masuk. Tidak ada yang dieliminasi.").withStyle(ChatFormatting.GRAY));
            return;
        }

        Map<UUID, Integer> voteCounts = new HashMap<>();
        for (UUID targetUUID : votes.values()) {
            voteCounts.put(targetUUID, voteCounts.getOrDefault(targetUUID, 0) + 1);
        }

        UUID highestVoted = null;
        int maxVotes = 0;
        boolean tie = false;

        for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                highestVoted = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxVotes) {
                tie = true;
            }
        }

        if (tie || highestVoted == null || ABSTAIN_UUID.equals(highestVoted)) {
            if (ABSTAIN_UUID.equals(highestVoted) && !tie) {
                broadcastToAll(server, Component.literal("⚖️ Mayoritas pemain memilih SKIP VOTE! Tidak ada pemain yang dieliminasi.").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            } else {
                broadcastToAll(server, Component.literal("⚖️ Voting imbang! Tidak ada pemain yang dieliminasi.").withStyle(ChatFormatting.YELLOW));
            }
            // Play indecisive sound
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID())) {
                    p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.MASTER, 1.0f, 1.0f);
                }
            }
        } else {
            ServerPlayer eliminated = server.getPlayerList().getPlayer(highestVoted);
            if (eliminated != null) {
                alivePlayers.remove(highestVoted);
                dropPouchIfCarried(eliminated);
                eliminated.setGameMode(GameType.SPECTATOR);
                PlayerRole role = getRole(highestVoted);

                // Dramatic elimination title
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (isParticipant(p.getUUID())) {
                        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
                        p.connection.send(new ClientboundSetTitleTextPacket(
                            Component.literal("ELIMINASI!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
                        p.connection.send(new ClientboundSetSubtitleTextPacket(
                            Component.literal(eliminated.getScoreboardName() + " adalah " + role.getDisplayName()).withStyle(role.getColor())));
                        p.level().playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.MASTER, 0.5f, 1.0f);
                    }
                }

                broadcastToAll(server, Component.literal("🗳️ " + eliminated.getScoreboardName() + " telah dieliminasi! (Vote: " + maxVotes + ") | Role: " + role.getDisplayName()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        }
    }

    public void setMafiaTarget(ServerPlayer mafia, ServerPlayer target) {
        if (currentPhase != GamePhase.NIGHT) {
            mafia.sendSystemMessage(Component.literal("Eksekusi Mafia hanya bisa dilakukan di malam hari!").withStyle(ChatFormatting.RED));
            return;
        }
        if (getRole(mafia.getUUID()) != PlayerRole.MAFIA) {
            mafia.sendSystemMessage(Component.literal("Hanya Mafia yang bisa menentukan target eksekusi!").withStyle(ChatFormatting.RED));
            return;
        }
        mafiaTarget = target.getUUID();
        mafia.sendSystemMessage(Component.literal("🔪 Target eksekusi malam ini: " + target.getScoreboardName()).withStyle(ChatFormatting.RED));

        // Notify other mafias
        for (UUID uuid : alivePlayers) {
            if (getRole(uuid) == PlayerRole.MAFIA && !uuid.equals(mafia.getUUID())) {
                ServerPlayer otherMafia = mafia.server.getPlayerList().getPlayer(uuid);
                if (otherMafia != null) {
                    otherMafia.sendSystemMessage(Component.literal("🔪 " + mafia.getScoreboardName() + " memilih target: " + target.getScoreboardName()).withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    public void setDoctorTarget(ServerPlayer doctor, ServerPlayer target) {
        if (currentPhase != GamePhase.NIGHT) {
            doctor.sendSystemMessage(Component.literal("Penyembuhan Doctor hanya bisa dilakukan di malam hari!").withStyle(ChatFormatting.RED));
            return;
        }
        if (getRole(doctor.getUUID()) != PlayerRole.DOCTOR) {
            doctor.sendSystemMessage(Component.literal("Hanya Doctor yang bisa menyembuhkan!").withStyle(ChatFormatting.RED));
            return;
        }
        doctorTarget = target.getUUID();
        doctor.sendSystemMessage(Component.literal("🛡️ Kamu melindungi: " + target.getScoreboardName() + " malam ini.").withStyle(ChatFormatting.BLUE));
    }

    public void setPoliceCheck(ServerPlayer police, ServerPlayer target) {
        policeInspect(police, target);
    }

    public void policeInspect(ServerPlayer police, ServerPlayer target) {
        if (currentPhase != GamePhase.NIGHT) {
            police.sendSystemMessage(Component.literal("Pemeriksaan Police hanya bisa dilakukan di malam hari!").withStyle(ChatFormatting.RED));
            return;
        }
        if (getRole(police.getUUID()) != PlayerRole.POLICE) {
            police.sendSystemMessage(Component.literal("Hanya Police yang bisa melakukan pemeriksaan!").withStyle(ChatFormatting.RED));
            return;
        }
        PlayerRole targetRole = getRole(target.getUUID());
        boolean isMafia = (targetRole == PlayerRole.MAFIA);

        // Dramatic investigation result
        police.sendSystemMessage(Component.literal("════════════════════════").withStyle(ChatFormatting.GOLD));
        police.sendSystemMessage(Component.literal("🔍 HASIL INVESTIGASI").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        police.sendSystemMessage(Component.literal("Target: " + target.getScoreboardName()).withStyle(ChatFormatting.WHITE));
        if (isMafia) {
            police.sendSystemMessage(Component.literal("Status: 🚨 MAFIA! 🚨").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            police.level().playSound(null, police.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.MASTER, 0.5f, 1.5f);
        } else {
            police.sendSystemMessage(Component.literal("Status: ✅ BUKAN MAFIA (Warga)").withStyle(ChatFormatting.GREEN));
            police.level().playSound(null, police.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.5f, 1.2f);
        }
        police.sendSystemMessage(Component.literal("════════════════════════").withStyle(ChatFormatting.GOLD));
    }

    private void resolveNightActions(MinecraftServer server) {
        if (mafiaTarget != null) {
            if (mafiaTarget.equals(doctorTarget)) {
                broadcastToAll(server, Component.literal("🚑 Doctor berhasil menyelamatkan korban malam ini! Tidak ada yang gugur.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                // Play save sound
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (isParticipant(p.getUUID())) {
                        p.level().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.MASTER, 0.6f, 1.0f);
                    }
                }
            } else {
                ServerPlayer victim = server.getPlayerList().getPlayer(mafiaTarget);
                if (victim != null) {
                    alivePlayers.remove(mafiaTarget);
                    dropPouchIfCarried(victim);
                    victim.setGameMode(GameType.SPECTATOR);

                    // Dramatic death announcement with title
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        if (isParticipant(p.getUUID())) {
                            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
                            p.connection.send(new ClientboundSetTitleTextPacket(
                                Component.literal("💀 KORBAN MALAM").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
                            p.connection.send(new ClientboundSetSubtitleTextPacket(
                                Component.literal(victim.getScoreboardName() + " ditemukan gugur...").withStyle(ChatFormatting.RED)));
                            p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 0.3f, 0.8f);
                        }
                    }

                    broadcastToAll(server, Component.literal("💀 " + victim.getScoreboardName() + " ditemukan gugur di pagi hari!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                }
            }
        } else {
            broadcastToAll(server, Component.literal("🌅 Pagi hari tiba dengan tenang. Tidak ada korban jiwa semalam.").withStyle(ChatFormatting.GRAY));
        }
    }

    public void dropPouchIfCarried(ServerPlayer player) {
        if (player == null) return;
        boolean hadPouch = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.MONEY_POUCH.get())) {
                hadPouch = true;
                player.getInventory().removeItem(stack);
            }
        }

        if (hadPouch) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                List<com.corazon.corazonmod.config.ArenaConfigManager.TaskPosData> taskLocs = 
                    com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getData().taskLocations;
                List<com.corazon.corazonmod.config.ArenaConfigManager.PosData> seatLocs = 
                    com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getDiscussionSeats();
                
                ServerLevel dontLieLevel = server.getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);
                ServerLevel level = dontLieLevel != null ? dontLieLevel : (ServerLevel) player.level();

                // Collect valid, walkable arena coordinates
                List<net.minecraft.world.phys.Vec3> validPositions = new ArrayList<>();
                if (taskLocs != null && !taskLocs.isEmpty()) {
                    for (var t : taskLocs) validPositions.add(new net.minecraft.world.phys.Vec3(t.x, t.y, t.z));
                }
                if (seatLocs != null && !seatLocs.isEmpty()) {
                    for (var s : seatLocs) validPositions.add(new net.minecraft.world.phys.Vec3(s.x, s.y, s.z));
                }

                net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(player.getX(), player.getY(), player.getZ());
                if (!validPositions.isEmpty()) {
                    targetPos = validPositions.get(new Random().nextInt(validPositions.size()));
                }

                // Spawn physical 3D MoneyPouchEntity cleanly on the floor level
                com.corazon.corazonmod.entity.MoneyPouchEntity pouchEntity = new com.corazon.corazonmod.entity.MoneyPouchEntity(
                    com.corazon.corazonmod.init.ModEntities.MONEY_POUCH.get(), level
                );
                pouchEntity.moveTo(targetPos.x, targetPos.y + 0.1, targetPos.z, 0.0F, 0.0F);
                level.addFreshEntity(pouchEntity);

                broadcastToAll(
                    server,
                    Component.literal("💰 MONEY POUCH TERSEMBUNYI KEMBALI! " + player.getScoreboardName() + " gugur dan Pouch terselip di salah satu ruangan arena! Cari kembali saat Search Phase!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                );
            }
        }
    }

    private boolean checkWinConditions(MinecraftServer server) {
        int livingMafia = 0;
        int livingCitizens = 0;

        for (UUID uuid : alivePlayers) {
            if (getRole(uuid) == PlayerRole.MAFIA) {
                livingMafia++;
            } else {
                livingCitizens++;
            }
        }

        if (livingMafia == 0) {
            currentPhase = GamePhase.ENDED;
            removeMafiaDaggers(server);

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID())) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    p.setGameMode(GameType.SURVIVAL);
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
                    p.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("🎉 CITIZENS WIN! 🎉").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal("Semua Mafia berhasil dieliminasi!").withStyle(ChatFormatting.YELLOW)));
                    p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
                }
            }

            syncGameStateToAll(server);
            return true;
        }

        if (livingMafia >= livingCitizens) {
            currentPhase = GamePhase.ENDED;
            removeMafiaDaggers(server);

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID())) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    p.setGameMode(GameType.SURVIVAL);
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
                    p.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("🔪 MAFIA WIN! 🔪").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal("Mafia menguasai permainan!").withStyle(ChatFormatting.DARK_RED)));
                    p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.MASTER, 0.5f, 0.8f);
                }
            }

            syncGameStateToAll(server);
            return true;
        }

        return false;
    }

    public void broadcastToAll(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    public void removeMafiaDaggers(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack stack = p.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(ModItems.MAFIA_DAGGER.get())) {
                    p.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private BossEvent.BossBarColor getBossBarColor(GamePhase phase) {
        return switch (phase) {
            case HIDING -> BossEvent.BossBarColor.RED;
            case SEARCH -> BossEvent.BossBarColor.YELLOW;
            case VOTING -> BossEvent.BossBarColor.BLUE;
            case NIGHT -> BossEvent.BossBarColor.PURPLE;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }
}
