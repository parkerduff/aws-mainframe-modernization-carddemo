package com.carddemo.accountview.web;

import com.carddemo.accountview.service.AccountViewResult;
import com.carddemo.accountview.service.AccountViewService;
import com.carddemo.accountview.service.AccountViewStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST front door for the Account View transaction. The single query parameter
 * stands in for the ACCTSID 3270 screen field.
 *
 * <p>HTTP status mapping: SUCCESS -> 200, VALIDATION_ERROR -> 400,
 * the three NOTFND outcomes -> 404. The response body always carries the legacy
 * status, error/info messages and (on success) the account details.
 */
@RestController
public class AccountViewController {

    private final AccountViewService service;

    public AccountViewController(AccountViewService service) {
        this.service = service;
    }

    @GetMapping("/api/accounts/view")
    public ResponseEntity<AccountViewResult> view(@RequestParam(name = "accountId", required = false) String accountId) {
        AccountViewResult result = service.viewAccount(accountId);
        return ResponseEntity.status(httpStatusFor(result.status())).body(result);
    }

    private static HttpStatus httpStatusFor(AccountViewStatus status) {
        return switch (status) {
            case SUCCESS -> HttpStatus.OK;
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case XREF_NOT_FOUND, ACCOUNT_NOT_FOUND, CUSTOMER_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
    }
}
