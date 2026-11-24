package com.sue.overlay.model;

public record HeroState(
        String name,
        int level,
        int hpPercent,
        int manaPercent,
        boolean alive,
        int respawnTimer
) {}