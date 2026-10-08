package com.example.todo.dto;

import com.example.todo.entity.Todo;
import java.time.Instant;
import java.time.LocalDate;

public record TodoResponse(
        Long id,
        String title,
        String description,
        boolean completed,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getDueDate(),
                todo.getCreatedAt(),
                todo.getUpdatedAt());
    }
}
