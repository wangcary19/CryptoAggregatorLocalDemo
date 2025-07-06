package com.bitwise.demo.CryptoAggregatorLocalDemo.repository;

import com.bitwise.demo.CryptoAggregatorLocalDemo.pojo.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, String> {
    // Find by compositeKey
    @Query("SELECT a FROM Asset a WHERE a.compositeKey = :compositeKey")
    List<Asset> findByCompositeKey(@Param("compositeKey") String compositeKey);

    // Find by compositeKey and timestamp range
    @Query("SELECT a FROM Asset a WHERE a.compositeKey = :compositeKey AND a.timestamp BETWEEN :startTime AND :endTime")
    List<Asset> findByCompositeKeyAndTimestampBetween(
            @Param("compositeKey") String compositeKey,
            @Param("startTime") long startTime,
            @Param("endTime") long endTime
    );
}