package com.carddemo.accountview;

import com.carddemo.accountview.model.Account;
import com.carddemo.accountview.model.CardXref;
import com.carddemo.accountview.model.Customer;
import com.carddemo.accountview.repository.InMemoryAccountRepository;
import com.carddemo.accountview.repository.InMemoryCardXrefRepository;
import com.carddemo.accountview.repository.InMemoryCustomerRepository;
import com.carddemo.accountview.service.AccountDetails;
import com.carddemo.accountview.service.AccountViewResult;
import com.carddemo.accountview.service.AccountViewService;
import com.carddemo.accountview.service.AccountViewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equivalence tests for {@link AccountViewService} against the documented
 * behavior of the legacy COBOL program COACTVWC (transaction CAVW).
 *
 * <p>Each test maps to a specific COBOL control-flow branch; see BEHAVIOR.md.
 */
class AccountViewServiceTest {

    private static final String ACCT_ID = "00000000001";
    private static final String CUST_ID = "000000001";

    private InMemoryCardXrefRepository xrefRepo;
    private InMemoryAccountRepository accountRepo;
    private InMemoryCustomerRepository customerRepo;
    private AccountViewService service;

    @BeforeEach
    void setUp() {
        xrefRepo = new InMemoryCardXrefRepository();
        accountRepo = new InMemoryAccountRepository();
        customerRepo = new InMemoryCustomerRepository();
        service = new AccountViewService(xrefRepo, accountRepo, customerRepo);
    }

    private void seedFullHappyPath() {
        xrefRepo.save(new CardXref("4111111111111111", CUST_ID, ACCT_ID));
        accountRepo.save(new Account(
                ACCT_ID, "Y",
                new BigDecimal("1250.75"),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                "2020-01-15", "2027-01-31", "2024-01-15",
                new BigDecimal("250.00"),
                new BigDecimal("75.50"),
                "30301", "DEFAULT"));
        customerRepo.save(new Customer(
                CUST_ID, "JOHN", "Q", "PUBLIC",
                "123 MAIN ST", "APT 4", "ATLANTA",
                "GA", "USA", "30301",
                "404-555-0100", "404-555-0101",
                "123456789", "GA-DL-987654", "1985-06-15",
                "EFT0000001", "Y", 720));
    }

    // ---------------------------------------------------------------------
    // 2210-EDIT-ACCOUNT : input validation
    // ---------------------------------------------------------------------

    @ParameterizedTest(name = "blank-like input [{0}] -> \"Account number not provided\"")
    @NullSource
    @ValueSource(strings = {"", "   ", "           ", "*", "  *  "})
    void blankInputIsRejectedWithPrompt(String input) {
        AccountViewResult result = service.viewAccount(input);

        assertThat(result.status()).isEqualTo(AccountViewStatus.VALIDATION_ERROR);
        assertThat(result.errorMessage()).isEqualTo("Account number not provided");
        assertThat(result.accountDetails()).isNull();
    }

    @ParameterizedTest(name = "non-11-digit input [{0}] -> \"...non-zero 11 digit number\"")
    @ValueSource(strings = {
            "123",              // too short -> space padded -> not numeric
            "1234567890",       // 10 digits
            "123456789012",     // 12 digits
            "0000000000A",      // 11 chars but not all digits
            "ABCDEFGHIJK",      // all alpha
            "0000000001 ",      // embedded space within 11
            "1234567890.",      // punctuation
    })
    void nonElevenDigitInputIsRejected(String input) {
        AccountViewResult result = service.viewAccount(input);

        assertThat(result.status()).isEqualTo(AccountViewStatus.VALIDATION_ERROR);
        assertThat(result.errorMessage()).isEqualTo("Account Filter must  be a non-zero 11 digit number");
        assertThat(result.accountDetails()).isNull();
    }

    @Test
    void allZeroAccountIdIsRejected() {
        AccountViewResult result = service.viewAccount("00000000000");

        assertThat(result.status()).isEqualTo(AccountViewStatus.VALIDATION_ERROR);
        assertThat(result.errorMessage()).isEqualTo("Account Filter must  be a non-zero 11 digit number");
    }

    // ---------------------------------------------------------------------
    // 9200 / 9300 / 9400 : read-and-validate not-found branches
    // ---------------------------------------------------------------------

    @Test
    void xrefNotFoundWhenAccountAbsentFromXref() {
        // valid id, but nothing seeded
        AccountViewResult result = service.viewAccount(ACCT_ID);

        assertThat(result.status()).isEqualTo(AccountViewStatus.XREF_NOT_FOUND);
        assertThat(result.errorMessage()).isEqualTo("Account:00000000001 not found in Cross ref file.");
        assertThat(result.accountDetails()).isNull();
    }

    @Test
    void accountNotFoundWhenXrefPresentButAccountMasterMissing() {
        xrefRepo.save(new CardXref("4111111111111111", CUST_ID, ACCT_ID));

        AccountViewResult result = service.viewAccount(ACCT_ID);

        assertThat(result.status()).isEqualTo(AccountViewStatus.ACCOUNT_NOT_FOUND);
        assertThat(result.errorMessage()).isEqualTo("Account:00000000001 not found in Acct Master file.");
    }

    @Test
    void customerNotFoundWhenXrefAndAccountPresentButCustomerMissing() {
        xrefRepo.save(new CardXref("4111111111111111", CUST_ID, ACCT_ID));
        accountRepo.save(new Account(
                ACCT_ID, "Y",
                new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                "2020-01-15", "2027-01-31", "2024-01-15",
                new BigDecimal("0.00"), new BigDecimal("0.00"),
                "30301", "DEFAULT"));

        AccountViewResult result = service.viewAccount(ACCT_ID);

        assertThat(result.status()).isEqualTo(AccountViewStatus.CUSTOMER_NOT_FOUND);
        assertThat(result.errorMessage()).isEqualTo("CustId:000000001 not found in customer master.");
    }

    @Test
    void customerLookupUsesCustomerIdFromXrefNotTheAccountId() {
        // xref points the account at a *different* customer id than any guess
        String realCustId = "000000777";
        xrefRepo.save(new CardXref("4111111111111111", realCustId, ACCT_ID));
        accountRepo.save(new Account(
                ACCT_ID, "Y",
                new BigDecimal("10.00"), new BigDecimal("20.00"), new BigDecimal("5.00"),
                "2020-01-15", "2027-01-31", "2024-01-15",
                new BigDecimal("1.00"), new BigDecimal("2.00"),
                "30301", "DEFAULT"));
        customerRepo.save(new Customer(
                realCustId, "JANE", "", "DOE",
                "1 A ST", "", "DENVER", "CO", "USA", "80014",
                "303-555-0100", "", "987654321", "CO-DL-1", "1990-02-02",
                "EFT0000777", "Y", 800));

        AccountViewResult result = service.viewAccount(ACCT_ID);

        assertThat(result.status()).isEqualTo(AccountViewStatus.SUCCESS);
        assertThat(result.accountDetails().customerId()).isEqualTo(realCustId);
    }

    // ---------------------------------------------------------------------
    // happy path : 1200-SETUP-SCREEN-VARS field mapping
    // ---------------------------------------------------------------------

    @Test
    void successReturnsFullyMappedAccountAndCustomerDetails() {
        seedFullHappyPath();

        AccountViewResult result = service.viewAccount(ACCT_ID);

        assertThat(result.status()).isEqualTo(AccountViewStatus.SUCCESS);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.errorMessage()).isEmpty();

        AccountDetails d = result.accountDetails();
        assertThat(d).isNotNull();
        // account-master fields
        assertThat(d.accountId()).isEqualTo(ACCT_ID);
        assertThat(d.activeStatus()).isEqualTo("Y");
        assertThat(d.currentBalance()).isEqualByComparingTo("1250.75");
        assertThat(d.creditLimit()).isEqualByComparingTo("5000.00");
        assertThat(d.cashCreditLimit()).isEqualByComparingTo("1000.00");
        assertThat(d.currentCycleCredit()).isEqualByComparingTo("250.00");
        assertThat(d.currentCycleDebit()).isEqualByComparingTo("75.50");
        assertThat(d.openDate()).isEqualTo("2020-01-15");
        assertThat(d.expirationDate()).isEqualTo("2027-01-31");
        assertThat(d.reissueDate()).isEqualTo("2024-01-15");
        assertThat(d.groupId()).isEqualTo("DEFAULT");
        // customer-master fields
        assertThat(d.customerId()).isEqualTo(CUST_ID);
        assertThat(d.ssnFormatted()).isEqualTo("123-45-6789");
        assertThat(d.ficoScore()).isEqualTo(720);
        assertThat(d.dateOfBirth()).isEqualTo("1985-06-15");
        assertThat(d.firstName()).isEqualTo("JOHN");
        assertThat(d.middleName()).isEqualTo("Q");
        assertThat(d.lastName()).isEqualTo("PUBLIC");
        assertThat(d.addrLine1()).isEqualTo("123 MAIN ST");
        assertThat(d.addrLine2()).isEqualTo("APT 4");
        assertThat(d.city()).isEqualTo("ATLANTA");
        assertThat(d.stateCode()).isEqualTo("GA");
        assertThat(d.zip()).isEqualTo("30301");
        assertThat(d.countryCode()).isEqualTo("USA");
        assertThat(d.phoneNum1()).isEqualTo("404-555-0100");
        assertThat(d.phoneNum2()).isEqualTo("404-555-0101");
        assertThat(d.govtIssuedId()).isEqualTo("GA-DL-987654");
        assertThat(d.eftAccountId()).isEqualTo("EFT0000001");
        assertThat(d.priCardHolderInd()).isEqualTo("Y");
    }

    @Test
    void monetaryScaleOfTwoIsPreservedExactly() {
        seedFullHappyPath();

        AccountDetails d = service.viewAccount(ACCT_ID).accountDetails();

        // S9(10)V99 -> exactly two decimal places, no binary-float drift
        assertThat(d.currentBalance().scale()).isEqualTo(2);
        assertThat(d.currentBalance().toPlainString()).isEqualTo("1250.75");
        assertThat(d.currentCycleDebit().toPlainString()).isEqualTo("75.50");
    }

    @Test
    void infoMessageAlwaysCarriesTheInputPrompt() {
        seedFullHappyPath();

        assertThat(service.viewAccount(ACCT_ID).infoMessage())
                .isEqualTo("Enter or update id of account to display");
        assertThat(service.viewAccount("").infoMessage())
                .isEqualTo("Enter or update id of account to display");
    }
}
