package com.breadcost.api;

import com.breadcost.multitenancy.TenantContext;
import com.breadcost.subscription.SubscriptionService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * BC-3103: Public (any authenticated user) endpoint for tenant features.
 * Used by the frontend to gate navigation sections by subscription tier.
 */
@RestController
@RequestMapping("/v2/tenant")
public class TenantFeatureController {

    private final SubscriptionService subscriptionService;

    public TenantFeatureController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /** GET /v2/tenant/features — returns feature list for the caller's tenant */
    @GetMapping("/features")
    public Map<String, Object> myFeatures() {
        String tenantId = TenantContext.getTenantId();
        return subscriptionService.getFeatureAccess(tenantId);
    }
}
