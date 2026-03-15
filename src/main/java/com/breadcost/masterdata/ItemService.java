package com.breadcost.masterdata;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Cacheable(value = "items", key = "#tenantId")
    public List<ItemEntity> listAll(String tenantId) {
        return itemRepository.findByTenantId(tenantId);
    }

    @Cacheable(value = "itemsActive", key = "#tenantId")
    public List<ItemEntity> listActive(String tenantId) {
        return itemRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    @Cacheable(value = "itemsByType", key = "#tenantId + ':' + #type")
    public List<ItemEntity> listByType(String tenantId, String type) {
        return itemRepository.findByTenantIdAndType(tenantId, type.toUpperCase());
    }

    @CacheEvict(value = {"items", "itemsActive", "itemsByType"}, allEntries = true)
    public ItemEntity create(String tenantId, CreateItemRequest req) {
        ItemEntity item = ItemEntity.builder()
                .itemId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .name(req.getName().trim())
                .type(req.getType().toUpperCase())
                .baseUom(req.getBaseUom().trim().toUpperCase())
                .description(req.getDescription())
                .minStockThreshold(req.getMinStockThreshold() != null ? req.getMinStockThreshold() : 0.0)
                .active(true)
                .build();
        return itemRepository.save(item);
    }

    @CacheEvict(value = {"items", "itemsActive", "itemsByType"}, allEntries = true)
    public ItemEntity update(String tenantId, String itemId, CreateItemRequest req) {
        ItemEntity item = itemRepository.findById(itemId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        item.setName(req.getName().trim());
        item.setType(req.getType().toUpperCase());
        item.setBaseUom(req.getBaseUom().trim().toUpperCase());
        item.setDescription(req.getDescription());
        if (req.getMinStockThreshold() != null) {
            item.setMinStockThreshold(req.getMinStockThreshold());
        }
        if (req.getActive() != null) {
            item.setActive(req.getActive());
        }
        return itemRepository.save(item);
    }

    // ─── CSV import ────────────────────────────────────────────────────────

    @CacheEvict(value = {"items", "itemsActive", "itemsByType"}, allEntries = true)
    public Map<String, Object> importCsv(String tenantId, MultipartFile file) {
        List<Map<String, String>> errors = new ArrayList<>();
        int created = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return Map.of("created", 0, "errors", List.of(Map.of("row", 0, "error", "Empty file")));
            }
            // strip BOM if present
            if (headerLine.startsWith("\uFEFF")) headerLine = headerLine.substring(1);
            String[] headers = headerLine.split(",");
            int nameIdx = -1, typeIdx = -1, uomIdx = -1, descIdx = -1, threshIdx = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().toLowerCase();
                switch (h) {
                    case "name" -> nameIdx = i;
                    case "type" -> typeIdx = i;
                    case "baseuom", "uom", "base_uom" -> uomIdx = i;
                    case "description" -> descIdx = i;
                    case "minstockthreshold", "min_stock_threshold", "min_stock" -> threshIdx = i;
                }
            }
            if (nameIdx < 0 || typeIdx < 0 || uomIdx < 0) {
                return Map.of("created", 0, "errors", List.of(
                        Map.of("row", 1, "error", "CSV must have columns: name, type, baseUom")));
            }
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                try {
                    String name = cols[nameIdx].trim();
                    String type = cols[typeIdx].trim().toUpperCase();
                    String uom = cols[uomIdx].trim().toUpperCase();
                    if (name.isEmpty() || type.isEmpty() || uom.isEmpty()) {
                        errors.add(Map.of("row", String.valueOf(row), "error", "name, type, and baseUom are required"));
                        continue;
                    }
                    String desc = descIdx >= 0 && descIdx < cols.length ? cols[descIdx].trim() : null;
                    double threshold = 0.0;
                    if (threshIdx >= 0 && threshIdx < cols.length && !cols[threshIdx].trim().isEmpty()) {
                        threshold = Double.parseDouble(cols[threshIdx].trim());
                    }
                    ItemEntity item = ItemEntity.builder()
                            .itemId(UUID.randomUUID().toString())
                            .tenantId(tenantId)
                            .name(name)
                            .type(type)
                            .baseUom(uom)
                            .description(desc != null && !desc.isEmpty() ? desc : null)
                            .minStockThreshold(threshold)
                            .active(true)
                            .build();
                    itemRepository.save(item);
                    created++;
                } catch (Exception e) {
                    errors.add(Map.of("row", String.valueOf(row), "error", e.getMessage()));
                }
            }
        } catch (Exception e) {
            return Map.of("created", created, "errors", List.of(Map.of("row", 0, "error", "Failed to read file: " + e.getMessage())));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("errors", errors);
        return result;
    }

    // ─── request DTO ─────────────────────────────────────────────────────────

    @lombok.Data
    public static class CreateItemRequest {
        private String name;
        private String type;
        private String baseUom;
        private String description;
        private Double minStockThreshold;
        private Boolean active;
    }
}
