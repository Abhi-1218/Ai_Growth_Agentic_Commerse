package com.growthpilot.controller;

import com.growthpilot.dto.CustomerIntelligenceDto;
import com.growthpilot.entity.Customer;
import com.growthpilot.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer Intelligence", description = "Customer data, RFM analysis, purchase intent signals, and churn risk evaluation")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Get paginated list of customers with search and filters")
    public ResponseEntity<Page<Customer>> getAllCustomers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String churnRisk,
            Pageable pageable) {
        return ResponseEntity.ok(customerService.getAllCustomers(query, segment, churnRisk, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get basic customer entity by ID")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/{id}/intelligence")
    @Operation(summary = "Get full Customer Intelligence view with intent factors, churn evaluation, recommendations, and recent orders")
    public ResponseEntity<CustomerIntelligenceDto> getCustomerIntelligence(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerIntelligence(id));
    }

    @GetMapping("/high-intent")
    @Operation(summary = "Get top high-intent customers ready for conversion")
    public ResponseEntity<List<Customer>> getHighIntentCustomers() {
        return ResponseEntity.ok(customerService.getHighIntentCustomers());
    }

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(customer));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer details")
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.updateCustomer(id, customer));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
