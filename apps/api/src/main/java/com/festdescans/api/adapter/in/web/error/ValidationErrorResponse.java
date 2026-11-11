package com.festdescans.api.adapter.in.web.error;

import java.util.List;
import java.util.Map;

/** 400 body, same shape as Zod's {@code error.flatten()} that the Node api returned. */
public record ValidationErrorResponse(String message, Errors errors) {

    public record Errors(List<String> formErrors, Map<String, List<String>> fieldErrors) {
    }
}
