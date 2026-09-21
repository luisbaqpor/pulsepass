package com.pulsepass.pulsepass.user;

import com.pulsepass.pulsepass.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private EntityManager entityManager;

    private UserProfile newProfile() {
        return new UserProfile("Ana", "Pérez", "3001234567", "Bogotá",
                LocalDate.of(2000, 5, 20));
    }

    @Test
    void shouldPersistAndRecoverUser() { // FR-USR-001
        userRepository.saveAndFlush(new User("ana.perez", "ana@example.com"));
        entityManager.clear();

        assertThat(userRepository.findByEmail("ana@example.com"))
                .isPresent()
                .get()
                .extracting(User::getUsername, User::isActive)
                .containsExactly("ana.perez", true);
    }

    @Test
    void shouldRejectDuplicateUsername() { // FR-USR-002
        userRepository.saveAndFlush(new User("dup.user", "uno@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("dup.user", "dos@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateEmail() { // FR-USR-002
        userRepository.saveAndFlush(new User("user.uno", "dup@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("user.dos", "dup@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistUserWithProfileAndRecoverIt() { // FR-USR-003, FR-USR-004
        User user = new User("luis.dev", "luis@example.com");
        user.assignProfile(newProfile());
        userRepository.saveAndFlush(user);
        entityManager.clear();

        User found = userRepository.findByEmail("luis@example.com").orElseThrow();

        assertThat(found.getProfile()).isNotNull();
        assertThat(found.getProfile().getFirstName()).isEqualTo("Ana");
        assertThat(found.getProfile().getCity()).isEqualTo("Bogotá");
        assertThat(found.getProfile().getUser().getUsername()).isEqualTo("luis.dev");
    }

    @Test
    void shouldRejectSecondProfileForTheSameUser() { // FR-USR-003, BR-004
        User user = userRepository.saveAndFlush(new User("solo.perfil", "solo@example.com"));

        UserProfile first = newProfile();
        first.assignUser(user);
        userProfileRepository.saveAndFlush(first);

        UserProfile second = newProfile();
        second.assignUser(user);

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

        @Test
    void shouldFindUserByEmailIgnoringCase() { // Sección 14 del PRD
        userRepository.saveAndFlush(new User("mayus.user", "Mayus@Example.com"));
        entityManager.clear();

        assertThat(userRepository.findByEmailIgnoreCase("mayus@example.COM")).isPresent();
    }
}