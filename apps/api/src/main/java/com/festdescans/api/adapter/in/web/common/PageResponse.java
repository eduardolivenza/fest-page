package com.festdescans.api.adapter.in.web.common;

import com.festdescans.api.domain.shared.Page;
import java.util.List;
import java.util.function.Function;

/** Paginated list, the shape of PaginatedResponse in packages/types. */
public record PageResponse<T>(List<T> data, long total, int page, int pageSize, int totalPages) {

    public static <D, T> PageResponse<T> of(Page<D> page, Function<D, T> mapper) {
        return new PageResponse<>(page.data().stream().map(mapper).toList(), page.total(), page.page(),
                page.pageSize(), page.totalPages());
    }
}
