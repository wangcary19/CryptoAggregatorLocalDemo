package com.bitwise.demo.CryptoAggregatorLocalDemo.handler;

/* This is a custom exception class that retrieves the appropriate error message before being thrown.
   Error messages are defined in the errors.properties file.
 */

import java.util.ResourceBundle;

public class CryptoAggregatorException extends RuntimeException {
    private final String errorId;

    public CryptoAggregatorException(String errorId) {
        super(getMessageFromProperties(errorId));
        this.errorId = errorId;
    }

    public String getErrorId() {
        return errorId;
    }

    private static String getMessageFromProperties(String errorId) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("errors");
            return bundle.getString(errorId);
        } catch (Exception e) {
            return "Unknown error code: " + errorId;
        }
    }
}
