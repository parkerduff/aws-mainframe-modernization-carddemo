package com.cardemo.service;

import com.cardemo.dto.request.AccountUpdateRequest;
import com.cardemo.dto.response.AccountResponse;
import com.cardemo.entity.Account;
import com.cardemo.exception.BusinessException;
import com.cardemo.exception.ResourceNotFoundException;
import com.cardemo.repository.AccountRepository;
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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAcctId(1000000001L);
        testAccount.setActiveStatus("Y");
        testAccount.setCurrBal(new BigDecimal("1500.00"));
        testAccount.setCreditLimit(new BigDecimal("5000.00"));
        testAccount.setCashCreditLimit(new BigDecimal("1500.00"));
        testAccount.setCurrCycCredit(BigDecimal.ZERO);
        testAccount.setCurrCycDebit(BigDecimal.ZERO);
    }

    @Test
    void getAccount_existingAccount_returnsResponse() {
        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(testAccount));

        AccountResponse response = accountService.getAccount(1000000001L);

        assertEquals(1000000001L, response.acctId());
        assertEquals("Y", response.activeStatus());
        assertEquals(new BigDecimal("1500.00"), response.currBal());
    }

    @Test
    void getAccount_nonExistingAccount_throwsNotFound() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccount(999L));
    }

    @Test
    void updateAccount_validUpdate_returnsUpdatedAccount() {
        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountUpdateRequest request = new AccountUpdateRequest(
                null, new BigDecimal("6000.00"), null, null);

        AccountResponse response = accountService.updateAccount(1000000001L, request);

        assertNotNull(response);
    }

    @Test
    void updateAccount_invalidActiveStatus_throwsBusinessException() {
        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(testAccount));

        AccountUpdateRequest request = new AccountUpdateRequest(
                "X", null, null, null);

        assertThrows(BusinessException.class,
                () -> accountService.updateAccount(1000000001L, request));
    }

    @Test
    void updateAccount_creditLimitBelowBalance_throwsBusinessException() {
        when(accountRepository.findById(1000000001L)).thenReturn(Optional.of(testAccount));

        AccountUpdateRequest request = new AccountUpdateRequest(
                null, new BigDecimal("500.00"), null, null);

        assertThrows(BusinessException.class,
                () -> accountService.updateAccount(1000000001L, request));
    }
}
