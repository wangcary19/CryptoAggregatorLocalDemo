package com.bitwise.demo.CryptoAggregatorLocalDemo.service;

import com.bitwise.demo.CryptoAggregatorLocalDemo.handler.CryptoAggregatorException;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.Asset;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AssetPriceFetchService {

    private final Logger logger = LoggerFactory.getLogger(AssetPriceFetchService.class);

    private final com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities tools;

    private final String baseURLforCurrentPrices = "https://api.coingecko.com/api/v3/simple/price?";
    private final String baseURLforHistoricalPrice = "https://api.coingecko.com/api/v3/coins/";
    private final String apiKeyParameter = "x_cg_demo_api_key=CG-nXwsM6A5wv77DZGvppaLqJi5";
    private final String referenceCurrencyParameter = "&vs_currencies=usd"; // Default to USD, can be changed later

    @Autowired // Constructor-based dependency injection for the utility class
    public AssetPriceFetchService(com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities tools) {
        this.tools = tools;
    }

    /**
     * This method assembles the query URL for the upstream API for the current prices of the user-requested IDs.
     * It filters out duplicate ID requests.
     *
     * @param requestedCurrencies Array of requested currency symbols.
     * @return The complete query URL as a String.
     * @throws CryptoAggregatorException If an invalid asset ID is requested.
     */
    public String assembleQueryURLforCurrentPrices(String[] requestedCurrencies) {
        StringBuffer newURL = new StringBuffer(baseURLforCurrentPrices + apiKeyParameter + referenceCurrencyParameter + "&ids=");
        Set<String> added = new HashSet<>();
        boolean first = true;

        for (String currency : requestedCurrencies) {
            String normalized = currency.trim().toLowerCase();
            if (tools.isValidAssetId(normalized)) {
                if (!added.contains(normalized)) {
                    if (!first) {
                        newURL.append(",");
                    }
                    newURL.append(normalized);
                    added.add(normalized);
                    first = false;
                }
            } else {
                logger.error("Invalid asset ID requested: {}", currency);
                throw new CryptoAggregatorException("REQUEST.02");
            }
        }
        newURL.append("&include_last_updated_at=true");
        return newURL.toString();
    }

    /**
     * This method assembles the query URL for the upstream API for the historical price of a user-requested ID.
     *
     * @param requestedCurrency Array of requested currency symbols.
     * @param date              The date for which the historical price is requested, in dd-MM-yyyy format; must not exceed more than 365 days before today, due to free API restrictions.
     * @return The complete query URL as a String.
     * @throws CryptoAggregatorException If an invalid asset ID is requested.
     */
    public String assembleQueryURLforHistoricalPrice(String requestedCurrency, String date) throws CryptoAggregatorException {
        StringBuffer newURL = new StringBuffer(baseURLforHistoricalPrice + requestedCurrency + "/history?date=" + date + "&" + apiKeyParameter);
        return newURL.toString();
    }

    /**
     * This method parses the response from the upstream API and converts it into a list of Asset objects.
     *
     * @param responseBody The JSON response body from the upstream API.
     * @return A list of Asset objects parsed from the response.
     * @throws CryptoAggregatorException If there is an error during parsing.
     */
    public List<Asset> parseResponseForCurrentPrices(String responseBody) throws CryptoAggregatorException {
        ObjectMapper mapper = new ObjectMapper();
        List<Asset> assets = new ArrayList<>();

        try {
            Map<String, Map<String, Object>> result = mapper.readValue(
                    responseBody,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Map.class)
            );

            for (Map.Entry<String, Map<String, Object>> entry : result.entrySet()) {
                String currency = entry.getKey();
                Map<String, Object> valueMap = entry.getValue();
                Map<String, Object> assetMap = new HashMap<>();
                assetMap.put("coin_symbol", currency);
                assetMap.putAll(valueMap);

                Asset asset = mapper.convertValue(assetMap, Asset.class);

                asset.setTimestamp(tools.getCurrentUnixTime()); // Set current timestamp as key for caching

                assets.add(cacheAsset(asset)); // Cache the asset before adding to the list
            }
            return assets;
        } catch (JsonProcessingException e) {
            logger.error("Error parsing response: {}", e.getMessage());
            throw new CryptoAggregatorException("PROCESS.01");
        }
    }

    /**
     * This method parses the historical price response from the upstream API and returns a map with the asset ID as the key and its price as the value.
     *
     * @param responseBody The JSON response body from the upstream API for historical prices.
     * @return A map with asset ID as key and its price as value.
     * @throws CryptoAggregatorException If there is an error during parsing.
     */
    @CachePut(value = "ASSETS", key = "#result.getTimestamp()")
    public Asset parseResponseForPastPrice(String responseBody, String dd_mm_yyyy) throws CryptoAggregatorException {
        ObjectMapper mapper = new ObjectMapper();
        Asset asset = new Asset();
        try {
            JsonNode rootNode = mapper.readTree(responseBody);

            asset.setID(rootNode.get("id").asText());

            JsonNode marketDataNode = rootNode.get("market_data");
            JsonNode currentPriceNode = marketDataNode.get("current_price");

            asset.setPrice(currentPriceNode.get("usd").floatValue());

            asset.setTimestamp(tools.convertToUnixTime(dd_mm_yyyy)); // Set key as historical date for cache hashing

            return cacheAsset(asset);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing response: {}", e.getMessage());
            throw new CryptoAggregatorException("PROCESS.01");
        }
    }

    @CachePut(value = "ASSETS", key = "#result.getTimestamp()")
    public Asset cacheAsset(Asset asset) {
        logger.debug("Attemping to cache asset: {}", asset.getTimestamp());
        return asset;
    }

    @Cacheable(value = "ASSETS", key = "#timestamp", unless = "#result == null")
    public Asset checkCacheForAsset(Long timestamp) {
        logger.debug("Cache miss for asset {} at timestamp {}", timestamp);
        return null; // Returns null on cache miss
    }
}
