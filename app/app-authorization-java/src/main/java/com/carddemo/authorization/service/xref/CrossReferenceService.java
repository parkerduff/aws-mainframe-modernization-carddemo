package com.carddemo.authorization.service.xref;

import java.util.Optional;

/**
 * Card cross-reference lookup (BR-07.1) — replaces the VSAM {@code CCXREF} read in
 * {@code COPAUA0C} {@code 5100-READ-XREF-RECORD}.
 */
public interface CrossReferenceService {

    Optional<CardXref> findByCardNum(String cardNum);
}

