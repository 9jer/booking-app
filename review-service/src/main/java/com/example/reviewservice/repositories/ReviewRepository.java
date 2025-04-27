package com.example.reviewservice.repositories;

import com.example.reviewservice.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByPropertyId(Long propertyId);
    Boolean existsByUserIdAndPropertyId(Long userId, Long propertyId);
}
