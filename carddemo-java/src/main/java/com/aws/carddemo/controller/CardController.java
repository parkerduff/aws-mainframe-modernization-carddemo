package com.aws.carddemo.controller;

import com.aws.carddemo.dto.CardDto;
import com.aws.carddemo.dto.CardSearchDto;
import com.aws.carddemo.dto.CardUpdateDto;
import com.aws.carddemo.service.CardService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Card endpoints, replacing {@code COCRDLIC} (list) / {@code COCRDSLC} (search) /
 * {@code COCRDUPC} (update).
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping(params = "acctId")
    public ResponseEntity<List<CardDto>> listCards(@RequestParam("acctId") long acctId) {
        return ResponseEntity.ok(cardService.listCards(acctId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CardDto>> searchCards(
            @RequestParam(value = "acctId", required = false) Long acctId,
            @RequestParam(value = "cardNum", required = false) String cardNum,
            @RequestParam(value = "activeStatus", required = false) String activeStatus) {
        return ResponseEntity.ok(
                cardService.searchCards(new CardSearchDto(acctId, cardNum, activeStatus)));
    }

    @PutMapping("/{cardNum}")
    public ResponseEntity<CardDto> updateCard(@PathVariable("cardNum") String cardNum,
                                              @Valid @RequestBody CardUpdateDto dto) {
        return ResponseEntity.ok(cardService.updateCard(cardNum, dto));
    }
}
