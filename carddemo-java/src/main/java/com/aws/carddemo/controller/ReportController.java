package com.aws.carddemo.controller;

import com.aws.carddemo.dto.AccountReportDto;
import com.aws.carddemo.dto.TransactionReportDto;
import com.aws.carddemo.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reporting endpoints, replacing {@code CORPT00C} (transaction reporting).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<TransactionReportDto> transactionReport(
            @RequestParam("cardNum") String cardNum,
            @RequestParam(value = "fromTs", required = false) String fromTs,
            @RequestParam(value = "toTs", required = false) String toTs) {
        return ResponseEntity.ok(reportService.generateTransactionReport(cardNum, fromTs, toTs));
    }

    @GetMapping("/accounts")
    public ResponseEntity<AccountReportDto> accountReport(
            @RequestParam(value = "activeStatus", required = false) String activeStatus) {
        return ResponseEntity.ok(reportService.generateAccountReport(activeStatus));
    }
}
