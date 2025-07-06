package com.bitwise.demo.CryptoAggregatorLocalDemo.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

// This is a simple timeout configuration for the RestClient used in the application.
// Note that this configuration method will be deprecated in future versions of Spring Boot.

@Configuration
public class TimeoutConfig {

    @Configuration
    public class RestTemplateConfig {
        @Bean
        public RestTemplate restTemplate(RestTemplateBuilder builder) {
            return builder
                    .setConnectTimeout(Duration.ofMillis(10000)) // Connection timeout
                    .setReadTimeout(Duration.ofMillis(10000))   // Read timeout
                    .build();
        }
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8080")
                .requestFactory(customRequestFactory())
                .build();
    }

    ClientHttpRequestFactory customRequestFactory() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(10000))
                .withReadTimeout(Duration.ofMillis(10000));
        return ClientHttpRequestFactories.get(settings);
    }
}

