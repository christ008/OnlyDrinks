package uy.christian.customers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('BARMAN')")
    public List<Customer> findAll() {
        log.debug("Listing all customers");
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('BARMAN')")
    public Optional<Customer> findById(@NotNull Long id) {
        log.debug("Looking up customer by id={}", id);
        return customerRepository.findById(id);
    }

    @PreAuthorize("hasRole('BARMAN')")
    public Customer save(@Valid @NotNull Customer customer) {
        Customer saved = customerRepository.save(customer);
        log.info("Customer saved id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    @PreAuthorize("hasRole('BARMAN')")
    public void delete(@NotNull Long id) {
        customerRepository.deleteById(id);
        log.info("Customer deleted id={}", id);
    }
}
