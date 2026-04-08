package com.cardemo.repository;

import com.cardemo.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByCardNum(String cardNum, Pageable pageable);

    List<Transaction> findByCardNumAndOrigTsBetween(String cardNum, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Transaction t WHERE t.cardNum IN :cardNums ORDER BY t.origTs DESC")
    Page<Transaction> findByCardNumIn(@Param("cardNums") List<String> cardNums, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.cardNum IN " +
           "(SELECT c.cardNum FROM Card c WHERE c.acctId = :acctId) ORDER BY t.origTs DESC")
    Page<Transaction> findByAccountId(@Param("acctId") Long acctId, Pageable pageable);
}
