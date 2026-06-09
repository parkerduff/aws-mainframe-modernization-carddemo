package com.aws.carddemo.service;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.dto.AccountReportDto;
import com.aws.carddemo.dto.TransactionDto;
import com.aws.carddemo.dto.TransactionReportDto;
import com.aws.carddemo.entity.AccountEntity;
import com.aws.carddemo.entity.TransactionEntity;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.TransactionRepository;
import com.aws.carddemo.util.CobolConversions;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Reporting service, replacing {@code app/cbl/CORPT00C.cbl} (transaction reporting).
 *
 * <p>CORPT00C drives the transaction-report batch (TRANREPT) producing a printed report of
 * transactions over a date range. Here it is exposed as on-demand report aggregations:
 * a transaction report for a card (with an optional timestamp range) and an account
 * summary report.</p>
 */
@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public ReportService(TransactionRepository transactionRepository,
                         AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public TransactionReportDto generateTransactionReport(String cardNum, String fromTs, String toTs) {
        List<TransactionEntity> transactions =
                transactionRepository.findByTranCardNumOrderByTranOrigTsAsc(cardNum).stream()
                        .filter(t -> !StringUtils.hasText(fromTs)
                                || (t.getTranOrigTs() != null && t.getTranOrigTs().compareTo(fromTs) >= 0))
                        .filter(t -> !StringUtils.hasText(toTs)
                                || (t.getTranOrigTs() != null && t.getTranOrigTs().compareTo(toTs) <= 0))
                        .toList();

        BigDecimal total = transactions.stream()
                .map(t -> CobolConversions.money(t.getTranAmt()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TransactionDto> dtos = transactions.stream().map(TransactionDto::fromEntity).toList();
        return new TransactionReportDto(cardNum, fromTs, toTs, dtos.size(),
                CobolConversions.money(total), dtos);
    }

    @Transactional(readOnly = true)
    public AccountReportDto generateAccountReport(String activeStatus) {
        List<AccountEntity> accounts = StringUtils.hasText(activeStatus)
                ? accountRepository.findByActiveStatus(activeStatus)
                : accountRepository.findAll();

        BigDecimal totalBal = accounts.stream()
                .map(a -> CobolConversions.money(a.getCurrBal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLimit = accounts.stream()
                .map(a -> CobolConversions.money(a.getCreditLimit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AccountDto> dtos = accounts.stream().map(AccountDto::fromEntity).toList();
        return new AccountReportDto(dtos.size(), CobolConversions.money(totalBal),
                CobolConversions.money(totalLimit), dtos);
    }
}
