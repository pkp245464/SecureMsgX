package com.secure.MsgX.features.repository;

import com.secure.MsgX.core.entity.ApiUsageMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiUsageMetricsRepository extends JpaRepository<ApiUsageMetrics, String> {

}
