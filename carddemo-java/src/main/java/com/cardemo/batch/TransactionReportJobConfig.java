package com.cardemo.batch;

import com.cardemo.entity.Transaction;
import com.cardemo.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;

/**
 * Spring Batch job for transaction reporting - migrated from COBOL program CBTRN03C.cbl.
 * Generates a summary report of all transactions, grouped by type.
 * Output is logged (replacing the COBOL SYSOUT DD print output).
 */
@Configuration
public class TransactionReportJobConfig {

    private static final Logger log = LoggerFactory.getLogger(TransactionReportJobConfig.class);

    private final TransactionRepository transactionRepo;

    public TransactionReportJobConfig(TransactionRepository transactionRepo) {
        this.transactionRepo = transactionRepo;
    }

    @Bean
    public Job transactionReportJob(JobRepository jobRepository, Step transactionReportStep) {
        return new JobBuilder("transactionReportJob", jobRepository)
                .start(transactionReportStep)
                .build();
    }

    @Bean
    public Step transactionReportStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("transactionReportStep", jobRepository)
                .<Transaction, Transaction>chunk(500, txManager)
                .reader(allTransactionReader())
                .writer(reportWriter())
                .build();
    }

    @Bean
    public ItemReader<Transaction> allTransactionReader() {
        return new ListItemReader<>(transactionRepo.findAll());
    }

    @Bean
    public ItemWriter<Transaction> reportWriter() {
        return transactions -> {
            BigDecimal totalAmount = BigDecimal.ZERO;
            int count = 0;
            for (Transaction txn : transactions) {
                totalAmount = totalAmount.add(txn.getAmount());
                count++;
            }
            log.info("=== Transaction Report ===");
            log.info("Total transactions: {}", count);
            log.info("Total amount: {}", totalAmount);
            log.info("=========================");
        };
    }
}
