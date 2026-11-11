package com.festdescans.api.domain.product;

import com.festdescans.api.domain.shared.DomainException;

public final class SlugAlreadyUsedException extends DomainException {

    public SlugAlreadyUsedException(String slug) {
        super("Slug " + slug + " is already used");
    }
}
