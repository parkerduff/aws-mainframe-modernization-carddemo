package com.cardemo.controller;

import com.cardemo.entity.Customer;
import com.cardemo.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customers", description = "Customer management")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "List customers", description = "Paginated customer listing")
    public ResponseEntity<Page<Customer>> listCustomers(Pageable pageable) {
        return ResponseEntity.ok(customerService.listCustomers(pageable));
    }

    @GetMapping("/{custId}")
    @Operation(summary = "View customer", description = "Customer details")
    public ResponseEntity<Customer> getCustomer(@PathVariable Long custId) {
        return ResponseEntity.ok(customerService.getCustomer(custId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers", description = "Search by name")
    public ResponseEntity<Page<Customer>> searchCustomers(@RequestParam String name, Pageable pageable) {
        return ResponseEntity.ok(customerService.searchCustomers(name, pageable));
    }
}
