package com.sue.overlay.model;

import java.time.LocalDateTime;

/** payload — сырой JSON события в виде строки. */
public record GameEvent(
        Long id,
        String eventType,
        int gameTime,
        String payload,
        LocalDateTime capturedAt) {
}
