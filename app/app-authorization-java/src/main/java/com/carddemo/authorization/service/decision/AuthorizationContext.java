package com.carddemo.authorization.service.decision;

import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.messaging.AuthorizationRequest;
import com.carddemo.authorization.service.xref.Account;
import java.util.Optional;

/**
 * Inputs to the authorization decision (BR-01).
 *
 * <p>Bundles the request together with the data gathered by
 * {@code COPAUA0C} {@code 5100}-{@code 5500} (cross-reference / account / summary
 * lookups) so the decision engine has everything {@code 6000-MAKE-DECISION} needs.
 */
public record AuthorizationContext(
        AuthorizationRequest request,
        Optional<PendingAuthSummary> summary,
        Optional<Account> account,
        boolean cardFoundInXref,
        boolean customerFound) {
}

