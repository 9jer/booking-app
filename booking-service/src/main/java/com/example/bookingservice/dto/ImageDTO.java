package com.example.bookingservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ImageDTO implements Serializable {

    private Long id;

    @NotEmpty(message = "Image URL should not be empty!")
    private String url;
}
