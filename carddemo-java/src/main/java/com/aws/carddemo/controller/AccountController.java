package com.aws.carddemo.controller;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.dto.AccountUpdateDto;
import com.aws.carddemo.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account endpoints, replacing the CAVW (view) / CAUP (update) CICS transactions
 * ({@code COACTVWC} / {@code COACTUPC}).
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable("id") long id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    /**
     * Account view via the card cross-reference chain (COACTVWC lookup by card number).
     */
    @GetMapping(params = "cardNum")
    public ResponseEntity<AccountDto> getAccountByCard(@RequestParam("cardNum") String cardNum) {
        return ResponseEntity.ok(accountService.getAccountByCard(cardNum));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable("id") long id,
                                                    @Valid @RequestBody AccountUpdateDto dto) {
        return ResponseEntity.ok(accountService.updateAccount(id, dto));
    }
}
