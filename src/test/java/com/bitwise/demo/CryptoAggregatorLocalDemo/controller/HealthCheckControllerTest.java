package com.bitwise.demo.CryptoAggregatorLocalDemo.controller;

import com.bitwise.demo.CryptoAggregatorLocalDemo.handler.CryptoAggregatorException;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.PingResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthCheckControllerTest {

    @InjectMocks
    private HealthCheckController controller;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject mock RestTemplate via reflection since it's private and final
        try {
            var field = HealthCheckController.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(controller, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testPingSelf() {
        Map<String, String> response = controller.pingSelf();
        assertNotNull(response);
        assertEquals("CryptoPriceAggregator API is up and running!", response.get("status"));
    }

    @Test
    void testPingCoinGeckoApi_Success() throws Exception {
        String json = "{\"gecko_says\":\"(V3) To the Moon!\"}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        // Patch ObjectMapper to parse the response
        ObjectMapper mapper = new ObjectMapper();
        PingResponse pr = mapper.readValue(json, PingResponse.class);
        assertEquals("(V3) To the Moon!", pr.getPingMessage());

        // Should not throw
        assertDoesNotThrow(() -> controller.pingCoinGeckoApi());
    }

    @Test
    void testPingCoinGeckoApi_Failure() {
        String json = "{}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        assertThrows(CryptoAggregatorException.class, () -> controller.pingCoinGeckoApi());
    }

    @Test
    void testPingCoinGeckoApi_JsonProcessingException() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("invalid_json");

        assertThrows(JsonProcessingException.class, () -> controller.pingCoinGeckoApi());
    }
}