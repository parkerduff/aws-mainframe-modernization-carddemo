package com.aws.carddemo.batch;

import com.aws.carddemo.entity.AccountEntity;
import com.aws.carddemo.entity.CardXrefEntity;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardXrefRepository;
import com.aws.carddemo.util.CobolConversions;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.batch.item.ItemProcessor;

/**
 * Validates each daily transaction, mirroring CBTRN02C 1500-VALIDATE-TRAN:
 * <ol>
 *   <li>1500-A-LOOKUP-XREF: the card number must exist in the cross-reference (else
 *       reason 100 "INVALID CARD NUMBER FOUND").</li>
 *   <li>1500-B-LOOKUP-ACCT: the account must exist (reason 101), must not be overlimit
 *       (reason 102: CREDIT-LIMIT &gt;= CYC-CREDIT - CYC-DEBIT + AMT), and the transaction
 *       must not be received after account expiration (reason 103).</li>
 * </ol>
 */
public class PostTransactionProcessor implements ItemProcessor<DailyTransaction, ProcessedTransaction> {

    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;

    public PostTransactionProcessor(CardXrefRepository cardXrefRepository,
                                    AccountRepository accountRepository) {
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public ProcessedTransaction process(DailyTransaction tran) {
        Optional<CardXrefEntity> xref = cardXrefRepository.findByCardNum(tran.cardNum());
        if (xref.isEmpty()) {
            return ProcessedTransaction.rejected(tran, 100, "INVALID CARD NUMBER FOUND");
        }

        Long acctId = xref.get().getAcctId();
        Optional<AccountEntity> accountOpt = accountRepository.findById(acctId);
        if (accountOpt.isEmpty()) {
            return ProcessedTransaction.rejected(tran, 101, "ACCOUNT RECORD NOT FOUND");
        }

        AccountEntity account = accountOpt.get();
        BigDecimal amount = CobolConversions.money(tran.amount());

        // COBOL: WS-TEMP-BAL = CYC-CREDIT - CYC-DEBIT + AMT; IF CREDIT-LIMIT >= WS-TEMP-BAL OK
        BigDecimal tempBal = CobolConversions.money(account.getCurrCycCredit())
                .subtract(CobolConversions.money(account.getCurrCycDebit()))
                .add(amount);
        if (CobolConversions.money(account.getCreditLimit()).compareTo(tempBal) < 0) {
            return ProcessedTransaction.rejected(tran, 102, "OVERLIMIT TRANSACTION");
        }

        // COBOL: IF ACCT-EXPIRAION-DATE >= DALYTRAN-ORIG-TS(1:10) OK
        if (account.getExpirationDate() != null && tran.origTs() != null
                && tran.origTs().length() >= 10) {
            String tranDate = tran.origTs().substring(0, 10);
            if (account.getExpirationDate().toString().compareTo(tranDate) < 0) {
                return ProcessedTransaction.rejected(tran, 103,
                        "TRANSACTION RECEIVED AFTER ACCT EXPIRATION");
            }
        }

        return ProcessedTransaction.valid(tran, acctId);
    }
}
