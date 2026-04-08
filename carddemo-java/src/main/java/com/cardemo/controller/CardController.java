package com.cardemo.controller;

import com.cardemo.dto.request.CardUpdateRequest;
import com.cardemo.entity.Card;
import com.cardemo.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Card controller - migrated from COBOL programs:
 * - COCRDLIC.cbl (CCLI transaction) - card listing
 * - COCRDSLC.cbl (CCDL transaction) - card detail view
 * - COCRDUPC.cbl (CCUP transaction) - card update
 * Replaces CICS screens COCRDLI, COCRDSL, COCRDUP (BMS maps).
 */
@RestController
@RequestMapping("/cards")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cards", description = "Card management (migrated from COCRDLIC/COCRDSLC/COCRDUPC)")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/{cardNum}")
    @Operation(summary = "View card details", description = "Card detail view (replaces CICS CCDL transaction)")
    public ResponseEntity<Card> getCard(@PathVariable String cardNum) {
        return ResponseEntity.ok(cardService.getCard(cardNum));
    }

    @GetMapping("/account/{acctId}")
    @Operation(summary = "List cards by account", description = "Cards for an account (replaces CICS CCLI transaction)")
    public ResponseEntity<List<Card>> getCardsByAccount(@PathVariable Long acctId) {
        return ResponseEntity.ok(cardService.getCardsByAccount(acctId));
    }

    @GetMapping("/customer/{custId}")
    @Operation(summary = "List cards by customer", description = "Cards for a customer via cross-reference")
    public ResponseEntity<List<Card>> getCardsByCustomer(@PathVariable Long custId) {
        return ResponseEntity.ok(cardService.getCardsByCustomer(custId));
    }

    @PutMapping("/{cardNum}")
    @Operation(summary = "Update card", description = "Update card fields (replaces CICS CCUP transaction)")
    public ResponseEntity<Card> updateCard(@PathVariable String cardNum,
                                           @Valid @RequestBody CardUpdateRequest request) {
        return ResponseEntity.ok(cardService.updateCard(cardNum, request));
    }
}
