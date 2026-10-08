package com.example.todo.service;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.PageResponse;
import com.example.todo.dto.TodoResponse;
import com.example.todo.entity.Todo;
import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.repository.TodoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;

    // Newest first; id breaks ties when two rows share created_at
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

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

    @Transactional(readOnly = true)
    public TodoResponse get(Long id) {
        return repository.findById(id).map(TodoResponse::from).orElseThrow(() -> new TodoNotFoundException(id));
    }

    // page is 1-based like the shared contract; out-of-range values are clamped, not rejected
    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> list(Boolean completed, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.clamp(size, 1, MAX_SIZE), NEWEST_FIRST);
        Page<Todo> result =
                completed == null ? repository.findAll(pageable) : repository.findByCompleted(completed, pageable);
        return PageResponse.from(result, TodoResponse::from);
    }
}
