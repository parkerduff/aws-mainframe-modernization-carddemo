package com.carddemo.interest.repository;

import com.carddemo.interest.domain.CardXrefRecord;
import java.util.Optional;

/**
 * Access to the card cross-reference file (XREFFILE) by account id (alternate key).
 * Mirrors paragraph 1110-GET-XREF-DATA.
 */
public interface CardXrefRepository {

    Optional<CardXrefRecord> findByAccountId(long acctId);
}
