package com.carddemo.interest.repository;

import com.carddemo.interest.domain.AccountRecord;
import java.util.Optional;

/**
 * Access to the account master file (ACCTFILE, VSAM KSDS keyed on account id).
 * Mirrors the random READ / REWRITE operations CBACT04C performs against it.
 */
public interface AccountRepository {

    /** Keyed READ — paragraph 1100-GET-ACCT-DATA. */
    Optional<AccountRecord> findById(long acctId);

    /** REWRITE — paragraph 1050-UPDATE-ACCOUNT. */
    void save(AccountRecord account);
}
