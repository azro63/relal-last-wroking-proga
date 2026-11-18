package com.sue.overlay.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_snapshots", indexes = {
        @Index(name = "idx_clock_time", columnList = "clock_time"),
        @Index(name = "idx_captured_at", columnList = "captured_at")
})
public class GameSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Игровое время в секундах (clock_time из Dota 2 GSI). */
    @Column(name = "clock_time", nullable = false)
    private int clockTime;

    /** Полное игровое время (может быть отрицательным до крипов). */
    @Column(name = "game_time")
    private Integer gameTime;

    /** День/ночь в игре. */
    @Column(name = "is_daytime")
    private Boolean daytime;

    /** Победившая команда ("radiant" / "dire"), если известна. */
    @Column(name = "win_team", length = 16)
    private String winTeam;

    /** Когда снимок был сохранён. */
    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    // --- Конструкторы ---
    public GameSnapshot() {
    }

    public GameSnapshot(int clockTime, LocalDateTime capturedAt) {
        this.clockTime = clockTime;
        this.capturedAt = capturedAt;
    }

    // --- Геттеры / сеттеры ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getClockTime() { return clockTime; }
    public void setClockTime(int clockTime) { this.clockTime = clockTime; }

    public Integer getGameTime() { return gameTime; }
    public void setGameTime(Integer gameTime) { this.gameTime = gameTime; }

    public Boolean getDaytime() { return daytime; }
    public void setDaytime(Boolean daytime) { this.daytime = daytime; }

    public String getWinTeam() { return winTeam; }
    public void setWinTeam(String winTeam) { this.winTeam = winTeam; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }

    @Override
    public String toString() {
        return "GameSnapshot{id=" + id +
                ", clockTime=" + clockTime +
                ", daytime=" + daytime +
                ", capturedAt=" + capturedAt + '}';
    }
}