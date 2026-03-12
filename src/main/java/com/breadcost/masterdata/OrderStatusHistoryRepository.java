package com.breadcost.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, String> {
    List<OrderStatusHistoryEntity> findByOrderIdOrderByTimestampEpochMsAsc(String orderId);
}
