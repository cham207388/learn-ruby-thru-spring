package com.example.todo.todos.dto;

import java.util.List;
import org.springframework.data.domain.Page;

// Shared list contract: {"items": [...], "page": 1, "size": 20, "total": 42}, page starts at 1.
// Spring's own Page JSON is 0-based and shaped differently, so it never leaves the service.
public record PageResponse<T>(List<T> items, int page, int size, long total) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(), page.getTotalElements());
    }
}
