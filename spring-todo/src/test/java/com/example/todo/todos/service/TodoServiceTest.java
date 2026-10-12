package com.example.todo.todos.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todo.TestcontainersConfiguration;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@RequiredArgsConstructor
class TodoServiceTest {

    private final TodoService todoService;

    @Test
    void rejectsNullId() {
        assertThatThrownBy(() -> todoService.getTodo(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("getTodo.id");
    }

    @Test
    void rejectsNullRequest() {
        assertThatThrownBy(() -> todoService.createTodo(null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("createTodo.request");
    }

    @Test
    void rejectsNullPageable() {
        assertThatThrownBy(() -> todoService.listTodos(null, null))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("listTodos.pageable");
    }
}
