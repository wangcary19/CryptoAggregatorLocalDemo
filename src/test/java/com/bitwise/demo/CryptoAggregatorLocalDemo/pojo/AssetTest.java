package com.bitwise.demo.CryptoAggregatorLocalDemo.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssetTest {

    private Asset asset;
    private ObjectMapper mapper;
    private static final float DELTA = 0.0001f;

    @BeforeEach
    void setUp() {
        asset = new Asset();
        mapper = new ObjectMapper();
    }

    @Test
    void testIDGetterSetter() {
        asset.setID("bitcoin");
        assertEquals("bitcoin", asset.getID());
    }

    @Test
    void testParameterizedConstructor() {
        Asset asset = new Asset(
                "bitcoin",
                "btc",
                67187.34f,
                1317802988326.25f,
                31260929299.52f,
                3.6372f,
                1711356300L
        );

        assertEquals("bitcoin", asset.getID());
        assertEquals("btc", asset.getCoinSymbol());
        assertEquals(67187.34f, asset.getPrice(), DELTA);
        assertEquals(1317802988326.25f, asset.getMarketCap(), DELTA);
        assertEquals(31260929299.52f, asset.getVolume(), DELTA);
        assertEquals(3.6372f, asset.getChange(), DELTA);
        assertEquals(1711356300L, asset.getTimestamp());
    }

    @Test
    void testCoinSymbolGetterSetter() {
        asset.setCoinSymbol("btc");
        assertEquals("btc", asset.getCoinSymbol());
    }

    @Test
    void testPriceGetterSetter() {
        asset.setPrice(67187.34f);
        assertEquals(67187.34f, asset.getPrice(), DELTA);
    }

    @Test
    void testMarketCapGetterSetter() {
        asset.setMarketCap(1317802988326.25f);
        assertEquals(1317802988326.25f, asset.getMarketCap(), DELTA);
    }

    @Test
    void testVolumeGetterSetter() {
        asset.setVolume(31260929299.52f);
        assertEquals(31260929299.52f, asset.getVolume(), DELTA);
    }

    @Test
    void testChangeGetterSetter() {
        asset.setChange(3.6372f);
        assertEquals(3.6372f, asset.getChange(), DELTA);
    }

    @Test
    void testTimestampGetterSetter() {
        asset.setTimestamp(1711356300L);
        assertEquals(1711356300L, asset.getTimestamp());
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = """
            {
                "coin_symbol": "btc",
                "usd": 67187.34,
                "usd_market_cap": 1317802988326.25,
                "usd_24h_vol": 31260929299.52,
                "usd_24h_change": 3.6372,
                "last_updated_at": 1711356300
            }""";

        Asset asset = mapper.readValue(json, Asset.class);

        assertEquals("btc", asset.getCoinSymbol());
        assertEquals(67187.34f, asset.getPrice(), DELTA);
        assertEquals(1317802988326.25f, asset.getMarketCap(), DELTA);
        assertEquals(31260929299.52f, asset.getVolume(), DELTA);
        assertEquals(3.6372f, asset.getChange(), DELTA);
        assertEquals(1711356300L, asset.getTimestamp());
    }

}