package com.example.todo.todos.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// PATCH body: every field is optional and null means "leave unchanged", so a field cannot be
// cleared (Rails can: "description": null). @Size and @Pattern pass on null, so a title is
// checked only when one is sent; @Pattern rejects a blank one.
public record UpdateTodoRequest(
        @Size(max = 200) @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String title,

        String description,
        LocalDate dueDate,
        Boolean completed) {}
