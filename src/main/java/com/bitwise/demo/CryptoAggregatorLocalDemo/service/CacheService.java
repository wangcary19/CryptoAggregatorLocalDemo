package com.bitwise.demo.CryptoAggregatorLocalDemo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    @Autowired
    private CacheManager cacheManager;

    public void clearAssetsCache() {
        if (cacheManager.getCache("ASSETS") != null) {
            cacheManager.getCache("ASSETS").clear();
        }
    }
}