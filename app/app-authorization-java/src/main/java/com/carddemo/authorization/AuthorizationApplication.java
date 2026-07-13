package com.carddemo.authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the CardDemo Credit Card Authorizations service.
 *
 * <p>This Spring Boot application is a business-requirements-driven rewrite of the
 * legacy COBOL/CICS/IMS/DB2/MQ authorization extension. See {@code REQUIREMENTS.md}
 * and {@code README.md} for the mapping between Java components and the original
 * programs.
 */
@SpringBootApplication
@EnableScheduling
public class AuthorizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationApplication.class, args);
    }
}

