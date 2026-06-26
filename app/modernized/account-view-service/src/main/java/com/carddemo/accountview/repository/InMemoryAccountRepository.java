package com.carddemo.accountview.repository;

import com.carddemo.accountview.model.Account;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple map-backed {@link AccountRepository}, keyed by account id, standing in
 * for the VSAM ACCTDAT read.
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> byAccountId = new LinkedHashMap<>();

    public void save(Account account) {
        byAccountId.put(account.accountId(), account);
    }

    @Override
    public Optional<Account> findById(String accountId) {
        return Optional.ofNullable(byAccountId.get(accountId));
    }
}
