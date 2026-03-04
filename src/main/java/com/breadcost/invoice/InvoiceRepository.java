package com.breadcost.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    List<InvoiceEntity> findByTenantId(String tenantId);
    List<InvoiceEntity> findByTenantIdAndCustomerId(String tenantId, String customerId);
    Optional<InvoiceEntity> findByTenantIdAndInvoiceId(String tenantId, String invoiceId);
    Optional<InvoiceEntity> findByTenantIdAndOrderId(String tenantId, String orderId);
    List<InvoiceEntity> findByTenantIdAndStatus(String tenantId, InvoiceEntity.InvoiceStatus status);
}
