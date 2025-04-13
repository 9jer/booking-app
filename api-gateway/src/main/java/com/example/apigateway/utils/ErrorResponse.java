package com.example.apigateway.utils;

import lombok.Data;

import java.util.Date;

@Data
public class ErrorResponse {
    private Integer status;
    private String message;
    private Date timestamp;

    public ErrorResponse(Integer status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = new Date();
    }
}
