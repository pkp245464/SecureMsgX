package com.secure.MsgX.features.repository;

import com.secure.MsgX.core.entity.ReadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadLogRepository extends JpaRepository<ReadLog, String> {

}
