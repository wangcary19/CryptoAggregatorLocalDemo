package com.bitwise.demo.CryptoAggregatorLocalDemo.service;

import com.bitwise.demo.CryptoAggregatorLocalDemo.handler.CryptoAggregatorException;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.Asset;
import com.bitwise.demo.CryptoAggregatorLocalDemo.repository.AssetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.bitwise.demo.CryptoAggregatorLocalDemo.component.Constants.BASE_URL_CURRENT_PRICES;
import static com.bitwise.demo.CryptoAggregatorLocalDemo.component.Constants.BASE_URL_PAST_PRICE;
import static com.bitwise.demo.CryptoAggregatorLocalDemo.component.Constants.BASE_URL_PRICE_HISTORY;
import static com.bitwise.demo.CryptoAggregatorLocalDemo.component.Constants.API_KEY;
import static com.bitwise.demo.CryptoAggregatorLocalDemo.component.Constants.REF_CURR_PARAM;

import java.io.IOException;
import java.util.*;

@Service
public class AssetPriceFetchService {

    private final Logger logger = LoggerFactory.getLogger(AssetPriceFetchService.class);

    private final com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities tools;
    private final AssetRepository db;

    @Autowired // Constructor-based dependency injection for the utility class
    public AssetPriceFetchService(com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities tools, AssetRepository assetRepository) {
        this.tools = tools;
        this.db = assetRepository;
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
        StringBuffer newURL = new StringBuffer(BASE_URL_CURRENT_PRICES + API_KEY + REF_CURR_PARAM + "&ids=");
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
    public String assembleQueryURLforPastPrice(String requestedCurrency, String date) throws CryptoAggregatorException {
        StringBuffer newURL = new StringBuffer(BASE_URL_PAST_PRICE + requestedCurrency + "/history?date=" + date + "&" + API_KEY);
        return newURL.toString();
    }

    /**
     * This method assembles the query URL for the upstream API for the historical price history of a user-requested ID.
     *
     * @param requestedCurrency The requested currency symbol.
     * @param fromDate          The start date for the price history in Unix timestamp format.
     * @param toDate            The end date for the price history in Unix timestamp format.
     * @return The complete query URL as a String.
     * @throws CryptoAggregatorException If an invalid asset ID is requested.
     */
    public String assembleQueryURLforPriceHistory(String requestedCurrency, String fromDate, String toDate) throws CryptoAggregatorException {
        StringBuffer newURL = new StringBuffer(BASE_URL_PRICE_HISTORY + requestedCurrency + "/market_chart/range?vs_currency=usd&from=" + fromDate + "&to=" + toDate + "&" + API_KEY);
        return newURL.toString();
    }

    /**
     * Parses the JSON response from CoinGecko's current prices API endpoint
     * and converts it into a list of Asset objects.
     *
     * @param response The JSON response string from the API
     * @return A list of Asset objects populated with id and price data
     * @throws CryptoAggregatorException if JSON parsing fails
     */
    public List<Asset> parseResponseForCurrentPrices(String response) throws CryptoAggregatorException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Asset> assets = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(response);

            // Iterate through each cryptocurrency in the response
            Iterator<Map.Entry<String, JsonNode>> coins = rootNode.fields();
            while (coins.hasNext()) {
                Map.Entry<String, JsonNode> coinEntry = coins.next();
                String coinId = coinEntry.getKey();  // This is "bitcoin", "ethereum", etc.
                JsonNode coinData = coinEntry.getValue();

                // Extract USD price
                if (coinData.has("usd")) {
                    float usdPrice = (float) coinData.get("usd").asDouble();

                    // Create and populate a new Asset object
                    Asset asset = new Asset();

                    asset.setID(coinId);
                    asset.setPrice(usdPrice);
                    asset.setTimestamp(System.currentTimeMillis() / 1000); // Current time in Unix epoch

                    saveAsset(asset); // Save the asset to the database
                    assets.add(cacheAsset(asset)); // Cache the asset before adding to the list
                }
            }

            return assets;
        } catch (IOException e) {
            logger.error("Failed to parse current prices response: {}", e.getMessage());
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

            saveAsset(asset);
            return cacheAsset(asset);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing response: {}", e.getMessage());
            throw new CryptoAggregatorException("PROCESS.01");
        }
    }

    /**
     * This method parses the price history response from the upstream API and returns a list of Asset objects.
     *
     * @param responseBody The JSON response body from the upstream API for price history.
     * @return A list of Asset objects parsed from the response.
     * @throws CryptoAggregatorException If there is an error during parsing.
     */
    public List<Asset> parseResponseForPriceHistory(String responseBody, String coinID) throws CryptoAggregatorException {
        ObjectMapper mapper = new ObjectMapper();
        List<Asset> assets = new ArrayList<>();

        try {
            JsonNode rootNode = mapper.readTree(responseBody);
            JsonNode pricesNode = rootNode.get("prices");

            if (pricesNode.isArray()) {
                for (JsonNode priceNode : pricesNode) {
                    Asset asset = new Asset();
                    asset.setID(coinID);
                    asset.setTimestamp(priceNode.get(0).asLong() / 1000); // Convert ms to seconds
                    asset.setPrice(priceNode.get(1).floatValue()); // Price in USD

                    saveAsset(asset);
                    assets.add(cacheAsset(asset)); // Cache the asset before adding to the list
                }
            }
            return assets;
        } catch (JsonProcessingException e) {
            logger.error("Error parsing response: {}", e.getMessage());
            throw new CryptoAggregatorException("PROCESS.01");
        }
    }

    /**
     * This method intercepts and caches the Asset object using its timestamp as the key, if there are one or more Asset objects.
     *
     * @param asset The Asset object to cache.
     * @return The cached Asset object.
     */
    @CachePut(value = "ASSETS", key = "#result.getID().concat('_').concat(#result.getTimestamp())")
    public Asset cacheAsset(Asset asset) {
        logger.debug("Attempting to cache asset: {}", asset.getTimestamp());
        return asset;
    }

    /**
     * This method checks the cache for an Asset object using its timestamp as the key.
     * If the asset is not found in the cache, it returns null.
     *
     * @param timestamp The timestamp to check in the cache.
     * @return The cached Asset object or null if not found.
     */
    @Cacheable(value = "ASSETS", key = "#id.concat('_').concat(#timestamp)", unless = "#result == null")
    public Asset checkCacheForAsset(String id, Long timestamp) {
        logger.debug("Cache miss for {} asset at timestamp {}", id, timestamp);
        return null; // Returns null on cache miss
    }

    /**
     * This method retrieves a list of Asset objects from the database using a composite key.
     * The composite key is formed by concatenating the asset ID and timestamp with an underscore.
     *
     * @param id        The asset ID.
     * @param timestamp The timestamp in Unix epoch format.
     * @return A list of Asset objects matching the composite key.
     */
    public List<Asset> retrieveAssets(String id, long timestamp) {
        String compositeKey = id + "_" + timestamp;
        List<Asset> listOfAssets = db.findByCompositeKey(compositeKey);
        if (listOfAssets == null || listOfAssets.isEmpty()) {
            logger.info("No assets found for composite key: {}", compositeKey);
            return null;
        } else {
            logger.info("Found assets for composite key {}", compositeKey);
            return listOfAssets;
        }
    }

    /**
     * This method adds an Asset object to the database if it is not already present.
     * It checks for existing assets using a composite key formed by the asset ID and timestamp.
     *
     * @param asset The Asset object to add.
     */
    public void saveAsset(Asset asset) {
        String compositeKey = asset.getCompositeKey();
        List<Asset> existingAssets = db.findByCompositeKey(compositeKey);
        if (existingAssets == null || existingAssets.isEmpty()) {
            db.save(asset);
            logger.info("Saved asset to database: {}", asset.getCompositeKey());
        }
    }

    /**
     * This method retrieves a list of Asset objects for a given asset ID within a specified date range.
     * The date range is defined by fromDate and toDate in dd-MM-yyyy format.
     *
     * @param id       The asset ID.
     * @param fromDate The start date in dd-MM-yyyy format.
     * @param toDate   The end date in dd-MM-yyyy format.
     * @return A list of Asset objects within the specified date range.
     */
    public List<Asset> retrieveAssetsForDateRange(String id, String fromDate, String toDate) {
        try {
            // Convert fromDate and toDate to Unix timestamps
            long fromTimestamp = tools.convertToUnixTime(fromDate);
            long toTimestamp = tools.convertToUnixTime(toDate);

            // Query the database for assets within the date range
            List<Asset> assets = db.findByIdAndTimestampBetween(id, fromTimestamp, toTimestamp);

            if (assets == null || assets.isEmpty()) {
                logger.info("No assets found for ID {} in the date range {} to {} in DB", id, fromDate, toDate);
                return Collections.emptyList();
            }

            logger.info("Found {} assets for ID {} in the date range {} to {} in DB", assets.size(), id, fromDate, toDate);
            return assets;
        } catch (Exception e) {
            logger.error("Error retrieving assets for ID {} in the date range {} to {}: {}", id, fromDate, toDate, e.getMessage());
            throw new CryptoAggregatorException("DATABASE.01");
        }
    }
}
