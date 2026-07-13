package com.carddemo.authorization.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.authorization.config.MessagingProperties;
import com.carddemo.authorization.service.xref.Account;
import com.carddemo.authorization.service.xref.CardXref;
import com.carddemo.authorization.service.xref.InMemoryReferenceDataStore;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

/**
 * End-to-end JMS test: request in -> reply out.
 *
 * <p>Traceability: BR-06.1/BR-06.2 (MQ message formats), BR-07 (request processing).
 * Runs against the embedded ActiveMQ Artemis broker (BR-10.2).
 */
@SpringBootTest(properties = {
        "carddemo.purge.enabled=false",
        "spring.jms.template.receive-timeout=10000"
})
class AuthorizationJmsIT {

    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private MessagingProperties messagingProperties;
    @Autowired
    private InMemoryReferenceDataStore referenceDataStore;

    @Test
    void approvesRequestAndPublishesReply() {
        String card = "4111222233334444";
        long acctId = 300000003L;
        referenceDataStore.registerXref(new CardXref(card, 33L, acctId));
        referenceDataStore.registerAccount(new Account(acctId, "Y",
                new BigDecimal("100.00"), new BigDecimal("5000.00"), new BigDecimal("1000.00")));

        String request = String.join(",",
                "240115", "120000", card, "0100", "2512", "0100", "0100",
                "000000", "250.00", "5411", "840", "90", "MERCH000000001", "TEST MERCHANT",
                "ANYTOWN", "NY", "10001", "TXJMS0000000001");

        jmsTemplate.convertAndSend(messagingProperties.requestQueue(), request);

        Object reply = jmsTemplate.receiveAndConvert(messagingProperties.replyQueue());

        assertThat(reply).isInstanceOf(String.class);
        String[] fields = ((String) reply).split(",", -1);
        assertThat(fields[0]).isEqualTo(card);          // CARD-NUM
        assertThat(fields[1]).isEqualTo("TXJMS0000000001"); // TRANSACTION-ID
        assertThat(fields[3]).isEqualTo("00");          // AUTH-RESP-CODE
        assertThat(fields[4]).isEqualTo("0000");        // AUTH-RESP-REASON
        assertThat(fields[5]).isEqualTo("+250.00");     // APPROVED-AMT
    }
}

