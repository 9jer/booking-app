package com.example.propertyservice.repositories;

import com.example.propertyservice.models.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Page<Favorite> findAllByUserId(Long userId, Pageable pageable);
    Optional<Favorite> findByUserIdAndPropertyId(Long userId, Long propertyId);
}