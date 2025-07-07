package com.bitwise.demo.CryptoAggregatorLocalDemo.controller;

import com.bitwise.demo.CryptoAggregatorLocalDemo.handler.CryptoAggregatorException;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.Asset;
import com.bitwise.demo.CryptoAggregatorLocalDemo.service.AssetPriceFetchService;
import com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.bitwise.demo.CryptoAggregatorLocalDemo.component.Constants.PREFETCH_LIMIT;

@RestController
public class AssetPriceFetchController {

    private final Logger logger = LoggerFactory.getLogger(AssetPriceFetchController.class);
    private final AssetPriceFetchService apfs; // Not to be confused with "Apple Proprietary File System"!
    private final Utilities tools;
    private RestTemplate restTemplate;

    @Autowired
    public AssetPriceFetchController(AssetPriceFetchService apfs, Utilities tools) {
        this.tools = tools; // Dependency injection via constructor for the utility class
        this.apfs = apfs; // Dependency injection via constructor for the service class
    }

    // Global prefetchLimit: 30 days ago
    private final LocalDate prefetchLimit = LocalDate.now().minusDays(PREFETCH_LIMIT);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");


    //TODO: Currently, this method only supports querying by ID, i.e. the full name of the coin.
    // To enable more robust service, a parameter resolver should be implemented in the service class.
    @GetMapping("/current-prices/{ids}")
    public Object getCurrentPrices(@PathVariable String ids) throws CryptoAggregatorException {
        // 1: Validate user request
        if (ids == null || ids.isEmpty()) {
            throw new CryptoAggregatorException("REQUEST.01"); // Custom exception for empty parameter
        }

        // 2: Build the query URL
        String queryURL = apfs.assembleQueryURLforCurrentPrices(ids.split(","));
        logger.info("Assembled query URL: {}", queryURL);

        // 3: Make the call to the upstream API
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.getForEntity(queryURL, String.class);

        // 4: Validate the response
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && !response.getBody().isEmpty()) {
            logger.info("Received response from upstream API for fetching current price(s): {}", response.getBody());
        } else {
            throw new CryptoAggregatorException("ALPHA.03");
        }

        // 5: Parse the response (this parse function will cache the data in Redis and save to the database)
        List<Asset> listOfAssets = apfs.parseResponseForCurrentPrices(response.getBody());

        // 6: Assemble the output
        return tools.buildCurrentPricesOutput(listOfAssets);
    }


    //TODO: Currently, this method only supports querying for prices in USD.  Add another path variable to support other currencies.
    @GetMapping("/past-price/{id}/{date}")
    public Object getPastPrice(@PathVariable String id, @PathVariable String date) throws CryptoAggregatorException {
        // 1: Validate asset ID
        if (id == null || id.isEmpty()) {
            throw new CryptoAggregatorException("REQUEST.01");
        }
        if (!tools.isValidAssetId(id)) {
            throw new CryptoAggregatorException("REQUEST.02");
        }

        // 2: Validate date format
        if (date == null || date.isEmpty()) {
            throw new CryptoAggregatorException("REQUEST.03");
        }
        if (!tools.isValidDate(date)) {
            throw new CryptoAggregatorException("REQUEST.04");
        }

        // 3a: Check if the date is already in the cache, if so, return the cached data
        long unixTime = tools.convertToUnixTime(date);
        if (apfs.checkCacheForAsset(id, unixTime) == null) {
            logger.info("Data not in cache, proceeding to check database");
        }
        else {
            logger.info("Cache hit on {}", date);
            Asset cachedAsset = apfs.checkCacheForAsset(id, unixTime);
            return tools.buildPastPriceOutput(cachedAsset);
        }

        // 3b: Check if the date is already in the database, if so, return the data from the database
        if (apfs.retrieveAssets(id, unixTime) != null && !apfs.retrieveAssets(id, unixTime).isEmpty()) {
            logger.info("Database hit on {}", date);
            Asset cachedAsset = apfs.retrieveAssets(id, unixTime).getFirst();
            return tools.buildPastPriceOutput(cachedAsset);
        }
        logger.info("Data not in database, proceeding with API call");

        // 3c: Build query URL for past price
        String queryURL = apfs.assembleQueryURLforPastPrice(id, date);
        logger.info("Assembled query URL: {}", queryURL);

        // 4: Make API call
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.getForEntity(queryURL, String.class);

        // 5: Validate response
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null || response.getBody().isEmpty()) {
            logger.error("Failed to fetch historical price data");
            throw new CryptoAggregatorException("ALPHA.03");
        }
        // Debug: the response body can be large, so it's commented out for performance
        //logger.info("Received response from upstream API for fetching historical price: {}", response.getBody());

        // 6: Parse the response (this parse function will cache the data in Redis and save to the database)
        Asset asset = apfs.parseResponseForPastPrice(response.getBody(), date);

        return tools.buildPastPriceOutput(asset);
    }


    @GetMapping("/history/{id}/{fromDate}/{toDate}")
    public Object getPriceHistory(@PathVariable String id, @PathVariable String fromDate, @PathVariable String toDate) throws CryptoAggregatorException {
        // 1: Validate asset ID
        if (id == null || id.isEmpty()) {
            throw new CryptoAggregatorException("REQUEST.01");
        }
        if (!tools.isValidAssetId(id)) {
            throw new CryptoAggregatorException("REQUEST.02");
        }

        // 2: Validate date formats
        if (fromDate == null || fromDate.isEmpty() || toDate == null || toDate.isEmpty()) {
            throw new CryptoAggregatorException("REQUEST.03");
        }
        if (!tools.isValidDate(fromDate) || !tools.isValidDate(toDate)) {
            throw new CryptoAggregatorException("REQUEST.04");
        }

        // 3a: If fromDate is before prefetchLimit, serve from DB
        LocalDate from = LocalDate.parse(fromDate, formatter);
        if (from.isAfter(prefetchLimit)) {
            logger.info("fromDate {} is before prefetchLimit {}, serving from DB if possible", fromDate, prefetchLimit.format(formatter));
            List<Asset> assetHistory = apfs.retrieveAssetsForDateRange(id, fromDate, toDate);
            return tools.buildPriceHistoryOutput(assetHistory);
        }

        // 3c: Build query URL for historical data
        String queryURL = apfs.assembleQueryURLforPriceHistory(id, String.valueOf(tools.convertToUnixTime(fromDate)), String.valueOf(tools.convertToUnixTime(toDate)));
        logger.info("Assembled historical query URL for price history: {}", queryURL);

        // 4: Make API call
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.getForEntity(queryURL, String.class);

        // 5: Validate response
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null || response.getBody().isEmpty()) {
            logger.error("Failed to fetch price history data");
            throw new CryptoAggregatorException("ALPHA.03");
        }
        // Debug: the response body can be large, so it's commented out for performance
        //logger.info("Received response from upstream API for fetching price history: {}", response.getBody());

        // 6: Parse the response (this parse function will cache the data in Redis and save to the database)
        List<Asset> assetHistory = apfs.parseResponseForPriceHistory(response.getBody(), id);

        return tools.buildPriceHistoryOutput(assetHistory);
    }

    // Add to AssetPriceFetchController.java
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}