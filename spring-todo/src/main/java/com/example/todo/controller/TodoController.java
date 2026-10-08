package com.example.todo.controller;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.PageResponse;
import com.example.todo.dto.TodoResponse;
import com.example.todo.service.TodoService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/todos")
public class TodoController implements TodoApi {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<TodoResponse> create(CreateTodoRequest request) {
        TodoResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Override
    public PageResponse<TodoResponse> list(Boolean completed, int page, int size) {
        return service.list(completed, page, size);
    }

    @Override
    public TodoResponse get(Long id) {
        return service.get(id);
    }
}
