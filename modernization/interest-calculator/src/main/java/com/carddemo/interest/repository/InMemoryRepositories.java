package com.carddemo.interest.repository;

import com.carddemo.interest.domain.AccountRecord;
import com.carddemo.interest.domain.CardXrefRecord;
import com.carddemo.interest.domain.DisclosureGroupRecord;
import com.carddemo.interest.domain.TransactionRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory implementations of the CBACT04C file gateways, backing equivalence
 * tests and the standalone batch runner. Each inner class corresponds to one VSAM
 * file; the disclosure-group lookup key format is centralized here so it matches
 * exactly between load and lookup.
 */
public final class InMemoryRepositories {

    private InMemoryRepositories() {
    }

    /** Composite disclosure-group key: 10-char group id + 2-char type + 4-char category. */
    public static String discGroupKey(String acctGroupId, String tranTypeCd, String tranCatCd) {
        return String.format("%-10s|%-2s|%4s", acctGroupId, tranTypeCd, tranCatCd);
    }

    public static final class Accounts implements AccountRepository {
        private final Map<Long, AccountRecord> store = new HashMap<>();

        public void put(AccountRecord account) {
            store.put(account.getAcctId(), account);
        }

        @Override
        public Optional<AccountRecord> findById(long acctId) {
            return Optional.ofNullable(store.get(acctId));
        }

        @Override
        public void save(AccountRecord account) {
            store.put(account.getAcctId(), account);
        }
    }

    public static final class CardXrefs implements CardXrefRepository {
        private final Map<Long, CardXrefRecord> byAccount = new HashMap<>();

        public void put(CardXrefRecord xref) {
            byAccount.put(xref.getXrefAcctId(), xref);
        }

        @Override
        public Optional<CardXrefRecord> findByAccountId(long acctId) {
            return Optional.ofNullable(byAccount.get(acctId));
        }
    }

    public static final class DisclosureGroups implements DisclosureGroupRepository {
        private final Map<String, DisclosureGroupRecord> store = new HashMap<>();

        public void put(DisclosureGroupRecord record) {
            store.put(discGroupKey(record.getDisAcctGroupId(), record.getDisTranTypeCd(),
                    record.getDisTranCatCd()), record);
        }

        @Override
        public Optional<DisclosureGroupRecord> find(String acctGroupId, String tranTypeCd, String tranCatCd) {
            return Optional.ofNullable(store.get(discGroupKey(acctGroupId, tranTypeCd, tranCatCd)));
        }
    }

    public static final class CollectingTransactionWriter implements TransactionWriter {
        private final List<TransactionRecord> written = new ArrayList<>();

        @Override
        public void write(TransactionRecord record) {
            written.add(record);
        }

        public List<TransactionRecord> getWritten() {
            return written;
        }
    }
}
