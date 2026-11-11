package com.festdescans.api.application.port.in;

import com.festdescans.api.domain.contact.ContactMessage;
import com.festdescans.api.domain.contact.ContactRequest;

/** Public contact form. */
public interface SubmitContactUseCase {

    /**
     * Stores the message after the anti-bot check.
     *
     * @param captchaToken Turnstile token from the form, may be null
     * @param visitorIp    the visitor's IP (not the proxy's), may be null
     * @throws CaptchaFailedException when the check fails
     */
    ContactRequest submitContact(ContactMessage message, String captchaToken, String visitorIp);
}
