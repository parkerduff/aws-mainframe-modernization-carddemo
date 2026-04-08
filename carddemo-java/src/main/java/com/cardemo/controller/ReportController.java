package com.cardemo.controller;

import com.cardemo.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Report controller - migrated from COBOL program CORPT00C.cbl (CR00 transaction).
 * Replaces CICS report generation screen CORPT00 (BMS map).
 */
@RestController
@RequestMapping("/reports")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports", description = "Report generation (migrated from CORPT00C)")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/transactions/{acctId}")
    @Operation(summary = "Transaction report", description = "Generate transaction report for an account (replaces CICS CR00)")
    public ResponseEntity<Map<String, Object>> transactionReport(
            @PathVariable Long acctId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(reportService.generateTransactionReport(acctId, startDate, endDate));
    }
}
