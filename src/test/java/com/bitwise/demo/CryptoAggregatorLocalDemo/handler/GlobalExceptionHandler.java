package com.bitwise.demo.CryptoAggregatorLocalDemo.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        globalExceptionHandler = new GlobalExceptionHandler(messageSource);
    }

    @Test
    void testHandleCryptoAggregatorException_ReturnsFormattedMessage() {
        String errorId = "REQUEST.01";
        String errorMessage = "Something went wrong";
        CryptoAggregatorException exception = new CryptoAggregatorException(errorId);

        when(messageSource.getMessage(eq(errorId), isNull(), any(Locale.class))).thenReturn(errorMessage);

        String result = globalExceptionHandler.handleCryptoAggregatorException(exception);

        assertEquals(errorId + ": " + errorMessage, result);
        verify(messageSource).getMessage(eq(errorId), isNull(), any(Locale.class));
    }
}