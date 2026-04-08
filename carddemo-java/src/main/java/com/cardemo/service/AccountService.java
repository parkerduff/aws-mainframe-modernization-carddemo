package com.cardemo.service;

import com.cardemo.dto.request.AccountUpdateRequest;
import com.cardemo.dto.response.AccountResponse;
import com.cardemo.entity.Account;
import com.cardemo.exception.BusinessException;
import com.cardemo.exception.ResourceNotFoundException;
import com.cardemo.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Account service - migrated from COBOL programs:
 * - COACTVWC.cbl (CAVW transaction) - account view
 * - COACTUPC.cbl (CAUP transaction) - account update with field-level validation
 * Replaces VSAM READ/REWRITE operations on ACCTDATA KSDS file.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse getAccount(Long acctId) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + acctId));
        return AccountResponse.from(account);
    }

    public Page<AccountResponse> listAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(AccountResponse::from);
    }

    @Transactional
    public AccountResponse updateAccount(Long acctId, AccountUpdateRequest request) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + acctId));

        if (request.activeStatus() != null) {
            if (!"Y".equals(request.activeStatus()) && !"N".equals(request.activeStatus())) {
                throw new BusinessException("Active status must be Y or N");
            }
            account.setActiveStatus(request.activeStatus());
        }
        if (request.creditLimit() != null) {
            if (request.creditLimit().compareTo(account.getCurrBal()) < 0) {
                throw new BusinessException("Credit limit cannot be less than current balance");
            }
            account.setCreditLimit(request.creditLimit());
        }
        if (request.cashCreditLimit() != null) {
            account.setCashCreditLimit(request.cashCreditLimit());
        }
        if (request.groupId() != null) {
            account.setGroupId(request.groupId());
        }
        account.setUpdatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);
        log.info("Account updated: {}", acctId);
        return AccountResponse.from(saved);
    }
}
