package com.carddemo.interest.config;

import com.carddemo.interest.domain.AccountRecord;
import com.carddemo.interest.domain.CardXrefRecord;
import com.carddemo.interest.domain.DisclosureGroupRecord;
import com.carddemo.interest.repository.AccountRepository;
import com.carddemo.interest.repository.CardXrefRepository;
import com.carddemo.interest.repository.DisclosureGroupRepository;
import com.carddemo.interest.repository.InMemoryRepositories;
import com.carddemo.interest.repository.TransactionWriter;
import com.carddemo.interest.service.TimestampProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Wires the CBACT04C file gateways as Spring beans, seeded from the CardDemo ASCII
 * flat files packaged on the classpath under {@code seed/}. This lets the modernized
 * batch run as a self-contained Spring Boot application without a mainframe or database.
 */
@Configuration
public class BatchConfig {

    @Bean
    public TimestampProvider timestampProvider() {
        return TimestampProvider.systemClock();
    }

    @Bean
    public AccountRepository accountRepository() {
        InMemoryRepositories.Accounts repo = new InMemoryRepositories.Accounts();
        for (AccountRecord a : FixedWidthFileLoader.loadAccounts(new ClassPathResource("seed/acctdata.txt"))) {
            repo.put(a);
        }
        return repo;
    }

    @Bean
    public CardXrefRepository cardXrefRepository() {
        InMemoryRepositories.CardXrefs repo = new InMemoryRepositories.CardXrefs();
        for (CardXrefRecord x : FixedWidthFileLoader.loadCardXrefs(new ClassPathResource("seed/cardxref.txt"))) {
            repo.put(x);
        }
        return repo;
    }

    @Bean
    public DisclosureGroupRepository disclosureGroupRepository() {
        InMemoryRepositories.DisclosureGroups repo = new InMemoryRepositories.DisclosureGroups();
        for (DisclosureGroupRecord d :
                FixedWidthFileLoader.loadDisclosureGroups(new ClassPathResource("seed/discgrp.txt"))) {
            repo.put(d);
        }
        return repo;
    }

    @Bean
    public TransactionWriter transactionWriter() {
        return new InMemoryRepositories.CollectingTransactionWriter();
    }
}
