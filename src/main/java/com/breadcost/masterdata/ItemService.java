package com.breadcost.masterdata;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public List<ItemEntity> listAll(String tenantId) {
        return itemRepository.findByTenantId(tenantId);
    }

    public List<ItemEntity> listActive(String tenantId) {
        return itemRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    public List<ItemEntity> listByType(String tenantId, String type) {
        return itemRepository.findByTenantIdAndType(tenantId, type.toUpperCase());
    }

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
