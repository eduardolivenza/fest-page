package com.festdescans.api.domain.shared;

/** 1-based page number and page size; out-of-range values are clamped instead of failing. */
public record PageRequest(int page, int pageSize) {

    public PageRequest {
        page = Math.max(page, 1);
        pageSize = Math.max(pageSize, 1);
    }

    /** Caps the page size, e.g. 50 for the public catalogue. */
    public static PageRequest of(int page, int pageSize, int maxPageSize) {
        return new PageRequest(page, Math.min(pageSize, maxPageSize));
    }

    public long offset() {
        return (long) (page - 1) * pageSize;
    }
}
