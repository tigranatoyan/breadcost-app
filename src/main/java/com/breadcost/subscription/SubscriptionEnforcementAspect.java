package com.breadcost.subscription;

import com.breadcost.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * AOP aspect that intercepts methods/classes annotated with {@link SubscriptionRequired}
 * and checks the tenant's subscription for feature access.
 * BC-3101
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEnforcementAspect {

    private final SubscriptionService subscriptionService;

    @Around("@within(subscriptionRequired) || @annotation(subscriptionRequired)")
    public Object enforce(ProceedingJoinPoint pjp, SubscriptionRequired subscriptionRequired) throws Throwable {
        // Resolve annotation — method-level takes precedence
        if (subscriptionRequired == null) {
            subscriptionRequired = pjp.getTarget().getClass().getAnnotation(SubscriptionRequired.class);
        }
        if (subscriptionRequired == null) {
            // Also check method-level
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            subscriptionRequired = sig.getMethod().getAnnotation(SubscriptionRequired.class);
        }
        if (subscriptionRequired == null) {
            return pjp.proceed();
        }

        String featureKey = subscriptionRequired.value();
        String tenantId = TenantContext.getTenantId();

        if (tenantId == null || tenantId.isBlank()) {
            // No tenant context — allow (might be public endpoint)
            return pjp.proceed();
        }

        if (!subscriptionService.hasFeature(tenantId, featureKey)) {
            log.warn("Subscription enforcement: tenant {} denied access to feature {}", tenantId, featureKey);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your subscription does not include the " + featureKey + " feature. " +
                    "Please upgrade your plan to access this functionality.");
        }

        return pjp.proceed();
    }
}
