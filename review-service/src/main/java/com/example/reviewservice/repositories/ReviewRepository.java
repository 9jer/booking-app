package com.example.reviewservice.repositories;

import com.example.reviewservice.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByPropertyId(Long propertyId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.propertyId = :propertyId")
    Double getAverageRating(Long propertyId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.propertyId = :propertyId")
    Long countReviews(Long propertyId);
}
