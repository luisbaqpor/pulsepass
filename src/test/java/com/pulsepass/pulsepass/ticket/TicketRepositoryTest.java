package com.pulsepass.pulsepass.ticket;

import com.pulsepass.pulsepass.TestcontainersConfiguration;
import com.pulsepass.pulsepass.event.Event;
import com.pulsepass.pulsepass.event.EventCategory;
import com.pulsepass.pulsepass.event.EventRepository;
import com.pulsepass.pulsepass.event.EventStatus;
import com.pulsepass.pulsepass.user.User;
import com.pulsepass.pulsepass.user.UserRepository;
import com.pulsepass.pulsepass.venue.Venue;
import com.pulsepass.pulsepass.venue.VenueRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        Venue venue = venueRepository.saveAndFlush(
                new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Carrera 1 # 2-3", 5000));
        event = eventRepository.saveAndFlush(new Event("CMF-2026", "Caribbean Music Fest 2026", null,
                EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 12, 18, 0), 0, venue));
        user = userRepository.saveAndFlush(new User("andrea", "andrea@example.com"));
    }

    private Ticket newTicket(String code, String price) {
        return new Ticket(code, TicketType.VIP, new BigDecimal(price), TicketStatus.PAID,
                LocalDateTime.of(2026, 9, 1, 10, 0), user, event);
    }

    @Test
    void shouldPersistTicketLinkedToUserAndEvent() { // FR-TKT-001, FR-TKT-004, FR-TKT-005, QT-006
        ticketRepository.saveAndFlush(newTicket("TCK-0001", "250000"));
        entityManager.clear();

        Ticket found = ticketRepository.findByTicketCode("TCK-0001").orElseThrow();

        assertThat(found.getType()).isEqualTo(TicketType.VIP);
        assertThat(found.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(found.getPrice()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(found.getUser().getUsername()).isEqualTo("andrea");
        assertThat(found.getEvent().getEventCode()).isEqualTo("CMF-2026");
    }

    @Test
    void shouldRejectDuplicateTicketCode() { // FR-TKT-002, AC-005, QT-009
        ticketRepository.saveAndFlush(newTicket("TCK-0001", "250000"));

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(newTicket("TCK-0001", "120000")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNegativePrice() { // FR-TKT-003
        assertThatThrownBy(() -> ticketRepository.saveAndFlush(newTicket("TCK-NEG", "-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAcceptFreeTicket() { // FR-TKT-003: el precio 0 es válido (>= 0)
        Ticket saved = ticketRepository.saveAndFlush(newTicket("TCK-FREE", "0"));

        assertThat(saved.getId()).isNotNull();
    }
}