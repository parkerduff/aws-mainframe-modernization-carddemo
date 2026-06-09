package com.aws.carddemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.entity.AccountEntity;
import com.aws.carddemo.entity.CardXrefEntity;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardXrefRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link AccountService}, focusing on the COACTVWC card -&gt; XREF -&gt; account
 * lookup chain and the not-found paths that mirror the COBOL INVALID KEY handling.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, cardXrefRepository);
    }

    private AccountEntity account(long id) {
        AccountEntity a = new AccountEntity();
        a.setAcctId(id);
        a.setActiveStatus("Y");
        a.setCurrBal(new BigDecimal("194.00"));
        a.setCreditLimit(new BigDecimal("2020.00"));
        return a;
    }

    @Test
    void getAccountByCardResolvesThroughXref() {
        CardXrefEntity xref = new CardXrefEntity();
        xref.setCardNum("0500024453765740");
        xref.setAcctId(50L);
        when(cardXrefRepository.findByCardNum("0500024453765740")).thenReturn(Optional.of(xref));
        when(accountRepository.findById(50L)).thenReturn(Optional.of(account(50L)));

        AccountDto dto = accountService.getAccountByCard("0500024453765740");

        assertThat(dto.acctId()).isEqualTo(50L);
        assertThat(dto.currBal()).isEqualByComparingTo("194.00");
    }

    @Test
    void getAccountByCardThrowsWhenCardMissingFromXref() {
        when(cardXrefRepository.findByCardNum("9999999999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByCard("9999999999999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("cross-reference");
    }

    @Test
    void getAccountThrowsWhenAccountMissing() {
        when(accountRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(123L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
