package com.cardemo.service;

import com.cardemo.entity.Customer;
import com.cardemo.exception.ResourceNotFoundException;
import com.cardemo.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Customer service - provides customer management operations.
 * Data sourced from CUSTDATA VSAM KSDS file (COBOL copybook CVCUS01Y.cpy).
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer getCustomer(Long custId) {
        return customerRepository.findById(custId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + custId));
    }

    public Page<Customer> listCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    public Page<Customer> searchCustomers(String name, Pageable pageable) {
        return customerRepository.findByName(name, pageable);
    }
}
