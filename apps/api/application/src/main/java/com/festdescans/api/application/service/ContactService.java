package com.festdescans.api.application.service;

import com.festdescans.api.application.port.in.CaptchaFailedException;
import com.festdescans.api.application.port.in.ManageContactsUseCase;
import com.festdescans.api.application.port.in.SubmitContactUseCase;
import com.festdescans.api.application.port.out.CaptchaVerifier;
import com.festdescans.api.application.port.out.ContactRepository;
import com.festdescans.api.application.port.out.IdGenerator;
import com.festdescans.api.domain.contact.ContactMessage;
import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.shared.NotFoundException;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.time.Clock;

public final class ContactService implements SubmitContactUseCase, ManageContactsUseCase {

    private final ContactRepository contacts;
    private final CaptchaVerifier captcha;
    private final IdGenerator ids;
    private final Clock clock;

    public ContactService(ContactRepository contacts, CaptchaVerifier captcha, IdGenerator ids, Clock clock) {
        this.contacts = contacts;
        this.captcha = captcha;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public ContactRequest submitContact(ContactMessage message, String captchaToken, String visitorIp) {
        if (!captcha.verify(captchaToken, visitorIp)) {
            throw new CaptchaFailedException();
        }
        // The token is only used for the check above: it is never stored.
        return contacts.save(ContactRequest.receive(ids.newId(), message, Timestamps.now(clock)));
    }

    @Override
    public Page<ContactRequest> listContacts(Boolean read, PageRequest page) {
        return contacts.findPage(read, page);
    }

    @Override
    public ContactRequest markContactRead(String id) {
        ContactRequest contact = contacts.findById(id).orElseThrow(() -> new NotFoundException("Contact", id));
        return contacts.save(contact.markRead());
    }

    @Override
    public void deleteContact(String id) {
        if (!contacts.deleteById(id)) {
            throw new NotFoundException("Contact", id);
        }
    }
}
