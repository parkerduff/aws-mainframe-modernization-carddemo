package com.cardemo.service;

import com.cardemo.dto.request.CardUpdateRequest;
import com.cardemo.entity.Card;
import com.cardemo.exception.ResourceNotFoundException;
import com.cardemo.repository.CardRepository;
import com.cardemo.repository.CardXrefRepository;
import com.cardemo.entity.CardXref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Card service - migrated from COBOL programs:
 * - COCRDLIC.cbl (CCLI transaction) - card listing
 * - COCRDSLC.cbl (CCDL transaction) - card detail view
 * - COCRDUPC.cbl (CCUP transaction) - card update
 * Replaces VSAM operations on CARDDATA and CARDXREF KSDS files.
 */
@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final CardXrefRepository cardXrefRepository;

    public CardService(CardRepository cardRepository, CardXrefRepository cardXrefRepository) {
        this.cardRepository = cardRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    public Card getCard(String cardNum) {
        return cardRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNum));
    }

    public List<Card> getCardsByAccount(Long acctId) {
        return cardRepository.findByAcctId(acctId);
    }

    public List<Card> getCardsByCustomer(Long custId) {
        List<CardXref> xrefs = cardXrefRepository.findByCustId(custId);
        List<String> cardNums = xrefs.stream().map(CardXref::getCardNum).toList();
        return cardRepository.findAllById(cardNums);
    }

    @Transactional
    public Card updateCard(String cardNum, CardUpdateRequest request) {
        Card card = cardRepository.findById(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNum));

        if (request.embossedName() != null) {
            card.setEmbossedName(request.embossedName());
        }
        if (request.activeStatus() != null) {
            card.setActiveStatus(request.activeStatus());
        }
        card.setUpdatedAt(LocalDateTime.now());

        Card saved = cardRepository.save(card);
        log.info("Card updated: {}", cardNum);
        return saved;
    }
}
