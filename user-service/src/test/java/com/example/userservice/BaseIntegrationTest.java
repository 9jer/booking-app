package com.example.userservice;

import com.example.userservice.models.Role;
import com.example.userservice.models.User;
import com.example.userservice.repositories.RoleRepository;
import com.example.userservice.repositories.UserRepository;
import com.example.userservice.security.CustomUserDetails;
import com.example.userservice.util.JwtTokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false"
        }
)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected JwtTokenUtils jwtTokenUtils;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE users, roles CASCADE");
    }

    protected String createTestUserAndGetToken(String username, String rawPassword, List<String> roleNames) {
        List<Role> roles = roleNames.stream().map(roleName -> {
            return roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName(roleName);
                        return roleRepository.save(newRole);
                    });
        }).collect(Collectors.toList());

        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(roles);

        user.setFirstName("TestName");
        user.setLastName("TestSurname");
        user.setPhone("+375290000000");

        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        return jwtTokenUtils.generateToken(userDetails);
    }
}