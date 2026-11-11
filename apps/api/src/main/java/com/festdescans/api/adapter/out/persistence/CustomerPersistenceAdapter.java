package com.festdescans.api.adapter.out.persistence;

import com.festdescans.api.adapter.out.persistence.entity.CustomerEntity;
import com.festdescans.api.adapter.out.persistence.jpa.CustomerJpaRepository;
import com.festdescans.api.application.port.out.CustomerRepository;
import com.festdescans.api.domain.order.Customer;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
class CustomerPersistenceAdapter implements CustomerRepository {

    private final CustomerJpaRepository customers;

    CustomerPersistenceAdapter(CustomerJpaRepository customers) {
        this.customers = customers;
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return customers.findByEmail(email).map(EntityMapper::customer);
    }

    @Override
    @Transactional
    public Customer save(Customer customer) {
        CustomerEntity entity = customers.findById(customer.id()).orElseGet(() -> {
            CustomerEntity created = new CustomerEntity(customer.id());
            created.setCreatedAt(EntityMapper.utc(customer.createdAt()));
            return created;
        });
        entity.setName(customer.name());
        entity.setEmail(customer.email());
        entity.setPhone(customer.phone());
        entity.setUpdatedAt(EntityMapper.utc(customer.updatedAt()));
        return EntityMapper.customer(customers.saveAndFlush(entity));
    }
}
