package com.carddemo.authorization.service.xref;

/**
 * Card cross-reference entry (BR-07.1).
 *
 * <p>Ports the VSAM {@code CCXREF} record (copybook {@code CVACT03Y}) mapping a card
 * number to its customer and account ids.
 */
public record CardXref(String cardNum, Long custId, Long acctId) {
}

