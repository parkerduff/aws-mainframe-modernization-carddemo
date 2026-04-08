package com.cardemo.repository;

import com.cardemo.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.lastName LIKE %:name% OR c.firstName LIKE %:name%")
    Page<Customer> findByName(@Param("name") String name, Pageable pageable);

    List<Customer> findByAddrStateCd(String stateCd);
}
