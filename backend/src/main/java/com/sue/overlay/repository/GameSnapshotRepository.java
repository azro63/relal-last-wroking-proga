package com.sue.overlay.repository;

import com.sue.overlay.persistence.entity.GameSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameSnapshotRepository extends JpaRepository<GameSnapshot, Long> {

    // ================================================================
    // 1. ПРОИЗВОДНЫЕ МЕТОДЫ (Derived Queries)
    // ================================================================

    /** Снимки с игровым временем больше заданного (напр. > 600 сек). */
    List<GameSnapshot> findByClockTimeGreaterThan(int clockTime);

    /** Снимки за указанный временной промежуток. */
    List<GameSnapshot> findByCapturedAtBetween(LocalDateTime from, LocalDateTime to);

    /** Только ночные снимки. */
    List<GameSnapshot> findByDaytimeIsFalse();

    /** Снимки по победившей команде. */
    List<GameSnapshot> findByWinTeam(String winTeam);

    // ================================================================
    // 2. @Query — JPQL
    // ================================================================

    /** Последние N снимков (передавайте PageRequest.of(0, N)). */
    @Query("SELECT gs FROM GameSnapshot gs ORDER BY gs.capturedAt DESC")
    List<GameSnapshot> findLatestSnapshots(Pageable pageable);

    /** Среднее игровое время для победившей команды. */
    @Query("SELECT AVG(gs.clockTime) FROM GameSnapshot gs WHERE gs.winTeam = :winTeam")
    Double findAverageClockTimeByWinTeam(@Param("winTeam") String winTeam);

    /** Снимки с игровым временем > minClockTime. */
    @Query("SELECT gs FROM GameSnapshot gs WHERE gs.clockTime > :minClockTime ORDER BY gs.clockTime")
    List<GameSnapshot> findSnapshotsAfterClockTime(@Param("minClockTime") int minClockTime);

    /** Максимальное игровое время (последний известный тик). */
    @Query("SELECT MAX(gs.clockTime) FROM GameSnapshot gs")
    Integer findMaxClockTime();

    // ================================================================
    // 3. Нативные SQL-запросы
    // ================================================================

    /** Все снимки по победившей команде через прямой SQL. */
    @Query(value = "SELECT * FROM game_snapshots WHERE win_team = ?1 ORDER BY captured_at DESC",
            nativeQuery = true)
    List<GameSnapshot> findSnapshotsByWinTeamNative(String winTeam);
}