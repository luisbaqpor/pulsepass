package com.pulsepass.pulsepass.event;

import com.pulsepass.pulsepass.TestcontainersConfiguration;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EntityManager entityManager;

    private Venue venue;

    @BeforeEach
    void setUp() {
        venue = venueRepository.saveAndFlush(
                new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Carrera 1 # 2-3", 5000));
    }

    private Event newEvent(String eventCode) {
        return new Event(eventCode, "Caribbean Music Fest 2026", "Evento de prueba",
                EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 12, 18, 0), 18, venue);
    }

    @Test
    void shouldPersistEventLinkedToItsVenue() { // FR-EVT-001, BR-001, AC-002
        eventRepository.saveAndFlush(newEvent("CMF-2026"));
        entityManager.clear();

        Event found = eventRepository.findByEventCode("CMF-2026").orElseThrow();

        assertThat(found.getCategory()).isEqualTo(EventCategory.MUSIC);
        assertThat(found.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(found.getVenue().getCode()).isEqualTo("VEN-SMR-01");
    }

    @Test
    void shouldRejectDuplicateEventCode() { // FR-EVT-002
        eventRepository.saveAndFlush(newEvent("EVT-DUP"));

        assertThatThrownBy(() -> eventRepository.saveAndFlush(newEvent("EVT-DUP")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}