package com.carddemo.accountview.repository;

import com.carddemo.accountview.model.Customer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simple map-backed {@link CustomerRepository}, keyed by customer id, standing in
 * for the VSAM CUSTDAT read.
 */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> byCustomerId = new LinkedHashMap<>();

    public void save(Customer customer) {
        byCustomerId.put(customer.customerId(), customer);
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(byCustomerId.get(customerId));
    }
}
