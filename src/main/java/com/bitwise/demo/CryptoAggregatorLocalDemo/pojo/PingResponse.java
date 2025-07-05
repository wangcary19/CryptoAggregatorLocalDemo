package com.bitwise.demo.CryptoAggregatorLocalDemo.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

/* Defines a POJO object for the CoinGecko API ping.
   Used by the Jackson mapper to auto-parse JSON into Java objects.
   Last updated on July 4th, 2025 according to https://api.coingecko.com/api/v3/ping
   Note that because CoinGecko does not publish a JSON schema, this class is not generated via schema at build time and must be updated manually.

    Example JSON response for a ping request:
    {
        "gecko_says": "(V3) To the Moon! (V3)"
    }
 */

public class PingResponse {

    @JsonProperty("gecko_says")
    private String pingMessage;

    public String getPingMessage() {
        return pingMessage;
    }

    public void setPingMessage(String pingMessage) {
        this.pingMessage = pingMessage;
    }

}
