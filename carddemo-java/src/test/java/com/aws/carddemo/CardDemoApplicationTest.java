package com.aws.carddemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Spring application context loads with all beans (JPA, Flyway, security,
 * batch) wired together against the in-memory H2 test profile.
 */
@SpringBootTest
@ActiveProfiles("test")
class CardDemoApplicationTest {

    @Test
    void contextLoads() {
    }
}
