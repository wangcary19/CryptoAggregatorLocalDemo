package com.bitwise.demo.CryptoAggregatorLocalDemo.controller;

import com.bitwise.demo.CryptoAggregatorLocalDemo.repository.AssetRepository;
import com.bitwise.demo.CryptoAggregatorLocalDemo.service.AssetPriceFetchService;
import com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.bitwise.demo.CryptoAggregatorLocalDemo.component.Constants.PREFETCH_LIMIT;

@Controller
public class PrefetchController {

    private final AssetRepository db;
    private final Utilities tools;
    private final AssetPriceFetchService apfs;
    private final AssetPriceFetchController apfc;

    @Autowired // Constructor-based dependency injection for the utility class
    public PrefetchController(com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities tools, AssetRepository assetRepository, AssetPriceFetchService assetPriceFetchService, AssetPriceFetchController assetPriceFetchController) {
        this.tools = tools;
        this.db = assetRepository;
        this.apfs = assetPriceFetchService;
        this.apfc = assetPriceFetchController;
    }

    @PostConstruct
    public void prefetch() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(PREFETCH_LIMIT);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String fromDate = thirtyDaysAgo.format(formatter);
        String toDate = today.format(formatter);

        apfc.getPriceHistory("bitcoin", fromDate, toDate);
        apfc.getPriceHistory("ethereum", fromDate, toDate);
    }
}
