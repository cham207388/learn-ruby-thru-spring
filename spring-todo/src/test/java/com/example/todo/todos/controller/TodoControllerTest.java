package com.example.todo.todos.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.todo.TestcontainersConfiguration;
import com.example.todo.todos.entity.Todo;
import com.example.todo.todos.repository.TodoRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
@RequiredArgsConstructor
class TodoControllerTest {

    private final MockMvcTester mockMvcTester;

    private final TodoRepository todoRepository;

    @BeforeEach
    void cleanTable() {
        todoRepository.deleteAll();
    }

    private MvcTestResult postTodo(String json) {
        return mockMvcTester
                .post()
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

        Long id = todoRepository.findAll().getFirst().getId();
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
        assertThat(todoRepository.count()).isZero();
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

    private Todo saveTodo(String title, boolean completed) {
        Todo todo = new Todo(title);
        todo.setCompleted(completed);
        return todoRepository.saveAndFlush(todo);
    }

    @Test
    void listsNewestFirstWithPageMetadata() {
        saveTodo("first", false);
        saveTodo("second", true);
        saveTodo("third", false);

        MvcTestResult result = mockMvcTester.get().uri("/api/todos").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.items[*].title").isEqualTo(List.of("third", "second", "first"));
        assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(20);
        assertThat(result).bodyJson().extractingPath("$.total").isEqualTo(3);
    }

    @Test
    void filtersByCompleted() {
        saveTodo("open", false);
        saveTodo("done", true);

        assertThat(mockMvcTester.get().uri("/api/todos?completed=true").exchange())
                .bodyJson()
                .extractingPath("$.items[*].title")
                .isEqualTo(List.of("done"));
        assertThat(mockMvcTester.get().uri("/api/todos?completed=false").exchange())
                .bodyJson()
                .extractingPath("$.items[*].title")
                .isEqualTo(List.of("open"));
    }

    @Test
    void paginatesWithOneBasedPages() {
        saveTodo("first", false);
        saveTodo("second", false);
        saveTodo("third", false);

        MvcTestResult result =
                mockMvcTester.get().uri("/api/todos?page=2&size=1").exchange();

        assertThat(result).bodyJson().extractingPath("$.items[*].title").isEqualTo(List.of("second"));
        assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.total").isEqualTo(3);
    }

    @Test
    void clampsSizeTo100() {
        assertThat(mockMvcTester.get().uri("/api/todos?size=500").exchange())
                .bodyJson()
                .extractingPath("$.size")
                .isEqualTo(100);
    }

    @Test
    void fallsBackToDefaultSizeForInvalidSize() {
        assertThat(mockMvcTester.get().uri("/api/todos?size=0").exchange())
                .bodyJson()
                .extractingPath("$.size")
                .isEqualTo(20);
    }

    @Test
    void getsOneTodo() {
        Todo todo = saveTodo("Buy milk", false);

        MvcTestResult result =
                mockMvcTester.get().uri("/api/todos/{id}", todo.getId()).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Buy milk");
    }

    @Test
    void unknownIdReturns404ProblemDetails() {
        MvcTestResult result = mockMvcTester.get().uri("/api/todos/999999").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Todo 999999 not found");
    }

    private MvcTestResult patchTodo(Long id, String json) {
        return mockMvcTester
                .patch()
                .uri("/api/todos/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    @Test
    void updatesOnlyTheFieldsSent() {
        Todo todo = new Todo("File taxes");
        todo.setDescription("Federal and state returns");
        Todo saved = todoRepository.saveAndFlush(todo);

        MvcTestResult result = patchTodo(saved.getId(), """
                {"title": "File 2026 taxes"}
                """);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("File 2026 taxes");
        assertThat(result).bodyJson().extractingPath("$.description").isEqualTo("Federal and state returns");
        assertThat(todoRepository.findById(saved.getId()))
                .hasValueSatisfying(updated -> assertThat(updated.getTitle()).isEqualTo("File 2026 taxes"));
    }

    @Test
    void marksATodoComplete() {
        Todo saved = saveTodo("Buy milk", false);

        MvcTestResult result = patchTodo(saved.getId(), """
                {"completed": true}
                """);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.completed").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Buy milk");
    }

    @Test
    void changesUpdatedAtButNotCreatedAt() throws Exception {
        Todo saved = saveTodo("Buy milk", false);

        MvcTestResult result = patchTodo(saved.getId(), """
                {"title": "Buy oat milk"}
                """);

        Todo updated = todoRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfter(saved.getUpdatedAt());
        // The response must carry the new updated_at too, not the value from before the flush
        String responseUpdatedAt = JsonPath.read(result.getResponse().getContentAsString(), "$.updated_at");
        assertThat(Instant.parse(responseUpdatedAt)).isAfter(saved.getUpdatedAt());
    }

    @Test
    void rejectsBlankTitleOnUpdate() {
        Todo saved = saveTodo("Buy milk", false);

        MvcTestResult result = patchTodo(saved.getId(), """
                {"title": "  "}
                """);

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errors.title").asArray().contains("must not be blank");
    }

    @Test
    void rejectsTitleOver200CharactersOnUpdate() {
        Todo saved = saveTodo("Buy milk", false);

        MvcTestResult result = patchTodo(saved.getId(), """
                {"title": "%s"}
                """.formatted("a".repeat(201)));

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.errors.title").asArray().isNotEmpty();
    }

    @Test
    void returns404WhenUpdatingUnknownId() {
        MvcTestResult result = patchTodo(999_999L, """
                {"title": "x"}
                """);

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void deletesATodo() {
        Todo saved = saveTodo("Buy milk", false);

        MvcTestResult result =
                mockMvcTester.delete().uri("/api/todos/{id}", saved.getId()).exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(result.getResponse().getContentLength()).isZero();
        assertThat(todoRepository.existsById(saved.getId())).isFalse();
        assertThat(mockMvcTester.get().uri("/api/todos/{id}", saved.getId()).exchange())
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void returns404WhenDeletingUnknownId() {
        MvcTestResult result = mockMvcTester.delete().uri("/api/todos/999999").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Todo 999999 not found");
    }
}
