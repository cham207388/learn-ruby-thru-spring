package com.example.todo.todos.repository;

import com.example.todo.todos.dto.TodoResponse;
import com.example.todo.todos.entity.Todo;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // Reads project straight into the response record: Spring Data writes
    // "select new TodoResponse(t.id, t.title, ...)", so no managed entities are loaded
    Page<TodoResponse> findAllBy(Pageable pageable);

    Page<TodoResponse> findByCompleted(boolean completed, Pageable pageable);

    Optional<TodoResponse> findTodoResponseById(Long id);
}
