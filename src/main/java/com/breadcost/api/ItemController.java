package com.breadcost.api;

import com.breadcost.masterdata.ItemEntity;
import com.breadcost.masterdata.ItemService;
import com.breadcost.masterdata.ItemService.CreateItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Items", description = "Raw material and ingredient master data")
@RestController
@RequestMapping("/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin','ProductionUser','FinanceUser','Viewer')")
    public ResponseEntity<List<ItemEntity>> getItems(
            @RequestParam String tenantId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {

        if (type != null) {
            return ResponseEntity.ok(itemService.listByType(tenantId, type));
        }
        if (activeOnly) {
            return ResponseEntity.ok(itemService.listActive(tenantId));
        }
        return ResponseEntity.ok(itemService.listAll(tenantId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<ItemEntity> createItem(
            @RequestParam String tenantId,
            @RequestBody CreateItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(tenantId, req));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<ItemEntity> updateItem(
            @PathVariable String itemId,
            @RequestParam String tenantId,
            @RequestBody CreateItemRequest req) {
        return ResponseEntity.ok(itemService.update(tenantId, itemId, req));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('Admin','ProductionUser')")
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestParam String tenantId,
            @RequestPart("file") MultipartFile file) {
        var result = itemService.importCsv(tenantId, file);
        return ResponseEntity.ok(result);
    }
}
