package com.example.propertyservice.util;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;

public class ErrorsUtil {
    public static void returnAllErrors(BindingResult bindingResult) {
        StringBuilder errorMsg = new StringBuilder();

        List<FieldError> fieldErrors = bindingResult.getFieldErrors();

        for(FieldError fieldError : fieldErrors) {
            errorMsg.append(fieldError.getField() + ": " + fieldError.getDefaultMessage()).append(";");
        }

        throw new PropertyException(errorMsg.toString());
    }
}
