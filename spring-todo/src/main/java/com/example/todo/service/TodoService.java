package com.example.todo.service;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.TodoResponse;
import com.example.todo.entity.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {

    private final TodoRepository repository;

    public TodoService(TodoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TodoResponse create(CreateTodoRequest request) {
        Todo todo = new Todo(request.title());
        todo.setDescription(request.description());
        todo.setDueDate(request.dueDate());
        return TodoResponse.from(repository.save(todo));
    }
}
