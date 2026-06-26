package com.carddemo.accountview.repository;

import com.carddemo.accountview.model.Customer;

import java.util.Optional;

/**
 * Read access path for the customer master file.
 *
 * <p>Legacy equivalent: CICS READ of dataset {@code CUSTDAT} keyed by customer id
 * in paragraph {@code 9400-GETCUSTDATA-BYCUST} of COACTVWC.
 */
public interface CustomerRepository {

    /**
     * @param customerId 9-digit customer id
     * @return the matching customer, or empty when the read returns NOTFND
     */
    Optional<Customer> findById(String customerId);
}
