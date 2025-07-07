package com.bitwise.demo.CryptoAggregatorLocalDemo.controller;

import com.bitwise.demo.CryptoAggregatorLocalDemo.service.CacheService;
import com.bitwise.demo.CryptoAggregatorLocalDemo.service.DatabaseManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;

class SystemControllerTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private DatabaseManagementService databaseManagementService;

    @InjectMocks
    private SystemController systemController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testClearCache_CallsServiceAndLogs() {
        systemController.clearCache();
        verify(cacheService).clearAssetsCache();
    }

    @Test
    void testClearDatabase_CallsServiceAndLogs() {
        systemController.clearDatabase();
        verify(databaseManagementService).clearDatabase();
    }
}