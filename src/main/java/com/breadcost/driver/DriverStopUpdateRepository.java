package com.breadcost.driver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverStopUpdateRepository extends JpaRepository<DriverStopUpdateEntity, String> {

    List<DriverStopUpdateEntity> findBySessionId(String sessionId);

    List<DriverStopUpdateEntity> findBySessionIdAndRunOrderId(String sessionId, String runOrderId);
}
