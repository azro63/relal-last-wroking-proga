package com.sue.overlay.repository;

import com.sue.overlay.persistence.entity.GameEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

    List<GameEvent> findByEventTypeOrderByGameTimeDesc(String eventType, Pageable pageable);

    @Query("SELECT e FROM GameEvent e ORDER BY e.id DESC")
    List<GameEvent> findLatest(Pageable pageable);

    @Query("SELECT e FROM GameEvent e WHERE e.gameTime > :gameTime ORDER BY e.gameTime")
    List<GameEvent> findAfterGameTime(@Param("gameTime") int gameTime);

    @Query("SELECT DISTINCT e.eventType FROM GameEvent e ORDER BY e.eventType")
    List<String> findAllEventTypes();

    @Query("SELECT e.eventType, COUNT(e) FROM GameEvent e GROUP BY e.eventType ORDER BY COUNT(e) DESC")
    List<Object[]> countByEventType();
}