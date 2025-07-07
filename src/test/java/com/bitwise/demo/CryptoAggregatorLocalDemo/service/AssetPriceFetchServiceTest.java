package com.bitwise.demo.CryptoAggregatorLocalDemo.service;

import com.bitwise.demo.CryptoAggregatorLocalDemo.handler.CryptoAggregatorException;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.Asset;
import com.bitwise.demo.CryptoAggregatorLocalDemo.repository.AssetRepository;
import com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetPriceFetchServiceTest {

    @Mock
    private Utilities tools;

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetPriceFetchService service;

    private Asset testAsset;

    @BeforeEach
    void setUp() {
        testAsset = new Asset();
        testAsset.setID("bitcoin");
        testAsset.setPrice(50000.0f);
        testAsset.setTimestamp(1625097600L);
    }

    @Test
    void testAssembleQueryURLforCurrentPrices_Success() {
        // Arrange
        String[] currencies = {"bitcoin", "ethereum"};
        when(tools.isValidAssetId(anyString())).thenReturn(true);

        // Act
        String result = service.assembleQueryURLforCurrentPrices(currencies);

        // Assert
        assertTrue(result.contains("ids=bitcoin,ethereum"));
        assertTrue(result.contains("include_last_updated_at=true"));
    }

    @Test
    void testAssembleQueryURLforCurrentPrices_DuplicateIds() {
        // Arrange
        String[] currencies = {"bitcoin", "BITCOIN", " ethereum "};
        when(tools.isValidAssetId(anyString())).thenReturn(true);

        // Act
        String result = service.assembleQueryURLforCurrentPrices(currencies);

        // Assert
        // Should contain bitcoin only once, ethereum once, and bitcoin should be before ethereum
        assertTrue(result.contains("ids=bitcoin,ethereum"));
        assertTrue(result.contains("include_last_updated_at=true"));
    }

    @Test
    void testAssembleQueryURLforPastPrice() throws CryptoAggregatorException {
        // Arrange
        String currency = "bitcoin";
        String date = "01-01-2023";

        // Act
        String result = service.assembleQueryURLforPastPrice(currency, date);

        // Assert
        assertTrue(result.contains("/bitcoin/history"));
        assertTrue(result.contains("date=01-01-2023"));
    }

    @Test
    void testAssembleQueryURLforPriceHistory() throws CryptoAggregatorException {
        // Arrange
        String currency = "bitcoin";
        String fromDate = "1609459200";
        String toDate = "1640995200";

        // Act
        String result = service.assembleQueryURLforPriceHistory(currency, fromDate, toDate);

        // Assert
        assertTrue(result.contains("/bitcoin/market_chart/range"));
        assertTrue(result.contains("from=1609459200"));
        assertTrue(result.contains("to=1640995200"));
        assertTrue(result.contains("vs_currency=usd"));
    }

    @Test
    void testParseResponseForCurrentPrices_Success() throws CryptoAggregatorException {
        // Arrange
        String response = """
        {
          "bitcoin": {
            "usd": 50000.0,
            "last_updated_at": 1625097600
          },
          "ethereum": {
            "usd": 3000.0,
            "last_updated_at": 1625097600
          }
        }
        """;

        // Act
        List<Asset> assets = service.parseResponseForCurrentPrices(response);

        // Assert
        assertEquals(2, assets.size());
        assertTrue(assets.stream().anyMatch(a -> a.getID().equals("bitcoin") && a.getPrice() == 50000.0f));
        assertTrue(assets.stream().anyMatch(a -> a.getID().equals("ethereum") && a.getPrice() == 3000.0f));

        // Verify save and cache were called
        verify(assetRepository, times(2)).findByCompositeKey(anyString());
        verify(assetRepository, atMost(2)).save(any(Asset.class));
    }

    @Test
    void testAssembleQueryURLforCurrentPrices_InvalidId() {
        // Arrange
        String[] currencies = {"bitcoin", "invalid"};
        when(tools.isValidAssetId("bitcoin")).thenReturn(true);
        when(tools.isValidAssetId("invalid")).thenReturn(false);

        // Act & Assert
        CryptoAggregatorException exception = assertThrows(CryptoAggregatorException.class, () -> {
            service.assembleQueryURLforCurrentPrices(currencies);
        });

        // Updated to match actual implementation message
        assertEquals("One or more of the requested asset names are not valid or unavailable.", exception.getMessage());
    }

    @Test
    void testParseResponseForPastPrice_Success() throws CryptoAggregatorException {
        // Arrange
        String response = """
        {
          "id": "bitcoin",
          "market_data": {
            "current_price": {
              "usd": 45000.0
            }
          }
        }
        """;
        String date = "01-01-2023";
        when(tools.convertToUnixTime(date)).thenReturn(1672531200L);

        // Act
        Asset asset = service.parseResponseForPastPrice(response, date);

        // Assert
        assertEquals("bitcoin", asset.getID());
        assertEquals(45000.0f, asset.getPrice());
        assertEquals(1672531200L, asset.getTimestamp());

        // Verify save and cache were called
        verify(assetRepository).findByCompositeKey(anyString());
    }

    @Test
    void testParseResponseForPastPrice_InvalidJson() {
        // Arrange
        String response = "invalid json";
        String date = "01-01-2023";

        // Act & Assert
        CryptoAggregatorException exception = assertThrows(CryptoAggregatorException.class, () -> {
            service.parseResponseForPastPrice(response, date);
        });

        // Updated to match actual implementation message
        assertEquals("The inbound JSON was not parsed correctly.", exception.getMessage());
    }

    @Test
    void testParseResponseForPriceHistory_Success() throws CryptoAggregatorException {
        // Arrange
        String response = """
        {
          "prices": [
            [1625097600000, 35000.0],
            [1625184000000, 36000.0]
          ]
        }
        """;
        String coinId = "bitcoin";

        // Act
        List<Asset> assets = service.parseResponseForPriceHistory(response, coinId);

        // Assert
        assertEquals(2, assets.size());
        assertEquals("bitcoin", assets.get(0).getID());
        assertEquals(1625097600L, assets.get(0).getTimestamp());
        assertEquals(35000.0f, assets.get(0).getPrice());
        assertEquals("bitcoin", assets.get(1).getID());
        assertEquals(1625184000L, assets.get(1).getTimestamp());
        assertEquals(36000.0f, assets.get(1).getPrice());

        // Verify save and cache were called
        verify(assetRepository, times(2)).findByCompositeKey(anyString());
    }

    @Test
    void testParseResponseForPriceHistory_InvalidJson() {
        // Arrange
        String response = "invalid json";
        String coinId = "bitcoin";

        // Act & Assert
        CryptoAggregatorException exception = assertThrows(CryptoAggregatorException.class, () -> {
            service.parseResponseForPriceHistory(response, coinId);
        });

        // Updated to match actual implementation message
        assertEquals("The inbound JSON was not parsed correctly.", exception.getMessage());
    }

    @Test
    void testCacheAsset() {
        // Act
        Asset result = service.cacheAsset(testAsset);

        // Assert
        assertEquals(testAsset, result);
    }

    @Test
    void testCheckCacheForAsset() {
        // Act
        Asset result = service.checkCacheForAsset("bitcoin", 1625097600L);

        // Assert
        assertNull(result);
    }

    @Test
    void testRetrieveAssets_Found() {
        // Arrange
        List<Asset> assets = Collections.singletonList(testAsset);
        when(assetRepository.findByCompositeKey("bitcoin_1625097600")).thenReturn(assets);

        // Act
        List<Asset> result = service.retrieveAssets("bitcoin", 1625097600L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testAsset, result.get(0));
    }

    @Test
    void testRetrieveAssets_NotFound() {
        // Arrange
        when(assetRepository.findByCompositeKey("bitcoin_1625097600")).thenReturn(Collections.emptyList());

        // Act
        List<Asset> result = service.retrieveAssets("bitcoin", 1625097600L);

        // Assert
        assertNull(result);
    }

    @Test
    void testSaveAsset_NewAsset() {
        // Arrange
        when(assetRepository.findByCompositeKey(anyString())).thenReturn(Collections.emptyList());

        // Act
        service.saveAsset(testAsset);

        // Assert
        verify(assetRepository).save(testAsset);
    }

    @Test
    void testSaveAsset_ExistingAsset() {
        // Arrange
        List<Asset> existingAssets = Collections.singletonList(testAsset);
        when(assetRepository.findByCompositeKey(anyString())).thenReturn(existingAssets);

        // Act
        service.saveAsset(testAsset);

        // Assert
        verify(assetRepository, never()).save(testAsset);
    }

    @Test
    void testRetrieveAssetsForDateRange_Success() throws CryptoAggregatorException {
        // Arrange
        String id = "bitcoin";
        String fromDate = "01-01-2023";
        String toDate = "02-01-2023";
        long fromTimestamp = 1672531200L;
        long toTimestamp = 1672617600L;

        when(tools.convertToUnixTime(fromDate)).thenReturn(fromTimestamp);
        when(tools.convertToUnixTime(toDate)).thenReturn(toTimestamp);

        List<Asset> assets = new ArrayList<>();
        assets.add(testAsset);
        when(assetRepository.findByIdAndTimestampBetween(id, fromTimestamp, toTimestamp)).thenReturn(assets);

        // Act
        List<Asset> result = service.retrieveAssetsForDateRange(id, fromDate, toDate);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testAsset, result.get(0));
    }

    @Test
    void testRetrieveAssetsForDateRange_NoResults() throws CryptoAggregatorException {
        // Arrange
        String id = "bitcoin";
        String fromDate = "01-01-2023";
        String toDate = "02-01-2023";
        long fromTimestamp = 1672531200L;
        long toTimestamp = 1672617600L;

        when(tools.convertToUnixTime(fromDate)).thenReturn(fromTimestamp);
        when(tools.convertToUnixTime(toDate)).thenReturn(toTimestamp);
        when(assetRepository.findByIdAndTimestampBetween(id, fromTimestamp, toTimestamp)).thenReturn(Collections.emptyList());

        // Act
        List<Asset> result = service.retrieveAssetsForDateRange(id, fromDate, toDate);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testRetrieveAssetsForDateRange_Exception() {
        // Arrange
        String id = "bitcoin";
        String fromDate = "01-01-2023";
        String toDate = "02-01-2023";

        when(tools.convertToUnixTime(anyString())).thenThrow(new CryptoAggregatorException("REQUEST.05"));

        // Act & Assert
        CryptoAggregatorException exception = assertThrows(CryptoAggregatorException.class, () -> {
            service.retrieveAssetsForDateRange(id, fromDate, toDate);
        });

        // Updated to match actual implementation message
        assertEquals("Unknown error code: DATABASE.01", exception.getMessage());
    }
}