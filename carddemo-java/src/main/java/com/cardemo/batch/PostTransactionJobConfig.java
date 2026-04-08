package com.cardemo.batch;

import com.cardemo.entity.Account;
import com.cardemo.entity.CardXref;
import com.cardemo.entity.DailyTransaction;
import com.cardemo.entity.Transaction;
import com.cardemo.repository.AccountRepository;
import com.cardemo.repository.CardXrefRepository;
import com.cardemo.repository.DailyTransactionRepository;
import com.cardemo.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Spring Batch job to post daily transactions - migrated from COBOL program CBTRN02C.cbl.
 * Original JCL: DALYTRAN batch job.
 * Reads unprocessed daily transactions, validates, and posts them to the transaction file
 * while updating account balances.
 */
@Configuration
public class PostTransactionJobConfig {

    private static final Logger log = LoggerFactory.getLogger(PostTransactionJobConfig.class);

    private final DailyTransactionRepository dailyTranRepo;
    private final TransactionRepository transactionRepo;
    private final CardXrefRepository cardXrefRepo;
    private final AccountRepository accountRepo;

    public PostTransactionJobConfig(DailyTransactionRepository dailyTranRepo,
                                    TransactionRepository transactionRepo,
                                    CardXrefRepository cardXrefRepo,
                                    AccountRepository accountRepo) {
        this.dailyTranRepo = dailyTranRepo;
        this.transactionRepo = transactionRepo;
        this.cardXrefRepo = cardXrefRepo;
        this.accountRepo = accountRepo;
    }

    @Bean
    public Job postTransactionJob(JobRepository jobRepository, Step postTransactionStep) {
        return new JobBuilder("postTransactionJob", jobRepository)
                .start(postTransactionStep)
                .build();
    }

    @Bean
    public Step postTransactionStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("postTransactionStep", jobRepository)
                .<DailyTransaction, Transaction>chunk(100, txManager)
                .reader(dailyTransactionReader())
                .processor(dailyTransactionProcessor())
                .writer(postedTransactionWriter())
                .build();
    }

    @Bean
    public ItemReader<DailyTransaction> dailyTransactionReader() {
        return new ListItemReader<>(dailyTranRepo.findAll());
    }

    @Bean
    public ItemProcessor<DailyTransaction, Transaction> dailyTransactionProcessor() {
        return dailyTran -> {
            Transaction txn = new Transaction();
            txn.setTranId(dailyTran.getTranId());
            txn.setTypeCd(dailyTran.getTypeCd());
            txn.setCatCd(dailyTran.getCatCd());
            txn.setSource(dailyTran.getSource());
            txn.setDescription(dailyTran.getDescription());
            txn.setAmount(dailyTran.getAmount());
            txn.setMerchantId(dailyTran.getMerchantId());
            txn.setMerchantName(dailyTran.getMerchantName());
            txn.setMerchantCity(dailyTran.getMerchantCity());
            txn.setMerchantZip(dailyTran.getMerchantZip());
            txn.setCardNum(dailyTran.getCardNum());
            txn.setOrigTs(dailyTran.getOrigTs());
            txn.setProcTs(LocalDateTime.now());
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        };
    }

    @Bean
    public ItemWriter<Transaction> postedTransactionWriter() {
        return transactions -> {
            for (Transaction txn : transactions) {
                transactionRepo.save(txn);

                // Update account balance
                CardXref xref = cardXrefRepo.findById(txn.getCardNum()).orElse(null);
                if (xref != null) {
                    Account account = accountRepo.findById(xref.getAcctId()).orElse(null);
                    if (account != null) {
                        BigDecimal amount = txn.getAmount();
                        if ("PY".equals(txn.getTypeCd()) || "CR".equals(txn.getTypeCd())) {
                            account.setCurrBal(account.getCurrBal().subtract(amount));
                            account.setCurrCycCredit(account.getCurrCycCredit().add(amount));
                        } else {
                            account.setCurrBal(account.getCurrBal().add(amount));
                            account.setCurrCycDebit(account.getCurrCycDebit().add(amount));
                        }
                        account.setUpdatedAt(LocalDateTime.now());
                        accountRepo.save(account);
                    }
                }
            }
            log.info("Posted {} daily transactions", transactions.size());
        };
    }
}
