package com.example.propertyservice.repositories;

import com.example.propertyservice.models.PropertyFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PropertyFeatureRepository extends JpaRepository<PropertyFeature, Long> {
    Optional<PropertyFeature> findByName(String name);
}
