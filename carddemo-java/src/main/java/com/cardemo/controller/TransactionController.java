package com.cardemo.controller;

import com.cardemo.dto.request.TransactionRequest;
import com.cardemo.dto.response.TransactionResponse;
import com.cardemo.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Transaction controller - migrated from COBOL programs:
 * - COTRN00C.cbl (CT00 transaction) - transaction list
 * - COTRN01C.cbl (CT01 transaction) - transaction view
 * - COTRN02C.cbl (CT02 transaction) - add transaction
 * Replaces CICS screens COTRN00, COTRN01, COTRN02 (BMS maps).
 */
@RestController
@RequestMapping("/transactions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Transaction processing (migrated from COTRN00C/COTRN01C/COTRN02C)")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{tranId}")
    @Operation(summary = "View transaction", description = "Transaction details (replaces CICS CT01 transaction)")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String tranId) {
        return ResponseEntity.ok(transactionService.getTransaction(tranId));
    }

    @GetMapping("/account/{acctId}")
    @Operation(summary = "List transactions by account", description = "Paginated transaction list (replaces CICS CT00)")
    public ResponseEntity<Page<TransactionResponse>> listByAccount(@PathVariable Long acctId, Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactionsByAccount(acctId, pageable));
    }

    @GetMapping("/card/{cardNum}")
    @Operation(summary = "List transactions by card", description = "Transactions for a specific card")
    public ResponseEntity<Page<TransactionResponse>> listByCard(@PathVariable String cardNum, Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactionsByCard(cardNum, pageable));
    }

    @PostMapping
    @Operation(summary = "Create transaction", description = "Add new transaction (replaces CICS CT02 transaction)")
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.createTransaction(request));
    }
}
