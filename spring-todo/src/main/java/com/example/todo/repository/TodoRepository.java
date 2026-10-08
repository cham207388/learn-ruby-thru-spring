package com.example.todo.repository;

import com.example.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // Derived query: Spring Data writes "where completed = ? order by ... limit ? offset ?" plus a count query
    Page<Todo> findByCompleted(boolean completed, Pageable pageable);
}
