package com.bitwise.demo.CryptoAggregatorLocalDemo.pojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

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
                67187.34f,
                1711356300L
        );

        assertEquals("bitcoin", asset.getID());
        assertEquals(67187.34f, asset.getPrice(), DELTA);
        assertEquals(1711356300L, asset.getTimestamp());
    }

    @Test
    void testPriceGetterSetter() {
        asset.setPrice(67187.34f);
        assertEquals(67187.34f, asset.getPrice(), DELTA);
    }

    @Test
    void testTimestampGetterSetter() {
        asset.setTimestamp(1711356300L);
        assertEquals(1711356300L, asset.getTimestamp());
    }

    @Test
    void testJsonDeserialization() throws Exception {
        // This JSON structure matches the format from CoinGecko API:
        // {"bitcoin": {"usd": 108796}}
        String json = """
        {
            "bitcoin": {
                "usd": 67187.34
            }
        }""";

        // Since we need to handle the nested structure manually:
        JsonNode rootNode = mapper.readTree(json);
        JsonNode bitcoinNode = rootNode.get("bitcoin");

        Asset asset = new Asset();
        asset.setID("bitcoin");
        asset.setPrice((float) bitcoinNode.get("usd").asDouble());

        assertEquals("bitcoin", asset.getID());
        assertEquals(67187.34f, asset.getPrice(), DELTA);
    }

    @Test
    void testMultipleAssetsDeserialization() throws Exception {
        String json = """
        {
            "bitcoin": {
                "usd": 108796
            },
            "ethereum": {
                "usd": 2547.7
            }
        }""";

        JsonNode rootNode = mapper.readTree(json);

        // Create list to hold the assets
        List<Asset> assets = new ArrayList<>();

        // Process each cryptocurrency in the JSON
        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String coinId = entry.getKey();
            JsonNode coinData = entry.getValue();

            Asset asset = new Asset();
            asset.setID(coinId);
            asset.setPrice((float) coinData.get("usd").asDouble());
            assets.add(asset);
        }

        assertEquals(2, assets.size());
        assertEquals("bitcoin", assets.get(0).getID());
        assertEquals(108796f, assets.get(0).getPrice(), DELTA);
        assertEquals("ethereum", assets.get(1).getID());
        assertEquals(2547.7f, assets.get(1).getPrice(), DELTA);
    }

}