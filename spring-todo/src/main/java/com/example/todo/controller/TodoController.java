package com.example.todo.controller;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.PageResponse;
import com.example.todo.dto.TodoResponse;
import com.example.todo.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Todos")
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

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
    public ResponseEntity<TodoResponse> create(@Valid @RequestBody CreateTodoRequest request) {
        TodoResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List todos, newest first")
    @GetMapping
    public PageResponse<TodoResponse> list(
            @Parameter(description = "Filter by completion; omit for all") @RequestParam(required = false)
                    Boolean completed,
            @Parameter(description = "1-based page number") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size, 1 to 100") @RequestParam(defaultValue = "20") int size) {
        return service.list(completed, page, size);
    }

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
    public TodoResponse get(@PathVariable Long id) {
        return service.get(id);
    }
}
