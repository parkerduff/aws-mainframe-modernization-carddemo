package com.carddemo.interest.repository;

import com.carddemo.interest.domain.TransactionRecord;

/**
 * Sink for the sequential transaction output file (TRANSACT).
 * Mirrors the sequential WRITE in paragraph 1300-B-WRITE-TX.
 */
public interface TransactionWriter {

    void write(TransactionRecord record);
}
