package com.aws.carddemo.service;

import com.aws.carddemo.dto.AccountDto;
import com.aws.carddemo.dto.AccountUpdateDto;
import com.aws.carddemo.entity.AccountEntity;
import com.aws.carddemo.entity.CardXrefEntity;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.CardXrefRepository;
import com.aws.carddemo.util.CobolConversions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account service, replacing {@code app/cbl/COACTVWC.cbl} (account view) and
 * {@code app/cbl/COACTUPC.cbl} (account update).
 *
 * <p>COACTVWC resolves a card number into an account by reading the card cross-reference
 * (XREF) to obtain the account id, then reads ACCTFILE. {@link #getAccountByCard(String)}
 * replicates that lookup chain. {@link #getAccount(long)} mirrors a direct account read by
 * the entered account id (after the COBOL numeric-edit validation of CC-ACCT-ID).</p>
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;

    public AccountService(AccountRepository accountRepository,
                          CardXrefRepository cardXrefRepository) {
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    @Transactional(readOnly = true)
    public AccountDto getAccount(long acctId) {
        return AccountDto.fromEntity(findAccount(acctId));
    }

    /**
     * Account view via the card cross-reference chain (card number -&gt; XREF -&gt; account id
     * -&gt; account), mirroring COACTVWC's use of the XREF file.
     */
    @Transactional(readOnly = true)
    public AccountDto getAccountByCard(String cardNum) {
        CardXrefEntity xref = cardXrefRepository.findByCardNum(cardNum)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Card number not found in cross-reference: " + cardNum));
        return AccountDto.fromEntity(findAccount(xref.getAcctId()));
    }

    @Transactional
    public AccountDto updateAccount(long acctId, AccountUpdateDto dto) {
        AccountEntity account = findAccount(acctId);

        // COBOL COACTUPC: only overlay fields the operator changed (non-null here).
        if (dto.activeStatus() != null) {
            account.setActiveStatus(dto.activeStatus());
        }
        if (dto.currBal() != null) {
            account.setCurrBal(CobolConversions.money(dto.currBal()));
        }
        if (dto.creditLimit() != null) {
            account.setCreditLimit(CobolConversions.money(dto.creditLimit()));
        }
        if (dto.cashCreditLimit() != null) {
            account.setCashCreditLimit(CobolConversions.money(dto.cashCreditLimit()));
        }
        if (dto.expirationDate() != null) {
            account.setExpirationDate(dto.expirationDate());
        }
        if (dto.reissueDate() != null) {
            account.setReissueDate(dto.reissueDate());
        }
        if (dto.currCycCredit() != null) {
            account.setCurrCycCredit(CobolConversions.money(dto.currCycCredit()));
        }
        if (dto.currCycDebit() != null) {
            account.setCurrCycDebit(CobolConversions.money(dto.currCycDebit()));
        }
        if (dto.addrZip() != null) {
            account.setAddrZip(dto.addrZip());
        }
        if (dto.groupId() != null) {
            account.setGroupId(dto.groupId());
        }

        return AccountDto.fromEntity(accountRepository.save(account));
    }

    private AccountEntity findAccount(long acctId) {
        return accountRepository.findById(acctId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + CobolConversions.numberToPicX(acctId, 11)));
    }
}
