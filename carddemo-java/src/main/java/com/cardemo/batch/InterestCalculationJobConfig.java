package com.cardemo.batch;

import com.cardemo.entity.Account;
import com.cardemo.entity.DisclosureGroup;
import com.cardemo.entity.TranCatBalance;
import com.cardemo.repository.AccountRepository;
import com.cardemo.repository.DisclosureGroupRepository;
import com.cardemo.repository.TranCatBalanceRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Batch job for interest calculation - migrated from COBOL program CBACT04C.cbl.
 * Calculates monthly interest charges based on account balances and disclosure group rates.
 * Uses TCATBALF (category balances) and DISCGRP (interest rates) to compute charges.
 */
@Configuration
public class InterestCalculationJobConfig {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationJobConfig.class);

    private final AccountRepository accountRepo;
    private final TranCatBalanceRepository tranCatBalanceRepo;
    private final DisclosureGroupRepository disclosureGroupRepo;

    public InterestCalculationJobConfig(AccountRepository accountRepo,
                                        TranCatBalanceRepository tranCatBalanceRepo,
                                        DisclosureGroupRepository disclosureGroupRepo) {
        this.accountRepo = accountRepo;
        this.tranCatBalanceRepo = tranCatBalanceRepo;
        this.disclosureGroupRepo = disclosureGroupRepo;
    }

    @Bean
    public Job interestCalculationJob(JobRepository jobRepository, Step interestCalculationStep) {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .start(interestCalculationStep)
                .build();
    }

    @Bean
    public Step interestCalculationStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .<Account, Account>chunk(100, txManager)
                .reader(activeAccountReader())
                .processor(interestProcessor())
                .writer(accountBalanceWriter())
                .build();
    }

    @Bean
    public ItemReader<Account> activeAccountReader() {
        return new ListItemReader<>(accountRepo.findAllByActiveStatus("Y"));
    }

    @Bean
    public ItemProcessor<Account, Account> interestProcessor() {
        return account -> {
            List<TranCatBalance> balances = tranCatBalanceRepo.findByAcctId(account.getAcctId());
            BigDecimal totalInterest = BigDecimal.ZERO;

            for (TranCatBalance catBal : balances) {
                List<DisclosureGroup> groups = disclosureGroupRepo.findByAcctGroupId(
                        account.getGroupId() != null ? account.getGroupId() : "DEFAULT");

                for (DisclosureGroup group : groups) {
                    if (group.getTranTypeCd().equals(catBal.getTypeCd())
                            && group.getTranCatCd().equals(catBal.getCatCd())) {
                        // Monthly interest = balance * (annual rate / 12 / 100)
                        BigDecimal monthlyRate = group.getIntRate()
                                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
                        BigDecimal interest = catBal.getBalance().multiply(monthlyRate)
                                .setScale(2, RoundingMode.HALF_UP);
                        totalInterest = totalInterest.add(interest);
                        break;
                    }
                }
            }

            if (totalInterest.compareTo(BigDecimal.ZERO) > 0) {
                account.setCurrBal(account.getCurrBal().add(totalInterest));
                account.setUpdatedAt(LocalDateTime.now());
                log.debug("Account {} interest charge: {}", account.getAcctId(), totalInterest);
            }
            return account;
        };
    }

    @Bean
    public ItemWriter<Account> accountBalanceWriter() {
        return accounts -> {
            int count = 0;
            for (Account account : accounts) {
                accountRepo.save(account);
                count++;
            }
            log.info("Processed interest for {} accounts", count);
        };
    }
}
