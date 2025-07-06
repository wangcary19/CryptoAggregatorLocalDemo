package com.bitwise.demo.CryptoAggregatorLocalDemo.component;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticConstantsTest {


    @Test
    void testBaseURLforCurrentPrices() {
        assertEquals("https://api.coingecko.com/api/v3/simple/price?",
                Constants.BASE_URL_CURRENT_PRICES);
    }

    @Test
    void testBaseURLforPastPrice() {
        assertEquals("https://api.coingecko.com/api/v3/coins/",
                Constants.BASE_URL_PAST_PRICE);
    }

    @Test
    void testBaseURLforPriceHistory() {
        assertEquals("https://api.coingecko.com/api/v3/coins/",
                Constants.BASE_URL_PRICE_HISTORY);
    }

    @Test
    void testReferenceCurrencyParameter() {
        assertEquals("&vs_currencies=usd",
                Constants.REF_CURR_PARAM);
    }
}