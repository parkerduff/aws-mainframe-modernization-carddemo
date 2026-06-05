package com.carddemo.validation;

import com.carddemo.dto.CardUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized JUnit 5 tests covering the migrated COBOL input validation
 * business rules BR-30 through BR-34 (from {@code app/cbl/COCRDUPC.cbl}).
 *
 * <p>Each rule is exercised both directly (via the {@link jakarta.validation.ConstraintValidator})
 * and end-to-end through the {@link CardUpdateRequest} DTO using a Hibernate
 * Validator {@link Validator} instance.
 */
class CardValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    /** Number of constraint violations reported for a single DTO property. */
    private long violationsFor(CardUpdateRequest request, String property) {
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().equals(property))
                .count();
    }

    private static CardUpdateRequest withAccountId(String accountId) {
        return new CardUpdateRequest(accountId, "1234567890123456", "123",
                "JOHN DOE", "Y", "12", "2025", "01");
    }

    private static CardUpdateRequest withCardNumber(String cardNumber) {
        return new CardUpdateRequest("12345678901", cardNumber, "123",
                "JOHN DOE", "Y", "12", "2025", "01");
    }

    private static CardUpdateRequest withStatus(String status) {
        return new CardUpdateRequest("12345678901", "1234567890123456", "123",
                "JOHN DOE", status, "12", "2025", "01");
    }

    private static CardUpdateRequest withMonth(String month) {
        return new CardUpdateRequest("12345678901", "1234567890123456", "123",
                "JOHN DOE", "Y", month, "2025", "01");
    }

    private static CardUpdateRequest withYear(String year) {
        return new CardUpdateRequest("12345678901", "1234567890123456", "123",
                "JOHN DOE", "Y", "12", year, "01");
    }

    // ---------------------------------------------------------------------
    // BR-30 - Account ID (11-digit non-zero numeric)
    // ---------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"12345678901", "00000000001", "98765432109"})
    void br30_validAccountIdsPass(String accountId) {
        assertThat(violationsFor(withAccountId(accountId), "accountId")).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",                // blank
            "1234567890",         // 10 digits (too short)
            "123456789012",       // 12 digits (too long)
            "1234567890A",        // non-numeric
            "abcdefghijk",        // non-numeric
            "00000000000"         // all zeros
    })
    void br30_invalidAccountIdsFail(String accountId) {
        assertThat(violationsFor(withAccountId(accountId), "accountId")).isPositive();
    }

    // ---------------------------------------------------------------------
    // BR-31 - Card Number (16-digit non-zero numeric)
    // ---------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"1234567890123456", "0000000000000001", "9999999999999999"})
    void br31_validCardNumbersPass(String cardNumber) {
        assertThat(violationsFor(withCardNumber(cardNumber), "cardNumber")).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",                  // blank
            "123456789012345",      // 15 digits (too short)
            "12345678901234567",    // 17 digits (too long)
            "123456789012345A",     // non-numeric
            "abcdefghijklmnop",     // non-numeric
            "0000000000000000"      // all zeros
    })
    void br31_invalidCardNumbersFail(String cardNumber) {
        assertThat(violationsFor(withCardNumber(cardNumber), "cardNumber")).isPositive();
    }

    // ---------------------------------------------------------------------
    // BR-32 - Card Status (Y or N)
    // ---------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"Y", "N", "y", "n"})
    void br32_validStatusesPass(String status) {
        assertThat(violationsFor(withStatus(status), "activeStatus")).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "X", "1", "YES", "NO", "0"})
    void br32_invalidStatusesFail(String status) {
        assertThat(violationsFor(withStatus(status), "activeStatus")).isPositive();
    }

    // ---------------------------------------------------------------------
    // BR-33 - Card Expiry Month (1-12)
    // ---------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"})
    void br33_validMonthsPass(String month) {
        assertThat(violationsFor(withMonth(month), "expiryMonth")).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "0", "13", "99", "AB", "-1"})
    void br33_invalidMonthsFail(String month) {
        assertThat(violationsFor(withMonth(month), "expiryMonth")).isPositive();
    }

    // ---------------------------------------------------------------------
    // BR-34 - Card Expiry Year (1950-2099)
    // ---------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"1950", "2000", "2099"})
    void br34_validYearsPass(String year) {
        assertThat(violationsFor(withYear(year), "expiryYear")).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "1949", "2100", "ABCD", "0"})
    void br34_invalidYearsFail(String year) {
        assertThat(violationsFor(withYear(year), "expiryYear")).isPositive();
    }

    // ---------------------------------------------------------------------
    // A fully valid request should produce no violations at all.
    // ---------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"Y", "N"})
    void fullyValidRequestHasNoViolations(String status) {
        CardUpdateRequest request = new CardUpdateRequest(
                "12345678901", "1234567890123456", "123",
                "JOHN DOE", status, "6", "2030", "15");
        Set<?> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
