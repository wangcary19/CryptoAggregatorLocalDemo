package com.bitwise.demo.CryptoAggregatorLocalDemo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseManagementService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void clearDatabase() {
        // Get all table names
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'", String.class);

        // Disable FK constraints
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        // Truncate each table
        for (String table : tables) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }

        // Enable FK constraints
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}