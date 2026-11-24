package com.sue.overlay.model;

import java.time.LocalDateTime;

public record GameSnapshot(
        Long id,
        int clockTime,
        Integer gameTime,
        Boolean daytime,
        String winTeam,
        LocalDateTime capturedAt) {
}
