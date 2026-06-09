package com.aws.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.aws.carddemo.repository.TransactionRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end test of the post-transaction batch (CBTRN02C + POSTTRAN.jcl) against the
 * bundled sample daily-transaction file: three valid records post, one bad card rejects.
 */
@SpringBootTest
@ActiveProfiles("test")
class PostTransactionJobTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("postTransactionJob")
    private Job postTransactionJob;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void postsValidTransactionsAndRejectsInvalidCard() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(postTransactionJob, params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(transactionRepository.count()).isEqualTo(3);

        Path rejectFile = Path.of("target/batch/dailyrejs.txt");
        assertThat(rejectFile).exists();
        List<String> rejects = Files.readAllLines(rejectFile);
        assertThat(rejects).hasSize(1);
        assertThat(rejects.get(0)).contains("9999999999999999");
    }
}
