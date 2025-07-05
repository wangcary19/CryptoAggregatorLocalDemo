package com.bitwise.demo.CryptoAggregatorLocalDemo.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingResponseTest {

    private PingResponse pingResponse;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        pingResponse = new PingResponse();
        mapper = new ObjectMapper();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(new PingResponse());
    }

    @Test
    void testGetterSetter() {
        pingResponse.setPingMessage("(V3) To the Moon! (V3)");
        assertEquals("(V3) To the Moon! (V3)", pingResponse.getPingMessage());
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"gecko_says\":\"(V3) To the Moon! (V3)\"}";
        PingResponse response = mapper.readValue(json, PingResponse.class);
        assertEquals("(V3) To the Moon! (V3)", response.getPingMessage());
    }
}