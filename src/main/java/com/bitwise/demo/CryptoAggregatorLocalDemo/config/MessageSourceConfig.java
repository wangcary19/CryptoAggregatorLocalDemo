package com.bitwise.demo.CryptoAggregatorLocalDemo.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration class for setting up a message source for custom errors, located in "errors.properties".
 */

@Configuration
public class MessageSourceConfig {
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("errors"); // This will look for errors.properties in the classpath
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}