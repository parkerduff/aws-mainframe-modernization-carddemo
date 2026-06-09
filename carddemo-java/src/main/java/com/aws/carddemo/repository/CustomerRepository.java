package com.aws.carddemo.repository;

import com.aws.carddemo.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link CustomerEntity}, replacing access to the CUSTFILE KSDS.
 * The primary key already is the customer id, so {@code findById} serves as findByCustId.
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
}
