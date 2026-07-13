package com.carddemo.authorization.web;

import com.carddemo.authorization.service.xref.Account;
import com.carddemo.authorization.service.xref.CardXref;
import com.carddemo.authorization.service.xref.InMemoryReferenceDataStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seeds the in-memory cross-reference/account store (BR-07.1, BR-10).
 *
 * <p>The legacy VSAM {@code CCXREF}/{@code ACCTDAT} files are not part of this
 * extension, so this endpoint lets a caller register the reference data used by
 * decisioning. In production the {@link InMemoryReferenceDataStore} would be replaced
 * by an adapter over the real system of record.
 */
@RestController
@RequestMapping("/api/reference")
public class ReferenceDataController {

    private final InMemoryReferenceDataStore referenceDataStore;

    public ReferenceDataController(InMemoryReferenceDataStore referenceDataStore) {
        this.referenceDataStore = referenceDataStore;
    }

    @PostMapping("/xref")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerXref(@RequestBody CardXref xref) {
        referenceDataStore.registerXref(xref);
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerAccount(@RequestBody Account account) {
        referenceDataStore.registerAccount(account);
    }
}

