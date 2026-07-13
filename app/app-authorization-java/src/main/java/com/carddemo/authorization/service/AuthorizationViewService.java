package com.carddemo.authorization.service;

import com.carddemo.authorization.repository.PendingAuthDetailRepository;
import com.carddemo.authorization.repository.PendingAuthSummaryRepository;
import com.carddemo.authorization.web.dto.AuthorizationDetailView;
import com.carddemo.authorization.web.dto.AuthorizationSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side service for authorization viewing (BR-08) — replaces the query logic of
 * the {@code COPAUS0C}/{@code COPAUS1C} BMS screens.
 */
@Service
@Transactional(readOnly = true)
public class AuthorizationViewService {

    private final PendingAuthSummaryRepository summaryRepository;
    private final PendingAuthDetailRepository detailRepository;

    public AuthorizationViewService(PendingAuthSummaryRepository summaryRepository,
                                    PendingAuthDetailRepository detailRepository) {
        this.summaryRepository = summaryRepository;
        this.detailRepository = detailRepository;
    }

    public AuthorizationSummaryView getSummary(Long acctId) {
        return summaryRepository.findById(acctId)
                .map(AuthorizationSummaryView::from)
                .orElseThrow(() -> new AuthorizationNotFoundException(
                        "No authorization summary for account " + acctId));
    }

    public Page<AuthorizationDetailView> listAuthorizations(Long acctId, Pageable pageable) {
        return detailRepository.findBySummaryAcctIdOrderByAuthTsDesc(acctId, pageable)
                .map(AuthorizationDetailView::from);
    }

    public AuthorizationDetailView getDetail(Long detailId) {
        return detailRepository.findById(detailId)
                .map(AuthorizationDetailView::from)
                .orElseThrow(() -> new AuthorizationNotFoundException(
                        "Authorization detail not found: " + detailId));
    }
}

