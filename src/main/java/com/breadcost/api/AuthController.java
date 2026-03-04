package com.breadcost.api;

import com.breadcost.masterdata.UserEntity;
import com.breadcost.masterdata.UserRepository;
import com.breadcost.security.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * POST /v1/auth/login  — returns JWT token
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private String username;
        private String displayName;
        private List<String> roles;
        private String tenantId;
        private String primaryRole;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        log.info("Login attempt: {}", req.getUsername());

        // Look up user in DB
        var userOpt = userRepository.findByUsername(req.getUsername());

        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Account is deactivated"));
            }
            if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid username or password"));
            }
            // Update last login
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            List<String> roles = user.getRoleList();
            String tenantId = user.getTenantId();
            String token = jwtUtil.generate(user.getUsername(), roles, tenantId);

            LoginResponse resp = new LoginResponse();
            resp.setToken(token);
            resp.setUsername(user.getUsername());
            resp.setDisplayName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            resp.setRoles(roles);
            resp.setTenantId(tenantId);
            resp.setPrimaryRole(roles.isEmpty() ? "Viewer" : roles.get(0));
            return ResponseEntity.ok(resp);
        }

        // Fallback: hardcoded demo accounts (bootstrap before first user is created)
        return fallbackLogin(req);
    }

    /** Demo fallback — used when DB has no users yet (first boot) */
    private ResponseEntity<?> fallbackLogin(LoginRequest req) {
        record DemoUser(String username, String password, List<String> roles) {}
        List<DemoUser> demos = List.of(
                new DemoUser("admin",      "admin",      List.of("Admin")),
                new DemoUser("production", "production", List.of("ProductionUser")),
                new DemoUser("finance",    "finance",    List.of("FinanceUser")),
                new DemoUser("viewer",     "viewer",     List.of("Viewer")),
                new DemoUser("cashier",    "cashier",    List.of("Cashier")),
                new DemoUser("warehouse",  "warehouse",  List.of("Warehouse")),
                new DemoUser("technologist","technologist", List.of("Technologist"))
        );
        for (DemoUser d : demos) {
            if (d.username().equals(req.getUsername()) && d.password().equals(req.getPassword())) {
                String tenantId = "tenant1";
                String token = jwtUtil.generate(d.username(), d.roles(), tenantId);
                LoginResponse resp = new LoginResponse();
                resp.setToken(token);
                resp.setUsername(d.username());
                resp.setDisplayName(d.username());
                resp.setRoles(d.roles());
                resp.setTenantId(tenantId);
                resp.setPrimaryRole(d.roles().get(0));
                return ResponseEntity.ok(resp);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid username or password"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(jakarta.servlet.http.HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No token"));
        }
        String token = header.substring(7);
        if (!jwtUtil.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token"));
        }
        return ResponseEntity.ok(Map.of(
                "username", jwtUtil.getUsername(token),
                "roles", jwtUtil.getRoles(token),
                "tenantId", jwtUtil.getTenantId(token)
        ));
    }
}
