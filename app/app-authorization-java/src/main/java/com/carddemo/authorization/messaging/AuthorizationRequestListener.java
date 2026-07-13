package com.carddemo.authorization.messaging;

import com.carddemo.authorization.config.MessagingProperties;
import com.carddemo.authorization.service.AuthorizationRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * JMS listener for authorization requests (BR-06, BR-07) — replaces the CICS MQ
 * trigger of {@code COPAUA0C}.
 *
 * <p>Consumes a comma-separated request from the request queue, delegates to the
 * decisioning/persistence pipeline, and publishes the formatted reply to the reply
 * queue.
 */
@Component
public class AuthorizationRequestListener {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationRequestListener.class);

    private final AuthorizationMessageCodec codec;
    private final AuthorizationRequestService requestService;
    private final JmsTemplate jmsTemplate;
    private final MessagingProperties messagingProperties;

    public AuthorizationRequestListener(AuthorizationMessageCodec codec,
                                        AuthorizationRequestService requestService,
                                        JmsTemplate jmsTemplate,
                                        MessagingProperties messagingProperties) {
        this.codec = codec;
        this.requestService = requestService;
        this.jmsTemplate = jmsTemplate;
        this.messagingProperties = messagingProperties;
    }

    @JmsListener(destination = "${carddemo.mq.request-queue:AWS.M2.CARDDEMO.PAUTH.REQUEST}")
    public void onRequest(String message) {
        log.debug("Received authorization request: {}", message);
        AuthorizationRequest request = codec.parseRequest(message);
        AuthorizationReply reply = requestService.process(request);
        String replyMessage = codec.formatReply(reply);
        jmsTemplate.convertAndSend(messagingProperties.replyQueue(), replyMessage);
        log.debug("Sent authorization reply: {}", replyMessage);
    }
}

