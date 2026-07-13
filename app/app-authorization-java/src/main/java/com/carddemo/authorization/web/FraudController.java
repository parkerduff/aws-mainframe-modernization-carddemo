package com.carddemo.authorization.web;

import com.carddemo.authorization.service.fraud.FraudService;
import com.carddemo.authorization.service.fraud.FraudToggleResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fraud marking endpoint (BR-03, BR-04, BR-05) — REST replacement for the PF5
 * fraud toggle on the {@code COPAUS1C} BMS detail screen.
 */
@RestController
@RequestMapping("/api")
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    /** Toggle the fraud flag on an authorization (mark if clear, unmark if set). */
    @PostMapping("/authorizations/{detailId}/fraud-toggle")
    public FraudToggleResult toggleFraud(@PathVariable Long detailId) {
        return fraudService.toggleFraud(detailId);
    }
}

