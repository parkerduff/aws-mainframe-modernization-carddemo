package com.carddemo.dataload;

import com.carddemo.repository.AccountRepository;
import com.carddemo.repository.CardXrefRepository;
import com.carddemo.repository.DailyTransactionRepository;
import com.carddemo.repository.TranCatBalanceRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time data-conversion utility that loads the fixed-width ASCII sample files
 * from {@code app/data/ASCII/} into the relational schema, preserving exact
 * numeric precision (amounts as scale-2 {@link java.math.BigDecimal}).
 * <p>
 * Runs automatically at startup only when {@code carddemo.dataload.enabled=true};
 * otherwise the {@link #load(Path)} method can be invoked directly (e.g. from a
 * test or another job). Field positions are derived from the copybook PIC widths
 * via {@link RecordParsers}.
 */
@Component
@ConditionalOnProperty(name = "carddemo.dataload.enabled", havingValue = "true")
public class FixedWidthLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FixedWidthLoader.class);

    static final String ACCOUNT_FILE = "acctdata.txt";
    static final String CARD_XREF_FILE = "cardxref.txt";
    static final String TRAN_CAT_BAL_FILE = "tcatbal.txt";
    static final String DAILY_TRANSACTION_FILE = "dailytran.txt";

    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final TranCatBalanceRepository tranCatBalanceRepository;
    private final DailyTransactionRepository dailyTransactionRepository;

    @Value("${carddemo.dataload.path}")
    private String dataPath;

    public FixedWidthLoader(AccountRepository accountRepository,
                            CardXrefRepository cardXrefRepository,
                            TranCatBalanceRepository tranCatBalanceRepository,
                            DailyTransactionRepository dailyTransactionRepository) {
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.tranCatBalanceRepository = tranCatBalanceRepository;
        this.dailyTransactionRepository = dailyTransactionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path dir = Path.of(dataPath);
        if (!Files.isDirectory(dir)) {
            log.warn("Data load skipped: directory '{}' does not exist (carddemo.dataload.path)", dir.toAbsolutePath());
            return;
        }
        LoadResult result = load(dir);
        log.info("Fixed-width data load complete: {}", result);
    }

    /**
     * Loads all four sample files from {@code asciiDir} into the database and
     * returns a summary of the row counts inserted.
     */
    @Transactional
    public LoadResult load(Path asciiDir) {
        long accounts = accountRepository.saveAll(
                parse(asciiDir.resolve(ACCOUNT_FILE), RecordParsers::parseAccount)).size();
        long cardXrefs = cardXrefRepository.saveAll(
                parse(asciiDir.resolve(CARD_XREF_FILE), RecordParsers::parseCardXref)).size();
        long tranCatBalances = tranCatBalanceRepository.saveAll(
                parse(asciiDir.resolve(TRAN_CAT_BAL_FILE), RecordParsers::parseTranCatBalance)).size();
        long dailyTransactions = dailyTransactionRepository.saveAll(
                parse(asciiDir.resolve(DAILY_TRANSACTION_FILE), RecordParsers::parseDailyTransaction)).size();
        return new LoadResult(accounts, cardXrefs, tranCatBalances, dailyTransactions);
    }

    private <T> List<T> parse(Path file, Function<String, T> parser) {
        List<T> parsed = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.ISO_8859_1)) {
                if (stripLineEnding(line).isEmpty()) {
                    continue;
                }
                parsed.add(parser.apply(line));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read fixed-width file: " + file, e);
        }
        return parsed;
    }

    private static String stripLineEnding(String s) {
        return s.replace("\r", "").replace("\n", "");
    }

    /** Summary of the rows inserted by a {@link #load(Path)} invocation. */
    public record LoadResult(long accounts, long cardXrefs, long tranCatBalances, long dailyTransactions) {
    }
}
