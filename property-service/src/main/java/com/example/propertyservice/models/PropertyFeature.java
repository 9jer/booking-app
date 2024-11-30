package com.example.propertyservice.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Entity
@Table(name = "Property_Feature")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class PropertyFeature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feature_id")
    private Long id;

    @NotEmpty(message = "Feature name should not be empty!")
    @Column(name = "name")
    private String name;
}
