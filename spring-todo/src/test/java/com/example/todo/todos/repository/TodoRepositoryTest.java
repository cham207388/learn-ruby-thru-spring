package com.example.todo.todos.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todo.TestcontainersConfiguration;
import com.example.todo.config.JpaAuditingConfig;
import com.example.todo.todos.entity.Todo;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
@RequiredArgsConstructor
class TodoRepositoryTest {

    private final TodoRepository todoRepository;

    @Test
    void savesValidTodoWithDefaultsAndAuditTimestamps() {
        Todo saved = todoRepository.saveAndFlush(new Todo("Buy milk"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isCompleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    // Request validation (CreateTodoRequest) rejects this first; the column length is the last guard
    @Test
    void databaseRejectsTitleOver200Characters() {
        assertThatThrownBy(() -> todoRepository.saveAndFlush(new Todo("a".repeat(201))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void projectsIntoResponseRecord() {
        Todo saved = todoRepository.saveAndFlush(new Todo("Buy milk"));

        assertThat(todoRepository.findTodoResponseById(saved.getId()))
                .hasValueSatisfying(response -> assertThat(response.title()).isEqualTo("Buy milk"));
    }

    @Test
    void updateChangesUpdatedAtButNotCreatedAt() throws InterruptedException {
        Todo todo = todoRepository.saveAndFlush(new Todo("Buy milk"));
        var createdAt = todo.getCreatedAt();
        var firstUpdatedAt = todo.getUpdatedAt();

        Thread.sleep(5);
        todo.setCompleted(true);
        todoRepository.saveAndFlush(todo);

        assertThat(todo.getCreatedAt()).isEqualTo(createdAt);
        assertThat(todo.getUpdatedAt()).isAfter(firstUpdatedAt);
    }
}
