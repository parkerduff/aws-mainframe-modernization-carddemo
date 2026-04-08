package com.cardemo.repository;

import com.cardemo.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByAcctId(Long acctId);

    List<Card> findByActiveStatus(String activeStatus);
}
