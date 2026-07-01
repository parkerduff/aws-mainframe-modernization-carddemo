package com.carddemo.interest.repository;

import com.carddemo.interest.domain.DisclosureGroupRecord;
import java.util.Optional;

/**
 * Access to the disclosure group file (DISCGRP) by composite key.
 * Mirrors paragraph 1200-GET-INTEREST-RATE, including the DEFAULT-group fallback.
 */
public interface DisclosureGroupRepository {

    Optional<DisclosureGroupRecord> find(String acctGroupId, String tranTypeCd, String tranCatCd);
}
