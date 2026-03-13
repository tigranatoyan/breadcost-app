package com.breadcost.unit.service;

import com.breadcost.masterdata.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock private ItemRepository itemRepo;
    @InjectMocks private ItemService svc;

    @Test
    void create_setsDefaultsAndTrimsFields() {
        when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new ItemService.CreateItemRequest();
        req.setName("  Flour  ");
        req.setType("raw_material");
        req.setBaseUom("  kg  ");
        req.setDescription("All purpose");
        req.setMinStockThreshold(null);

        var item = svc.create("t1", req);

        assertEquals("Flour", item.getName());
        assertEquals("RAW_MATERIAL", item.getType());
        assertEquals("KG", item.getBaseUom());
        assertEquals(0.0, item.getMinStockThreshold());
        assertTrue(item.isActive());
    }

    @Test
    void create_withThreshold() {
        when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new ItemService.CreateItemRequest();
        req.setName("Sugar");
        req.setType("RAW_MATERIAL");
        req.setBaseUom("KG");
        req.setMinStockThreshold(50.0);

        var item = svc.create("t1", req);

        assertEquals(50.0, item.getMinStockThreshold());
    }

    @Test
    void update_setsFieldsAndValidatesTenant() {
        var existing = ItemEntity.builder().itemId("i1").tenantId("t1")
                .name("Old").type("RAW_MATERIAL").baseUom("KG").active(true).build();
        when(itemRepo.findById("i1")).thenReturn(Optional.of(existing));
        when(itemRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new ItemService.CreateItemRequest();
        req.setName("New Name");
        req.setType("packaging");
        req.setBaseUom("pcs");
        req.setActive(false);

        var result = svc.update("t1", "i1", req);

        assertEquals("New Name", result.getName());
        assertEquals("PACKAGING", result.getType());
        assertEquals("PCS", result.getBaseUom());
        assertFalse(result.isActive());
    }

    @Test
    void update_wrongTenant_throws() {
        var existing = ItemEntity.builder().itemId("i1").tenantId("t2").build();
        when(itemRepo.findById("i1")).thenReturn(Optional.of(existing));

        var req = new ItemService.CreateItemRequest();
        req.setName("X");
        req.setType("Y");
        req.setBaseUom("Z");

        assertThrows(IllegalArgumentException.class, () -> svc.update("t1", "i1", req));
    }

    @Test
    void update_notFound_throws() {
        when(itemRepo.findById("bad")).thenReturn(Optional.empty());

        var req = new ItemService.CreateItemRequest();
        req.setName("X");
        req.setType("Y");
        req.setBaseUom("Z");

        assertThrows(IllegalArgumentException.class, () -> svc.update("t1", "bad", req));
    }
}
