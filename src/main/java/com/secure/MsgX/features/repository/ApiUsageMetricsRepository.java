package com.secure.MsgX.features.repository;

import com.secure.MsgX.core.entity.ApiUsageMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiUsageMetricsRepository extends JpaRepository<ApiUsageMetrics, String> {
    @Query("SELECT a FROM ApiUsageMetrics a ORDER BY a.hitCount DESC")
    List<ApiUsageMetrics> findAllOrderByHitCountDesc();
}
