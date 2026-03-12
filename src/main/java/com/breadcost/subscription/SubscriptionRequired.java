package com.breadcost.subscription;

import java.lang.annotation.*;

/**
 * Marks a controller or method as requiring a specific subscription feature.
 * The AOP aspect checks the tenant's subscription tier for feature access.
 * BC-3101
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SubscriptionRequired {
    /** The feature key to check, e.g. "AI_BOT", "DELIVERY", "INVOICING". */
    String value();
}
