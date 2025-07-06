package com.bitwise.demo.CryptoAggregatorLocalDemo.controller;

import com.bitwise.demo.CryptoAggregatorLocalDemo.service.CacheService;
import com.bitwise.demo.CryptoAggregatorLocalDemo.service.DatabaseManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SystemController {

    private final Logger logger = LoggerFactory.getLogger(SystemController.class);
    private final CacheService cs;
    private final DatabaseManagementService dms;

    @Autowired
    public SystemController(CacheService cs, DatabaseManagementService dms) {
        this.cs = cs;
        this.dms = dms;
    }

    @PostMapping("/system/clear-cache")
    public void clearCache() {
        cs.clearAssetsCache();
        logger.warn("Assets cache cleared successfully.");
    }

    @PostMapping("/system/clear-database")
    public void clearDatabase() {
        dms.clearDatabase();
        logger.warn("Database cleared successfully.");
    }
}
