package com.example.todo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todo.TestcontainersConfiguration;
import com.example.todo.config.JpaAuditingConfig;
import com.example.todo.entity.Todo;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class TodoRepositoryTest {

    @Autowired
    private TodoRepository repository;

    @Test
    void savesValidTodoWithDefaultsAndAuditTimestamps() {
        Todo saved = repository.saveAndFlush(new Todo("Buy milk"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isCompleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> repository.saveAndFlush(new Todo("  ")))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("title");
    }

    @Test
    void rejectsTitleOver200Characters() {
        assertThatThrownBy(() -> repository.saveAndFlush(new Todo("a".repeat(201))))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("title");
    }

    @Test
    void updateChangesUpdatedAtButNotCreatedAt() throws InterruptedException {
        Todo todo = repository.saveAndFlush(new Todo("Buy milk"));
        var createdAt = todo.getCreatedAt();
        var firstUpdatedAt = todo.getUpdatedAt();

        Thread.sleep(5);
        todo.setCompleted(true);
        repository.saveAndFlush(todo);

        assertThat(todo.getCreatedAt()).isEqualTo(createdAt);
        assertThat(todo.getUpdatedAt()).isAfter(firstUpdatedAt);
    }
}
