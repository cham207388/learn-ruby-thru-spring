package com.example.todo.todos.repository;

import com.example.todo.todos.dto.TodoResponse;
import com.example.todo.todos.entity.Todo;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // Reads project straight into the response record: Spring Data writes
    // "select new TodoResponse(t.id, t.title, ...)", so no managed entities are loaded
    Page<TodoResponse> findAllBy(Pageable pageable);

    Page<TodoResponse> findByCompleted(boolean completed, Pageable pageable);

    Optional<TodoResponse> findTodoResponseById(Long id);

    // One DELETE statement that reports how many rows it removed (0 = unknown id).
    // JpaRepository.deleteById ignores a missing id, so it would need an extra existsById query.
    @Modifying
    @Query("delete from Todo t where t.id = :id")
    int deleteTodoById(Long id);
}
