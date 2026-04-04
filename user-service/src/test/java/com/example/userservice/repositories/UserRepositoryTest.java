package com.example.userservice.repositories;

import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16:///user_test_db",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUsername_ShouldReturnUserWithRoles() {
        User savedUser = createUserWithRole("testuser", "test@test.com", "ROLE_USER");

        entityManager.flush();
        entityManager.clear();

        Optional<User> foundUser = userRepository.findByUsername("testuser");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");

        assertThat(foundUser.get().getRoles()).hasSize(1);
        assertThat(foundUser.get().getRoles())
                .extracting(Role::getName)
                .containsExactly("ROLE_USER");
    }

    @Test
    void findByEmail_ShouldReturnUser() {
        createUserWithRole("john_doe", "john@example.com", "ROLE_GUEST");

        entityManager.flush();
        entityManager.clear();

        Optional<User> foundUser = userRepository.findByEmail("john@example.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    void findAll_ShouldReturnPagedUsersWithRoles() {
        createUserWithRole("user1", "user1@test.com", "ROLE_GUEST");
        createUserWithRole("user2", "user2@test.com", "ROLE_OWNER");

        entityManager.flush();
        entityManager.clear();

        Page<User> usersPage = userRepository.findAll(PageRequest.of(0, 10));

        assertThat(usersPage.getContent().size()).isGreaterThanOrEqualTo(2);

        assertThat(usersPage.getContent())
                .extracting(User::getUsername)
                .contains("user1", "user2");

        User user1 = usersPage.getContent().stream()
                .filter(u -> u.getUsername().equals("user1"))
                .findFirst()
                .orElseThrow();
        assertThat(user1.getRoles()).isNotEmpty();
    }

    private User createUserWithRole(String username, String email, String roleName) {
        List<Role> existingRoles = entityManager.getEntityManager()
                .createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                .setParameter("name", roleName)
                .getResultList();

        Role role;
        if (existingRoles.isEmpty()) {
            role = new Role();
            role.setName(roleName);
            role = entityManager.persist(role);
        } else {
            role = existingRoles.get(0);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("secret_password");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhone("+1234567890");
        user.setCreatedAt(LocalDateTime.now());
        user.setRoles(List.of(role));

        return entityManager.persist(user);
    }
}