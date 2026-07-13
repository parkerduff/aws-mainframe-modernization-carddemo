package com.carddemo.authorization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQ queue configuration (BR-06, BR-10.2).
 *
 * @param requestQueue inbound authorization request queue
 *                     (default {@code AWS.M2.CARDDEMO.PAUTH.REQUEST}).
 * @param replyQueue   outbound authorization reply queue
 *                     (default {@code AWS.M2.CARDDEMO.PAUTH.REPLY}).
 */
@ConfigurationProperties(prefix = "carddemo.mq")
public record MessagingProperties(String requestQueue, String replyQueue) {

    public MessagingProperties {
        if (requestQueue == null || requestQueue.isBlank()) {
            requestQueue = "AWS.M2.CARDDEMO.PAUTH.REQUEST";
        }
        if (replyQueue == null || replyQueue.isBlank()) {
            replyQueue = "AWS.M2.CARDDEMO.PAUTH.REPLY";
        }
    }
}

