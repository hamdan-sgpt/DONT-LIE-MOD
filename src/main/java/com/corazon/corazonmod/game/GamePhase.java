package com.corazon.corazonmod.game;

import net.minecraft.ChatFormatting;

public enum GamePhase {
    LOBBY("Lobby", ChatFormatting.GRAY, 0),
    HIDING("Money Hiding Phase", ChatFormatting.RED, 60),
    MINIGAME("Minigame Extra Time", ChatFormatting.LIGHT_PURPLE, 120),
    SEARCH("Treasure Hunt Phase", ChatFormatting.YELLOW, 300),
    DISCUSSION("Discussion Phase", ChatFormatting.AQUA, 90),
    VOTING("Voting Phase", ChatFormatting.GOLD, 30),
    NIGHT("Night Elimination", ChatFormatting.DARK_PURPLE, 45),
    ENDED("Game Ended", ChatFormatting.GOLD, 0);

    private final String displayName;
    private final ChatFormatting color;
    private final int defaultDurationSeconds;

    GamePhase(String displayName, ChatFormatting color, int defaultDurationSeconds) {
        this.displayName = displayName;
        this.color = color;
        this.defaultDurationSeconds = defaultDurationSeconds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public int getDefaultDurationSeconds() {
        return defaultDurationSeconds;
    }
}
