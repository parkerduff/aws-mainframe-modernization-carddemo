package com.cardemo.repository;

import com.cardemo.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Page<Account> findByActiveStatus(String activeStatus, Pageable pageable);

    List<Account> findAllByActiveStatus(String activeStatus);
}
