package com.carddemo.authorization.web;

import com.carddemo.authorization.service.AuthorizationViewService;
import com.carddemo.authorization.web.dto.AuthorizationDetailView;
import com.carddemo.authorization.web.dto.AuthorizationSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authorization viewing endpoints (BR-08) — REST replacement for the
 * {@code COPAUS0C} (summary list) and {@code COPAUS1C} (detail) BMS screens.
 */
@RestController
@RequestMapping("/api")
public class AuthorizationViewController {

    private final AuthorizationViewService viewService;

    public AuthorizationViewController(AuthorizationViewService viewService) {
        this.viewService = viewService;
    }

    /** Account authorization summary header (BR-08.1). */
    @GetMapping("/accounts/{acctId}/summary")
    public AuthorizationSummaryView getSummary(@PathVariable Long acctId) {
        return viewService.getSummary(acctId);
    }

    /**
     * List an account's authorizations, most recent first, paginated
     * (BR-08.1 — default page size 5 to match the BMS 5-row screen).
     */
    @GetMapping("/accounts/{acctId}/authorizations")
    public Page<AuthorizationDetailView> listAuthorizations(
            @PathVariable Long acctId,
            @PageableDefault(size = 5) Pageable pageable) {
        return viewService.listAuthorizations(acctId, pageable);
    }

    /** View a single authorization detail (BR-08.2). */
    @GetMapping("/authorizations/{detailId}")
    public AuthorizationDetailView getDetail(@PathVariable Long detailId) {
        return viewService.getDetail(detailId);
    }
}

