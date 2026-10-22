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
    public Customer getCustomerByPrincipal(com.foodnest.security.UserPrincipal principal) {
        if (principal == null || principal.getRole() != com.foodnest.security.UserRole.CUSTOMER) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Customer account required.");
        }
        return customerRepository.findById(principal.getId())
                .orElseGet(() -> customerRepository.findByEmail(principal.getUsername())
                        .orElseThrow(() -> new ResourceNotFoundException("Customer account not found for: " + principal.getUsername())));
    }

    @Transactional
    public CustomerProfileDto getProfileByPrincipal(com.foodnest.security.UserPrincipal principal) {
        Customer customer = getCustomerByPrincipal(principal);
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
        Customer customer = getCustomerByPrincipal(principal);
        return updateProfile(customer.getId(), req);
    }
}
