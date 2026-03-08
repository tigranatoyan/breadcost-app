package com.breadcost.multitenancy;

import com.breadcost.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts tenantId from the JWT Bearer token and stores it in
 * {@link TenantContext} so services/repos can access the current tenant
 * without explicit parameters.
 *
 * Runs AFTER {@link com.breadcost.security.JwtAuthFilter} (which sets
 * the SecurityContext) but BEFORE controllers.
 */
@Component
@Order(1)   // run after JwtAuthFilter (default order 0)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        try {
            String tenantId = extractTenantId(request);
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractTenantId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token)) {
                return jwtUtil.getTenantId(token);
            }
        }
        return null;
    }
}
