package com.bitwise.demo.CryptoAggregatorLocalDemo.pojo;

/* Defines a POJO object for information about a cryptocurrency coin.
   Used by the Jackson mapper to auto-parse JSON into Java objects.
   Last updated on July 4th, 2025 according to https://api.coingecko.com/api/v3/coins/list
   Note that because CoinGecko does not publish a JSON schema, this class is not generated via schema at build time and must be updated manually.

    Example JSON response for Bitcoin asset:
    {
        {
        "id": "apricot",
        "symbol": "aprt",
        "name": "Apricot"
        }
    }
 */


import com.fasterxml.jackson.annotation.JsonProperty;

public class CoinInfo {
    @JsonProperty("id")
    public String coinID;

    @JsonProperty("symbol")
    public String coinSymbol; // Price of asset, in USD

    @JsonProperty("name")
    public String coinName; // Market cap of asset, in USD

    public CoinInfo() { // No-args constructor for Jackson to use
        // This constructor is required for Jackson to deserialize JSON into this class.
    }

    public CoinInfo(String coinID, String coinSymbol, String coinName) {
        this.coinID = coinID;
        this.coinSymbol = coinSymbol;
        this.coinName = coinName;
    }

    public String getCoinSymbol() {
        return coinSymbol;
    }

    public void setCoinSymbol(String coinSymbol) {
        this.coinSymbol = coinSymbol;
    }

    public String getCoinID() {
        return coinID;
    }

    public void setCoinID(String coinID) {
        this.coinID = coinID;
    }

    public String getCoinName() {
        return coinName;
    }

    public void setCoinName(String coinName) {
        this.coinName = coinName;
    }
}
