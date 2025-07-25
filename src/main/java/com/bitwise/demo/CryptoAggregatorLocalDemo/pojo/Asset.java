package com.bitwise.demo.CryptoAggregatorLocalDemo.pojo;

/* Defines a POJO object for a single cryptocurrency asset.
   Used by the Jackson mapper to auto-parse JSON into Java objects.
   Last updated on July 4th, 2025 according to https://docs.coingecko.com/v3.0.1/reference/simple-price
   Note that because CoinGecko does not publish a JSON schema, this class is not generated via schema at build time and must be updated manually.

    Example JSON response for Bitcoin asset:
    {
        "bitcoin": {
        "usd": 67187.3358936566,
        "usd_market_cap": 1317802988326.25,
        "usd_24h_vol": 31260929299.5248,
        "usd_24h_change": 3.63727894677354,
        "last_updated_at": 1711356300
        }
    }
 */


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
public class Asset {

    @JsonProperty("id")
    public String id;

    @JsonProperty("usd")
    public float price; // Price of asset, in USD

    @JsonProperty("last_updated_at")
    public long timestamp; // Last updated timestamp of asset, in Unix epoch format, which is implemented as a long in Java

    @Id
    public String compositeKey; // Composite key for the asset, which is matches the Redis combination of the asset ID and the timestamp

    public Asset() { // No-args constructor for Jackson to use
        // This constructor is required for Jackson to deserialize JSON into this class.
    }

    public Asset(String id, float price, long timestamp) {
        this.id = id;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
        if (this.timestamp > 0) {
            generateCompositeKey();
        }
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        if (this.id != null && !this.id.isEmpty()) {
            generateCompositeKey();
        }
    }

    public String getCompositeKey() {return compositeKey;}

    public void setCompositeKey(String compositeKey) {this.compositeKey = compositeKey;}

    public void generateCompositeKey() {this.compositeKey = id + "_" + timestamp;}
}
