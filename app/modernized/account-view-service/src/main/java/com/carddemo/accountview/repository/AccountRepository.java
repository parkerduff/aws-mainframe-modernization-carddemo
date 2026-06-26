package com.carddemo.accountview.repository;

import com.carddemo.accountview.model.Account;

import java.util.Optional;

/**
 * Read access path for the account master file.
 *
 * <p>Legacy equivalent: CICS READ of dataset {@code ACCTDAT} keyed by account id
 * in paragraph {@code 9300-GETACCTDATA-BYACCT} of COACTVWC.
 */
public interface AccountRepository {

    /**
     * @param accountId 11-digit account id
     * @return the matching account, or empty when the read returns NOTFND
     */
    Optional<Account> findById(String accountId);
}
