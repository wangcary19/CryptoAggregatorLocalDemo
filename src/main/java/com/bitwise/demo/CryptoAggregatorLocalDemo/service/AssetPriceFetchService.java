package com.bitwise.demo.CryptoAggregatorLocalDemo.service;

import com.bitwise.demo.CryptoAggregatorLocalDemo.handler.CryptoAggregatorException;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.Asset;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.CoinInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AssetPriceFetchService {

    private final Logger logger = LoggerFactory.getLogger(AssetPriceFetchService.class);
    private final List<String> validAssetIds = new CopyOnWriteArrayList<>(); // Use thread-safe list that blocks race writes on startup

    private static final String SUPPORTED_ASSET_IDS_URL = "https://api.coingecko.com/api/v3/coins/list";
    private final String baseURL = "https://api.coingecko.com/api/v3/simple/price?";
    private final String apiKeyParameter = "x_cg_demo_api_key=CG-nXwsM6A5wv77DZGvppaLqJi5";
    private final String referenceCurrencyParameter = "&vs_currencies=usd"; // Default to USD, can be changed later

    /**
     * This method is called at service startup to fetch a list of valid asset IDs from the upstream API.
     * It populates the validAssetIds list, which is used for parameter validation in other methods.
     *
     * @throws CryptoAggregatorException If there is an error fetching the asset IDs.
     */
    @PostConstruct
    public void fetchAndUpdateAssetIds() throws CryptoAggregatorException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Fetch JSON response as a String
            String response = restTemplate.getForObject("https://api.coingecko.com/api/v3/coins/list", String.class);

            if (response != null) {
                // Map JSON array to List<CoinInfo>
                List<CoinInfo> coins = mapper.readValue(
                        response,
                        mapper.getTypeFactory().constructCollectionType(List.class, CoinInfo.class)
                );

                // Clear and populate validAssetIds with coin IDs
                validAssetIds.clear();
                for (CoinInfo coin : coins) {
                    validAssetIds.add(coin.getCoinID().toLowerCase());
                }

                logger.info("Successfully fetched {} valid asset IDs from upstream API", validAssetIds.toString());
            } else {
                logger.error("Failed to fetch valid asset IDs - null response from API");
                throw new CryptoAggregatorException("ALPHA.02");
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse coin list response: {}", e.getMessage());
            throw new CryptoAggregatorException("PROCESS.01");
        }
    }

    /**
     * This method checks if the provided asset ID is valid by comparing it against a list of known valid asset IDs.
     *
     * @param assetId The asset ID to validate.
     * @return true if the asset ID is valid, false otherwise.
     */
    public boolean isValidAssetId(String assetId) {
        if (assetId == null || assetId.isEmpty()) {
            return false; // Null or empty asset ID is invalid
        }
        return validAssetIds.contains(assetId.trim().toLowerCase());
    }

    /**
     * This method assembles the query URL for the upstream API based on the requested currencies.
     * It filters out duplicate ID requests.
     *
     * @param requestedCurrencies Array of requested currency symbols.
     * @return The complete query URL as a String.
     * @throws CryptoAggregatorException If an invalid asset ID is requested.
     */

    public String assembleQueryURL(String[] requestedCurrencies) {
        StringBuffer newURL = new StringBuffer(baseURL + apiKeyParameter + referenceCurrencyParameter + "&ids=");
        Set<String> added = new HashSet<>();
        boolean first = true;

        for (String currency : requestedCurrencies) {
            String normalized = currency.trim().toLowerCase();
            if (isValidAssetId(normalized)) {
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
     * This method parses the response from the upstream API and converts it into a list of Asset objects.
     *
     * @param responseBody The JSON response body from the upstream API.
     * @return A list of Asset objects parsed from the response.
     * @throws CryptoAggregatorException If there is an error during parsing.
     */
    public List<Asset> parseResponse(String responseBody) throws CryptoAggregatorException {
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
                assets.add(asset);
            }
            return assets;
        } catch (JsonProcessingException e) {
            logger.error("Error parsing response: {}", e.getMessage());
            throw new CryptoAggregatorException("PROCESS.01");
        }
    }

    /**
     * This method builds a LinkedHashMap that auto-resolves to JSON format when returned as a response, in the format:
     * {{
     *   "coin_symbol": "price"
     * }}
     *
     * @param assets List of Asset objects
     * @return Map with coin symbols as keys and their prices as values
     */
    public Map<String, Object> buildOutput(List<Asset> assets) {
        Map<String, Object> output = new LinkedHashMap<>();
        for (Asset asset : assets) {
            output.put(asset.getCoinSymbol(), asset.getPrice());
        }
        return output;
    }



}
