package com.growthpilot.security;

import com.growthpilot.entity.User;
import com.growthpilot.repository.UserRepository;
import com.growthpilot.entity.Customer;
import com.growthpilot.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public SecurityUtils(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Optional<User> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }

    public Optional<Customer> getCurrentCustomerOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !authentication.getName().startsWith("customer:")) {
            return Optional.empty();
        }
        return customerRepository.findByCustomerId(authentication.getName().substring("customer:".length()));
    }

    public Customer getCurrentCustomer() {
        return getCurrentCustomerOptional().orElseThrow(() -> new RuntimeException("No authenticated customer"));
    }
}
