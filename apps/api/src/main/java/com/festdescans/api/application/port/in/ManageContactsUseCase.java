package com.festdescans.api.application.port.in;

import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;

/** Back-office inbox of contact requests. */
public interface ManageContactsUseCase {

    /** Newest first. {@code read} null means all. */
    Page<ContactRequest> listContacts(Boolean read, PageRequest page);

    /** @throws com.festdescans.api.domain.shared.NotFoundException if there is no such request */
    ContactRequest markContactRead(String id);

    /** @throws com.festdescans.api.domain.shared.NotFoundException if there is no such request */
    void deleteContact(String id);
}
