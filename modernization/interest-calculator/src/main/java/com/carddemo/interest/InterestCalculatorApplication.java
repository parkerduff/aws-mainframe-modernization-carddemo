package com.carddemo.interest;

import com.carddemo.interest.domain.TranCatBalRecord;
import com.carddemo.interest.repository.InMemoryRepositories;
import com.carddemo.interest.repository.TransactionWriter;
import com.carddemo.interest.service.InterestCalculationBatch;
import com.carddemo.interest.config.FixedWidthFileLoader;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

/**
 * Spring Boot entry point for the modernized CBACT04C interest-calculation batch.
 *
 * <p>Run the batch against the packaged CardDemo seed data with the {@code batch} profile:
 * {@code mvn spring-boot:run -Dspring-boot.run.profiles=batch}.
 */
@SpringBootApplication
public class InterestCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterestCalculatorApplication.class, args);
    }

    /**
     * Standalone batch driver (equivalent to submitting the CBACT04C JCL step). Reads the
     * transaction-category-balance seed file and runs the modernized calculation, printing
     * a run summary. Enabled only under the {@code batch} profile so a plain context boot
     * (and the test suite) is unaffected.
     */
    @Bean
    @Profile("batch")
    public CommandLineRunner runInterestBatch(InterestCalculationBatch batch,
                                              TransactionWriter transactionWriter) {
        return args -> {
            List<TranCatBalRecord> balances =
                    FixedWidthFileLoader.loadTranCatBal(new ClassPathResource("seed/tcatbal.txt"));
            String parmDate = LocalDate.now().toString();
            InterestCalculationBatch.Result result = batch.process(parmDate, balances);
            int written = ((InMemoryRepositories.CollectingTransactionWriter) transactionWriter)
                    .getWritten().size();
            System.out.printf(
                    "CBACT04C (Java) complete: records read=%d, interest transactions written=%d%n",
                    result.recordsRead(), written);
        };
    }
}
