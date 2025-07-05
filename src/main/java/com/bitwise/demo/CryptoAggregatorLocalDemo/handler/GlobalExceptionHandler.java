package com.bitwise.demo.CryptoAggregatorLocalDemo.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice // Binds the below handler to all controllers
public class GlobalExceptionHandler {

    private MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) { // Constructor for MessageSource dependency injection
        this.messageSource = messageSource;
    }

    @ExceptionHandler(CryptoAggregatorException.class)
    public String handleCryptoAggregatorException(CryptoAggregatorException ex) {
        String errorMessage = messageSource.getMessage(ex.getErrorId(), null, Locale.getDefault()); // Change this Locale via a property file to enable dynamic environment support
        return ex.getErrorId() + ": " + errorMessage; // Return a status object if moving to integrated environment
    }
}
