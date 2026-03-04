package com.breadcost.finance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Finance service
 * Implements posting rules, PPA, and financial calculations
 */
@Service
@Slf4j
public class FinanceService {

    /**
     * Determine if entry is eligible for FG adjustment
     * Per FG_ADJ_ELIGIBILITY rules
     */
    public boolean isEligibleForFGAdjustment(String eventType, Long ledgerSeq, Long recognitionCutoffSeq) {
        // Entry must be after recognition cutoff
        if (ledgerSeq <= recognitionCutoffSeq) {
            return false;
        }

        // Check event type eligibility
        return switch (eventType) {
            case "IssueToBatch", "ApplyOverhead", "RecordLabor" -> true;
            default -> false;
        };
    }

    /**
     * Calculate unit cost for lot
     */
    public BigDecimal calculateUnitCost(BigDecimal totalCost, BigDecimal qty) {
        if (qty.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(qty, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Apply posting rule for event
     * Per POSTING_RULES.yaml
     */
    public PostingResult applyPostingRule(String eventType) {
        return switch (eventType) {
            case "ReceiveLot" -> PostingResult.builder()
                    .debitAccount("INV_RM")
                    .creditAccount("AP")
                    .build();
            case "IssueToBatch" -> PostingResult.builder()
                    .debitAccount("WIP_MATERIAL")
                    .creditAccount("INV_RM")
                    .build();
            case "RecognizeProduction" -> PostingResult.builder()
                    .debitAccount("INV_FG")
                    .creditAccount("WIP_TOTAL")
                    .build();
            case "TransferInventory" -> PostingResult.builder()
                    .operational(true)
                    .build();
            default -> PostingResult.builder().build();
        };
    }

    public static class PostingResult {
        public String debitAccount;
        public String creditAccount;
        public boolean operational;

        public static PostingResultBuilder builder() {
            return new PostingResultBuilder();
        }

        public static class PostingResultBuilder {
            private String debitAccount;
            private String creditAccount;
            private boolean operational;

            public PostingResultBuilder debitAccount(String debitAccount) {
                this.debitAccount = debitAccount;
                return this;
            }

            public PostingResultBuilder creditAccount(String creditAccount) {
                this.creditAccount = creditAccount;
                return this;
            }

            public PostingResultBuilder operational(boolean operational) {
                this.operational = operational;
                return this;
            }

            public PostingResult build() {
                PostingResult result = new PostingResult();
                result.debitAccount = this.debitAccount;
                result.creditAccount = this.creditAccount;
                result.operational = this.operational;
                return result;
            }
        }
    }
}
