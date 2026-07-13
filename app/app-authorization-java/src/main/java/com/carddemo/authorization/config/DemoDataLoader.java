package com.carddemo.authorization.config;

import com.carddemo.authorization.service.xref.Account;
import com.carddemo.authorization.service.xref.CardXref;
import com.carddemo.authorization.service.xref.InMemoryReferenceDataStore;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds demo cross-reference/account data on startup for the {@code demo} profile,
 * so the app can be exercised end-to-end without an external system of record.
 */
@Component
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final InMemoryReferenceDataStore store;

    public DemoDataLoader(InMemoryReferenceDataStore store) {
        this.store = store;
    }

    @Override
    public void run(String... args) {
        store.registerXref(new CardXref("4111111111111111", 11L, 100000001L));
        store.registerAccount(new Account(
                100000001L, "Y",
                new BigDecimal("250.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00")));
        log.info("Demo reference data loaded: card 4111111111111111 -> account 100000001");
    }
}

