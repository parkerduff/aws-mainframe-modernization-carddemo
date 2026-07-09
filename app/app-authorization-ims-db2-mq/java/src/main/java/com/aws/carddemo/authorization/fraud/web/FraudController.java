package com.aws.carddemo.authorization.fraud.web;

import com.aws.carddemo.authorization.fraud.dto.FraudActionResult;
import com.aws.carddemo.authorization.fraud.service.FraudService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entrypoint for the fraud-marking flow, replacing the BMS PF5 key handled by COPAUS1C
 * on the online authorization summary screen.
 */
@RestController
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    /**
     * Toggle the fraud status of the authorization identified by {@code cardNum} + {@code authTs}.
     *
     * @param authTs ISO-8601 date-time of the authorization (e.g. {@code 2024-01-31T12:34:56})
     */
    @PostMapping("/api/authorizations/{cardNum}/{authTs}/fraud-toggle")
    public ResponseEntity<FraudActionResult> toggleFraud(
            @PathVariable String cardNum,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime authTs) {
        FraudActionResult result = fraudService.markAuthFraud(cardNum, authTs);
        if (result.success()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }
}
