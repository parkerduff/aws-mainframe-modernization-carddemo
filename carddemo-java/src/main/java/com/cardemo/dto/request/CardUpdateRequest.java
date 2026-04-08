package com.cardemo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Card update request - migrated from COCRDUPC card update transaction")
public record CardUpdateRequest(
    @Schema(description = "Embossed name on card") String embossedName,
    @Schema(description = "Active status (Y/N)") String activeStatus
) {}
