package com.aws.carddemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the CardDemo Spring Boot application.
 *
 * <p>This is the Java 21 / Spring Boot migration of the COBOL CardDemo mainframe
 * application. The legacy system was a CICS online + JCL batch suite backed by VSAM
 * files; this application exposes the same business capabilities as a REST API backed
 * by a relational database.</p>
 */
@SpringBootApplication
public class CardDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoApplication.class, args);
    }
}
