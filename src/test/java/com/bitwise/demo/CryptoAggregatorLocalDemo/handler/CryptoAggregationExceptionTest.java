package com.bitwise.demo.CryptoAggregatorLocalDemo.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoAggregatorExceptionTest {

    private static final String TEST_ERROR_ID = "REQUEST.01";
    private static final String TEST_ERROR_MESSAGE = "Test error message";

    @Test
    void constructor_ShouldSetErrorId() {
        // Mock ResourceBundle to return a test message
        try (MockedStatic<ResourceBundle> mockedStatic = Mockito.mockStatic(ResourceBundle.class)) {
            ResourceBundle mockBundle = mock(ResourceBundle.class);
            when(mockBundle.getString(TEST_ERROR_ID)).thenReturn(TEST_ERROR_MESSAGE);
            mockedStatic.when(() -> ResourceBundle.getBundle("errors")).thenReturn(mockBundle);

            // Create exception
            CryptoAggregatorException exception = new CryptoAggregatorException(TEST_ERROR_ID);

            // Verify errorId is set correctly
            assertEquals(TEST_ERROR_ID, exception.getErrorId());
            // Verify message is set from properties
            assertEquals(TEST_ERROR_MESSAGE, exception.getMessage());
        }
    }

    @Test
    void constructor_WhenResourceBundleThrowsException_ShouldUseDefaultMessage() {
        // Mock ResourceBundle to throw an exception
        try (MockedStatic<ResourceBundle> mockedStatic = Mockito.mockStatic(ResourceBundle.class)) {
            mockedStatic.when(() -> ResourceBundle.getBundle("errors"))
                    .thenThrow(new MissingResourceException("Test exception", "errors", TEST_ERROR_ID));

            // Create exception
            CryptoAggregatorException exception = new CryptoAggregatorException(TEST_ERROR_ID);

            // Verify errorId is set correctly
            assertEquals(TEST_ERROR_ID, exception.getErrorId());
            // Verify default message is used
            assertEquals("Unknown error code: " + TEST_ERROR_ID, exception.getMessage());
        }
    }

    @Test
    void getErrorId_ShouldReturnCorrectErrorId() {
        // Mock ResourceBundle to return a test message
        try (MockedStatic<ResourceBundle> mockedStatic = Mockito.mockStatic(ResourceBundle.class)) {
            ResourceBundle mockBundle = mock(ResourceBundle.class);
            when(mockBundle.getString(TEST_ERROR_ID)).thenReturn(TEST_ERROR_MESSAGE);
            mockedStatic.when(() -> ResourceBundle.getBundle("errors")).thenReturn(mockBundle);

            // Create exception and verify getErrorId
            CryptoAggregatorException exception = new CryptoAggregatorException(TEST_ERROR_ID);
            assertEquals(TEST_ERROR_ID, exception.getErrorId());
        }
    }
}