package com.example.todo.todos.controller;

import com.example.todo.todos.dto.CreateTodoRequest;
import com.example.todo.todos.dto.PageResponse;
import com.example.todo.todos.dto.TodoResponse;
import com.example.todo.todos.service.TodoService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController implements TodoApi {

    private final TodoService todoService;

    @Override
    public ResponseEntity<TodoResponse> createTodo(CreateTodoRequest request) {
        TodoResponse created = todoService.createTodo(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Override
    public PageResponse<TodoResponse> listTodos(Boolean completed, Pageable pageable) {
        return todoService.listTodos(completed, pageable);
    }

    @Override
    public TodoResponse getTodo(Long id) {
        return todoService.getTodo(id);
    }
}
