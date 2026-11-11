package com.festdescans.api.application.service;

import static com.festdescans.api.application.fakes.Fixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festdescans.api.application.fakes.Fixtures;
import com.festdescans.api.application.fakes.InMemoryDatabase;
import com.festdescans.api.application.fakes.MutableClock;
import com.festdescans.api.application.fakes.RecordingCaptchaVerifier;
import com.festdescans.api.application.fakes.SequentialIdGenerator;
import com.festdescans.api.application.port.in.CaptchaFailedException;
import com.festdescans.api.domain.contact.ContactMessage;
import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.shared.NotFoundException;
import com.festdescans.api.domain.shared.PageRequest;
import org.junit.jupiter.api.Test;

class ContactServiceTest {

    private final InMemoryDatabase db = new InMemoryDatabase();
    private final RecordingCaptchaVerifier captcha = new RecordingCaptchaVerifier();
    private final ContactService service =
            new ContactService(db.contactRepository, captcha, new SequentialIdGenerator(), new MutableClock(T0));

    private final ContactMessage message =
            new ContactMessage("Maria", "maria@example.com", null, "Voldria informació sobre matalassos.");

    @Test
    void storesAMessageThatPassesTheCaptcha() {
        ContactRequest saved = service.submitContact(message, "tok", "203.0.113.7");

        assertThat(saved.read()).isFalse();
        assertThat(db.contacts).containsKey(saved.id());
        assertThat(captcha.checks()).containsExactly(new RecordingCaptchaVerifier.Check("tok", "203.0.113.7"));
    }

    @Test
    void rejectsAMessageThatFailsTheCaptcha() {
        captcha.answer(false);

        assertThatThrownBy(() -> service.submitContact(message, "bad", null)).isInstanceOf(CaptchaFailedException.class);
        assertThat(db.contacts).isEmpty();
    }

    @Test
    void listsMarksAsReadAndDeletes() {
        db.contacts.put("c1", Fixtures.contact("c1", false, T0));
        db.contacts.put("c2", Fixtures.contact("c2", true, T0.plusSeconds(1)));

        assertThat(service.listContacts(null, new PageRequest(1, 20)).data()).extracting(ContactRequest::id)
                .containsExactly("c2", "c1");
        assertThat(service.listContacts(false, new PageRequest(1, 20)).data()).extracting(ContactRequest::id)
                .containsExactly("c1");

        assertThat(service.markContactRead("c1").read()).isTrue();
        service.deleteContact("c1");

        assertThat(db.contacts).containsOnlyKeys("c2");
        assertThatThrownBy(() -> service.markContactRead("c1")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deleteContact("c1")).isInstanceOf(NotFoundException.class);
    }
}
