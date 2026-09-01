package com.corazon.corazonmod.game;

import com.corazon.corazonmod.init.ModItems;
import com.corazon.corazonmod.network.GameStateUpdatePacket;
import com.corazon.corazonmod.network.ModMessages;
import com.corazon.corazonmod.network.NightActionPacket;
import com.corazon.corazonmod.network.OpenAdminMenuPacket;
import com.corazon.corazonmod.network.OpenNightActionPacket;
import com.corazon.corazonmod.network.OpenRoleRevealPacket;
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

import java.util.*;

public class DontLieGame {
    private static final DontLieGame INSTANCE = new DontLieGame();

    private final Map<UUID, PlayerRole> playerRoles = new HashMap<>();
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Map<UUID, UUID> votes = new HashMap<>();

    private GamePhase currentPhase = GamePhase.LOBBY;
    private int phaseTimeRemaining = 0;
    private int tickCounter = 0;
    private int hudSyncCounter = 0;

    private int customHidingDuration = 60;
    private int customSearchDuration = 300;

    private UUID moneyHolder = null;
    private UUID mafiaTarget = null;
    private UUID doctorTarget = null;

    public void setCustomDurations(int hidingSeconds, int searchSeconds) {
        if (hidingSeconds > 0) this.customHidingDuration = hidingSeconds;
        if (searchSeconds > 0) this.customSearchDuration = searchSeconds;
    }

    public int getCustomHidingDuration() {
        return customHidingDuration;
    }

    public int getCustomSearchDuration() {
        return customSearchDuration;
    }

    private ServerBossEvent bossBar;
    private final Set<UUID> allPlayerUUIDs = new HashSet<>();

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

    public void startNewGame(MinecraftServer server, List<ServerPlayer> players) {
        if (players.size() < 2) {
            broadcastToAll(server, Component.literal("[Don't Lie] Minimal 2 pemain untuk memulai game!").withStyle(ChatFormatting.RED));
            return;
        }

        // Teleport all participants to Don't Lie dimension & discussion seats
        ServerLevel dontLieLevel = server.getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);
        List<com.corazon.corazonmod.config.ArenaConfigManager.PosData> seats = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getDiscussionSeats();
        if (dontLieLevel != null) {
            int seatIdx = 0;
            for (ServerPlayer p : players) {
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

        // Shuffle players and assign roles
        List<ServerPlayer> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        int mafiaCount = Math.max(1, Math.min(3, shuffled.size() / 3));
        int index = 0;

        // Assign Mafias
        for (int i = 0; i < mafiaCount; i++) {
            ServerPlayer p = shuffled.get(index++);
            playerRoles.put(p.getUUID(), PlayerRole.MAFIA);
            alivePlayers.add(p.getUUID());
            allPlayerUUIDs.add(p.getUUID());
        }

        // Assign Doctor
        if (index < shuffled.size()) {
            ServerPlayer p = shuffled.get(index++);
            playerRoles.put(p.getUUID(), PlayerRole.DOCTOR);
            alivePlayers.add(p.getUUID());
            allPlayerUUIDs.add(p.getUUID());
        }

        // Assign Police
        if (index < shuffled.size()) {
            ServerPlayer p = shuffled.get(index++);
            playerRoles.put(p.getUUID(), PlayerRole.POLICE);
            alivePlayers.add(p.getUUID());
            allPlayerUUIDs.add(p.getUUID());
        }

        // Assign Broker (if 7+ players)
        if (shuffled.size() >= 7 && index < shuffled.size()) {
            ServerPlayer p = shuffled.get(index++);
            playerRoles.put(p.getUUID(), PlayerRole.BROKER);
            alivePlayers.add(p.getUUID());
            allPlayerUUIDs.add(p.getUUID());
        }

        // Assign Citizens for the rest
        while (index < shuffled.size()) {
            ServerPlayer p = shuffled.get(index++);
            playerRoles.put(p.getUUID(), PlayerRole.CITIZEN);
            alivePlayers.add(p.getUUID());
            allPlayerUUIDs.add(p.getUUID());
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

        // Give Mafia Dagger to all Mafias
        for (ServerPlayer p : players) {
            if (getRole(p.getUUID()) == PlayerRole.MAFIA) {
                p.getInventory().add(new ItemStack(ModItems.MAFIA_DAGGER.get()));
            }
        }

        // Initialize BossBar
        if (bossBar != null) {
            bossBar.removeAllPlayers();
        }
        bossBar = new ServerBossEvent(Component.literal("GOING SEVENTEEN: DON'T LIE"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        for (ServerPlayer p : players) {
            bossBar.addPlayer(p);
        }

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

        // Start Hiding Phase with blindness for citizens and speed for mafia
        setPhase(server, GamePhase.HIDING);
    }

    public void stopGame(MinecraftServer server) {
        currentPhase = GamePhase.ENDED;
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar = null;
        }

        // Remove effects from all players
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.removeEffect(MobEffects.BLINDNESS);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }

        // Send game ended state to clients
        syncGameStateToAll(server);

        broadcastToAll(server, Component.literal("[Don't Lie] Game dihentikan oleh Admin.").withStyle(ChatFormatting.RED));
    }

    public void addTime(int seconds) {
        phaseTimeRemaining += seconds;
    }

    public void setPhase(MinecraftServer server, GamePhase phase) {
        this.currentPhase = phase;
        if (phase == GamePhase.HIDING && customHidingDuration > 0) {
            this.phaseTimeRemaining = customHidingDuration;
        } else if (phase == GamePhase.SEARCH && customSearchDuration > 0) {
            this.phaseTimeRemaining = customSearchDuration;
        } else {
            this.phaseTimeRemaining = phase.getDefaultDurationSeconds();
        }
        this.votes.clear();
        this.mafiaTarget = null;
        this.doctorTarget = null;

        if (bossBar != null) {
            bossBar.setColor(getBossBarColor(phase));
        }

        // Play phase transition sounds
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID())) {
                switch (phase) {
                    case SEARCH -> p.level().playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.MASTER, 0.8f, 1.2f);
                    case VOTING -> p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.MASTER, 1.0f, 1.0f);
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

        // Handle Hiding and Night Phase effects
        if (phase == GamePhase.HIDING) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
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
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                    PlayerRole role = getRole(p.getUUID());
                    if (role != PlayerRole.MAFIA) {
                        // Citizens get blindness
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, phase.getDefaultDurationSeconds() * 20, 1, false, false));
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, phase.getDefaultDurationSeconds() * 20, 2, false, false));
                    }

                    // *** Open Night Action GUI for special roles ***
                    switch (role) {
                        case MAFIA -> ModMessages.sendToPlayer(new OpenNightActionPacket("MAFIA_KILL", getAlivePlayerEntries(server, p.getUUID(), false)), p);
                        case DOCTOR -> ModMessages.sendToPlayer(new OpenNightActionPacket("DOCTOR_SAVE", getAlivePlayerEntries(server, p.getUUID(), true)), p);
                        case POLICE -> ModMessages.sendToPlayer(new OpenNightActionPacket("POLICE_CHECK", getAlivePlayerEntries(server, p.getUUID(), false)), p);
                    }
                }
            }
        } else if (phase == GamePhase.VOTING) {
            // Teleport all alive players to their respective seats at the Discussion/Meeting table
            List<com.corazon.corazonmod.config.ArenaConfigManager.PosData> seats = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getDiscussionSeats();
            ServerLevel dontLieLevel = server.getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);
            
            int seatIndex = 0;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                    if (dontLieLevel != null && !seats.isEmpty()) {
                        com.corazon.corazonmod.config.ArenaConfigManager.PosData seat = seats.get(seatIndex % seats.size());
                        p.teleportTo(dontLieLevel, seat.x, seat.y, seat.z, seat.yaw, seat.pitch);
                        seatIndex++;
                    }
                    // *** Open Voting GUI for all alive players (including self-vote option) ***
                    List<OpenVotingScreenPacket.PlayerEntry> targets = getAlivePlayerEntries(server, p.getUUID(), true);
                    ModMessages.sendToPlayer(new OpenVotingScreenPacket(targets), p);
                }
            }
        } else if (phase == GamePhase.SEARCH) {
            // Teleport Mafia and all alive players back to their seats at the Discussion table
            List<com.corazon.corazonmod.config.ArenaConfigManager.PosData> seats = com.corazon.corazonmod.config.ArenaConfigManager.getInstance().getDiscussionSeats();
            ServerLevel dontLieLevel = server.getLevel(ArenaBuilder.DONTLIE_DIMENSION_KEY);

            int seatIndex = 0;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID()) && isAlive(p.getUUID())) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    p.removeEffect(MobEffects.MOVEMENT_SPEED);

                    if (dontLieLevel != null && !seats.isEmpty()) {
                        com.corazon.corazonmod.config.ArenaConfigManager.PosData seat = seats.get(seatIndex % seats.size());
                        p.teleportTo(dontLieLevel, seat.x, seat.y, seat.z, seat.yaw, seat.pitch);
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
    }

    public void tick(MinecraftServer server) {
        if (!isGameRunning()) return;

        tickCounter++;

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

            // Update BossBar
            if (bossBar != null) {
                float progress = Math.max(0.0f, Math.min(1.0f, (float) phaseTimeRemaining / Math.max(1, currentPhase.getDefaultDurationSeconds())));
                bossBar.setProgress(progress);
                bossBar.setName(Component.literal("[" + currentPhase.getDisplayName() + "] Sisa Waktu: " + formatTime(phaseTimeRemaining)));
            }

            // Phase Timer Expired
            if (phaseTimeRemaining <= 0) {
                advancePhase(server);
            }
        }
    }

    private void syncGameStateToAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (isParticipant(p.getUUID())) {
                PlayerRole role = getRole(p.getUUID());
                ModMessages.sendToPlayer(new GameStateUpdatePacket(
                    currentPhase.getDisplayName(),
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
            case SEARCH -> "CARI UANG! Periksa semua chest dan sudut ruangan!";
            case VOTING -> "Kumpul di Meeting Room! Diskusi dan voting Mafia!";
            case NIGHT -> "Malam tiba. Mafia, Doctor, dan Police beraksi...";
            default -> "";
        };
    }

    private void advancePhase(MinecraftServer server) {
        switch (currentPhase) {
            case HIDING -> setPhase(server, GamePhase.SEARCH);
            case SEARCH -> setPhase(server, GamePhase.VOTING);
            case VOTING -> {
                resolveVoting(server);
                if (checkWinConditions(server)) return;
                setPhase(server, GamePhase.NIGHT);
            }
            case NIGHT -> {
                resolveNightActions(server);
                if (checkWinConditions(server)) return;
                setPhase(server, GamePhase.SEARCH);
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
            if (bossBar != null) {
                bossBar.removeAllPlayers();
                bossBar = null;
            }

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID())) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
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
            if (bossBar != null) {
                bossBar.removeAllPlayers();
                bossBar = null;
            }

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (isParticipant(p.getUUID())) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
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
