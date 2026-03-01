package uy.christian.customers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findById(@NotNull Long id) {
        return customerRepository.findById(id);
    }

    public Customer save(@Valid @NotNull Customer customer) {
        return customerRepository.save(customer);
    }

    public void delete(@NotNull Long id) {
        customerRepository.deleteById(id);
    }
}
