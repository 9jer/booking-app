package com.example.propertyservice.repositories;

import com.example.propertyservice.models.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16:///test_db",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PropertyRepositoryTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Property propertySydneyBudget;
    private Property propertySydneyLuxury;
    private Property propertyCalifornia;

    @BeforeEach
    void setUp() {
        propertySydneyBudget = createProperty(1L, "Budget Sydney", "sydney", BigDecimal.valueOf(50));
        propertySydneyLuxury = createProperty(1L, "Luxury Sydney", "sydney", BigDecimal.valueOf(200));
        propertyCalifornia = createProperty(2L, "California House", "california", BigDecimal.valueOf(100));

        propertyRepository.save(propertySydneyBudget);
        propertyRepository.save(propertySydneyLuxury);
        propertyRepository.save(propertyCalifornia);
    }

    @Test
    void searchProperties_ByLocationAndPriceRange_ShouldReturnMatched() {
        Page<Property> result = propertyRepository.searchProperties(
                "%sydney%",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(300),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Luxury Sydney");
    }

    @Test
    void searchProperties_WithNullFilters_ShouldReturnAll() {
        Page<Property> result = propertyRepository.searchProperties(null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    void updateRating_ShouldUpdateRatingInDatabase() {
        Long propertyId = propertySydneyBudget.getId();
        BigDecimal newRating = BigDecimal.valueOf(4.8);

        propertyRepository.updateRating(propertyId, newRating);

        entityManager.flush();
        entityManager.clear();

        Property updatedProperty = propertyRepository.findById(propertyId).orElseThrow();
        assertThat(updatedProperty.getAverageRating()).isEqualByComparingTo(newRating);
    }

    @Test
    void findByOwnerId_ShouldReturnPropertiesOfSpecificOwner() {
        Page<Property> result = propertyRepository.findByOwnerId(1L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Property::getOwnerId).containsOnly(1L);
    }

    @Test
    void findById_ShouldReturnProperty() {
        Optional<Property> found = propertyRepository.findById(propertySydneyBudget.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Budget Sydney");
    }

    private Property createProperty(Long ownerId, String title, String location, BigDecimal price) {
        Property property = new Property();
        property.setOwnerId(ownerId);
        property.setTitle(title);
        property.setDescription("Default description for test");
        property.setLocation(location);
        property.setPricePerNight(price);
        property.setCapacity(2);
        property.setAverageRating(BigDecimal.ZERO);
        property.setCreatedAt(LocalDateTime.now());
        return property;
    }
}