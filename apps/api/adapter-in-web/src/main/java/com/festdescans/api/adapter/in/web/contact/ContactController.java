package com.festdescans.api.adapter.in.web.contact;

import com.festdescans.api.application.port.in.SubmitContactUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
class ContactController {

    private final SubmitContactUseCase contacts;

    ContactController(SubmitContactUseCase contacts) {
        this.contacts = contacts;
    }

    /**
     * Turnstile-protected. Behind the Cloudflare Tunnel the remote address is cloudflared: the visitor IP
     * comes in CF-Connecting-IP.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ContactResponse create(@RequestBody @Valid CreateContactRequest body,
            @RequestHeader(name = "CF-Connecting-IP", required = false) String visitorIp) {
        return ContactResponse.from(contacts.submitContact(body.toDomain(), body.turnstileToken(), visitorIp));
    }
}
