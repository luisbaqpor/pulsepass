package com.pulsepass.pulsepass.event;

import com.pulsepass.pulsepass.TestcontainersConfiguration;
import com.pulsepass.pulsepass.artist.Artist;
import com.pulsepass.pulsepass.artist.ArtistRepository;
import com.pulsepass.pulsepass.venue.Venue;
import com.pulsepass.pulsepass.venue.VenueRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventSearchTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 21, 0, 0);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        Venue santaMarta = venueRepository.saveAndFlush(
                new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Carrera 1 # 2-3", 5000));
        Venue bogota = venueRepository.saveAndFlush(
                new Venue("VEN-BOG-01", "Teatro Central", "Bogotá", "Calle 26 # 5-10", 2000));

        // Artistas cargados por la migración V2
        Artist solarBeat = artistRepository.findByStageName("Solar Beat").orElseThrow();
        Artist neonWaves = artistRepository.findByStageName("Neon Waves").orElseThrow();
        Artist caribbean = artistRepository.findByStageName("Caribbean Sound").orElseThrow();
        Artist oceanDrive = artistRepository.findByStageName("Ocean Drive").orElseThrow();

        newEvent("CMF-2026", EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 12, 18, 0),
                santaMarta, solarBeat, neonWaves, caribbean);
        newEvent("EVT-BOG-01", EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 5, 20, 0),
                bogota, solarBeat, oceanDrive);
        newEvent("EVT-DRAFT", EventStatus.DRAFT, LocalDateTime.of(2026, 10, 1, 20, 0),
                santaMarta, solarBeat);
        newEvent("EVT-CANC", EventStatus.CANCELLED, LocalDateTime.of(2026, 10, 15, 20, 0),
                santaMarta);

        entityManager.flush();
        entityManager.clear();
    }

    private void newEvent(String code, EventStatus status, LocalDateTime date,
                          Venue venue, Artist... artists) {
        Event event = new Event(code, "Evento " + code, null, EventCategory.MUSIC,
                status, date, 0, venue);
        for (Artist artist : artists) {
            event.addArtist(artist);
        }
        eventRepository.save(event);
    }

    @Test
    void shouldListOnlyPublishedEventsSortedByDate() { // FR-EVT-005, AC-006, UC-06
        assertThat(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED))
                .extracting(Event::getEventCode)
                .containsExactly("EVT-BOG-01", "CMF-2026");
    }

    @Test
    void shouldListEventsOfAVenueByItsCode() { // FR-VEN-004, QT-003, QT-007
        assertThat(eventRepository.findByVenueCode("VEN-SMR-01"))
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("CMF-2026", "EVT-DRAFT", "EVT-CANC");
    }

    @Test
    void shouldFindEventsByArtistWithoutDuplicates() { // FR-SRC-001, FR-ART-004, AC-007, QT-008
        assertThat(eventRepository.findEventsByArtist("Solar Beat"))
                .extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("CMF-2026", "EVT-BOG-01", "EVT-DRAFT");
    }

    @Test
    void shouldFindEventsByCityAndArtist() { // FR-SRC-002
        assertThat(eventRepository.findEventsByCityAndArtist("Bogotá", "Solar Beat"))
                .extracting(Event::getEventCode)
                .containsExactly("EVT-BOG-01");
        assertThat(eventRepository.findEventsByCityAndArtist("Santa Marta", "Ocean Drive"))
                .isEmpty();
    }

    @Test
    void shouldRecommendPublishedEventsIgnoringCase() { // FR-SRC-003
        assertThat(eventRepository.findRecommended(NOW, "Santa Marta", "SOLAR"))
                .extracting(Event::getEventCode)
                .containsExactly("CMF-2026"); // EVT-DRAFT queda fuera: no está PUBLISHED
    }

    @Test
    void shouldNotDuplicateRecommendedEventWhenSeveralArtistsMatch() { // FR-SRC-003: DISTINCT
        // "a" coincide con Solar Beat, Neon Waves y Caribbean Sound del mismo evento
        assertThat(eventRepository.findRecommended(NOW, "Santa Marta", "a"))
                .extracting(Event::getEventCode)
                .containsExactly("CMF-2026");
    }

    @Test
    void shouldNotRecommendEventsBeforeTheGivenDate() { // FR-SRC-003
        assertThat(eventRepository.findRecommended(LocalDateTime.of(2027, 1, 1, 0, 0),
                "Santa Marta", "Solar")).isEmpty();
    }
}