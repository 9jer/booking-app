package com.example.propertyservice.mapper;

import com.example.propertyservice.dto.GetPropertyDTO;
import com.example.propertyservice.dto.PropertyDTO;
import com.example.propertyservice.dto.PropertyFeatureDTO;
import com.example.propertyservice.models.Property;
import com.example.propertyservice.models.PropertyFeature;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PropertyMapper {

    GetPropertyDTO toGetPropertyDTO(Property property);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Property toProperty(PropertyDTO propertyDTO);

    PropertyFeatureDTO toPropertyFeatureDTO(PropertyFeature feature);
}