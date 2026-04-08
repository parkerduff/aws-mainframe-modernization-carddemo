package com.cardemo.repository;

import com.cardemo.entity.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    List<CardXref> findByCustId(Long custId);

    List<CardXref> findByAcctId(Long acctId);
}
