package com.breadcost.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command execution result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandResult {
    private Boolean success;
    private String resultRef;
    private Long ledgerSeq;
    private String message;
    private String errorCode;
}
