package com.carddemo.accountview;

import com.carddemo.accountview.model.Account;
import com.carddemo.accountview.model.CardXref;
import com.carddemo.accountview.model.Customer;
import com.carddemo.accountview.repository.AccountRepository;
import com.carddemo.accountview.repository.CardXrefRepository;
import com.carddemo.accountview.repository.CustomerRepository;
import com.carddemo.accountview.repository.InMemoryAccountRepository;
import com.carddemo.accountview.repository.InMemoryCardXrefRepository;
import com.carddemo.accountview.repository.InMemoryCustomerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Wires the in-memory repositories and seeds one representative account so the
 * service is runnable as a standalone Spring Boot app (e.g. for the REST demo
 * endpoint). The seed values follow the CardDemo sample-data conventions.
 */
@Configuration
public class RepositoryConfig {

    @Bean
    CardXrefRepository cardXrefRepository() {
        InMemoryCardXrefRepository repo = new InMemoryCardXrefRepository();
        repo.save(new CardXref("4111111111111111", "000000001", "00000000001"));
        return repo;
    }

    @Bean
    AccountRepository accountRepository() {
        InMemoryAccountRepository repo = new InMemoryAccountRepository();
        repo.save(new Account(
                "00000000001",
                "Y",
                new BigDecimal("1250.75"),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                "2020-01-15",
                "2027-01-31",
                "2024-01-15",
                new BigDecimal("250.00"),
                new BigDecimal("75.50"),
                "30301",
                "DEFAULT"));
        return repo;
    }

    @Bean
    CustomerRepository customerRepository() {
        InMemoryCustomerRepository repo = new InMemoryCustomerRepository();
        repo.save(new Customer(
                "000000001",
                "JOHN",
                "Q",
                "PUBLIC",
                "123 MAIN ST",
                "APT 4",
                "ATLANTA",
                "GA",
                "USA",
                "30301",
                "404-555-0100",
                "404-555-0101",
                "123456789",
                "GA-DL-987654",
                "1985-06-15",
                "EFT0000001",
                "Y",
                720));
        return repo;
    }
}
