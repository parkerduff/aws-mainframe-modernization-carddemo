package com.aws.carddemo.service;

import com.aws.carddemo.dto.CardDto;
import com.aws.carddemo.dto.CardSearchDto;
import com.aws.carddemo.dto.CardUpdateDto;
import com.aws.carddemo.entity.CardEntity;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.repository.CardRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Card service, replacing {@code app/cbl/COCRDLIC.cbl} (card list),
 * {@code app/cbl/COCRDSLC.cbl} (card search) and {@code app/cbl/COCRDUPC.cbl} (card update).
 *
 * <p>COCRDLIC lists the cards for an account (via the account alternate index over CARDFILE);
 * COCRDSLC searches by card number / account; COCRDUPC reads, overlays changed fields, and
 * rewrites a CARD-RECORD (CVACT02Y.cpy).</p>
 */
@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Transactional(readOnly = true)
    public List<CardDto> listCards(long acctId) {
        return cardRepository.findByAcctId(acctId).stream()
                .map(CardDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CardDto> searchCards(CardSearchDto criteria) {
        return cardRepository.findAll().stream()
                .filter(c -> criteria.acctId() == null || criteria.acctId().equals(c.getAcctId()))
                .filter(c -> !StringUtils.hasText(criteria.cardNum())
                        || criteria.cardNum().equals(c.getCardNum()))
                .filter(c -> !StringUtils.hasText(criteria.activeStatus())
                        || criteria.activeStatus().equalsIgnoreCase(c.getActiveStatus()))
                .map(CardDto::fromEntity)
                .toList();
    }

    @Transactional
    public CardDto updateCard(String cardNum, CardUpdateDto dto) {
        CardEntity card = cardRepository.findByCardNum(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardNum));

        if (dto.embossedName() != null) {
            card.setEmbossedName(dto.embossedName());
        }
        if (dto.expirationDate() != null) {
            card.setExpirationDate(dto.expirationDate());
        }
        if (dto.activeStatus() != null) {
            card.setActiveStatus(dto.activeStatus());
        }

        return CardDto.fromEntity(cardRepository.save(card));
    }
}
