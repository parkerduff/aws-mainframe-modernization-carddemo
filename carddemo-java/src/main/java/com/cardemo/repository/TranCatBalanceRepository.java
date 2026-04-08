package com.cardemo.repository;

import com.cardemo.entity.TranCatBalance;
import com.cardemo.entity.TranCatBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranCatBalanceRepository extends JpaRepository<TranCatBalance, TranCatBalanceId> {

    List<TranCatBalance> findByAcctId(Long acctId);
}
