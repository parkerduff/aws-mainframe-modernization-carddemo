package com.cardemo.service;

import com.cardemo.dto.request.TransactionRequest;
import com.cardemo.dto.response.TransactionResponse;
import com.cardemo.entity.Account;
import com.cardemo.entity.Card;
import com.cardemo.entity.CardXref;
import com.cardemo.entity.Transaction;
import com.cardemo.entity.TranCatBalance;
import com.cardemo.entity.TranCatBalanceId;
import com.cardemo.exception.BusinessException;
import com.cardemo.exception.ResourceNotFoundException;
import com.cardemo.repository.AccountRepository;
import com.cardemo.repository.CardRepository;
import com.cardemo.repository.CardXrefRepository;
import com.cardemo.repository.TranCatBalanceRepository;
import com.cardemo.repository.TransactionRepository;
import com.cardemo.repository.TransactionTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Transaction service - migrated from COBOL programs:
 * - COTRN00C.cbl (CT00 transaction) - transaction list
 * - COTRN01C.cbl (CT01 transaction) - transaction view
 * - COTRN02C.cbl (CT02 transaction) - add transaction
 * Replaces VSAM operations on TRANSACT KSDS file and DALYTRAN processing.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final DateTimeFormatter TRAN_ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TranCatBalanceRepository tranCatBalanceRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              CardRepository cardRepository,
                              CardXrefRepository cardXrefRepository,
                              AccountRepository accountRepository,
                              TransactionTypeRepository transactionTypeRepository,
                              TranCatBalanceRepository tranCatBalanceRepository) {
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.tranCatBalanceRepository = tranCatBalanceRepository;
    }

    public TransactionResponse getTransaction(String tranId) {
        Transaction txn = transactionRepository.findById(tranId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + tranId));
        return TransactionResponse.from(txn);
    }

    public Page<TransactionResponse> listTransactionsByAccount(Long acctId, Pageable pageable) {
        return transactionRepository.findByAccountId(acctId, pageable)
                .map(TransactionResponse::from);
    }

    public Page<TransactionResponse> listTransactionsByCard(String cardNum, Pageable pageable) {
        return transactionRepository.findByCardNum(cardNum, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        // Validate card exists and is active
        Card card = cardRepository.findById(request.cardNum())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + request.cardNum()));
        if (!"Y".equals(card.getActiveStatus())) {
            throw new BusinessException("Card is not active: " + request.cardNum());
        }

        // Validate transaction type exists
        transactionTypeRepository.findById(request.typeCd())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type not found: " + request.typeCd()));

        // Get the account via card cross-reference
        CardXref xref = cardXrefRepository.findById(request.cardNum())
                .orElseThrow(() -> new ResourceNotFoundException("Card cross-reference not found: " + request.cardNum()));
        Account account = accountRepository.findById(xref.getAcctId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + xref.getAcctId()));

        // Validate credit availability (like COBOL COACTUPC validation)
        if ("SA".equals(request.typeCd()) || "CA".equals(request.typeCd())) {
            if (!account.hasAvailableCredit(request.amount())) {
                throw new BusinessException("Insufficient credit for transaction amount: " + request.amount());
            }
        }
        if ("CA".equals(request.typeCd())) {
            if (!account.hasAvailableCashCredit(request.amount())) {
                throw new BusinessException("Insufficient cash credit limit for amount: " + request.amount());
            }
        }

        // Create transaction record
        LocalDateTime now = LocalDateTime.now();
        String tranId = "TRN" + now.format(TRAN_ID_FORMAT).substring(0, 13);

        Transaction txn = new Transaction();
        txn.setTranId(tranId);
        txn.setTypeCd(request.typeCd());
        txn.setCatCd(request.catCd());
        txn.setSource(request.source());
        txn.setDescription(request.description());
        txn.setAmount(request.amount());
        txn.setMerchantId(request.merchantId());
        txn.setMerchantName(request.merchantName());
        txn.setMerchantCity(request.merchantCity());
        txn.setMerchantZip(request.merchantZip());
        txn.setCardNum(request.cardNum());
        txn.setOrigTs(now);
        txn.setProcTs(now);
        txn.setCreatedAt(now);

        Transaction saved = transactionRepository.save(txn);

        // Update account balance (like COBOL batch posting)
        BigDecimal newBal;
        if ("PY".equals(request.typeCd()) || "CR".equals(request.typeCd())) {
            newBal = account.getCurrBal().subtract(request.amount());
            account.setCurrCycCredit(account.getCurrCycCredit().add(request.amount()));
        } else {
            newBal = account.getCurrBal().add(request.amount());
            account.setCurrCycDebit(account.getCurrCycDebit().add(request.amount()));
        }
        account.setCurrBal(newBal);
        account.setUpdatedAt(now);
        accountRepository.save(account);

        // Update category balance
        updateCategoryBalance(xref.getAcctId(), request.typeCd(), request.catCd(), request.amount());

        log.info("Transaction created: {} for card {} amount {}", tranId, request.cardNum(), request.amount());
        return TransactionResponse.from(saved);
    }

    private void updateCategoryBalance(Long acctId, String typeCd, Integer catCd, BigDecimal amount) {
        if (catCd == null) return;
        TranCatBalanceId id = new TranCatBalanceId(acctId, typeCd, catCd);
        TranCatBalance balance = tranCatBalanceRepository.findById(id).orElseGet(() -> {
            TranCatBalance newBal = new TranCatBalance();
            newBal.setAcctId(acctId);
            newBal.setTypeCd(typeCd);
            newBal.setCatCd(catCd);
            newBal.setBalance(BigDecimal.ZERO);
            newBal.setCreatedAt(LocalDateTime.now());
            return newBal;
        });
        balance.setBalance(balance.getBalance().add(amount));
        balance.setUpdatedAt(LocalDateTime.now());
        tranCatBalanceRepository.save(balance);
    }
}
