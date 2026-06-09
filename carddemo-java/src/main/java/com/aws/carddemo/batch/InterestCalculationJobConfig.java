package com.aws.carddemo.batch;

import com.aws.carddemo.entity.AccountEntity;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.util.CobolConversions;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job replacing {@code app/cbl/CBACT04C.cbl} (interest calculation).
 *
 * <p>CBACT04C walks the accounts, looks up the disclosure-group interest rate, and for each
 * category computes monthly interest {@code = (balance * rate) / 1200} which it accumulates
 * back into the account balance. This job applies the same monthly-interest formula per
 * account using a configurable annual percentage rate, then updates the account balance.</p>
 */
@Configuration
public class InterestCalculationJobConfig {

    public static final String JOB_NAME = "interestCalculationJob";

    private final AccountRepository accountRepository;

    @Value("${carddemo.batch.annual-interest-rate:18.00}")
    private BigDecimal annualInterestRate;

    public InterestCalculationJobConfig(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Bean
    public RepositoryItemReader<AccountEntity> accountReader() {
        return new RepositoryItemReaderBuilder<AccountEntity>()
                .name("accountReader")
                .repository(accountRepository)
                .methodName("findAll")
                .pageSize(50)
                .sorts(Map.of("acctId", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<AccountEntity, AccountEntity> interestProcessor() {
        return account -> {
            BigDecimal balance = CobolConversions.money(account.getCurrBal());
            // COBOL: WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
            BigDecimal monthlyInterest = balance
                    .multiply(annualInterestRate)
                    .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);
            account.setCurrBal(balance.add(monthlyInterest));
            return account;
        };
    }

    @Bean
    public RepositoryItemWriter<AccountEntity> accountWriter() {
        return new RepositoryItemWriterBuilder<AccountEntity>()
                .repository(accountRepository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step interestCalculationStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .<AccountEntity, AccountEntity>chunk(50, transactionManager)
                .reader(accountReader())
                .processor(interestProcessor())
                .writer(accountWriter())
                .build();
    }

    @Bean
    public Job interestCalculationJob(JobRepository jobRepository, Step interestCalculationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(interestCalculationStep)
                .build();
    }
}
