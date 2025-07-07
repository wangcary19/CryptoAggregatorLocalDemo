package com.bitwise.demo.CryptoAggregatorLocalDemo.controller;

import com.bitwise.demo.CryptoAggregatorLocalDemo.handler.CryptoAggregatorException;
import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.Asset;
import com.bitwise.demo.CryptoAggregatorLocalDemo.service.AssetPriceFetchService;
import com.bitwise.demo.CryptoAggregatorLocalDemo.utility.Utilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Allow lenient stubbing to avoid UnnecessaryStubbingException
class AssetPriceFetchControllerTest {

    @Mock
    private AssetPriceFetchService apfs;

    @Mock
    private Utilities tools;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AssetPriceFetchController controller;

    private Asset bitcoinAsset;
    private List<Asset> assetList;
    private Map<String, Object> responseMap;

    @BeforeEach
    void setUp() {
        bitcoinAsset = new Asset("bitcoin", 50000.0f, 1625097600L);
        Asset ethereumAsset = new Asset("ethereum", 3000.0f, 1625097600L);
        assetList = Arrays.asList(bitcoinAsset, ethereumAsset);

        responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("data", assetList);

        controller.setRestTemplate(restTemplate);
    }

    @Test
    void testGetPastPrice_FromCache() throws Exception {
        String id = "bitcoin";
        String date = "01-01-2023";
        long unixTime = 1672531200L;

        when(tools.isValidAssetId(id)).thenReturn(true);
        when(tools.isValidDate(date)).thenReturn(true);
        when(tools.convertToUnixTime(date)).thenReturn(unixTime);

        when(apfs.checkCacheForAsset(id, unixTime)).thenReturn(bitcoinAsset);
        when(tools.buildPastPriceOutput(bitcoinAsset)).thenReturn(responseMap);

        Object result = controller.getPastPrice(id, date);

        assertNotNull(result);
        assertEquals(responseMap, result);
        verify(tools).isValidAssetId(id);
        verify(tools).isValidDate(date);
        verify(tools).convertToUnixTime(date);
        verify(apfs, atLeastOnce()).checkCacheForAsset(id, unixTime);
        verify(tools).buildPastPriceOutput(bitcoinAsset);
        verify(restTemplate, never()).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void testGetPastPrice_FromDatabase() throws Exception {
        String id = "bitcoin";
        String date = "01-01-2023";
        long unixTime = 1672531200L;
        List<Asset> dbResults = Collections.singletonList(bitcoinAsset);

        when(tools.isValidAssetId(id)).thenReturn(true);
        when(tools.isValidDate(date)).thenReturn(true);
        when(tools.convertToUnixTime(date)).thenReturn(unixTime);

        when(apfs.checkCacheForAsset(id, unixTime)).thenReturn(null);
        when(apfs.retrieveAssets(eq(id), eq(unixTime))).thenReturn(dbResults);
        when(tools.buildPastPriceOutput(bitcoinAsset)).thenReturn(responseMap);

        Object result = controller.getPastPrice(id, date);

        assertNotNull(result);
        assertEquals(responseMap, result);
        verify(tools).isValidAssetId(id);
        verify(tools).isValidDate(date);
        verify(tools).convertToUnixTime(date);
        verify(apfs, atLeastOnce()).checkCacheForAsset(id, unixTime);
        verify(apfs, atLeastOnce()).retrieveAssets(eq(id), eq(unixTime));
        verify(tools).buildPastPriceOutput(bitcoinAsset);
        verify(restTemplate, never()).getForEntity(anyString(), eq(String.class));
    }

    @Test
    void testGetPastPrice_InvalidAssetId() {
        String id = "invalid";
        String date = "01-01-2023";

        when(tools.isValidAssetId(id)).thenReturn(false);

        CryptoAggregatorException exception = assertThrows(CryptoAggregatorException.class, () -> {
            controller.getPastPrice(id, date);
        });
        assertEquals("One or more of the requested asset names are not valid or unavailable.", exception.getMessage());
    }

    @Test
    void testGetPastPrice_InvalidDate() {
        String id = "bitcoin";
        String date = "invalid";

        when(tools.isValidAssetId(id)).thenReturn(true);
        when(tools.isValidDate(date)).thenReturn(false);

        CryptoAggregatorException exception = assertThrows(CryptoAggregatorException.class, () -> {
            controller.getPastPrice(id, date);
        });
        assertEquals("The request contains an invalid date format. Please use the format DD-MM-YYYY.", exception.getMessage());
    }

    @Test
    void testGetPriceHistory_InvalidAssetId() {
        String id = "invalid";
        String fromDate = "01-01-2023";
        String toDate = "10-01-2023";

        when(tools.isValidAssetId(id)).thenReturn(false);

        CryptoAggregatorException exception = assertThrows(CryptoAggregatorException.class, () -> {
            controller.getPriceHistory(id, fromDate, toDate);
        });
        assertEquals("One or more of the requested asset names are not valid or unavailable.", exception.getMessage());
    }
}