package com.carddemo.authorization.batch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Schedules the expired-authorization purge (BR-09.1) — replaces the {@code CBPAUP0J}
 * JCL trigger. Enabled by default; disable with {@code carddemo.purge.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "carddemo.purge.enabled", havingValue = "true", matchIfMissing = true)
public class PurgeScheduler {

    private final ExpiredAuthorizationPurgeService purgeService;
    private final PurgeProperties properties;

    public PurgeScheduler(ExpiredAuthorizationPurgeService purgeService, PurgeProperties properties) {
        this.purgeService = purgeService;
        this.properties = properties;
    }

    @Scheduled(cron = "${carddemo.purge.cron:0 0 2 * * *}")
    public void runScheduledPurge() {
        purgeService.purgeExpired(properties.expiryDays());
    }
}

