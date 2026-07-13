package com.carddemo.authorization.config;

import com.carddemo.authorization.batch.PurgeProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core beans and configuration-properties registration.
 */
@Configuration
@EnableConfigurationProperties({MessagingProperties.class, PurgeProperties.class})
public class ApplicationConfig {

    /** System clock — injected everywhere time is needed so tests can use a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

