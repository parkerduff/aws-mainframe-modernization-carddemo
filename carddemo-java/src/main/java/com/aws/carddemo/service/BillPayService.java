package com.aws.carddemo.service;

import com.aws.carddemo.dto.BillPayDto;
import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.entity.AccountEntity;
import com.aws.carddemo.entity.CardXrefEntity;
import com.aws.carddemo.entity.TransactionEntity;
import com.aws.carddemo.exception.BusinessRuleException;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardXrefRepository;
import com.aws.carddemo.repository.TransactionRepository;
import com.aws.carddemo.util.CobolConversions;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bill payment service, replacing {@code app/cbl/COBIL00C.cbl} (online bill pay).
 *
 * <p>COBIL00C reads the account, pays the current balance (or a specified amount), writes a
 * payment TRAN-RECORD and updates ACCTFILE so the balance is reduced. This service mirrors
 * that: it records a payment transaction (transaction type {@code 02} = Payment) with a
 * negative amount and decrements the account balance accordingly.</p>
 */
@Service
public class BillPayService {

    private static final String PAYMENT_TYPE_CD = "02";
    private static final int PAYMENT_CAT_CD = 1;
    private static final String PAYMENT_SOURCE = "BILLPAY";
    private static final DateTimeFormatter COBOL_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final TransactionRepository transactionRepository;

    public BillPayService(AccountRepository accountRepository,
                          CardXrefRepository cardXrefRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionDto processPayment(BillPayDto dto) {
        AccountEntity account = accountRepository.findById(dto.acctId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + dto.acctId()));

        BigDecimal currentBalance = CobolConversions.money(account.getCurrBal());

        // COBIL00C: PB-PAY-AMT defaults to the full current balance.
        BigDecimal paymentAmount = dto.payFullBalance() || dto.amount() == null
                ? currentBalance
                : CobolConversions.money(dto.amount());

        if (paymentAmount.signum() <= 0) {
            throw new BusinessRuleException("You have nothing to pay...");
        }

        List<CardXrefEntity> xrefs = cardXrefRepository.findByAcctId(dto.acctId());
        if (xrefs.isEmpty()) {
            throw new BusinessRuleException("No card found for account: " + dto.acctId());
        }
        String cardNum = xrefs.get(0).getCardNum();

        // Payment is a credit to the account: balance and current-cycle credit decrease.
        BigDecimal signedAmount = paymentAmount.negate();
        account.setCurrBal(currentBalance.add(signedAmount));
        account.setCurrCycCredit(CobolConversions.money(account.getCurrCycCredit()).add(signedAmount));
        accountRepository.save(account);

        TransactionEntity tran = new TransactionEntity();
        tran.setTranId(String.format("%016d", System.currentTimeMillis()));
        tran.setTranTypeCd(PAYMENT_TYPE_CD);
        tran.setTranCatCd(PAYMENT_CAT_CD);
        tran.setTranSource(PAYMENT_SOURCE);
        tran.setTranDesc("BILL PAYMENT - ONLINE");
        tran.setTranAmt(signedAmount);
        tran.setTranCardNum(cardNum);
        tran.setTranOrigTs(LocalDateTime.now().format(COBOL_TS));
        tran.setTranProcTs(LocalDateTime.now().format(COBOL_TS));

        return TransactionDto.fromEntity(transactionRepository.save(tran));
    }
}
