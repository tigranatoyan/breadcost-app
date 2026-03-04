package com.breadcost.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLineEntity, String> {
    List<InvoiceLineEntity> findByInvoiceId(String invoiceId);
    void deleteByInvoiceId(String invoiceId);
}
