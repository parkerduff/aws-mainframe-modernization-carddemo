package com.cardemo.service;

import com.cardemo.dto.request.TransactionRequest;
import com.cardemo.dto.response.TransactionResponse;
import com.cardemo.entity.*;
import com.cardemo.exception.BusinessException;
import com.cardemo.exception.ResourceNotFoundException;
import com.cardemo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CardXrefRepository cardXrefRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionTypeRepository transactionTypeRepository;
    @Mock private TranCatBalanceRepository tranCatBalanceRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Card testCard;
    private CardXref testXref;
    private Account testAccount;
    private TransactionType testType;

    @BeforeEach
    void setUp() {
        testCard = new Card();
        testCard.setCardNum("4111111111111111");
        testCard.setAcctId(1000000001L);
        testCard.setActiveStatus("Y");

        testXref = new CardXref();
        testXref.setCardNum("4111111111111111");
        testXref.setCustId(1L);
        testXref.setAcctId(1000000001L);

        testAccount = new Account();
        testAccount.setAcctId(1000000001L);
        testAccount.setActiveStatus("Y");
        testAccount.setCurrBal(new BigDecimal("1500.00"));
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        testAccount.setCashCreditLimit(new BigDecimal("1500.00"));
        testAccount.setCurrCycCredit(BigDecimal.ZERO);
        testAccount.setCurrCycDebit(BigDecimal.ZERO);

        testType = new TransactionType();
        testType.setTypeCd("SA");
        testType.setTypeDesc("Sale");
    }

    @Test
    void createTransaction_validSale_succeeds() {
        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(testCard));
        when(transactionTypeRepository.findById("SA")).thenReturn(Optional.of(testType));
        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testXref));
        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArguments()[0]);

        TransactionRequest request = new TransactionRequest(
                "SA", 5001, "ONLINE", "PURCHASE", new BigDecimal("100.00"),
                null, "TEST MERCHANT", "NEW YORK", "10001", "4111111111111111");

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertEquals("SA", response.typeCd());
        assertEquals(new BigDecimal("100.00"), response.amount());
    }

    @Test
    void createTransaction_inactiveCard_throwsBusinessException() {
        testCard.setActiveStatus("N");
        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(testCard));

        TransactionRequest request = new TransactionRequest(
                "SA", 5001, "ONLINE", "PURCHASE", new BigDecimal("100.00"),
                null, "TEST MERCHANT", "NEW YORK", "10001", "4111111111111111");

        assertThrows(BusinessException.class, () -> transactionService.createTransaction(request));
    }

    @Test
    void createTransaction_invalidCard_throwsNotFound() {
        when(cardRepository.findById("0000000000000000")).thenReturn(Optional.empty());

        TransactionRequest request = new TransactionRequest(
                "SA", 5001, "ONLINE", "PURCHASE", new BigDecimal("100.00"),
                null, "TEST MERCHANT", "NEW YORK", "10001", "0000000000000000");

        assertThrows(ResourceNotFoundException.class, () -> transactionService.createTransaction(request));
    }

    @Test
    void createTransaction_exceedsCreditLimit_throwsBusinessException() {
        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(testCard));
        when(transactionTypeRepository.findById("SA")).thenReturn(Optional.of(testType));
        when(cardXrefRepository.findById("4111111111111111")).thenReturn(Optional.of(testXref));
        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(testAccount));

        TransactionRequest request = new TransactionRequest(
                "SA", 5001, "ONLINE", "PURCHASE", new BigDecimal("10000.00"),
                null, "TEST MERCHANT", "NEW YORK", "10001", "4111111111111111");

        assertThrows(BusinessException.class, () -> transactionService.createTransaction(request));
    }
}
