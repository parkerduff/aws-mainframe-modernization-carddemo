package com.aws.carddemo.controller;

import com.aws.carddemo.dto.TransactionCreateDto;
import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transaction endpoints, replacing the CT00 (list) / CT01 (view) / CT02 (add) CICS
 * transactions ({@code COTRN00C} / {@code COTRN01C} / {@code COTRN02C}).
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> listTransactions(
            @RequestParam("cardNum") String cardNum,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(transactionService.listTransactions(cardNum, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable("id") String id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> addTransaction(@Valid @RequestBody TransactionCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.addTransaction(dto));
    }
}
