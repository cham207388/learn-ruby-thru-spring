package com.example.todo.todos.controller;

import com.example.todo.todos.dto.CreateTodoRequest;
import com.example.todo.todos.dto.PageResponse;
import com.example.todo.todos.dto.TodoResponse;
import com.example.todo.todos.dto.UpdateTodoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Todos")
// HTTP contract and OpenAPI docs; TodoController holds only the implementation
public interface TodoApi {

    @Operation(summary = "Create a todo")
    @ApiResponse(
            responseCode = "201",
            description = "Created; Location header points to the new todo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TodoResponse.class)))
    @ApiResponse(
            responseCode = "422",
            description = "Validation failed",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    ResponseEntity<TodoResponse> createTodo(@Valid @RequestBody CreateTodoRequest request);

    // Spring Data resolves page and size (spring.data.web.pageable.* in application.yaml).
    // They are documented here and the Pageable itself is hidden, so Swagger does not
    // advertise a sort parameter the API ignores.
    @Operation(summary = "List todos, newest first")
    @Parameter(
            name = "page",
            in = ParameterIn.QUERY,
            description = "1-based page number",
            schema = @Schema(type = "integer", defaultValue = "1"))
    @Parameter(
            name = "size",
            in = ParameterIn.QUERY,
            description = "Page size, 1 to 100",
            schema = @Schema(type = "integer", defaultValue = "20"))
    @GetMapping
    PageResponse<TodoResponse> listTodos(
            @Parameter(description = "Filter by completion; omit for all") @RequestParam(required = false)
                    Boolean completed,
            @Parameter(hidden = true) Pageable pageable);

    @Operation(summary = "Get one todo")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TodoResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No todo with this id",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    TodoResponse getTodo(@PathVariable Long id);

    @Operation(summary = "Update a todo (only the fields sent)")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TodoResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No todo with this id",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "422",
            description = "Validation failed",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @PatchMapping("/{id}")
    TodoResponse updateTodo(@PathVariable Long id, @Valid @RequestBody UpdateTodoRequest request);

    @Operation(summary = "Delete a todo")
    @ApiResponse(responseCode = "204", description = "Deleted; no body")
    @ApiResponse(
            responseCode = "404",
            description = "No todo with this id",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteTodo(@PathVariable Long id);
}
