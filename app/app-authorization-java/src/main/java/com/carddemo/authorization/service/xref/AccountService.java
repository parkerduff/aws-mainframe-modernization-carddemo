package com.carddemo.authorization.service.xref;

import java.util.Optional;

/**
 * Account master lookup (BR-01.3) — replaces the VSAM account read in
 * {@code COPAUA0C} {@code 5200-READ-ACCT-RECORD}.
 */
public interface AccountService {

    Optional<Account> findByAcctId(Long acctId);
}

