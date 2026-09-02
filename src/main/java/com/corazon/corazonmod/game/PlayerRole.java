package com.corazon.corazonmod.game;

import net.minecraft.ChatFormatting;

public enum PlayerRole {
    CITIZEN("Citizen", ChatFormatting.GREEN, "Cari uang dan temukan Mafia melalui voting!"),
    MAFIA("Mafia", ChatFormatting.RED, "Sembunyikan uang dan habisi para Citizen di malam hari!"),
    DOCTOR("Doctor", ChatFormatting.BLUE, "Selamatkan 1 pemain dari serangan Mafia setiap malam!"),
    POLICE("Police", ChatFormatting.GOLD, "Periksa peran 1 pemain setiap malam!");

    private final String displayName;
    private final ChatFormatting color;
    private final String description;

    PlayerRole(String displayName, ChatFormatting color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }
}
