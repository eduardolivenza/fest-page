package com.festdescans.api.application.port.out;

import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.util.Optional;

public interface ContactRepository {

    /** Newest first; {@code read} null means all. */
    Page<ContactRequest> findPage(Boolean read, PageRequest page);

    Optional<ContactRequest> findById(String id);

    ContactRequest save(ContactRequest contact);

    /** @return false if there was no such request */
    boolean deleteById(String id);
}
