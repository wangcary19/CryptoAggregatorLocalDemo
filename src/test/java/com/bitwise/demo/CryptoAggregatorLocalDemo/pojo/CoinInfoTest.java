package com.bitwise.demo.CryptoAggregatorLocalDemo.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CoinInfoTest {

    private CoinInfo coinInfo;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        coinInfo = new CoinInfo();
        mapper = new ObjectMapper();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(new CoinInfo());
    }

    @Test
    void testParameterizedConstructor() {
        CoinInfo coin = new CoinInfo("bitcoin", "btc", "Bitcoin");
        assertEquals("bitcoin", coin.getCoinID());
        assertEquals("btc", coin.getCoinSymbol());
        assertEquals("Bitcoin", coin.getCoinName());
    }

    @Test
    void testCoinIDGetterSetter() {
        coinInfo.setCoinID("ethereum");
        assertEquals("ethereum", coinInfo.getCoinID());
    }

    @Test
    void testCoinSymbolGetterSetter() {
        coinInfo.setCoinSymbol("eth");
        assertEquals("eth", coinInfo.getCoinSymbol());
    }

    @Test
    void testCoinNameGetterSetter() {
        coinInfo.setCoinName("Ethereum");
        assertEquals("Ethereum", coinInfo.getCoinName());
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"id\":\"apricot\",\"symbol\":\"aprt\",\"name\":\"Apricot\"}";
        CoinInfo coin = mapper.readValue(json, CoinInfo.class);

        assertEquals("apricot", coin.getCoinID());
        assertEquals("aprt", coin.getCoinSymbol());
        assertEquals("Apricot", coin.getCoinName());
    }
}