package com.carddemo.accountview.service;

import com.carddemo.accountview.model.Account;
import com.carddemo.accountview.model.CardXref;
import com.carddemo.accountview.model.Customer;
import com.carddemo.accountview.repository.AccountRepository;
import com.carddemo.accountview.repository.CardXrefRepository;
import com.carddemo.accountview.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Modernized equivalent of the COBOL/CICS program <b>COACTVWC</b> (transaction
 * <b>CAVW</b>, "Account View").
 *
 * <p>It reproduces the input-edit and read-and-validate business logic of the
 * legacy program while dropping the CICS/BMS presentation plumbing (screen send/
 * receive, PF-key routing, commarea chaining). Specifically it mirrors:
 * <ul>
 *   <li>{@code 2210-EDIT-ACCOUNT} — account-id input validation</li>
 *   <li>{@code 9200-GETCARDXREF-BYACCT} — card xref read by account id</li>
 *   <li>{@code 9300-GETACCTDATA-BYACCT} — account master read</li>
 *   <li>{@code 9400-GETCUSTDATA-BYCUST} — customer master read</li>
 *   <li>{@code 1200-SETUP-SCREEN-VARS} — mapping of records onto output fields</li>
 * </ul>
 *
 * @see <a href="../../../../../../../BEHAVIOR.md">BEHAVIOR.md</a> for the full behavior spec.
 */
@Service
public class AccountViewService {

    /** WS-PROMPT-FOR-ACCT (COACTVWC line 121-122). */
    static final String MSG_ACCT_NOT_PROVIDED = "Account number not provided";

    /** Literal moved to WS-RETURN-MSG in 2210-EDIT-ACCOUNT (line 671-672); note the double space. */
    static final String MSG_ACCT_NOT_NUMERIC = "Account Filter must  be a non-zero 11 digit number";

    /** WS-PROMPT-FOR-INPUT (COACTVWC line 113-114) — the info message left active on the map. */
    static final String MSG_PROMPT_FOR_INPUT = "Enter or update id of account to display";

    private static final int ACCT_ID_LENGTH = 11;

    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountViewService(CardXrefRepository cardXrefRepository,
                              AccountRepository accountRepository,
                              CustomerRepository customerRepository) {
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Process an Account View request for the given screen input value.
     *
     * @param accountIdInput the raw value of the ACCTSID screen field (the same
     *                       string a 3270 user would type). {@code null}, blank
     *                       and {@code "*"} are all treated as "no input".
     */
    public AccountViewResult viewAccount(String accountIdInput) {
        // --- 2200-EDIT-MAP-INPUTS: replace '*' / spaces with "no value" ---
        String ccAcctId = normalizeInput(accountIdInput);

        // --- 2210-EDIT-ACCOUNT: individual field edits ---
        if (ccAcctId.isEmpty()) {
            // CC-ACCT-ID == LOW-VALUES/SPACES -> blank filter
            return validationError(MSG_ACCT_NOT_PROVIDED);
        }
        if (!isElevenDigit(ccAcctId) || isAllZeroes(ccAcctId)) {
            // NOT NUMERIC (incl. wrong length) OR equal to zeroes
            return validationError(MSG_ACCT_NOT_NUMERIC);
        }

        // --- 9000-READ-ACCT: xref -> account -> customer ---
        Optional<CardXref> xref = cardXrefRepository.findByAccountId(ccAcctId);
        if (xref.isEmpty()) {
            return notFound(AccountViewStatus.XREF_NOT_FOUND,
                    "Account:" + ccAcctId + " not found in Cross ref file.");
        }

        Optional<Account> account = accountRepository.findById(ccAcctId);
        if (account.isEmpty()) {
            return notFound(AccountViewStatus.ACCOUNT_NOT_FOUND,
                    "Account:" + ccAcctId + " not found in Acct Master file.");
        }

        String customerId = xref.get().customerId();
        Optional<Customer> customer = customerRepository.findById(customerId);
        if (customer.isEmpty()) {
            return notFound(AccountViewStatus.CUSTOMER_NOT_FOUND,
                    "CustId:" + customerId + " not found in customer master.");
        }

        AccountDetails details = mapToDetails(account.get(), customer.get());
        return new AccountViewResult(AccountViewStatus.SUCCESS, "", MSG_PROMPT_FOR_INPUT, details);
    }

    /**
     * 2200-EDIT-MAP-INPUTS: {@code IF ACCTSIDI = '*' OR SPACES MOVE LOW-VALUES}.
     * Returns "" to represent the legacy "no value" (LOW-VALUES) state.
     */
    private static String normalizeInput(String accountIdInput) {
        if (accountIdInput == null) {
            return "";
        }
        String stripped = accountIdInput.strip();
        if (stripped.isEmpty() || stripped.equals("*")) {
            return "";
        }
        return accountIdInput;
    }

    /**
     * COBOL {@code IS NUMERIC} on the X(11) field: every one of the 11 positions
     * must be a digit. An input shorter than 11 characters is space/low-value
     * padded on the screen and therefore fails this test.
     */
    private static boolean isElevenDigit(String value) {
        if (value.length() != ACCT_ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllZeroes(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    /** CUST-SSN (9 digits) -> XXX-XX-XXXX (COACTVWC lines 496-503). */
    static String formatSsn(String ssn) {
        if (ssn == null || ssn.length() != 9) {
            return ssn;
        }
        return ssn.substring(0, 3) + "-" + ssn.substring(3, 5) + "-" + ssn.substring(5, 9);
    }

    private static AccountDetails mapToDetails(Account a, Customer c) {
        return new AccountDetails(
                a.accountId(),
                a.activeStatus(),
                a.currentBalance(),
                a.creditLimit(),
                a.cashCreditLimit(),
                a.currentCycleCredit(),
                a.currentCycleDebit(),
                a.openDate(),
                a.expirationDate(),
                a.reissueDate(),
                a.groupId(),
                c.customerId(),
                formatSsn(c.ssn()),
                c.ficoScore(),
                c.dateOfBirth(),
                c.firstName(),
                c.middleName(),
                c.lastName(),
                c.addrLine1(),
                c.addrLine2(),
                c.addrLine3(),
                c.stateCode(),
                c.zip(),
                c.countryCode(),
                c.phoneNum1(),
                c.phoneNum2(),
                c.govtIssuedId(),
                c.eftAccountId(),
                c.priCardHolderInd());
    }

    private static AccountViewResult validationError(String message) {
        return new AccountViewResult(AccountViewStatus.VALIDATION_ERROR, message, MSG_PROMPT_FOR_INPUT, null);
    }

    private static AccountViewResult notFound(AccountViewStatus status, String message) {
        return new AccountViewResult(status, message, MSG_PROMPT_FOR_INPUT, null);
    }
}
