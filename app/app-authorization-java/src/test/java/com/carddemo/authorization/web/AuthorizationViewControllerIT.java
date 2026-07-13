package com.carddemo.authorization.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carddemo.authorization.domain.FraudFlag;
import com.carddemo.authorization.domain.MatchStatus;
import com.carddemo.authorization.domain.PendingAuthDetail;
import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.repository.PendingAuthSummaryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * REST tests for authorization viewing + fraud toggle endpoints.
 *
 * <p>Traceability: BR-08.1 (list + pagination), BR-08.2 (detail), BR-03 (fraud toggle
 * endpoint).
 */
@SpringBootTest(properties = "carddemo.purge.enabled=false")
@AutoConfigureMockMvc
class AuthorizationViewControllerIT {

    private static final long ACCT_ID = 400000004L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PendingAuthSummaryRepository summaryRepository;

    private Long firstDetailId;

    @BeforeEach
    void seed() {
        PendingAuthSummary summary = new PendingAuthSummary(ACCT_ID, 44L);
        summary.setCreditLimit(new BigDecimal("5000.00"));
        summary.setCreditBalance(new BigDecimal("600.00"));
        summary.setApprovedAuthCount(6);
        for (int i = 0; i < 6; i++) {
            PendingAuthDetail detail = new PendingAuthDetail();
            detail.setAuthTs(LocalDateTime.of(2024, 1, 1, 0, 0).plusMinutes(i));
            detail.setCardNum("4111111111111111");
            detail.setAuthRespCode("00");
            detail.setTransactionAmt(new BigDecimal("100.00"));
            detail.setApprovedAmt(new BigDecimal("100.00"));
            detail.setMatchStatus(MatchStatus.PENDING.code());
            detail.setAuthFraud(FraudFlag.NONE);
            detail.setTransactionId("TX" + i);
            summary.addDetail(detail);
        }
        PendingAuthSummary saved = summaryRepository.saveAndFlush(summary);
        firstDetailId = saved.getDetails().get(0).getId();
    }

    @AfterEach
    void cleanUp() {
        summaryRepository.deleteById(ACCT_ID);
    }

    @Test
    void listsAuthorizationsWithDefaultPageSizeFive() throws Exception {
        mockMvc.perform(get("/api/accounts/{acctId}/authorizations", ACCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @Test
    void returnsSummaryForAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/{acctId}/summary", ACCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acctId").value(ACCT_ID))
                .andExpect(jsonPath("$.availableAmount").value(4400.00));
    }

    @Test
    void returnsDetailById() throws Exception {
        mockMvc.perform(get("/api/authorizations/{id}", firstDetailId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstDetailId))
                .andExpect(jsonPath("$.approved").value(true));
    }

    @Test
    void unknownDetailReturns404() throws Exception {
        mockMvc.perform(get("/api/authorizations/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void fraudToggleMarksAuthorization() throws Exception {
        mockMvc.perform(post("/api/authorizations/{id}/fraud-toggle", firstDetailId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fraudConfirmed").value(true))
                .andExpect(jsonPath("$.action").value("REPORT"));
    }
}

