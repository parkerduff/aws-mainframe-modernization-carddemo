package com.carddemo.repository;

import com.carddemo.domain.TranCatBalance;
import com.carddemo.domain.TranCatBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TranCatBalanceRepository extends JpaRepository<TranCatBalance, TranCatBalanceId> {
}
