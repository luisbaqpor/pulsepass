package com.pulsepass.pulsepass.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventCode(String eventCode);

    // FR-EVT-005: cartelera. Query Method: filtro simple + orden.
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // FR-VEN-004: Query Method navegando la relación venue.code.
    List<Event> findByVenueCode(String venueCode);

    // FR-SRC-001 / FR-ART-004: JPQL con JOIN a la relación N:M. DISTINCT evita duplicados.
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.artists a
            WHERE a.stageName = :stageName
            """)
    List<Event> findEventsByArtist(@Param("stageName") String stageName);

    // FR-SRC-002: filtra por venue.city y artist.stageName.
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE v.city = :city AND a.stageName = :stageName
            """)
    List<Event> findEventsByCityAndArtist(@Param("city") String city,
                                          @Param("stageName") String stageName);

    // FR-SRC-003: publicados, posteriores a una fecha, en una ciudad, artista que contenga texto.
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE e.status = :status
              AND e.eventDate > :after
              AND v.city = :city
              AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> searchRecommended(@Param("status") EventStatus status,
                                  @Param("after") LocalDateTime after,
                                  @Param("city") String city,
                                  @Param("artistText") String artistText);

    default List<Event> findRecommended(LocalDateTime after, String city, String artistText) {
        return searchRecommended(EventStatus.PUBLISHED, after, city, artistText);
    }
}