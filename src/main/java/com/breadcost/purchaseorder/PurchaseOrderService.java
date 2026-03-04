package com.breadcost.purchaseorder;

import com.breadcost.supplier.SupplierCatalogItemRepository;
import com.breadcost.supplier.SupplierCatalogItemEntity;
import com.breadcost.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Purchase order service — BC-E13
 *
 * BC-1302: PO suggestion generation
 * BC-1303: PO review and approval
 * BC-1304: PO Excel export
 * BC-1305: Delivery matching against PO
 * BC-1306: FX rate per purchase transaction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository lineRepository;
    private final SupplierDeliveryRepository deliveryRepository;
    private final SupplierDeliveryLineRepository deliveryLineRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierCatalogItemRepository catalogItemRepository;

    // ── BC-1302: PO suggestion ────────────────────────────────────────────────

    /**
     * Generate PO suggestions from supplier catalog items.
     * Returns a DRAFT PO per supplier for items that have been added to their catalog.
     * (Full reorder-level scanning requires inventory integration — this version
     * creates a suggestion for each supplier with catalog items.)
     */
    @Transactional
    public List<PurchaseOrderEntity> suggestPOs(String tenantId) {
        List<SupplierCatalogItemEntity> allItems =
                catalogItemRepository.findAll().stream()
                        .filter(i -> i.getTenantId().equals(tenantId))
                        .collect(Collectors.toList());

        // Group by supplierId
        Map<String, List<SupplierCatalogItemEntity>> bySupplierId =
                allItems.stream().collect(Collectors.groupingBy(SupplierCatalogItemEntity::getSupplierId));

        List<PurchaseOrderEntity> suggestions = new ArrayList<>();
        for (Map.Entry<String, List<SupplierCatalogItemEntity>> entry : bySupplierId.entrySet()) {
            String supplierId = entry.getKey();
            List<SupplierCatalogItemEntity> items = entry.getValue();

            String supplierName = supplierRepository.findById(supplierId)
                    .map(s -> s.getName()).orElse(supplierId);

            PurchaseOrderEntity po = PurchaseOrderEntity.builder()
                    .poId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .supplierId(supplierId)
                    .supplierName(supplierName)
                    .status(PurchaseOrderEntity.PoStatus.DRAFT)
                    .notes("Auto-suggested PO")
                    .build();
            poRepository.save(po);

            for (SupplierCatalogItemEntity item : items) {
                PurchaseOrderLineEntity line = PurchaseOrderLineEntity.builder()
                        .lineId(UUID.randomUUID().toString())
                        .poId(po.getPoId())
                        .tenantId(tenantId)
                        .ingredientId(item.getIngredientId())
                        .ingredientName(item.getIngredientName())
                        .qty(item.getMoq())
                        .unit(item.getUnit())
                        .unitPrice(item.getUnitPrice())
                        .currency(item.getCurrency())
                        .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getMoq())))
                        .build();
                lineRepository.save(line);
            }

            suggestions.add(po);
            log.info("Suggested PO for supplier {}: {} lines", supplierId, items.size());
        }
        return suggestions;
    }

    // ── BC-1303: PO creation + approval ──────────────────────────────────────

    public record LineInput(String ingredientId, String ingredientName,
                            double qty, String unit,
                            BigDecimal unitPrice, String currency) {}

    @Transactional
    public PurchaseOrderEntity createPO(String tenantId, String supplierId,
                                        List<LineInput> lines, String notes,
                                        double fxRate, String fxCurrencyCode) {
        String supplierName = supplierRepository.findById(supplierId)
                .map(s -> s.getName()).orElse(supplierId);

        PurchaseOrderEntity po = PurchaseOrderEntity.builder()
                .poId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .supplierId(supplierId)
                .supplierName(supplierName)
                .status(PurchaseOrderEntity.PoStatus.PENDING_APPROVAL)
                .notes(notes)
                .fxRate(fxRate > 0 ? fxRate : 1.0)
                .fxCurrencyCode(fxCurrencyCode != null ? fxCurrencyCode : "USD")
                .build();
        poRepository.save(po);

        BigDecimal total = BigDecimal.ZERO;
        if (lines != null) {
            for (LineInput li : lines) {
                BigDecimal lineTotal = li.unitPrice() != null
                        ? li.unitPrice().multiply(BigDecimal.valueOf(li.qty()))
                        : BigDecimal.ZERO;
                PurchaseOrderLineEntity line = PurchaseOrderLineEntity.builder()
                        .lineId(UUID.randomUUID().toString())
                        .poId(po.getPoId())
                        .tenantId(tenantId)
                        .ingredientId(li.ingredientId())
                        .ingredientName(li.ingredientName())
                        .qty(li.qty())
                        .unit(li.unit())
                        .unitPrice(li.unitPrice() != null ? li.unitPrice() : BigDecimal.ZERO)
                        .currency(li.currency() != null ? li.currency() : "USD")
                        .lineTotal(lineTotal)
                        .build();
                lineRepository.save(line);
                total = total.add(lineTotal);
            }
        }

        po.setTotalAmount(total);
        return poRepository.save(po);
    }

    @Transactional
    public PurchaseOrderEntity approvePO(String tenantId, String poId, String approvedBy) {
        PurchaseOrderEntity po = poRepository.findByTenantIdAndPoId(tenantId, poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + poId));

        if (po.getStatus() != PurchaseOrderEntity.PoStatus.PENDING_APPROVAL
                && po.getStatus() != PurchaseOrderEntity.PoStatus.DRAFT) {
            throw new IllegalStateException("PO cannot be approved from status: " + po.getStatus());
        }

        po.setStatus(PurchaseOrderEntity.PoStatus.APPROVED);
        po.setApprovedBy(approvedBy);
        po.setApprovedAt(Instant.now());
        log.info("PO approved: tenantId={} poId={} by={}", tenantId, poId, approvedBy);
        return poRepository.save(po);
    }

    public List<PurchaseOrderEntity> listPOs(String tenantId) {
        return poRepository.findByTenantId(tenantId);
    }

    public PurchaseOrderEntity getPO(String tenantId, String poId) {
        return poRepository.findByTenantIdAndPoId(tenantId, poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found: " + poId));
    }

    public List<PurchaseOrderLineEntity> getPOLines(String poId) {
        return lineRepository.findByPoId(poId);
    }

    // ── BC-1304: Excel export ─────────────────────────────────────────────────

    public byte[] exportToExcel(String tenantId, String poId) {
        PurchaseOrderEntity po = getPO(tenantId, poId);
        List<PurchaseOrderLineEntity> lines = lineRepository.findByPoId(poId);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("PO-" + poId.substring(0, 8));

            // Header row
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row hdr = sheet.createRow(0);
            String[] cols = {"PO ID", "Supplier", "Status", "FX Rate", "Currency", "Total"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = hdr.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            // PO summary
            Row r = sheet.createRow(1);
            r.createCell(0).setCellValue(po.getPoId());
            r.createCell(1).setCellValue(po.getSupplierName() != null ? po.getSupplierName() : po.getSupplierId());
            r.createCell(2).setCellValue(po.getStatus().name());
            r.createCell(3).setCellValue(po.getFxRate());
            r.createCell(4).setCellValue(po.getFxCurrencyCode());
            r.createCell(5).setCellValue(po.getTotalAmount().doubleValue());

            // Lines section
            Row lineHdr = sheet.createRow(3);
            String[] lineCols = {"Ingredient", "Qty", "Unit", "Unit Price", "Currency", "Line Total"};
            for (int i = 0; i < lineCols.length; i++) {
                Cell c = lineHdr.createCell(i);
                c.setCellValue(lineCols[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            for (PurchaseOrderLineEntity line : lines) {
                Row lr = sheet.createRow(rowIdx++);
                lr.createCell(0).setCellValue(line.getIngredientName() != null ? line.getIngredientName() : line.getIngredientId());
                lr.createCell(1).setCellValue(line.getQty());
                lr.createCell(2).setCellValue(line.getUnit() != null ? line.getUnit() : "");
                lr.createCell(3).setCellValue(line.getUnitPrice().doubleValue());
                lr.createCell(4).setCellValue(line.getCurrency());
                lr.createCell(5).setCellValue(line.getLineTotal().doubleValue());
            }

            for (int i = 0; i < lineCols.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    // ── BC-1305: Delivery matching ────────────────────────────────────────────

    public record DeliveryLineInput(String ingredientId, String ingredientName,
                                     double qtyReceived, String unit,
                                     BigDecimal unitPrice) {}

    @Transactional
    public SupplierDeliveryEntity matchDelivery(String tenantId, String poId,
                                                 List<DeliveryLineInput> receivedLines,
                                                 String notes) {
        PurchaseOrderEntity po = getPO(tenantId, poId);

        // Build ordered quantity map from PO lines
        Map<String, Double> orderedQty = lineRepository.findByPoId(poId)
                .stream()
                .collect(Collectors.toMap(
                        PurchaseOrderLineEntity::getIngredientId,
                        PurchaseOrderLineEntity::getQty,
                        Double::sum));

        SupplierDeliveryEntity delivery = SupplierDeliveryEntity.builder()
                .deliveryId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .poId(poId)
                .supplierId(po.getSupplierId())
                .notes(notes)
                .build();

        boolean anyDiscrepancy = false;
        deliveryRepository.save(delivery);

        for (DeliveryLineInput dl : receivedLines) {
            double expected = orderedQty.getOrDefault(dl.ingredientId(), 0.0);
            boolean discrepancy = Math.abs(dl.qtyReceived() - expected) > 0.001;
            if (discrepancy) anyDiscrepancy = true;

            SupplierDeliveryLineEntity dline = SupplierDeliveryLineEntity.builder()
                    .lineId(UUID.randomUUID().toString())
                    .deliveryId(delivery.getDeliveryId())
                    .tenantId(tenantId)
                    .ingredientId(dl.ingredientId())
                    .ingredientName(dl.ingredientName())
                    .qtyOrdered(expected)
                    .qtyReceived(dl.qtyReceived())
                    .unit(dl.unit())
                    .unitPrice(dl.unitPrice() != null ? dl.unitPrice() : BigDecimal.ZERO)
                    .discrepancy(discrepancy)
                    .discrepancyNote(discrepancy
                            ? String.format("Expected %.2f, received %.2f", expected, dl.qtyReceived())
                            : null)
                    .build();
            deliveryLineRepository.save(dline);
        }

        delivery.setHasDiscrepancy(anyDiscrepancy);
        delivery.setStatus(anyDiscrepancy
                ? SupplierDeliveryEntity.DeliveryStatus.DISCREPANCY
                : SupplierDeliveryEntity.DeliveryStatus.MATCHED);

        // Mark PO as received
        po.setStatus(PurchaseOrderEntity.PoStatus.RECEIVED);
        poRepository.save(po);

        log.info("Delivery matched: poId={} hasDiscrepancy={}", poId, anyDiscrepancy);
        return deliveryRepository.save(delivery);
    }

    public List<SupplierDeliveryLineEntity> getDeliveryLines(String deliveryId) {
        return deliveryLineRepository.findByDeliveryId(deliveryId);
    }

    public SupplierDeliveryEntity getDelivery(String tenantId, String deliveryId) {
        return deliveryRepository.findByTenantIdAndDeliveryId(tenantId, deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
    }
}
