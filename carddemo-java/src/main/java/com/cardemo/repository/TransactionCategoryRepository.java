package com.cardemo.repository;

import com.cardemo.entity.TransactionCategory;
import com.cardemo.entity.TransactionCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, TransactionCategoryId> {

    List<TransactionCategory> findByTypeCd(String typeCd);
}
