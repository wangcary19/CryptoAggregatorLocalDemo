package com.bitwise.demo.CryptoAggregatorLocalDemo.constants;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticConstantsTest {

    @Test
    void testApiKeyParameter() {
        assertEquals("x_cg_demo_api_key=CG-nXwsM6A5wv77DZGvppaLqJi5",
                StaticConstants.apiKeyParameter);
    }

    @Test
    void testBaseURLforCurrentPrices() {
        assertEquals("https://api.coingecko.com/api/v3/simple/price?",
                StaticConstants.baseURLforCurrentPrices);
    }

    @Test
    void testBaseURLforPastPrice() {
        assertEquals("https://api.coingecko.com/api/v3/coins/",
                StaticConstants.baseURLforPastPrice);
    }

    @Test
    void testBaseURLforPriceHistory() {
        assertEquals("https://api.coingecko.com/api/v3/coins/",
                StaticConstants.baseURLforPriceHistory);
    }

    @Test
    void testReferenceCurrencyParameter() {
        assertEquals("&vs_currencies=usd",
                StaticConstants.referenceCurrencyParameter);
    }
}