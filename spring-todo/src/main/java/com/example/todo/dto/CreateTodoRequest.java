package com.example.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// Only these fields can be set by a client; unknown JSON fields (id, completed, ...) are ignored
public record CreateTodoRequest(@NotBlank @Size(max = 200) String title, String description, LocalDate dueDate) {}
