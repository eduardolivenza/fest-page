package com.festdescans.api.adapter.in.web.contact;

import com.festdescans.api.adapter.in.web.common.PageResponse;
import com.festdescans.api.application.port.in.ManageContactsUseCase;
import com.festdescans.api.domain.shared.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contacts")
class AdminContactController {

    static final int MAX_PAGE_SIZE = 100;

    private final ManageContactsUseCase contacts;

    AdminContactController(ManageContactsUseCase contacts) {
        this.contacts = contacts;
    }

    /** {@code read=true} only read ones, any other value only unread ones, absent all. */
    @GetMapping
    PageResponse<ContactResponse> list(@RequestParam(required = false) String read,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        Boolean filter = read == null ? null : "true".equals(read);
        return PageResponse.of(contacts.listContacts(filter, PageRequest.of(page, pageSize, MAX_PAGE_SIZE)),
                ContactResponse::from);
    }

    @PatchMapping("/{id}/read")
    ContactResponse markRead(@PathVariable String id) {
        return ContactResponse.from(contacts.markContactRead(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) {
        contacts.deleteContact(id);
    }
}
