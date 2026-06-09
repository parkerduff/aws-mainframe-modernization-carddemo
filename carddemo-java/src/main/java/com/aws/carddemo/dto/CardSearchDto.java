package com.aws.carddemo.dto;

/**
 * Card search criteria, mirroring COCRDSLC (card search). All criteria are optional and
 * combined with AND semantics; null/blank criteria are ignored.
 */
public record CardSearchDto(
        Long acctId,
        String cardNum,
        String activeStatus) {
}
