package com.pulsepass.pulsepass.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    // FR-TKT-006: Query Methods navegando Ticket -> User -> email.
    List<Ticket> findByUserEmailIgnoreCase(String email);

    List<Ticket> findByUserEmailIgnoreCaseAndStatus(String email, TicketStatus status);

    // FR-TKT-007: Query Method. Se eligió porque es un filtro simple sobre dos campos;
    // no hace falta JPQL para algo que el nombre del método expresa con claridad.
    List<Ticket> findByEvent_EventCodeAndStatus(String eventCode, TicketStatus status);

    // FR-TKT-008: JPQL con COUNT.
    @Query("""
            SELECT COUNT(t) FROM Ticket t
            WHERE t.event.eventCode = :eventCode AND t.status = :status
            """)
    long countByEventAndStatus(@Param("eventCode") String eventCode,
                               @Param("status") TicketStatus status);

    default long countPaidByEventCode(String eventCode) {
        return countByEventAndStatus(eventCode, TicketStatus.PAID);
    }

    // FR-SRC-004: tickets de eventos posteriores a una fecha, en orden cronológico.
    @Query("""
            SELECT t FROM Ticket t
            JOIN t.event e
            WHERE e.eventDate > :date
            ORDER BY e.eventDate ASC, t.ticketCode ASC
            """)
    List<Ticket> findForEventsAfter(@Param("date") LocalDateTime date);
}