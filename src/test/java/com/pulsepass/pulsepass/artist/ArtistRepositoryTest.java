package com.pulsepass.pulsepass.artist;

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
class ArtistRepositoryTest {

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndRecoverArtist() { // FR-ART-001
        Artist saved = artistRepository.saveAndFlush(
                new Artist("Test Artist Solo", "Colombia", "Pop"));
        entityManager.clear();

        assertThat(artistRepository.findById(saved.getId())).isPresent();
        assertThat(artistRepository.findByStageName("Test Artist Solo"))
                .isPresent()
                .get()
                .extracting(Artist::getCountry, Artist::getGenre, Artist::isActive)
                .containsExactly("Colombia", "Pop", true);
    }

    @Test
    void shouldRejectDuplicateStageName() { // FR-ART-002
        artistRepository.saveAndFlush(new Artist("Test Artist Dup", "Chile", "Rock"));

        assertThatThrownBy(() -> artistRepository.saveAndFlush(
                new Artist("Test Artist Dup", "Peru", "Jazz")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}