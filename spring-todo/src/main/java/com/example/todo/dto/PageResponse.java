package com.example.todo.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

// Shared list contract: {"items": [...], "page": 1, "size": 20, "total": 42}, page starts at 1.
// Spring's own Page JSON is 0-based and shaped differently, so it never leaves the service.
public record PageResponse<T>(List<T> items, int page, int size, long total) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements());
    }
}
