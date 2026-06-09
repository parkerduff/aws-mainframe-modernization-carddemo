package com.aws.carddemo.batch;

import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardXrefRepository;
import com.aws.carddemo.repository.TranCatBalRepository;
import com.aws.carddemo.repository.TransactionRepository;
import com.aws.carddemo.util.ZonedDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job replacing {@code app/cbl/CBTRN02C.cbl} and {@code app/jcl/POSTTRAN.jcl}.
 *
 * <p>Reads the fixed-width daily transaction file (CVTRA06Y layout), validates each record
 * against the card cross-reference and account master, posts valid transactions (saving the
 * transaction, updating account balances and category balances) and routes rejected records
 * to a reject file — exactly mirroring the CBTRN02C main loop (DALYTRAN -&gt; XREFFILE -&gt;
 * ACCTFILE -&gt; TRANSACT/ACCTFILE/TCATBALF with rejects to DALYREJS).</p>
 */
@Configuration
public class PostTransactionJobConfig {

    public static final String JOB_NAME = "postTransactionJob";

    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TranCatBalRepository tranCatBalRepository;

    @Value("${carddemo.batch.daily-tran-input:classpath:batch/dailytran-sample.txt}")
    private Resource dailyTranInput;

    @Value("${carddemo.batch.reject-output:target/batch/dailyrejs.txt}")
    private String rejectOutputPath;

    public PostTransactionJobConfig(CardXrefRepository cardXrefRepository,
                                    AccountRepository accountRepository,
                                    TransactionRepository transactionRepository,
                                    TranCatBalRepository tranCatBalRepository) {
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.tranCatBalRepository = tranCatBalRepository;
    }

    @Bean
    public FlatFileItemReader<DailyTransaction> dailyTransactionReader() {
        return new FlatFileItemReaderBuilder<DailyTransaction>()
                .name("dailyTransactionReader")
                .resource(dailyTranInput)
                .strict(false)
                .fixedLength()
                .columns(
                        new Range(1, 16),    // tranId
                        new Range(17, 18),   // typeCd
                        new Range(19, 22),   // catCd
                        new Range(23, 32),   // source
                        new Range(33, 132),  // desc
                        new Range(133, 143), // amount (zoned decimal)
                        new Range(144, 152), // merchantId
                        new Range(153, 202), // merchantName
                        new Range(203, 252), // merchantCity
                        new Range(253, 262), // merchantZip
                        new Range(263, 278), // cardNum
                        new Range(279, 304), // origTs
                        new Range(305, 330)) // procTs
                .names("tranId", "typeCd", "catCd", "source", "desc", "amount", "merchantId",
                        "merchantName", "merchantCity", "merchantZip", "cardNum", "origTs", "procTs")
                .strict(false)
                .fieldSetMapper(fs -> new DailyTransaction(
                        fs.readString("tranId").trim(),
                        fs.readString("typeCd").trim(),
                        parseInt(fs.readString("catCd")),
                        fs.readString("source").trim(),
                        fs.readString("desc").trim(),
                        ZonedDecimal.decode(fs.readString("amount"), 2),
                        parseLong(fs.readString("merchantId")),
                        fs.readString("merchantName").trim(),
                        fs.readString("merchantCity").trim(),
                        fs.readString("merchantZip").trim(),
                        fs.readString("cardNum").trim(),
                        fs.readString("origTs").trim(),
                        fs.readString("procTs").trim()))
                .build();
    }

    @Bean
    public PostTransactionProcessor postTransactionProcessor() {
        return new PostTransactionProcessor(cardXrefRepository, accountRepository);
    }

    @Bean
    public PostingItemWriter postingItemWriter() {
        return new PostingItemWriter(transactionRepository, accountRepository, tranCatBalRepository);
    }

    @Bean
    public FlatFileItemWriter<ProcessedTransaction> rejectItemWriter() {
        Path output = Path.of(rejectOutputPath);
        try {
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create reject output directory", e);
        }
        return new FlatFileItemWriterBuilder<ProcessedTransaction>()
                .name("rejectItemWriter")
                .resource(new FileSystemResource(output))
                .shouldDeleteIfEmpty(true)
                .lineAggregator(p -> String.format("%03d|%s|%s|%s",
                        p.rejectReasonCode(),
                        p.rejectReasonDesc(),
                        p.transaction().tranId(),
                        p.transaction().cardNum()))
                .build();
    }

    @Bean
    public ClassifierCompositeItemWriter<ProcessedTransaction> postTransactionWriter() {
        ItemWriter<ProcessedTransaction> posting = postingItemWriter();
        ItemWriter<ProcessedTransaction> reject = rejectItemWriter();
        ClassifierCompositeItemWriter<ProcessedTransaction> writer = new ClassifierCompositeItemWriter<>();
        writer.setClassifier(item -> item.valid() ? posting : reject);
        return writer;
    }

    @Bean
    public Step postTransactionStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager) {
        return new StepBuilder("postTransactionStep", jobRepository)
                .<DailyTransaction, ProcessedTransaction>chunk(10, transactionManager)
                .reader(dailyTransactionReader())
                .processor(postTransactionProcessor())
                .writer(postTransactionWriter())
                .stream(rejectItemWriter())
                .build();
    }

    @Bean
    public Job postTransactionJob(JobRepository jobRepository, Step postTransactionStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(postTransactionStep)
                .build();
    }

    private static Integer parseInt(String s) {
        String t = s == null ? "" : s.trim();
        return t.isEmpty() ? 0 : Integer.parseInt(t);
    }

    private static Long parseLong(String s) {
        String t = s == null ? "" : s.trim();
        return t.isEmpty() ? 0L : Long.parseLong(t);
    }
}
