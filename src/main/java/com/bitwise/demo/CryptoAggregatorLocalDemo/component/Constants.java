package com.bitwise.demo.CryptoAggregatorLocalDemo.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Constants {

    public static int PREFETCH_LIMIT;
    public static String API_KEY;
    public static int RATE_LIMITER_LIMIT;

    public static final String BASE_URL_CURRENT_PRICES = "https://api.coingecko.com/api/v3/simple/price?";
    public static final String BASE_URL_PAST_PRICE = "https://api.coingecko.com/api/v3/coins/";
    public static final String BASE_URL_PRICE_HISTORY = "https://api.coingecko.com/api/v3/coins/";
    public static final String REF_CURR_PARAM = "&vs_currencies=usd"; // Default to USD, can be changed later

    public Constants(
            @Value("${crypto.prefetch.limit}") int prefetchLimit,
            @Value("${crypto.api.key}") String apiKey,
            @Value("${rate.limiter.limit}") int rateLimiterLimit) {

        PREFETCH_LIMIT = prefetchLimit;
        API_KEY = "x_cg_demo_api_key=" + apiKey;
        RATE_LIMITER_LIMIT = rateLimiterLimit;
    }
}
