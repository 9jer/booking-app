package com.example.reviewservice.repositories;

import com.example.reviewservice.models.Review;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16:///test_db",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByPropertyId_ShouldReturnPagedReviewsForSpecificProperty() {
        // Arrange
        Review review1 = createReview(100L, 1L, 5);
        Review review2 = createReview(100L, 2L, 4);
        Review reviewOther = createReview(200L, 3L, 5);

        reviewRepository.save(review1);
        reviewRepository.save(review2);
        reviewRepository.save(reviewOther);

        // Act
        Page<Review> result = reviewRepository.findByPropertyId(100L, PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Review::getPropertyId).containsOnly(100L);
    }

    @Test
    void getAverageRating_ShouldReturnCorrectAverage() {
        // Arrange
        reviewRepository.save(createReview(100L, 1L, 5));
        reviewRepository.save(createReview(100L, 2L, 4));
        reviewRepository.save(createReview(100L, 3L, 3));

        entityManager.flush();
        entityManager.clear();

        // Act
        Double average = reviewRepository.getAverageRating(100L);

        // Assert
        assertThat(average).isEqualTo(4.0);
    }

    @Test
    void getAverageRating_WhenNoReviews_ShouldReturnZero() {
        Double average = reviewRepository.getAverageRating(999L);
        assertThat(average).isEqualTo(0.0);
    }

    @Test
    void countReviews_ShouldReturnTotalNumberOfReviewsForProperty() {
        // Arrange
        reviewRepository.save(createReview(100L, 1L, 5));
        reviewRepository.save(createReview(100L, 2L, 4));

        entityManager.flush();
        entityManager.clear();

        // Act
        Long count = reviewRepository.countReviews(100L);

        // Assert
        assertThat(count).isEqualTo(2L);
    }

    private Review createReview(Long propertyId, Long userId, int rating) {
        Review review = new Review();
        review.setPropertyId(propertyId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setComment("Test comment");
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }
}