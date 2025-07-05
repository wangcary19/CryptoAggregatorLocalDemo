package com.bitwise.demo.CryptoAggregatorLocalDemo.controller;

import com.bitwise.demo.CryptoAggregatorLocalDemo.handler.CryptoAggregatorException;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.PingResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/*
  This controller will ping the CoinGecko API every 10 minutes to check if it is reachable.
  To disable, remove the @EnableScheduling annotation from the main application class.
 */

@RestController
public class HealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);
    private static final String PING_URL = "https://api.coingecko.com/api/v3/ping"; // Check the API documentation if the ping stops working!  Endpoints may change
    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 600_000)
    public void pingCoinGeckoApi() throws RuntimeException, JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();

        String jsonResponse = restTemplate.getForObject(PING_URL, String.class);
        PingResponse pr = mapper.readValue(jsonResponse, PingResponse.class);

        if (pr.getPingMessage() != null && !pr.getPingMessage().isEmpty()) {
            logger.info("CoinGecko API ping successful: {}", pr.getPingMessage());
        } else {
            logger.error("CoinGecko API is not responding or not reachable.");
            throw new CryptoAggregatorException("ALPHA.01");
        }
    }

    @GetMapping("/ping")
    public Map<String, String> pingSelf() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "CryptoPriceAggregator API is up and running!");
        return response;
    }
}
