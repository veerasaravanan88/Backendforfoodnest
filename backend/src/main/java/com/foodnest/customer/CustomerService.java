package com.foodnest.customer;

import com.foodnest.common.BadRequestException;
import com.foodnest.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerProfileDto getProfile(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        return new CustomerProfileDto(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getCreatedAt()
        );
    }

    @Transactional
    public CustomerProfileDto updateProfile(Long customerId, UpdateCustomerRequest req) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        customerRepository.findByPhone(req.getPhone()).ifPresent(existing -> {
            if (!existing.getId().equals(customerId)) {
                throw new BadRequestException("Phone number " + req.getPhone() + " is already associated with another account.");
            }
        });

        customer.setFullName(req.getFullName());
        customer.setPhone(req.getPhone());
        customer.setAddress(req.getAddress());

        Customer saved = customerRepository.save(customer);
        return new CustomerProfileDto(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getAddress(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public Customer getOrCreateCustomerByPrincipal(com.foodnest.security.UserPrincipal principal, String defaultAddress) {
        if (principal.getRole() == com.foodnest.security.UserRole.CUSTOMER) {
            return customerRepository.findById(principal.getId())
                    .orElseGet(() -> customerRepository.findByEmail(principal.getUsername())
                            .orElseGet(() -> createCustomerFromPrincipal(principal, defaultAddress)));
        }
        return customerRepository.findByEmail(principal.getUsername())
                .orElseGet(() -> createCustomerFromPrincipal(principal, defaultAddress));
    }

    private Customer createCustomerFromPrincipal(com.foodnest.security.UserPrincipal principal, String defaultAddress) {
        Customer c = new Customer();
        c.setEmail(principal.getUsername());
        String name = principal.getFullName();
        if (name == null || name.isBlank()) {
            name = principal.getUsername().split("@")[0];
        }
        c.setFullName(name);
        c.setPhone("9" + String.format("%09d", Math.abs(principal.getUsername().hashCode()) % 1000000000L));
        c.setPasswordHash(principal.getPassword() != null && !principal.getPassword().isBlank() ? principal.getPassword() : "AUTO_PROVISIONED");
        c.setAddress(defaultAddress != null ? defaultAddress : "");
        return customerRepository.save(c);
    }

    @Transactional
    public CustomerProfileDto getProfileByPrincipal(com.foodnest.security.UserPrincipal principal) {
        Customer customer = getOrCreateCustomerByPrincipal(principal, "");
        return new CustomerProfileDto(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getCreatedAt()
        );
    }

    @Transactional
    public CustomerProfileDto updateProfileByPrincipal(com.foodnest.security.UserPrincipal principal, UpdateCustomerRequest req) {
        Customer customer = getOrCreateCustomerByPrincipal(principal, req.getAddress());
        return updateProfile(customer.getId(), req);
    }
}
