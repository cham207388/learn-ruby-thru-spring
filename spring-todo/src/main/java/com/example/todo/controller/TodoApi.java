package com.example.todo.controller;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.PageResponse;
import com.example.todo.dto.TodoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    ResponseEntity<TodoResponse> create(@Valid @RequestBody CreateTodoRequest request);

    @Operation(summary = "List todos, newest first")
    @GetMapping
    PageResponse<TodoResponse> list(
            @Parameter(description = "Filter by completion; omit for all") @RequestParam(required = false)
                    Boolean completed,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, 1 to 100") @RequestParam(defaultValue = "20") int size);

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
    TodoResponse get(@PathVariable Long id);
}
