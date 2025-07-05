package com.bitwise.demo.CryptoAggregatorLocalDemo.controller;

import com.bitwise.demo.CryptoAggregatorLocalDemo.handler.CryptoAggregatorException;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.Asset;
import com.bitwise.demo.CryptoAggregatorLocalDemo.service.AssetPriceFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
public class AssetPriceFetchController {

    private final Logger logger = LoggerFactory.getLogger(AssetPriceFetchController.class);
    private final AssetPriceFetchService apfs; // Not to be confused with "Apple Proprietary File System"!

    @Autowired
    public AssetPriceFetchController(AssetPriceFetchService apfs) {
        this.apfs = apfs; // Dependency injection via constructor for the service class
    }

    //TODO: Currently, this method only supports querying by ID, i.e. the full name of the coin.
    // To enable more robust service, a parameter resolver should be implemented in the service class.
    @GetMapping("/current-prices/{ids}")
    public Object getAssetPrices(@PathVariable String ids) throws CryptoAggregatorException {
        // 1: Validate user request
        if (ids == null || ids.isEmpty()) {
            throw new CryptoAggregatorException("REQUEST.01"); // Custom exception for empty parameter
        }

        // 2: Build the query URL
        String queryURL = apfs.assembleQueryURL(ids.split(","));
        logger.info("Assembled query URL: {}", queryURL);

        // 3: Make the call to the upstream API
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.getForEntity(queryURL, String.class);

        // 4: Validate the response
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && !response.getBody().isEmpty()) {
            logger.info("Received response from upstream API for fetching current price(s): {}", response.getBody());
        }
        else {
            throw new CryptoAggregatorException("ALPHA.03");
        }

        // 5: Parse the response
        List<Asset> listOfAssets = apfs.parseResponse(response.getBody());

        // 6: Assemble the output
        return apfs.buildOutput(listOfAssets);
    }

    //TODO: Currently, this method only supports querying by price in USD.
    //@GetMapping("/past-price/{id}/{date}")



}
