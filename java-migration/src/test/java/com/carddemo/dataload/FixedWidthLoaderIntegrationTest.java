package com.carddemo.dataload;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.domain.Account;
import com.carddemo.domain.CardXref;
import com.carddemo.domain.DailyTransaction;
import com.carddemo.domain.TranCatBalance;
import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.DailyTransactionRepository;
import com.carddemo.repository.TranCatBalanceRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * End-to-end load of the fixed-width ASCII sample files into the Flyway-managed
 * relational schema. Enabling {@code carddemo.dataload.enabled=true} activates
 * the {@link FixedWidthLoader} {@code ApplicationRunner}, which loads the data at
 * context startup; this test then asserts the persisted rows.
 */
@SpringBootTest(properties = {
        "carddemo.dataload.enabled=true",
        "carddemo.dataload.path=../app/data/ASCII"
})
class FixedWidthLoaderIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CardXrefRepository cardXrefRepository;
    @Autowired
    private TranCatBalanceRepository tranCatBalanceRepository;
    @Autowired
    private DailyTransactionRepository dailyTransactionRepository;

    @Test
    void loadsAllSampleFilesWithExpectedCounts() {
        assertThat(accountRepository.count()).isEqualTo(50);
        assertThat(cardXrefRepository.count()).isEqualTo(50);
        assertThat(tranCatBalanceRepository.count()).isEqualTo(50);
        assertThat(dailyTransactionRepository.count()).isEqualTo(300);
    }

    @Test
    void persistsAccountFieldsWithScaleTwoPrecision() {
        Account a = accountRepository.findById(1L).orElseThrow();
        assertThat(a.getAcctActiveStatus()).isEqualTo("Y");
        assertThat(a.getAcctCurrBal()).isEqualTo(new BigDecimal("194.00"));
        assertThat(a.getAcctCreditLimit()).isEqualTo(new BigDecimal("2020.00"));
        assertThat(a.getAcctCashCreditLimit()).isEqualTo(new BigDecimal("1020.00"));
    }

    @Test
    void persistsCardXref() {
        CardXref x = cardXrefRepository.findById("0500024453765740").orElseThrow();
        assertThat(x.getXrefCustId()).isEqualTo(50L);
        assertThat(x.getXrefAcctId()).isEqualTo(50L);
    }

    @Test
    void retainsExactAmountPrecisionAcrossAllRows() {
        // No amount field may drift from scale 2 after the DECIMAL round-trip.
        for (Account a : accountRepository.findAll()) {
            assertScale2(a.getAcctCurrBal());
            assertScale2(a.getAcctCreditLimit());
            assertScale2(a.getAcctCashCreditLimit());
            assertScale2(a.getAcctCurrCycCredit());
            assertScale2(a.getAcctCurrCycDebit());
        }
        for (TranCatBalance b : tranCatBalanceRepository.findAll()) {
            assertScale2(b.getTranCatBal());
        }
        for (DailyTransaction d : dailyTransactionRepository.findAll()) {
            assertScale2(d.getDalytranAmt());
        }
    }

    @Test
    void preservesSignedNegativeDailyTransactionAmount() {
        DailyTransaction negative = dailyTransactionRepository.findAll().stream()
                .filter(d -> "0000000001774260".equals(d.getDalytranId()))
                .findFirst()
                .orElseThrow();
        assertThat(negative.getDalytranAmt()).isEqualTo(new BigDecimal("-919.00"));
    }

    private static void assertScale2(BigDecimal value) {
        if (value != null) {
            assertThat(value.scale()).isEqualTo(2);
        }
    }
}
