package com.festdescans.api.application.port.out;

import com.festdescans.api.domain.order.Customer;
import java.util.Optional;

public interface CustomerRepository {

    Optional<Customer> findByEmail(String email);

    Customer save(Customer customer);
}
