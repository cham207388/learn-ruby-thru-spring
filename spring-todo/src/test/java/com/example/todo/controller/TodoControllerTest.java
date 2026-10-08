package com.example.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todo.TestcontainersConfiguration;
import com.example.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TodoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private TodoRepository repository;

    @BeforeEach
    void cleanTable() {
        repository.deleteAll();
    }

    private MvcTestResult postTodo(String json) {
        return mvc.post()
                .uri("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    @Test
    void createsTodoAndReturnsLocation() {
        MvcTestResult result = postTodo("""
                {"title": "Buy milk", "description": "2 liters", "due_date": "2026-12-01"}
                """);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Buy milk");
        assertThat(result).bodyJson().extractingPath("$.due_date").isEqualTo("2026-12-01");
        assertThat(result).bodyJson().extractingPath("$.completed").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$.created_at").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.updated_at").isNotNull();

        Long id = repository.findAll().getFirst().getId();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/api/todos/" + id);
    }

    @Test
    void rejectsBlankTitleWithProblemDetails() {
        MvcTestResult result = postTodo("""
                {"title": "  "}
                """);

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(422);
        assertThat(result).bodyJson().extractingPath("$.errors.title").asArray().isNotEmpty();
        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsTitleOver200Characters() {
        MvcTestResult result = postTodo("""
                {"title": "%s"}
                """.formatted("a".repeat(201)));

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.errors.title").asArray().isNotEmpty();
    }

    @Test
    void ignoresUnknownAndReadOnlyFields() {
        MvcTestResult result = postTodo("""
                {"title": "x", "id": 999, "completed": true, "foo": "bar"}
                """);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isNotEqualTo(999);
        assertThat(result).bodyJson().extractingPath("$.completed").isEqualTo(false);
    }

    @Test
    void malformedJsonIsProblemDetailsToo() {
        MvcTestResult result = postTodo("{not json");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
