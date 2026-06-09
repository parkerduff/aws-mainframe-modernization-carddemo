package com.aws.carddemo.dto;

import com.aws.carddemo.entity.CardEntity;

/**
 * Card response, mirroring COCRDLIC (card list) / COCRDSLC (card detail).
 */
public record CardDto(
        String cardNum,
        Long acctId,
        String cvvCd,
        String embossedName,
        String expirationDate,
        String activeStatus) {

    public static CardDto fromEntity(CardEntity e) {
        return new CardDto(
                e.getCardNum(),
                e.getAcctId(),
                e.getCvvCd(),
                e.getEmbossedName(),
                e.getExpirationDate(),
                e.getActiveStatus());
    }
}
