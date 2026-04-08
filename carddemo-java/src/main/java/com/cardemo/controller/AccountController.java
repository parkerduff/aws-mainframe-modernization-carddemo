package com.cardemo.controller;

import com.cardemo.dto.request.AccountUpdateRequest;
import com.cardemo.dto.response.AccountResponse;
import com.cardemo.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Account controller - migrated from COBOL programs:
 * - COACTVWC.cbl (CAVW transaction) - account view
 * - COACTUPC.cbl (CAUP transaction) - account update
 * Replaces CICS screens COACTVW and COACTUP (BMS maps).
 */
@RestController
@RequestMapping("/accounts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Accounts", description = "Account management (migrated from COACTVWC/COACTUPC)")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @Operation(summary = "List accounts", description = "Paginated account listing (replaces CICS CACT transaction)")
    public ResponseEntity<Page<AccountResponse>> listAccounts(Pageable pageable) {
        return ResponseEntity.ok(accountService.listAccounts(pageable));
    }

    @GetMapping("/{acctId}")
    @Operation(summary = "View account", description = "Account details (replaces CICS CAVW transaction)")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long acctId) {
        return ResponseEntity.ok(accountService.getAccount(acctId));
    }

    @PutMapping("/{acctId}")
    @Operation(summary = "Update account", description = "Update account fields (replaces CICS CAUP transaction)")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long acctId,
                                                         @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(acctId, request));
    }
}
