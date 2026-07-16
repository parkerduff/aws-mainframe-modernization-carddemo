package com.carddemo.repository;

import com.carddemo.domain.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardXrefRepository extends JpaRepository<CardXref, String> {
}
