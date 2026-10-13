package com.example.todo.todos.service;

import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.todos.dto.CreateTodoRequest;
import com.example.todo.todos.dto.PageResponse;
import com.example.todo.todos.dto.TodoResponse;
import com.example.todo.todos.dto.UpdateTodoRequest;
import com.example.todo.todos.entity.Todo;
import com.example.todo.todos.repository.TodoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

// @Validated: Spring checks the parameter constraints below on every call, not only HTTP ones
@Service
@Validated
@RequiredArgsConstructor
public class TodoService {

    // Newest first; id breaks ties when two rows share created_at
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final TodoRepository todoRepository;

    @Transactional
    public TodoResponse createTodo(@NotNull @Valid CreateTodoRequest request) {
        Todo todo = new Todo(request.title());
        todo.setDescription(request.description());
        todo.setDueDate(request.dueDate());
        return TodoResponse.from(todoRepository.save(todo));
    }

    // Applies only the fields sent; dirty checking writes the UPDATE. The flush runs it now,
    // so the auditing listener sets updatedAt before the response is built from the entity
    @Transactional
    public TodoResponse updateTodo(@NotNull Long id, @NotNull @Valid UpdateTodoRequest request) {
        Todo todo = todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        if (request.title() != null) {
            todo.setTitle(request.title());
        }
        if (request.description() != null) {
            todo.setDescription(request.description());
        }
        if (request.dueDate() != null) {
            todo.setDueDate(request.dueDate());
        }
        if (request.completed() != null) {
            todo.setCompleted(request.completed());
        }
        todoRepository.flush();
        return TodoResponse.from(todo);
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(@NotNull Long id) {
        return todoRepository.findTodoResponseById(id).orElseThrow(() -> new TodoNotFoundException(id));
    }

    // Page and size come from Spring Data's resolver (1-based, default 20, max 100);
    // the order is fixed here, so a client-sent sort parameter is ignored
    @Transactional(readOnly = true)
    public PageResponse<TodoResponse> listTodos(Boolean completed, @NotNull Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        Page<TodoResponse> todos = completed == null
                ? todoRepository.findAllBy(newestFirst)
                : todoRepository.findByCompleted(completed, newestFirst);
        return PageResponse.from(todos);
    }
}
