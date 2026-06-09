package com.aws.carddemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.entity.TransactionEntity;
import com.aws.carddemo.repository.CardXrefRepository;
import com.aws.carddemo.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Tests for {@link TransactionService}, focusing on the COTRN00C transaction-list paging.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, cardXrefRepository);
    }

    private TransactionEntity tran(String id, String card, String amt) {
        TransactionEntity t = new TransactionEntity();
        t.setTranId(id);
        t.setTranCardNum(card);
        t.setTranAmt(new BigDecimal(amt));
        t.setTranOrigTs("2024-01-15-10.00.00.000000");
        return t;
    }

    @Test
    void listTransactionsReturnsPagedResultsForCard() {
        String card = "0500024453765740";
        Pageable pageable = PageRequest.of(0, 2);
        List<TransactionEntity> rows = List.of(
                tran("0000000000000001", card, "125.50"),
                tran("0000000000000002", card, "89.99"));
        when(transactionRepository.findByTranCardNumOrderByTranOrigTsDesc(eq(card), eq(pageable)))
                .thenReturn(new PageImpl<>(rows, pageable, 2));

        Page<TransactionDto> page = transactionService.listTransactions(card, pageable);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(TransactionDto::tranId)
                .containsExactly("0000000000000001", "0000000000000002");
        assertThat(page.getContent().get(0).tranAmt()).isEqualByComparingTo("125.50");
    }
}
