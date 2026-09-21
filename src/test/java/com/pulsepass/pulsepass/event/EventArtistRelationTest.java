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
class EventArtistRelationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private EntityManager entityManager;

    private Venue venue;
    private Artist artistA;
    private Artist artistB;

    @BeforeEach
    void setUp() {
        venue = venueRepository.saveAndFlush(
                new Venue("VEN-ART", "Teatro Sur", "Bogotá", "Calle 50", 1500));
        artistA = artistRepository.saveAndFlush(new Artist("Test Artist A", "Colombia", "Pop"));
        artistB = artistRepository.saveAndFlush(new Artist("Test Artist B", "Mexico", "Rock"));
    }

    private Event newEvent(String eventCode) {
        return eventRepository.saveAndFlush(new Event(eventCode, "Concierto de prueba", null,
                EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 11, 20, 19, 0), 0, venue));
    }

    @Test
    void shouldAssociateSeveralArtistsWithOneEvent() { // FR-ART-003, BR-003
        Event event = newEvent("EVT-ART-1");
        event.addArtist(artistA);
        event.addArtist(artistB);
        eventRepository.saveAndFlush(event);
        entityManager.clear();

        Event found = eventRepository.findByEventCode("EVT-ART-1").orElseThrow();

        assertThat(found.getArtists())
                .extracting(Artist::getStageName)
                .containsExactlyInAnyOrder("Test Artist A", "Test Artist B");
    }

    @Test
    void shouldNotDuplicateTheSameArtistInAnEvent() { // FR-ART-003
        Event event = newEvent("EVT-ART-2");
        event.addArtist(artistA);
        event.addArtist(artistA);
        eventRepository.saveAndFlush(event);
        entityManager.clear();

        Event found = eventRepository.findByEventCode("EVT-ART-2").orElseThrow();

        assertThat(found.getArtists()).hasSize(1);
    }

    @Test
    void shouldAllowTheSameArtistInSeveralEvents() { // BR-003
        Event first = newEvent("EVT-ART-3");
        Event second = newEvent("EVT-ART-4");
        first.addArtist(artistA);
        second.addArtist(artistA);
        eventRepository.saveAllAndFlush(java.util.List.of(first, second));
        entityManager.clear();

        assertThat(eventRepository.findByEventCode("EVT-ART-3").orElseThrow().getArtists())
                .extracting(Artist::getStageName).containsExactly("Test Artist A");
        assertThat(eventRepository.findByEventCode("EVT-ART-4").orElseThrow().getArtists())
                .extracting(Artist::getStageName).containsExactly("Test Artist A");
    }
}