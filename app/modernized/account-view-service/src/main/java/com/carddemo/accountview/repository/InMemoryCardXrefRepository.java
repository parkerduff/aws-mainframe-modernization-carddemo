package com.carddemo.accountview.repository;

import com.carddemo.accountview.model.CardXref;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple map-backed {@link CardXrefRepository}, keyed by account id, used to
 * stand in for the VSAM alternate-index read. The map preserves the exact
 * field values defined by the copybook so behavioral equivalence can be tested
 * without a mainframe.
 */
public class InMemoryCardXrefRepository implements CardXrefRepository {

    private final Map<String, CardXref> byAccountId = new LinkedHashMap<>();

    public void save(CardXref xref) {
        byAccountId.put(xref.accountId(), xref);
    }

    @Override
    public Optional<CardXref> findByAccountId(String accountId) {
        return Optional.ofNullable(byAccountId.get(accountId));
    }
}
