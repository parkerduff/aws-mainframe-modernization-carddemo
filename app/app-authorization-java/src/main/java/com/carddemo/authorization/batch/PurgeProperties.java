package com.carddemo.authorization.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the expired-authorization purge (BR-09).
 *
 * @param expiryDays authorizations older than this many days are purged; the legacy
 *                   {@code CBPAUP0C} default of {@code 5} is preserved.
 * @param cron       schedule for the automatic purge (BR-09.1).
 * @param enabled    whether the scheduled purge runs.
 */
@ConfigurationProperties(prefix = "carddemo.purge")
public record PurgeProperties(Integer expiryDays, String cron, Boolean enabled) {

    public PurgeProperties {
        if (expiryDays == null || expiryDays < 0) {
            expiryDays = 5;
        }
        if (cron == null || cron.isBlank()) {
            cron = "0 0 2 * * *";
        }
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
    }
}

