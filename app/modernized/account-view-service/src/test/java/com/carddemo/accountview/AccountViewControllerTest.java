package com.carddemo.accountview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the REST front door maps the legacy outcomes onto HTTP status codes
 * and JSON, using the seed data from {@link RepositoryConfig}.
 */
@SpringBootTest
class AccountViewControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void seededAccountReturns200WithDetails() throws Exception {
        mockMvc().perform(get("/api/accounts/view").param("accountId", "00000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.accountDetails.ssnFormatted").value("123-45-6789"))
                .andExpect(jsonPath("$.accountDetails.currentBalance").value(1250.75));
    }

    @Test
    void blankInputReturns400() throws Exception {
        mockMvc().perform(get("/api/accounts/view"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorMessage").value("Account number not provided"));
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        mockMvc().perform(get("/api/accounts/view").param("accountId", "00000000999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("XREF_NOT_FOUND"));
    }
}
