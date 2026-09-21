package com.pulsepass.pulsepass;

import com.pulsepass.pulsepass.artist.ArtistRepository;
import com.pulsepass.pulsepass.event.Event;
import com.pulsepass.pulsepass.event.EventCategory;
import com.pulsepass.pulsepass.event.EventRepository;
import com.pulsepass.pulsepass.event.EventStatus;
import com.pulsepass.pulsepass.venue.Venue;
import com.pulsepass.pulsepass.venue.VenueRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class FlywayMigrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldApplyV1V2V3FromEmptyDatabase() { // QT-001, NFR-003
        List<String> versions = entityManager.createNativeQuery(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank",
                String.class).getResultList();

        assertThat(versions).containsExactly("1", "2", "3");
    }

    @Test
    void shouldHaveInitialArtistsLoadedByV2() {
        assertThat(List.of("Solar Beat", "Neon Waves", "Caribbean Sound", "Ocean Drive", "Digital Pulse"))
                .allSatisfy(name -> assertThat(artistRepository.findByStageName(name)).isPresent());
    }

    @Test
    void shouldStoreOptionalStreamingUrlAddedByV3() { // FR-EVT-006
        Venue venue = venueRepository.saveAndFlush(
                new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Carrera 1 # 2-3", 5000));
        Event withUrl = new Event("EVT-HYB", "Evento híbrido", null, EventCategory.TECHNOLOGY,
                EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 1, 18, 0), 0, venue);
        withUrl.changeStreamingUrl("https://stream.example.com/evt-hyb");
        Event withoutUrl = new Event("EVT-PRES", "Evento presencial", null, EventCategory.CULTURE,
                EventStatus.PUBLISHED, LocalDateTime.of(2026, 11, 2, 18, 0), 0, venue);
        eventRepository.saveAllAndFlush(List.of(withUrl, withoutUrl));
        entityManager.clear();

        assertThat(eventRepository.findByEventCode("EVT-HYB").orElseThrow().getStreamingUrl())
                .isEqualTo("https://stream.example.com/evt-hyb");
        assertThat(eventRepository.findByEventCode("EVT-PRES").orElseThrow().getStreamingUrl())
                .isNull();
    }
}