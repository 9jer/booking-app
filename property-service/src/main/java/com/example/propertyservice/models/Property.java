package com.example.propertyservice.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "Properties")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_id")
    private Long id;

    @Column(name = "owner_id")
    @NotNull(message = "Owner id should not be empty!")
    private Long ownerId;

    @Column(name = "title")
    @NotEmpty(message = "Title should not be empty!")
    private String title;

    @Column(name = "description")
    @NotEmpty(message = "Description should not be empty!")
    private String description;

    @Column(name = "location")
    @NotEmpty(message = "Location should not be empty!")
    private String location;

    @Column(name = "price_per_night", precision = 10, scale = 2)
    @NotNull(message = "Price per night should not be empty!")
    private BigDecimal pricePerNight;

    @Column(name = "capacity")
    @NotNull(message = "Capacity should not be empty!")
    private Integer capacity;

    @ManyToMany(cascade = {CascadeType.PERSIST})
    @JoinTable(
            name = "Properties_Property_Feature",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private Set<PropertyFeature> features;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
