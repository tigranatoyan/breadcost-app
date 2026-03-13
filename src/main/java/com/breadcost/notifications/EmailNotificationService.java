package com.breadcost.notifications;

import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * R5: Email notification service — sends transactional emails using notification templates.
 * Integrates with NotificationTemplateEntity (channel=EMAIL) for template resolution
 * and variable substitution ({{orderNumber}}, {{customerName}}, {{status}}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final NotificationTemplateRepository templateRepo;
    private final CustomerRepository customerRepo;

    @Value("${breadcost.mail.from:noreply@breadcost.app}")
    private String fromAddress;

    @Value("${breadcost.mail.enabled:false}")
    private boolean emailEnabled;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * Send a templated email for a given notification type.
     * Resolves the EMAIL template for the tenant/type, substitutes variables, and sends.
     *
     * @return true if sent, false if skipped (no template, disabled, no email address)
     */
    public boolean sendTemplatedEmail(String tenantId, String customerId,
                                       String notificationType, Map<String, String> variables) {
        if (!emailEnabled) {
            log.debug("Email disabled — skipping {} for customer={}", notificationType, customerId);
            return false;
        }

        // Resolve template
        Optional<NotificationTemplateEntity> templateOpt =
                templateRepo.findByTenantIdAndTypeAndChannel(tenantId, notificationType, "EMAIL");
        if (templateOpt.isEmpty() || !templateOpt.get().isActive()) {
            log.debug("No active EMAIL template for tenant={} type={}", tenantId, notificationType);
            return false;
        }

        // Resolve customer email
        Optional<CustomerEntity> customerOpt = customerRepo.findByTenantIdAndCustomerId(tenantId, customerId);
        if (customerOpt.isEmpty() || customerOpt.get().getEmail() == null || customerOpt.get().getEmail().isBlank()) {
            log.debug("No email for customer={} — skipping", customerId);
            return false;
        }

        NotificationTemplateEntity template = templateOpt.get();
        String toEmail = customerOpt.get().getEmail();
        String subject = substituteVariables(template.getSubject(), variables);
        String body = substituteVariables(template.getBodyTemplate(), variables);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            log.info("Email sent: to={} type={} subject={}", toEmail, notificationType, subject);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to={} type={}: {}", toEmail, notificationType, e.getMessage());
            return false;
        }
    }

    /**
     * Convenience: send order status change email.
     */
    public boolean sendOrderStatusEmail(String tenantId, String customerId,
                                         String orderId, String customerName, String newStatus) {
        return sendTemplatedEmail(tenantId, customerId, mapStatusToTemplateType(newStatus),
                Map.of(
                        "orderNumber", orderId,
                        "customerName", customerName != null ? customerName : "",
                        "status", newStatus
                ));
    }

    String substituteVariables(String template, Map<String, String> variables) {
        if (template == null) return "";
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.getOrDefault(varName, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String mapStatusToTemplateType(String status) {
        return switch (status) {
            case "CONFIRMED" -> "ORDER_CONFIRMATION";
            case "IN_PRODUCTION" -> "PRODUCTION_STARTED";
            case "READY" -> "READY_FOR_DELIVERY";
            case "OUT_FOR_DELIVERY" -> "OUT_FOR_DELIVERY";
            case "DELIVERED" -> "DELIVERED";
            default -> "ORDER_STATUS_UPDATE";
        };
    }
}
