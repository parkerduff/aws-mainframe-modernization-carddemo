package com.aws.carddemo.controller;

import com.aws.carddemo.dto.BillPayDto;
import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.service.BillPayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bill-payment endpoint, replacing the CB00 CICS transaction / {@code COBIL00C}.
 */
@RestController
@RequestMapping("/api/billpay")
public class BillPayController {

    private final BillPayService billPayService;

    public BillPayController(BillPayService billPayService) {
        this.billPayService = billPayService;
    }

    @PostMapping
    public ResponseEntity<TransactionDto> processPayment(@Valid @RequestBody BillPayDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billPayService.processPayment(dto));
    }
}
