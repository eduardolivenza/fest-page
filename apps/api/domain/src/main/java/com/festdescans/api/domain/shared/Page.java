package com.festdescans.api.domain.shared;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> data, long total, int page, int pageSize) {

    public Page {
        data = List.copyOf(data);
    }

    public int totalPages() {
        return (int) Math.ceilDiv(total, (long) pageSize);
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        return new Page<>(data.stream().<R>map(mapper).toList(), total, page, pageSize);
    }
}
