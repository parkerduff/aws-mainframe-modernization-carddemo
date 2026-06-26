package com.carddemo.accountview.repository;

import com.carddemo.accountview.model.CardXref;

import java.util.Optional;

/**
 * Read access path for the card cross-reference file.
 *
 * <p>Legacy equivalent: CICS READ of dataset {@code CXACAIX} (alternate index
 * over the card xref file, keyed by account id) in paragraph
 * {@code 9200-GETCARDXREF-BYACCT} of COACTVWC.
 */
public interface CardXrefRepository {

    /**
     * @param accountId 11-digit account id
     * @return the matching xref, or empty when the read returns NOTFND
     */
    Optional<CardXref> findByAccountId(String accountId);
}
