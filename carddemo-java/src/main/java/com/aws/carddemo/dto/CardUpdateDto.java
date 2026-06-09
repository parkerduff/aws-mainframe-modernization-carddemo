package com.aws.carddemo.dto;

/**
 * Card update request, mirroring COCRDUPC (card update). Null fields are left unchanged.
 */
public record CardUpdateDto(
        String embossedName,
        String expirationDate,
        String activeStatus) {
}
