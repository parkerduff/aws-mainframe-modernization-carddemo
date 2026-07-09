package com.aws.carddemo.authorization.fraud.web;

import static com.aws.carddemo.authorization.fraud.support.PendingAuthDetailFixture.AUTH_TS;
import static com.aws.carddemo.authorization.fraud.support.PendingAuthDetailFixture.CARD_NUM;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aws.carddemo.authorization.fraud.repository.PendingAuthDetailRepository;
import com.aws.carddemo.authorization.fraud.support.PendingAuthDetailFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer test for the fraud-toggle endpoint that replaces the BMS PF5 key (COPAUS1C).
 */
@SpringBootTest
@AutoConfigureMockMvc
class FraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PendingAuthDetailRepository pendingAuthDetailRepository;

    @BeforeEach
    void reset() {
        pendingAuthDetailRepository.deleteAll();
    }

    @Test
    void toggleFraud_returnsOkAndMessage() throws Exception {
        pendingAuthDetailRepository.save(PendingAuthDetailFixture.newDetail());

        mockMvc.perform(post("/api/authorizations/{cardNum}/{authTs}/fraud-toggle", CARD_NUM, AUTH_TS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AUTH MARKED FRAUD..."));
    }

    @Test
    void toggleFraud_missingDetail_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/authorizations/{cardNum}/{authTs}/fraud-toggle", CARD_NUM, AUTH_TS))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
