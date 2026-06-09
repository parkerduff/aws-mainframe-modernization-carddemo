package com.aws.carddemo.batch;

import com.aws.carddemo.entity.AccountEntity;
import com.aws.carddemo.entity.TranCatBalEntity;
import com.aws.carddemo.entity.TranCatBalId;
import com.aws.carddemo.entity.TransactionEntity;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.TranCatBalRepository;
import com.aws.carddemo.repository.TransactionRepository;
import com.aws.carddemo.util.CobolConversions;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * Persists valid postings, mirroring CBTRN02C 2000-POST-TRANSACTION:
 * <ul>
 *   <li>2900-WRITE-TRANSACTION-FILE: write the TRAN-RECORD;</li>
 *   <li>2800-UPDATE-ACCOUNT-REC: add the amount to the balance, and to current-cycle
 *       credit (amount &gt;= 0) or debit (amount &lt; 0);</li>
 *   <li>2700-UPDATE-TCATBAL: accumulate the amount into the (account, type, category)
 *       balance.</li>
 * </ul>
 */
public class PostingItemWriter implements ItemWriter<ProcessedTransaction> {

    private static final DateTimeFormatter COBOL_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TranCatBalRepository tranCatBalRepository;

    public PostingItemWriter(TransactionRepository transactionRepository,
                             AccountRepository accountRepository,
                             TranCatBalRepository tranCatBalRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.tranCatBalRepository = tranCatBalRepository;
    }

    @Override
    public void write(Chunk<? extends ProcessedTransaction> chunk) {
        for (ProcessedTransaction processed : chunk) {
            DailyTransaction t = processed.transaction();
            BigDecimal amount = CobolConversions.money(t.amount());

            writeTransaction(t, amount);
            updateAccount(processed.acctId(), amount);
            updateTranCatBalance(processed.acctId(), t, amount);
        }
    }

    private void writeTransaction(DailyTransaction t, BigDecimal amount) {
        TransactionEntity tran = new TransactionEntity();
        tran.setTranId(t.tranId());
        tran.setTranTypeCd(t.typeCd());
        tran.setTranCatCd(t.catCd());
        tran.setTranSource(t.source());
        tran.setTranDesc(t.desc());
        tran.setTranAmt(amount);
        tran.setTranMerchantId(t.merchantId());
        tran.setTranMerchantName(t.merchantName());
        tran.setTranMerchantCity(t.merchantCity());
        tran.setTranMerchantZip(t.merchantZip());
        tran.setTranCardNum(t.cardNum());
        tran.setTranOrigTs(t.origTs());
        tran.setTranProcTs(LocalDateTime.now().format(COBOL_TS));
        transactionRepository.save(tran);
    }

    private void updateAccount(Long acctId, BigDecimal amount) {
        AccountEntity account = accountRepository.findById(acctId).orElseThrow();
        account.setCurrBal(CobolConversions.money(account.getCurrBal()).add(amount));
        if (amount.signum() >= 0) {
            account.setCurrCycCredit(CobolConversions.money(account.getCurrCycCredit()).add(amount));
        } else {
            account.setCurrCycDebit(CobolConversions.money(account.getCurrCycDebit()).add(amount));
        }
        accountRepository.save(account);
    }

    private void updateTranCatBalance(Long acctId, DailyTransaction t, BigDecimal amount) {
        TranCatBalId id = new TranCatBalId(acctId, t.typeCd(), t.catCd());
        TranCatBalEntity bal = tranCatBalRepository.findById(id)
                .orElseGet(() -> new TranCatBalEntity(id, BigDecimal.ZERO.setScale(2)));
        bal.setBalance(CobolConversions.money(bal.getBalance()).add(amount));
        tranCatBalRepository.save(bal);
    }
}
