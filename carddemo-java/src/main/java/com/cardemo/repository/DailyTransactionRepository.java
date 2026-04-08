package com.cardemo.repository;

import com.cardemo.entity.DailyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyTransactionRepository extends JpaRepository<DailyTransaction, String> {

    List<DailyTransaction> findByCardNum(String cardNum);
}
