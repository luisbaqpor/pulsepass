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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketQueryTest {

    private static final LocalDateTime PURCHASE = LocalDateTime.of(2026, 9, 1, 10, 0);

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

    @BeforeEach
    void setUp() {
        Venue venue = venueRepository.saveAndFlush(
                new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Carrera 1 # 2-3", 5000));

        Event cmf = newEvent("CMF-2026", LocalDateTime.of(2026, 12, 12, 18, 0), EventStatus.PUBLISHED, venue);
        Event november = newEvent("EVT-NOV", LocalDateTime.of(2026, 11, 5, 20, 0), EventStatus.PUBLISHED, venue);
        Event old = newEvent("EVT-OLD", LocalDateTime.of(2026, 6, 1, 20, 0), EventStatus.FINISHED, venue);

        User andrea = userRepository.saveAndFlush(new User("andrea", "andrea@example.com"));
        User carlos = userRepository.saveAndFlush(new User("carlos", "carlos@example.com"));
        User laura = userRepository.saveAndFlush(new User("laura", "laura@example.com"));
        User miguel = userRepository.saveAndFlush(new User("miguel", "miguel@example.com"));

        newTicket("TCK-0001", TicketType.VIP, TicketStatus.PAID, "250000", andrea, cmf);
        newTicket("TCK-0002", TicketType.GENERAL, TicketStatus.PAID, "120000", carlos, cmf);
        newTicket("TCK-0003", TicketType.GENERAL, TicketStatus.RESERVED, "120000", laura, cmf);
        newTicket("TCK-0004", TicketType.VIP, TicketStatus.CANCELLED, "250000", miguel, cmf);
        newTicket("TCK-0005", TicketType.GENERAL, TicketStatus.PAID, "100000", andrea, november);
        newTicket("TCK-0006", TicketType.GENERAL, TicketStatus.PAID, "80000", carlos, old);

        entityManager.flush();
        entityManager.clear();
    }

    private Event newEvent(String code, LocalDateTime date, EventStatus status, Venue venue) {
        return eventRepository.saveAndFlush(new Event(code, "Evento " + code, null,
                EventCategory.MUSIC, status, date, 0, venue));
    }

    private void newTicket(String code, TicketType type, TicketStatus status, String price,
                           User user, Event event) {
        ticketRepository.saveAndFlush(
                new Ticket(code, type, new BigDecimal(price), status, PURCHASE, user, event));
    }

    @Test
    void shouldFindUserTicketsByEmailIgnoringCase() { // FR-TKT-006
        assertThat(ticketRepository.findByUserEmailIgnoreCase("ANDREA@Example.com"))
                .extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder("TCK-0001", "TCK-0005");
    }

    @Test
    void shouldFilterUserTicketsByStatus() { // FR-TKT-006
        assertThat(ticketRepository.findByUserEmailIgnoreCaseAndStatus("laura@example.com", TicketStatus.RESERVED))
                .extracting(Ticket::getTicketCode)
                .containsExactly("TCK-0003");
        assertThat(ticketRepository.findByUserEmailIgnoreCaseAndStatus("laura@example.com", TicketStatus.PAID))
                .isEmpty();
    }

    @Test
    void shouldListOnlyPaidTicketsOfAnEvent() { // FR-TKT-007, UC-08
        assertThat(ticketRepository.findByEvent_EventCodeAndStatus("CMF-2026", TicketStatus.PAID))
                .extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder("TCK-0001", "TCK-0002");
    }

    @Test
    void shouldCountOnlyPaidTickets() { // FR-TKT-008, AC-008, QT-008
        assertThat(ticketRepository.countPaidByEventCode("CMF-2026")).isEqualTo(2L);
        assertThat(ticketRepository.countByEventAndStatus("CMF-2026", TicketStatus.RESERVED)).isEqualTo(1L);
        assertThat(ticketRepository.countPaidByEventCode("EVT-OLD")).isEqualTo(1L);
    }

    @Test
    void shouldListTicketsOfFutureEventsInChronologicalOrder() { // FR-SRC-004
        var result = ticketRepository.findForEventsAfter(LocalDateTime.of(2026, 9, 21, 0, 0));

        assertThat(result)
                .extracting(Ticket::getTicketCode)
                .hasSize(5)
                .doesNotContain("TCK-0006"); // su evento ya pasó
        assertThat(result)
                .extracting(ticket -> ticket.getEvent().getEventDate())
                .isSorted();
    }
}