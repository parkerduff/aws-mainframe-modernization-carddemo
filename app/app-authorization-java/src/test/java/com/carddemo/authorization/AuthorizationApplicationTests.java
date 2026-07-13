package com.carddemo.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies the full Spring context (JPA + JMS + scheduling) starts.
 */
@SpringBootTest(properties = "carddemo.purge.enabled=false")
class AuthorizationApplicationTests {

    @Test
    void contextLoads() {
    }
}

