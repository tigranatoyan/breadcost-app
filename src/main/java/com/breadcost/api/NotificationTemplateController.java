package com.breadcost.api;

import com.breadcost.notifications.NotificationTemplateEntity;
import com.breadcost.notifications.NotificationTemplateRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Notification Templates CRUD — /v3/notifications/templates
 *
 * BC-3002: Admin/Management can manage notification templates.
 * Templates support variable placeholders: {{orderNumber}}, {{customerName}}, {{status}}.
 */
@RestController
@RequestMapping("/v3/notifications/templates")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('Admin','Manager')")
public class NotificationTemplateController {

    private final NotificationTemplateRepository templateRepository;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class CreateTemplateRequest {
        @NotBlank private String tenantId;
        @NotBlank private String type;
        @NotBlank private String channel;
        private String subject;
        private String bodyTemplate;
    }

    @Data
    public static class UpdateTemplateRequest {
        private String subject;
        private String bodyTemplate;
        private Boolean active;
    }

    @Data
    public static class PreviewRequest {
        private Map<String, String> variables;
    }

    // ── List all ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<NotificationTemplateEntity>> listTemplates(
            @RequestParam String tenantId) {
        return ResponseEntity.ok(templateRepository.findByTenantId(tenantId));
    }

    // ── Get by ID ────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<NotificationTemplateEntity> getTemplate(
            @PathVariable("id") String templateId) {

        NotificationTemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + templateId));
        return ResponseEntity.ok(template);
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<NotificationTemplateEntity> createTemplate(
            @Valid @RequestBody CreateTemplateRequest req) {

        NotificationTemplateEntity template = NotificationTemplateEntity.builder()
                .templateId(UUID.randomUUID().toString())
                .tenantId(req.getTenantId())
                .type(req.getType())
                .channel(req.getChannel())
                .subject(req.getSubject())
                .bodyTemplate(req.getBodyTemplate())
                .active(true)
                .build();

        log.info("Creating notification template: tenant={} type={} channel={}",
                req.getTenantId(), req.getType(), req.getChannel());
        return ResponseEntity.status(HttpStatus.CREATED).body(templateRepository.save(template));
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplateEntity> updateTemplate(
            @PathVariable("id") String templateId,
            @RequestBody UpdateTemplateRequest req) {

        NotificationTemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + templateId));

        if (req.getSubject() != null)      template.setSubject(req.getSubject());
        if (req.getBodyTemplate() != null)  template.setBodyTemplate(req.getBodyTemplate());
        if (req.getActive() != null)        template.setActive(req.getActive());

        return ResponseEntity.ok(templateRepository.save(template));
    }

    // ── Delete (soft) ────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("id") String templateId) {
        NotificationTemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + templateId));

        template.setActive(false);
        templateRepository.save(template);
        return ResponseEntity.noContent().build();
    }

    // ── Preview — render template with sample data ───────────────────────────

    @PostMapping("/{id}/preview")
    public ResponseEntity<Map<String, String>> previewTemplate(
            @PathVariable("id") String templateId,
            @RequestBody(required = false) PreviewRequest req) {

        NotificationTemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + templateId));

        Map<String, String> vars = (req != null && req.getVariables() != null)
                ? req.getVariables()
                : Map.of(
                    "orderNumber", "ORD-12345",
                    "customerName", "Sample Customer",
                    "status", "CONFIRMED"
                );

        String rendered = renderTemplate(template.getBodyTemplate(), vars);
        String renderedSubject = renderTemplate(template.getSubject(), vars);

        return ResponseEntity.ok(Map.of(
                "subject", renderedSubject != null ? renderedSubject : "",
                "body", rendered != null ? rendered : ""
        ));
    }

    /** Simple {{variable}} substitution */
    private String renderTemplate(String tpl, Map<String, String> vars) {
        if (tpl == null) return null;
        String result = tpl;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
