package com.pulsepass.pulsepass.venue;

import com.pulsepass.pulsepass.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class VenueRepositoryTest {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndRecoverVenueByIdAndCode() { // FR-VEN-001, AC-001
        Venue saved = venueRepository.saveAndFlush(
                new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Carrera 1 # 2-3", 5000));
        entityManager.clear();

        assertThat(venueRepository.findById(saved.getId())).isPresent();
        assertThat(venueRepository.findByCode("VEN-SMR-01"))
                .isPresent()
                .get()
                .extracting(Venue::getName, Venue::getCapacity, Venue::isActive)
                .containsExactly("Marina Convention Center", 5000, true);
    }

    @Test
    void shouldRejectDuplicateVenueCode() { // FR-VEN-002
        venueRepository.saveAndFlush(new Venue("VEN-DUP", "Sala A", "Cali", "Carrera 5", 300));

        assertThatThrownBy(() -> venueRepository.saveAndFlush(
                new Venue("VEN-DUP", "Sala B", "Cali", "Carrera 6", 400)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNonPositiveCapacity() { // FR-VEN-003
        assertThatThrownBy(() -> venueRepository.saveAndFlush(
                new Venue("VEN-BAD", "Sala Cero", "Cali", "Carrera 7", 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}