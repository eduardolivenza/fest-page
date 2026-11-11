package com.festdescans.api.adapter.in.web.error;

/** Every error body is {@code { "message": "..." }}. */
public record ErrorResponse(String message) {
}
