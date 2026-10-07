package com.weddingevent.common.api;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Page shape of list endpoints (detailed spec §1.2): {@code {items, page, size, totalElements, totalPages}}. */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
