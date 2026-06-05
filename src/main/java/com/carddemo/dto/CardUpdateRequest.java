package com.carddemo.dto;

import com.carddemo.validation.AccountId;
import com.carddemo.validation.CardExpiryMonth;
import com.carddemo.validation.CardExpiryYear;
import com.carddemo.validation.CardNumber;
import com.carddemo.validation.CardStatus;

/**
 * Request payload for a card update, carrying the fields validated by business
 * rules BR-30 through BR-34.
 *
 * <p>Mirrors the COBOL {@code CCUP-NEW-DETAILS} structure defined in
 * {@code app/cbl/COCRDUPC.cbl} (lines 303-313):
 * <pre>
 *   05 CCUP-NEW-DETAILS.
 *      10 CCUP-NEW-ACCTID    PIC X(11).   &rarr; accountId   (BR-30)
 *      10 CCUP-NEW-CARDID    PIC X(16).   &rarr; cardNumber  (BR-31)
 *      10 CCUP-NEW-CVV-CD    PIC X(3).    &rarr; cvvCode
 *      10 CCUP-NEW-CARDDATA.
 *         20 CCUP-NEW-CRDNAME PIC X(50).  &rarr; embossedName
 *         20 CCUP-NEW-EXPIRAION-DATE.
 *            25 CCUP-NEW-EXPYEAR PIC X(4). &rarr; expiryYear  (BR-34)
 *            25 CCUP-NEW-EXPMON  PIC X(2). &rarr; expiryMonth (BR-33)
 *            25 CCUP-NEW-EXPDAY  PIC X(2). &rarr; expiryDay
 *         20 CCUP-NEW-CRDSTCD PIC X(1).   &rarr; activeStatus (BR-32)
 * </pre>
 *
 * @param accountId    11-digit non-zero account id (BR-30)
 * @param cardNumber   16-digit non-zero card number (BR-31)
 * @param cvvCode      card verification value
 * @param embossedName name embossed on the card
 * @param activeStatus card active status, {@code Y} or {@code N} (BR-32)
 * @param expiryMonth  card expiry month, 1-12 (BR-33)
 * @param expiryYear   card expiry year, 1950-2099 (BR-34)
 * @param expiryDay    card expiry day
 */
public record CardUpdateRequest(
        @AccountId String accountId,
        @CardNumber String cardNumber,
        String cvvCode,
        String embossedName,
        @CardStatus String activeStatus,
        @CardExpiryMonth String expiryMonth,
        @CardExpiryYear String expiryYear,
        String expiryDay
) {
}
