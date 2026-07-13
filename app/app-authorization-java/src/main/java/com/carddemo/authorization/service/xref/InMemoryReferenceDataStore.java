package com.carddemo.authorization.service.xref;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory cross-reference and account store (BR-07.1, BR-10).
 *
 * <p>The VSAM {@code CCXREF}/{@code ACCTDAT} files that the legacy program reads are
 * not part of this extension, so reference data is held in memory and seeded (via the
 * demo data loader or the reference-data REST endpoints). In production these methods
 * would be backed by the real account/cross-reference system of record.
 */
@Component
public class InMemoryReferenceDataStore implements CrossReferenceService, AccountService {

    private final Map<String, CardXref> xrefByCardNum = new ConcurrentHashMap<>();
    private final Map<Long, Account> accountsById = new ConcurrentHashMap<>();

    @Override
    public Optional<CardXref> findByCardNum(String cardNum) {
        return Optional.ofNullable(xrefByCardNum.get(cardNum));
    }

    @Override
    public Optional<Account> findByAcctId(Long acctId) {
        return Optional.ofNullable(accountsById.get(acctId));
    }

    public void registerXref(CardXref xref) {
        xrefByCardNum.put(xref.cardNum(), xref);
    }

    public void registerAccount(Account account) {
        accountsById.put(account.acctId(), account);
    }

    public void clear() {
        xrefByCardNum.clear();
        accountsById.clear();
    }
}

