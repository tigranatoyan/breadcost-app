package com.breadcost.ai;

import com.breadcost.customers.CustomerEntity;
import com.breadcost.customers.CustomerRepository;
import com.breadcost.masterdata.OrderService;
import com.breadcost.masterdata.ProductEntity;
import com.breadcost.masterdata.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI WhatsApp ordering service — BC-1801..1804.
 *
 * <p>Handles:
 * <ul>
 *   <li>BC-1801: parse incoming WhatsApp messages → extract order intent</li>
 *   <li>BC-1802: two-way conversation flow (greeting → parse → confirm → create order)</li>
 *   <li>BC-1803: upsell suggestions based on product catalog</li>
 *   <li>BC-1804: escalation to human operator when AI cannot resolve</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiConversationService {

    private final AiConversationRepository conversationRepo;
    private final AiMessageRepository messageRepo;
    private final AiDraftOrderRepository draftOrderRepo;
    private final AiDraftOrderLineRepository draftOrderLineRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final OrderService orderService;

    // simple pattern: "<qty> <unit> <product-name>" or "<product-name> <qty>"
    private static final Pattern LINE_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(kg|pcs|pc|units?|loaves?|boxes?)?\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE_PATTERN_ALT =
            Pattern.compile("(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(kg|pcs|pc|units?|loaves?|boxes?)?", Pattern.CASE_INSENSITIVE);

    // ── BC-1802: main entry point ─────────────────────────────────────────────

    /**
     * Process an inbound WhatsApp message and return the AI response text.
     */
    @Transactional
    public String handleIncomingMessage(String tenantId, String phone, String text) {
        // 1. Resolve or create conversation
        AiConversationEntity conv = conversationRepo
                .findByTenantIdAndCustomerPhoneAndStatus(tenantId, phone, "ACTIVE")
                .orElseGet(() -> createConversation(tenantId, phone));

        if (conv.isEscalated()) {
            return "Your conversation has been transferred to an operator. They will reply shortly.";
        }

        // 2. Persist inbound message
        saveMessage(conv, tenantId, "INBOUND", text, null, null);

        // 3. Parse intent
        ParsedIntent intent = parseIntent(text);
        log.info("AI parsed intent={} confidence={} for conv={}", intent.intent, intent.confidence, conv.getConversationId());

        // 4. Route by intent
        String response = switch (intent.intent) {
            case "GREETING"    -> handleGreeting(conv, tenantId);
            case "ORDER"       -> handleOrderIntent(conv, tenantId, text);
            case "CONFIRM"     -> handleConfirm(conv, tenantId);
            case "CANCEL"      -> handleCancel(conv, tenantId);
            case "HELP"        -> escalateToHuman(conv, tenantId, "Customer requested help");
            default            -> handleUnknown(conv, tenantId, intent.confidence);
        };

        // 5. Persist outbound response
        saveMessage(conv, tenantId, "OUTBOUND", response, intent.intent, intent.confidence);

        return response;
    }

    // ── BC-1801: intent parsing ───────────────────────────────────────────────

    ParsedIntent parseIntent(String text) {
        String lower = text.toLowerCase().trim();

        if (lower.matches("^(hi|hello|hey|good\\s*(morning|afternoon|evening)|barev).*")) {
            return new ParsedIntent("GREETING", 0.95);
        }
        if (lower.matches(".*(yes|confirm|ok|proceed|approve).*") &&
            lower.length() < 30) {
            return new ParsedIntent("CONFIRM", 0.90);
        }
        if (lower.matches(".*(no|cancel|never\\s*mind|stop).*") &&
            lower.length() < 30) {
            return new ParsedIntent("CANCEL", 0.85);
        }
        if (lower.matches(".*(help|operator|human|agent|speak\\s+to).*")) {
            return new ParsedIntent("HELP", 0.90);
        }
        // If it has numbers + text, likely an order
        if (lower.matches(".*\\d+.*[a-z]{2,}.*") || lower.matches(".*[a-z]{2,}.*\\d+.*")) {
            return new ParsedIntent("ORDER", 0.75);
        }
        return new ParsedIntent("UNKNOWN", 0.30);
    }

    List<ParsedOrderLine> parseOrderLines(String tenantId, String text) {
        List<ParsedOrderLine> lines = new ArrayList<>();
        for (String segment : text.split("[,;\\n]+")) {
            String s = segment.trim();
            if (s.isEmpty()) continue;

            Matcher m = LINE_PATTERN.matcher(s);
            if (m.matches()) {
                lines.add(resolveProduct(tenantId, m.group(3).trim(),
                        Double.parseDouble(m.group(1)), normalizeUnit(m.group(2))));
                continue;
            }
            Matcher m2 = LINE_PATTERN_ALT.matcher(s);
            if (m2.matches()) {
                lines.add(resolveProduct(tenantId, m2.group(1).trim(),
                        Double.parseDouble(m2.group(2)), normalizeUnit(m2.group(3))));
            }
        }
        return lines;
    }

    private ParsedOrderLine resolveProduct(String tenantId, String rawName, double qty, String unit) {
        // D3.3: Enhanced product matching with fuzzy search
        List<ProductEntity> matches = productRepo.findByTenantIdAndNameContainingIgnoreCase(tenantId, rawName);
        ParsedOrderLine line = new ParsedOrderLine();
        line.rawName = rawName;
        line.qty = qty;
        line.unit = unit;
        if (!matches.isEmpty()) {
            ProductEntity best = matches.getFirst();
            line.productId = best.getProductId();
            line.resolvedName = best.getName();
            line.unitPrice = best.getPrice();
        } else {
            // Fuzzy fallback: find closest match by edit distance
            List<ProductEntity> allProducts = productRepo.findByTenantId(tenantId);
            ProductEntity fuzzyMatch = null;
            int bestDistance = Integer.MAX_VALUE;
            String lowerRaw = rawName.toLowerCase();
            for (ProductEntity p : allProducts) {
                int dist = levenshtein(lowerRaw, p.getName().toLowerCase());
                // Also check if the product name starts with or contains any word from raw
                boolean partialMatch = p.getName().toLowerCase().contains(lowerRaw)
                        || lowerRaw.contains(p.getName().toLowerCase().split("\\s+")[0]);
                if (partialMatch) dist = Math.max(0, dist - 3); // boost partial matches
                if (dist < bestDistance && dist <= Math.max(3, rawName.length() / 2)) {
                    bestDistance = dist;
                    fuzzyMatch = p;
                }
            }
            if (fuzzyMatch != null) {
                line.productId = fuzzyMatch.getProductId();
                line.resolvedName = fuzzyMatch.getName();
                line.unitPrice = fuzzyMatch.getPrice();
            }
        }
        return line;
    }

    /** Levenshtein edit distance for fuzzy matching. */
    static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j] + 1,
                        Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost));
            }
        }
        return dp[a.length()][b.length()];
    }

    private String normalizeUnit(String raw) {
        if (raw == null) return "PCS";
        return switch (raw.toLowerCase().replaceAll("s$", "")) {
            case "kg" -> "KG";
            case "pc", "piece" -> "PCS";
            case "loaf", "loave" -> "PCS";
            case "box" -> "BOX";
            case "unit" -> "PCS";
            default -> "PCS";
        };
    }

    // ── Conversation flow handlers ────────────────────────────────────────────

    private String handleGreeting(AiConversationEntity conv, String tenantId) {
        CustomerEntity customer = findCustomerByPhone(tenantId, conv.getCustomerPhone());
        String name = customer != null ? customer.getName() : "there";
        return "Hello " + name + "! 👋 I'm the BreadCost ordering assistant. " +
               "Please send your order as a list, for example:\n" +
               "  5 loaves White Bread\n" +
               "  2 kg Lavash\n" +
               "  10 pcs Croissant";
    }

    private String handleOrderIntent(AiConversationEntity conv, String tenantId, String text) {
        List<ParsedOrderLine> lines = parseOrderLines(tenantId, text);
        if (lines.isEmpty()) {
            return "I couldn't parse any items from your message. " +
                   "Please try: <quantity> <unit> <product name>, e.g. '5 loaves White Bread'";
        }

        // Create draft order
        AiDraftOrderEntity draft = AiDraftOrderEntity.builder()
                .draftId(UUID.randomUUID().toString())
                .conversationId(conv.getConversationId())
                .tenantId(tenantId)
                .customerId(conv.getCustomerId())
                .build();
        draftOrderRepo.save(draft);

        // Create lines
        for (ParsedOrderLine line : lines) {
            AiDraftOrderLineEntity draftLine = AiDraftOrderLineEntity.builder()
                    .lineId(UUID.randomUUID().toString())
                    .draftId(draft.getDraftId())
                    .tenantId(tenantId)
                    .productId(line.productId)
                    .productName(line.resolvedName != null ? line.resolvedName : line.rawName)
                    .qty(line.qty)
                    .unit(line.unit)
                    .build();
            draftOrderLineRepo.save(draftLine);
        }

        // Build confirmation message
        StringBuilder sb = new StringBuilder("📋 Here's what I understood:\n\n");
        for (ParsedOrderLine l : lines) {
            String name = l.resolvedName != null ? l.resolvedName : l.rawName + " ⚠️";
            sb.append("  • ").append(l.qty).append(" ").append(l.unit).append(" ").append(name);
            if (l.unitPrice != null) {
                sb.append(" @ ").append(l.unitPrice).append(" ea");
            }
            sb.append("\n");
        }

        // BC-1803: Upsell
        String upsell = generateUpsellSuggestion(tenantId, lines);
        if (upsell != null) {
            sb.append("\n💡 ").append(upsell).append("\n");
            draft.setUpsellOffered(true);
            draftOrderRepo.save(draft);
        }

        sb.append("\nReply YES to confirm or CANCEL to discard.");
        return sb.toString();
    }

    private String handleConfirm(AiConversationEntity conv, String tenantId) {
        Optional<AiDraftOrderEntity> pendingOpt =
                draftOrderRepo.findByConversationIdAndStatus(conv.getConversationId(), "PENDING_CONFIRMATION");
        if (pendingOpt.isEmpty()) {
            return "No pending order to confirm. Please send a new order.";
        }
        AiDraftOrderEntity draft = pendingOpt.get();
        List<AiDraftOrderLineEntity> lines = draftOrderLineRepo.findByDraftId(draft.getDraftId());

        // Convert to real order
        CustomerEntity customer = findCustomerByPhone(tenantId, conv.getCustomerPhone());
        List<OrderService.CreateOrderLineRequest> orderLines = lines.stream()
                .map(l -> OrderService.CreateOrderLineRequest.builder()
                        .productId(l.getProductId())
                        .productName(l.getProductName())
                        .qty(l.getQty())
                        .uom(l.getUnit())
                        .unitPrice(BigDecimal.ZERO) // price resolved by OrderService
                        .build())
                .collect(Collectors.toList());

        String customerId = customer != null ? customer.getCustomerId() : null;
        String customerName = customer != null ? customer.getName() : conv.getCustomerPhone();

        OrderService.CreateOrderLineRequest first = orderLines.isEmpty() ? null : orderLines.getFirst();
        var order = orderService.createOrder(
                tenantId, "default", customerId, customerName,
                "AI_WHATSAPP", null, false, null,
                "Created via WhatsApp AI (conv: " + conv.getConversationId() + ")",
                orderLines, UUID.randomUUID().toString());

        // Update draft
        draft.setStatus("CONFIRMED");
        draft.setConfirmedOrderId(order.getOrderId());
        draftOrderRepo.save(draft);

        // Close conversation
        conv.setStatus("COMPLETED");
        conversationRepo.save(conv);

        return "✅ Order #" + order.getOrderId() + " placed successfully! " +
               "You'll receive delivery details soon. Thank you!";
    }

    private String handleCancel(AiConversationEntity conv, String tenantId) {
        Optional<AiDraftOrderEntity> pendingOpt =
                draftOrderRepo.findByConversationIdAndStatus(conv.getConversationId(), "PENDING_CONFIRMATION");
        pendingOpt.ifPresent(draft -> {
            draft.setStatus("CANCELLED");
            draftOrderRepo.save(draft);
        });
        return "Order cancelled. Send a new message anytime to start another order.";
    }

    private String handleUnknown(AiConversationEntity conv, String tenantId, double confidence) {
        if (confidence < 0.3) {
            // Very low confidence — escalate
            return escalateToHuman(conv, tenantId, "Could not understand message (confidence=" + confidence + ")");
        }
        return "I'm not sure I understood. You can:\n" +
               "  • Send an order: 5 loaves White Bread, 2 kg Lavash\n" +
               "  • Type HELP to speak with an operator";
    }

    // ── BC-1803: Upsell ──────────────────────────────────────────────────────

    String generateUpsellSuggestion(String tenantId, List<ParsedOrderLine> orderLines) {
        if (orderLines.isEmpty()) return null;

        // Simple rule: if ordering bread, suggest pastries; vice versa
        boolean hasBread = orderLines.stream().anyMatch(l ->
                l.rawName.toLowerCase().matches(".*(bread|lavash|loaf|baguette).*"));
        boolean hasPastry = orderLines.stream().anyMatch(l ->
                l.rawName.toLowerCase().matches(".*(croissant|pastry|cake|muffin|cookie).*"));

        if (hasBread && !hasPastry) {
            List<ProductEntity> pastries = productRepo.findByTenantIdAndNameContainingIgnoreCase(tenantId, "croissant");
            if (!pastries.isEmpty()) {
                return "Customers who order bread also love our " + pastries.getFirst().getName() + "! Add some?";
            }
        }
        if (hasPastry && !hasBread) {
            List<ProductEntity> breads = productRepo.findByTenantIdAndNameContainingIgnoreCase(tenantId, "bread");
            if (!breads.isEmpty()) {
                return "Don't forget fresh " + breads.getFirst().getName() + " with your pastry order!";
            }
        }
        return null;
    }

    // ── BC-1804: Escalation ──────────────────────────────────────────────────

    @Transactional
    public String escalateToHuman(AiConversationEntity conv, String tenantId, String reason) {
        conv.setEscalated(true);
        conv.setEscalationReason(reason);
        conv.setStatus("ESCALATED");
        conversationRepo.save(conv);
        log.warn("Conversation {} escalated: {}", conv.getConversationId(), reason);
        return "I'm connecting you with a team member who can help. " +
               "Please wait a moment — they'll be with you shortly.";
    }

    /**
     * Operator resolves an escalated conversation, optionally closing it.
     */
    @Transactional
    public void resolveEscalation(String tenantId, String conversationId, boolean close) {
        AiConversationEntity conv = conversationRepo.findById(conversationId)
                .filter(c -> tenantId.equals(c.getTenantId()))
                .orElseThrow(() -> new NoSuchElementException("Conversation not found: " + conversationId));
        conv.setEscalated(false);
        conv.setStatus(close ? "COMPLETED" : "ACTIVE");
        conversationRepo.save(conv);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<AiConversationEntity> listConversations(String tenantId) {
        return conversationRepo.findByTenantId(tenantId);
    }

    public List<AiConversationEntity> listEscalated(String tenantId) {
        return conversationRepo.findByTenantIdAndStatus(tenantId, "ESCALATED");
    }

    public List<AiMessageEntity> getMessages(String conversationId) {
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public List<AiDraftOrderEntity> getDrafts(String conversationId) {
        return draftOrderRepo.findByConversationId(conversationId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AiConversationEntity createConversation(String tenantId, String phone) {
        CustomerEntity customer = findCustomerByPhone(tenantId, phone);
        AiConversationEntity conv = AiConversationEntity.builder()
                .conversationId(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .customerPhone(phone)
                .customerId(customer != null ? customer.getCustomerId() : null)
                .build();
        return conversationRepo.save(conv);
    }

    private CustomerEntity findCustomerByPhone(String tenantId, String phone) {
        return customerRepo.findByTenantIdAndPhone(tenantId, phone).orElse(null);
    }

    private void saveMessage(AiConversationEntity conv, String tenantId,
                             String direction, String content,
                             String parsedIntent, Double confidence) {
        AiMessageEntity msg = AiMessageEntity.builder()
                .messageId(UUID.randomUUID().toString())
                .conversationId(conv.getConversationId())
                .tenantId(tenantId)
                .direction(direction)
                .content(content)
                .parsedIntent(parsedIntent)
                .confidence(confidence)
                .build();
        messageRepo.save(msg);
    }

    // ── Inner DTOs ───────────────────────────────────────────────────────────

    record ParsedIntent(String intent, double confidence) {}

    static class ParsedOrderLine {
        String rawName;
        String productId;
        String resolvedName;
        double qty;
        String unit;
        BigDecimal unitPrice;
    }
}
