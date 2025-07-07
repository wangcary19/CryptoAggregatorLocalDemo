// File: src/test/java/com/bitwise/demo/CryptoAggregatorLocalDemo/controller/PrefetchControllerTest.java
package com.bitwise.demo.CryptoAggregatorLocalDemo.controller;

import com.bitwise.demo.CryptoAggregatorLocalDemo.repository.AssetRepository;
import com.bitwise.demo.CryptoAggregatorLocalDemo.service.AssetPriceFetchService;
import com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrefetchControllerTest {

    @Mock
    private Utilities tools;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetPriceFetchService assetPriceFetchService;

    @Mock
    private AssetPriceFetchController assetPriceFetchController;

    @InjectMocks
    private PrefetchController prefetchController;

    @BeforeEach
    void setUp() {
        // No-op, handled by @InjectMocks
    }

    @Test
    void testPrefetch_CallsGetPriceHistoryForBitcoinAndEthereum() {
        // Arrange
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String date = today.format(formatter);

        // Act
        prefetchController.prefetch();

        // Assert
        verify(assetPriceFetchController).getPriceHistory(eq("bitcoin"), eq(date), eq(date));
        verify(assetPriceFetchController).getPriceHistory(eq("ethereum"), eq(date), eq(date));
        verifyNoMoreInteractions(assetPriceFetchController);
        verifyNoInteractions(assetRepository, assetPriceFetchService, tools);
    }
}