package com.festdescans.api.adapter.out.persistence;

import com.festdescans.api.adapter.out.persistence.entity.ContactRequestEntity;
import com.festdescans.api.adapter.out.persistence.jpa.ContactRequestJpaRepository;
import com.festdescans.api.application.port.out.ContactRepository;
import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
class ContactPersistenceAdapter implements ContactRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"));

    private final ContactRequestJpaRepository contacts;

    ContactPersistenceAdapter(ContactRequestJpaRepository contacts) {
        this.contacts = contacts;
    }

    @Override
    public Page<ContactRequest> findPage(Boolean read, PageRequest page) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page.page() - 1, page.pageSize(),
                NEWEST_FIRST);
        var result = read == null ? contacts.findAll(pageable) : contacts.findByRead(read, pageable);
        return EntityMapper.page(result, page.page(), EntityMapper::contact);
    }

    @Override
    public Optional<ContactRequest> findById(String id) {
        return contacts.findById(id).map(EntityMapper::contact);
    }

    @Override
    @Transactional
    public ContactRequest save(ContactRequest contact) {
        ContactRequestEntity entity = contacts.findById(contact.id())
                .orElseGet(() -> new ContactRequestEntity(contact.id()));
        entity.setName(contact.name());
        entity.setEmail(contact.email());
        entity.setPhone(contact.phone());
        entity.setMessage(contact.message());
        entity.setRead(contact.read());
        entity.setCreatedAt(EntityMapper.utc(contact.createdAt()));
        return EntityMapper.contact(contacts.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public boolean deleteById(String id) {
        if (!contacts.existsById(id)) {
            return false;
        }
        contacts.deleteById(id);
        return true;
    }
}
