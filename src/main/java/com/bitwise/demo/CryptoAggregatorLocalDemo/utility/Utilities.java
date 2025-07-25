package com.bitwise.demo.CryptoAggregatorLocalDemo.utility;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class Utilities {

    private final Logger logger = LoggerFactory.getLogger(Utilities.class);

    private static final String SUPPORTED_ASSET_IDS_URL = "https://api.coingecko.com/api/v3/coins/list";

    private final List<String> validAssetIds = new CopyOnWriteArrayList<>(); // Use thread-safe list that blocks race writes on startup

    /**
     * This method is called at service startup to fetch a list of valid asset IDs from the upstream API.
     * It populates the validAssetIds list, which is used for parameter validation in other methods.
     *
     * @throws CryptoAggregatorException If there is an error fetching the asset IDs.
     */
    @PostConstruct
    public void fetchAndUpdateAssetIDs() throws CryptoAggregatorException {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Fetch JSON response as a String
            String response = restTemplate.getForObject(SUPPORTED_ASSET_IDS_URL, String.class);

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

                // Uncomment to view the full list of valid asset IDs; commented out as the response body is large
                //logger.info("Successfully fetched {} valid asset IDs from upstream API", validAssetIds.toString());
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
     * Validates that a given date string is in dd-mm-yyyy format, not in the future,
     * and not more than one year in the past.
     *
     * @param dateStr The date string to validate
     * @throws CryptoAggregatorException if date is invalid, in future, or more than a year in the past
     */
    public boolean isValidDate(String dateStr) throws CryptoAggregatorException {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate parsedDate = LocalDate.parse(dateStr, formatter);
            LocalDate today = LocalDate.now();

            if (parsedDate.isAfter(today)) {
                logger.error("Date cannot be in future: {}", dateStr);
                throw new CryptoAggregatorException("REQUEST.03");
            }

            LocalDate oneYearAgo = today.minusYears(1);
            if (parsedDate.isBefore(oneYearAgo)) {
                logger.error("Date cannot be more than one year in the past: {}", dateStr);
                throw new CryptoAggregatorException("REQUEST.06");
            }

            return true;
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format (should be dd-mm-yyyy): {}", dateStr);
            throw new CryptoAggregatorException("REQUEST.05");
        }
    }

    /**
     * This method builds a LinkedHashMap that auto-resolves to JSON format when returned as a response, in the format:
     * {{
     * "coin_symbol": "price"
     * }}
     *
     * @param assets List of Asset objects
     * @return Map with coin symbols as keys and their prices as values
     */
    public Map<String, Object> buildCurrentPricesOutput(List<Asset> assets) {
        Map<String, Object> output = new LinkedHashMap<>();
        for (Asset asset : assets) {
            output.put(asset.getID(), asset.getPrice());
        }
        return output;
    }

    /**
     * This method builds a LinkedHashMap that auto-resolves to JSON format when returned as a response, in the format:
     * {{
     * "coin_id": "price"
     * }}
     *
     * @param asset An Asset object
     * @return Map with coin symbols as keys and their prices as values
     */
    public Map<String, Object> buildPastPriceOutput(Asset asset) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(asset.getID(), asset.getPrice());
        return output;
    }

    /**
     * This method builds a LinkedHashMap that auto-resolves to JSON format when returned as a response, in the format:
     * {{
     * "coin_symbol": "price"
     * }}
     *
     * @param assets List of Asset objects
     * @return Map with coin symbols as keys and their prices as values
     */
    public Map<String, Object> buildPriceHistoryOutput(List<Asset> assets) {
        Map<String, Object> output = new LinkedHashMap<>();
        for (Asset asset : assets) {
            output.put(String.valueOf(asset.getTimestamp()), asset.getPrice());
        }
        return output;
    }

    /**
     * Converts a date string in dd-MM-yyyy format to Unix timestamp (seconds since epoch).
     *
     * @param dateStr The date string in dd-MM-yyyy format
     * @return Unix timestamp in seconds
     * @throws CryptoAggregatorException if date format is invalid
     */
    public long convertToUnixTime(String dateStr) throws CryptoAggregatorException {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate date = LocalDate.parse(dateStr, formatter);
            return date.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format while converting to Unix time: {}", dateStr);
            throw new CryptoAggregatorException("REQUEST.05");
        }
    }
}
