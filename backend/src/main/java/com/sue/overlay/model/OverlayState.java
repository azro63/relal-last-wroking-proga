package com.sue.overlay.model;

import java.util.List;

public record OverlayState(
        int gameTime,
        boolean daytime,
        int radiantScore,
        int direScore,
        double radiantEconomy,
        double direEconomy,
        List<HeroState> radiantHeroes,
        List<HeroState> direHeroes
) {}