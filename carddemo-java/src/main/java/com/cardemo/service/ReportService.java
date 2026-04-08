package com.cardemo.service;

import com.cardemo.entity.Account;
import com.cardemo.entity.Card;
import com.cardemo.entity.Transaction;
import com.cardemo.exception.ResourceNotFoundException;
import com.cardemo.repository.AccountRepository;
import com.cardemo.repository.CardRepository;
import com.cardemo.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report service - migrated from COBOL programs:
 * - CORPT00C.cbl (CR00 transaction) - report generation
 * - CBTRN03C.cbl (batch) - transaction detail report
 * Generates transaction reports and account summaries.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;

    public ReportService(TransactionRepository transactionRepository,
                         AccountRepository accountRepository,
                         CardRepository cardRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
    }

    public Map<String, Object> generateTransactionReport(Long acctId, LocalDateTime startDate, LocalDateTime endDate) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + acctId));

        List<Card> cards = cardRepository.findByAcctId(acctId);
        List<String> cardNums = cards.stream().map(Card::getCardNum).toList();

        List<Transaction> transactions = cardNums.stream()
                .flatMap(cn -> transactionRepository.findByCardNumAndOrigTsBetween(cn, startDate, endDate).stream())
                .toList();

        Map<String, Object> report = new HashMap<>();
        report.put("accountId", acctId);
        report.put("accountStatus", account.getActiveStatus());
        report.put("currentBalance", account.getCurrBal());
        report.put("creditLimit", account.getCreditLimit());
        report.put("periodStart", startDate);
        report.put("periodEnd", endDate);
        report.put("totalTransactions", transactions.size());
        report.put("transactions", transactions);
        report.put("generatedAt", LocalDateTime.now());

        log.info("Transaction report generated for account {} ({} transactions)", acctId, transactions.size());
        return report;
    }
}
