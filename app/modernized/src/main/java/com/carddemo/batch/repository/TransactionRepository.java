package com.carddemo.batch.repository;

import com.carddemo.batch.model.TransactionRecord;

import java.util.List;
import java.util.Optional;

/**
 * Data access abstraction for {@link com.carddemo.batch.service.TransactionReportService}.
 *
 * <p>Replaces the four VSAM files read by {@code CBTRN03C.cbl}:
 * <ul>
 *   <li>{@code TRANSACT-FILE} (sequential transactions) &rarr; {@link #findAllTransactions()}</li>
 *   <li>{@code XREF-FILE} (card cross-reference, {@code CVACT03Y}) &rarr; {@link #findAccountIdByCardNum(String)}</li>
 *   <li>{@code TRANTYPE-FILE} (transaction type, {@code CVTRA03Y}) &rarr; {@link #findTypeDescription(String)}</li>
 *   <li>{@code TRANCATG-FILE} (transaction category, {@code CVTRA04Y}) &rarr; {@link #findCategoryDescription(String, int)}</li>
 * </ul>
 */
public interface TransactionRepository {

    /**
     * Returns all transactions in processing order (the order they would have
     * been read sequentially from {@code TRANSACT-FILE}).
     */
    List<TransactionRecord> findAllTransactions();

    /**
     * Resolves a card number to its account id via the card cross-reference
     * ({@code XREF-ACCT-ID} in {@code CVACT03Y}). Returns empty when the card is
     * unknown.
     */
    Optional<String> findAccountIdByCardNum(String cardNum);

    /**
     * Resolves a transaction type code to its description
     * ({@code TRAN-TYPE-DESC} in {@code CVTRA03Y}). Returns empty when unknown.
     */
    Optional<String> findTypeDescription(String typeCd);

    /**
     * Resolves a (type code, category code) pair to its category description
     * ({@code TRAN-CAT-TYPE-DESC} in {@code CVTRA04Y}). Returns empty when
     * unknown.
     */
    Optional<String> findCategoryDescription(String typeCd, int catCd);
}
