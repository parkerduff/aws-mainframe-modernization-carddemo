package com.aws.carddemo.authorization.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the CardDemo fraud detection module.
 *
 * <p>This module is a Java migration of the COBOL programs COPAUS1C (fraud
 * marking orchestration) and COPAUS2C (DB2 upsert into CARDDEMO.AUTHFRDS)
 * from {@code app/app-authorization-ims-db2-mq/cbl/}.
 */
@SpringBootApplication
public class FraudApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudApplication.class, args);
    }
}
