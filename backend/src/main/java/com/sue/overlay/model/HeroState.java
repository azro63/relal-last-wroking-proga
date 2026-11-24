package com.sue.overlay.model;

public record HeroState(
        String name,
        String heroName, // например, "npc_dota_hero_axe"
        int level,
        int hpPercent,
        int manaPercent,
        boolean alive,
        int respawnTimer
) {}