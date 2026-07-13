package com.carddemo.authorization.service;

import com.carddemo.authorization.domain.FraudFlag;
import com.carddemo.authorization.domain.MatchStatus;
import com.carddemo.authorization.domain.PendingAuthDetail;
import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.messaging.AuthorizationReply;
import com.carddemo.authorization.messaging.AuthorizationRequest;
import com.carddemo.authorization.repository.PendingAuthSummaryRepository;
import com.carddemo.authorization.service.decision.AuthorizationContext;
import com.carddemo.authorization.service.decision.AuthorizationDecision;
import com.carddemo.authorization.service.decision.AuthorizationDecisionEngine;
import com.carddemo.authorization.service.xref.Account;
import com.carddemo.authorization.service.xref.AccountService;
import com.carddemo.authorization.service.xref.CardXref;
import com.carddemo.authorization.service.xref.CrossReferenceService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authorization request orchestration (BR-07) — ports {@code COPAUA0C}
 * {@code 5000-PROCESS-AUTH} / {@code 8000-WRITE-AUTH-TO-DB}.
 *
 * <p>Resolves the card through the cross-reference/account services, runs the
 * decision engine, builds the reply, and (when the card is found) persists the
 * summary + detail records.
 */
@Service
public class AuthorizationRequestService {

    private final CrossReferenceService crossReferenceService;
    private final AccountService accountService;
    private final PendingAuthSummaryRepository summaryRepository;
    private final AuthorizationDecisionEngine decisionEngine;
    private final Clock clock;

    public AuthorizationRequestService(CrossReferenceService crossReferenceService,
                                       AccountService accountService,
                                       PendingAuthSummaryRepository summaryRepository,
                                       AuthorizationDecisionEngine decisionEngine,
                                       Clock clock) {
        this.crossReferenceService = crossReferenceService;
        this.accountService = accountService;
        this.summaryRepository = summaryRepository;
        this.decisionEngine = decisionEngine;
        this.clock = clock;
    }

    @Transactional
    public AuthorizationReply process(AuthorizationRequest request) {
        Optional<CardXref> xref = crossReferenceService.findByCardNum(request.cardNum());
        boolean cardFoundInXref = xref.isPresent();

        Optional<Account> account = xref.flatMap(x -> accountService.findByAcctId(x.acctId()));
        Optional<PendingAuthSummary> summary = xref.flatMap(x -> summaryRepository.findById(x.acctId()));

        AuthorizationContext context = new AuthorizationContext(
                request, summary, account, cardFoundInXref, cardFoundInXref);
        AuthorizationDecision decision = decisionEngine.decide(context);

        AuthorizationReply reply = buildReply(request, decision);

        // BR-07.3 / BR-07.4 — only persist when the card resolved in the cross-reference.
        if (cardFoundInXref) {
            persist(request, reply, decision, xref.get(), account, summary);
        }
        return reply;
    }

    private AuthorizationReply buildReply(AuthorizationRequest request, AuthorizationDecision decision) {
        BigDecimal approvedAmt = decision.approved() ? request.transactionAmt() : BigDecimal.ZERO;
        return new AuthorizationReply(
                request.cardNum(),
                request.transactionId(),
                request.authTime(),
                decision.respCode(),
                decision.reason().code(),
                approvedAmt);
    }

    private void persist(AuthorizationRequest request,
                         AuthorizationReply reply,
                         AuthorizationDecision decision,
                         CardXref xref,
                         Optional<Account> account,
                         Optional<PendingAuthSummary> existingSummary) {
        PendingAuthSummary summary = existingSummary.orElseGet(
                () -> new PendingAuthSummary(xref.acctId(), xref.custId()));

        account.ifPresent(a -> {
            summary.setCreditLimit(nvl(a.creditLimit()));
            summary.setCashLimit(nvl(a.cashCreditLimit()));
        });

        BigDecimal approvedAmt = reply.approvedAmt();
        if (decision.approved()) {
            summary.setApprovedAuthCount(summary.getApprovedAuthCount() + 1);
            summary.setApprovedAuthAmount(summary.getApprovedAuthAmount().add(approvedAmt));
            summary.setCreditBalance(summary.getCreditBalance().add(approvedAmt));
            summary.setCashBalance(BigDecimal.ZERO);
        } else {
            summary.setDeclinedAuthCount(summary.getDeclinedAuthCount() + 1);
            summary.setDeclinedAuthAmount(summary.getDeclinedAuthAmount().add(request.transactionAmt()));
        }

        summary.addDetail(buildDetail(request, reply, decision));
        summaryRepository.save(summary);
    }

    private PendingAuthDetail buildDetail(AuthorizationRequest request,
                                          AuthorizationReply reply,
                                          AuthorizationDecision decision) {
        PendingAuthDetail detail = new PendingAuthDetail();
        detail.setAuthTs(LocalDateTime.now(clock));
        detail.setAuthOrigDate(request.authDate());
        detail.setAuthOrigTime(request.authTime());
        detail.setCardNum(request.cardNum());
        detail.setAuthType(request.authType());
        detail.setCardExpiryDate(request.cardExpiryDate());
        detail.setMessageType(request.messageType());
        detail.setMessageSource(request.messageSource());
        detail.setAuthIdCode(reply.authIdCode());
        detail.setAuthRespCode(reply.authRespCode());
        detail.setAuthRespReason(reply.authRespReason());
        detail.setProcessingCode(request.processingCode());
        detail.setTransactionAmt(request.transactionAmt());
        detail.setApprovedAmt(reply.approvedAmt());
        detail.setMerchantCategoryCode(request.merchantCategoryCode());
        detail.setAcqrCountryCode(request.acqrCountryCode());
        detail.setPosEntryMode(request.posEntryMode());
        detail.setMerchantId(request.merchantId());
        detail.setMerchantName(request.merchantName());
        detail.setMerchantCity(request.merchantCity());
        detail.setMerchantState(request.merchantState());
        detail.setMerchantZip(request.merchantZip());
        detail.setTransactionId(request.transactionId());
        detail.setMatchStatus(decision.approved()
                ? MatchStatus.PENDING.code()
                : MatchStatus.AUTH_DECLINED.code());
        detail.setAuthFraud(FraudFlag.NONE);
        return detail;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

