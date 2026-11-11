package com.festdescans.api.adapter.in.web.contact;

import com.festdescans.api.adapter.in.web.common.Validation;
import com.festdescans.api.domain.contact.ContactMessage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contact form. {@code turnstileToken} is only used for the anti-bot check, never stored. */
public record CreateContactRequest(
        @NotNull @Size(min = 2, max = 100) String name,
        @NotNull @Pattern(regexp = Validation.EMAIL) String email,
        String phone,
        @NotNull @Size(min = 10, max = 2000) String message,
        @Size(max = 2048) String turnstileToken) {

    ContactMessage toDomain() {
        return new ContactMessage(name, email, phone, message);
    }
}
