package com.aws.carddemo.service;

import com.aws.carddemo.dto.TransactionCreateDto;
import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.entity.TransactionEntity;
import com.aws.carddemo.exception.BusinessRuleException;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.repository.CardXrefRepository;
import com.aws.carddemo.repository.TransactionRepository;
import com.aws.carddemo.util.CobolConversions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction service, replacing {@code app/cbl/COTRN00C.cbl} (transaction list),
 * {@code app/cbl/COTRN01C.cbl} (transaction detail) and {@code app/cbl/COTRN02C.cbl}
 * (add transaction).
 *
 * <p>COTRN00C pages through TRANSACT for a card; {@link #listTransactions} reproduces this
 * with Spring Data paging ordered by original timestamp descending. COTRN02C builds a new
 * TRAN-RECORD (CVTRA05Y.cpy) and writes it; {@link #addTransaction} mirrors this, validating
 * the card against the cross-reference first.</p>
 */
@Service
public class TransactionService {

    private static final DateTimeFormatter COBOL_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final TransactionRepository transactionRepository;
    private final CardXrefRepository cardXrefRepository;
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() % 1_000_000L);

    public TransactionService(TransactionRepository transactionRepository,
                              CardXrefRepository cardXrefRepository) {
        this.transactionRepository = transactionRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> listTransactions(String cardNum, Pageable pageable) {
        return transactionRepository
                .findByTranCardNumOrderByTranOrigTsDesc(cardNum, pageable)
                .map(TransactionDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransaction(String tranId) {
        return transactionRepository.findById(tranId)
                .map(TransactionDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + tranId));
    }

    @Transactional
    public TransactionDto addTransaction(TransactionCreateDto dto) {
        // COTRN02C validates that the card exists before writing the transaction.
        cardXrefRepository.findByCardNum(dto.tranCardNum())
                .orElseThrow(() -> new BusinessRuleException(
                        "Invalid card number: " + dto.tranCardNum()));

        TransactionEntity tran = new TransactionEntity();
        tran.setTranId(generateTranId());
        tran.setTranTypeCd(dto.tranTypeCd());
        tran.setTranCatCd(dto.tranCatCd());
        tran.setTranSource(dto.tranSource());
        tran.setTranDesc(dto.tranDesc());
        tran.setTranAmt(CobolConversions.money(dto.tranAmt()));
        tran.setTranMerchantId(dto.tranMerchantId());
        tran.setTranMerchantName(dto.tranMerchantName());
        tran.setTranMerchantCity(dto.tranMerchantCity());
        tran.setTranMerchantZip(dto.tranMerchantZip());
        tran.setTranCardNum(dto.tranCardNum());

        String origTs = dto.tranOrigTs() != null && !dto.tranOrigTs().isBlank()
                ? dto.tranOrigTs()
                : LocalDateTime.now().format(COBOL_TS);
        tran.setTranOrigTs(origTs);
        tran.setTranProcTs(LocalDateTime.now().format(COBOL_TS));

        return TransactionDto.fromEntity(transactionRepository.save(tran));
    }

    /**
     * Generate a 16-character transaction id (TRAN-ID PIC X(16)). COTRN02C derives a unique
     * id from a timestamp + sequence; here we combine the epoch millis with a rolling
     * sequence to stay within 16 digits.
     */
    private String generateTranId() {
        long base = (System.currentTimeMillis() % 10_000_000_000L) * 1_000_000L;
        long id = base + (sequence.incrementAndGet() % 1_000_000L);
        return String.format("%016d", id);
    }
}
