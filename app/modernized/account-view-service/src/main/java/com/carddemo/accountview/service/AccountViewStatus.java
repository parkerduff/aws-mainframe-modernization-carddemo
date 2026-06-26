package com.carddemo.accountview.service;

/**
 * Outcome classifications for the Account View (CAVW / COACTVWC) request.
 *
 * <p>These map directly to the legacy control-flow branches:
 * <ul>
 *   <li>{@link #SUCCESS} — all three reads (xref, account, customer) returned NORMAL.</li>
 *   <li>{@link #VALIDATION_ERROR} — input edit failed in {@code 2210-EDIT-ACCOUNT}.</li>
 *   <li>{@link #XREF_NOT_FOUND} — {@code 9200} read of CXACAIX returned NOTFND.</li>
 *   <li>{@link #ACCOUNT_NOT_FOUND} — {@code 9300} read of ACCTDAT returned NOTFND.</li>
 *   <li>{@link #CUSTOMER_NOT_FOUND} — {@code 9400} read of CUSTDAT returned NOTFND.</li>
 * </ul>
 * In every non-success branch the legacy program sets INPUT-ERROR and redisplays
 * the map with an error message.
 */
public enum AccountViewStatus {
    SUCCESS,
    VALIDATION_ERROR,
    XREF_NOT_FOUND,
    ACCOUNT_NOT_FOUND,
    CUSTOMER_NOT_FOUND
}
