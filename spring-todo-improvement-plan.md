# spring-todo: improvement plan

Audit of `spring-todo/` against the `spring-boot-java` guidelines (Java 25, Gradle 9.8+, Spring Boot 4, lean). Each step is small, leaves the build green, and can be its own commit. Do them in order: the ArchUnit test (step 2) goes in before the refactors so it guards them.

Versions were looked up on 2026-10-12: Spring Boot 4.1.1 (start.spring.io default), Gradle 9.8.1, Spotless plugin 8.10.4, palantir-java-format 2.102.0, ArchUnit 1.5.1, springdoc 3.1.1, dependency-management plugin 1.1.7.

## What is already right

Keep these as they are:

- Version catalog plus Kotlin DSL; the Boot BOM owns library versions; Spotless with palantir, `removeUnusedImports()`, trailing whitespace and final newline
- Boot 4 starter names (`-webmvc`, `-webmvc-test`, `-data-jpa-test`, `-flyway-test`)
- Records for request/response bodies with a `static from(...)` factory; no mapper classes
- JPA entity with `@Getter`/`@Setter`, a protected no-args constructor, `@Setter(AccessLevel.NONE)` on id and audit fields, no `@Data`
- Constructor injection in production code; `@Transactional` on the service, `readOnly = true` on reads
- Filtering by `completed` in the repository (`findByCompleted`), not in a stream
- One `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`, RFC 9457 `ProblemDetail`, typed `TodoNotFoundException` mapped to 404
- `open-in-view: false`, `ddl-auto: validate`, Flyway, Testcontainers with `@ServiceConnection`
- `TodoApi` interface holding the OpenAPI annotations (the allowed exception to "one implementation, no interface")
- No `@Value`, no wildcard imports, no fully qualified type names, no compiler warnings even with `-Xlint:all`

## Findings

| # | Guideline | Current state | Change | Step |
| --- | --- | --- | --- | --- |
| 1 | Gradle 9.8+ | Wrapper is 9.7.1 | Upgrade to 9.8.1 | 1 |
| 2 | Latest dependencies | Spotless plugin 8.10.3 | Upgrade to 8.10.4 | 1 |
| 3 | No unused variables, no deprecated APIs | Nothing makes the build fail on them | `-Xlint:all,-processing,-serial -Werror` | 1 |
| 4 | No wildcard imports | Not enforced | Spotless `forbidWildcardImports()` | 1 |
| 5 | ArchUnit enforces architecture | No ArchUnit at all | `archunit-junit6` 1.5.1 and one `ArchitectureTest` | 2 |
| 6 | Package-by-feature with layer packages inside | Package by layer (`com.example.todo.controller`, ...) | Move to `com.example.todo.todos.{controller,service,repository,dto,entity}` | 3 |
| 7 | Field name = type name | `private final TodoService service`, `TodoRepository repository` | `todoService`, `todoRepository` | 4 |
| 8 | Lombok to reduce boilerplate | Hand-written constructors in `TodoController` and `TodoService` | `@RequiredArgsConstructor` | 4 |
| 9 | Meaningful method names | `create`, `get`, `list` | `createTodo`, `getTodo`, `listTodos` | 4 |
| 10 | No unused variables | `TodoService.DEFAULT_SIZE` is never read (javac does not flag unused package-private constants) | Deleted by step 5 | 5 |
| 11 | Validation via Spring, not by hand; no reinventing | Paging clamped by hand with `Math.max`/`Math.clamp` and constants | Spring Data's `Pageable` resolver, configured with `spring.data.web.pageable.*` properties | 5 |
| 12 | No unnecessary mapping when the repository can do it | Reads load managed entities, then map each to `TodoResponse` | Repository returns `TodoResponse` records directly (class-based DTO projection) | 6 |
| 13 | Validate once at the boundary; do not re-check | `@NotBlank @Size(max = 200)` on both `CreateTodoRequest` and the `Todo` entity | Remove from the entity; the database column stays the last guard | 7 |
| 14 | No reinventing the wheel | `GlobalExceptionHandler.snakeCase()` regex duplicates Jackson's naming strategy | Dropped: Jackson 3 no longer exposes `translate()` publicly | 8 |
| 15 | Constructor injection; field name = type name | Tests use `@Autowired` fields named `mvc`, `repository` | Constructor injection in tests; `mockMvcTester`, `todoRepository` | 9 |
| 16 | Plan text matches the code | Learning plan says "package by layer" and requires entity-level validation | Update the learning plan; give US-3.3 and US-3.4 guideline notes | 10 |

## Steps

### Step 1: Build hygiene

- `cd spring-todo && ./gradlew wrapper --gradle-version 9.8.1`, then run it a second time so the wrapper jar and scripts update too
- `libs.versions.toml`: `spotless = "8.10.4"`
- `build.gradle.kts`:

```kotlin
spotless {
    java {
        palantirJavaFormat(libs.versions.palantir.java.format.get())
        removeUnusedImports()
        forbidWildcardImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    // kotlinGradle block unchanged
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-serial", "-Werror"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

**Validate:** `make spring-format spring-build` is green (Docker running). The build already compiles with no warnings under `-Xlint:all`, so `-Werror` should not break anything today.

### Step 2: ArchUnit

- Catalog: `archunit = "1.5.1"` and `archunit-junit6 = { module = "com.tngtech.archunit:archunit-junit6", version.ref = "archunit" }`. `build.gradle.kts`: `testImplementation(libs.archunit.junit6)`
- One class, `src/test/java/com/example/todo/ArchitectureTest.java`. Its rules guard the guidelines that Spotless cannot see:

```java
@AnalyzeClasses(packages = "com.example.todo", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule noCycles = slices().matching("com.example.todo.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule noFieldInjection = noFields().should().beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule noValueInjection = noClasses()
            .should().dependOnClassesThat().areAssignableTo(Value.class)
            .because("bind settings with @ConfigurationProperties records");

    @ArchTest
    static final ArchRule controllersNeverSeeEntities = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..entity..");

    @ArchTest
    static final ArchRule transactionsBelongToServices = noClasses()
            .that().resideInAPackage("..controller..")
            .should().beAnnotatedWith(Transactional.class)
            .orShould().dependOnClassesThat().areAssignableTo(Transactional.class);

    @ArchTest
    static final ArchRule dtosAreRecords = classes()
            .that().resideInAPackage("..dto..")
            .should().beAssignableTo(Record.class);
}
```

- There is no `Function` layer: the app is not event-driven, and ArchUnit fails on an empty layer. Add it together with the first Spring Cloud Function bean.
- Lombok annotations such as `@Data` are compile-time only, so ArchUnit cannot see them in bytecode. "No `@Data` on entities" stays a review rule.

**Validate:** `cd spring-todo && ./gradlew test --tests '*ArchitectureTest'` is green. Then break each rule once and watch it fail: inject `TodoRepository` into `TodoController`, or put `@Autowired` on a field.

### Step 3: Package by feature

Target layout (the feature package is `todos`, which avoids `com.example.todo.todo`):

```
com.example.todo
  SpringTodoApplication
  config/        JpaAuditingConfig, OpenApiConfig
  exception/     GlobalExceptionHandler, TodoNotFoundException
  todos/
    controller/  TodoApi, TodoController
    service/     TodoService
    repository/  TodoRepository
    dto/         CreateTodoRequest, TodoResponse, PageResponse
    entity/      Todo
```

- Move the classes with IntelliJ's Move refactoring so imports update. Move the tests to match.
- `PageResponse` stays in `todos/dto` until a second feature needs it. Do not create a `common` package for a single feature.

**Validate:** `make spring-build` is green; `ArchitectureTest` passes unchanged because it matches `..controller..`, `..service..` and `..repository..` at any depth.

### Step 4: Lombok, field names, method names

- `TodoController` and `TodoService`: delete the constructors and add `@RequiredArgsConstructor`
- Fields: `private final TodoService todoService;` and `private final TodoRepository todoRepository;`
- Service: `createTodo(CreateTodoRequest)`, `getTodo(Long)`, `listTodos(Boolean completed, Pageable)`. `TodoApi`/`TodoController`: `createTodo`, `getTodo`, `listTodos`

**Validate:** `make spring-build` is green; the HTTP contract is unchanged.

### Step 5: Paging with Spring Data instead of hand-written clamping

`application.yaml`:

```yaml
spring:
  data:
    web:
      pageable:
        one-indexed-parameters: true
        default-page-size: 20
        max-page-size: 100
```

- `TodoApi.listTodos(@RequestParam(required = false) Boolean completed, @Parameter(hidden = true) Pageable pageable)`. Document `page` and `size` with method-level `@Parameter(name = "page", in = ParameterIn.QUERY, ...)` annotations, so Swagger does not advertise a `sort` parameter the API ignores
- `TodoService.listTodos` keeps the fixed order: `PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST)`. Delete `DEFAULT_SIZE`, `MAX_SIZE` and the `Math` calls
- No `@ConfigurationProperties` record is needed: Spring Data's own properties hold the paging settings

I probed the resolver with these settings; it keeps today's contract except for two edge cases:

| Request | Today | With `Pageable` |
| --- | --- | --- |
| none | page 1, size 20 | same |
| `page=2&size=1` | page 2, size 1 | same |
| `size=500` | size 100 | same |
| `page=0`, `page=-3` | page 1 | same |
| `size=0`, `size=-1` | size **1** | size **20** (default) |
| `page=abc` | **400** problem detail | page 1 (falls back to the default) |

- Decide whether the two edge cases are acceptable. I recommend accepting them, adding a controller test that pins the new behavior, and noting the change in the shared contract so the Rails app matches.

**Validate:** the existing paging tests pass (`listsNewestFirstWithPageMetadata`, `paginatesWithOneBasedPages`, `clampsSizeTo100`); the new edge-case test is green.

### Step 6: Read straight into records

- `TodoRepository`:

```java
Page<TodoResponse> findAllBy(Pageable pageable);

Page<TodoResponse> findByCompleted(boolean completed, Pageable pageable);

Optional<TodoResponse> findTodoResponseById(Long id);
```

  Spring Data maps a record whose component names match the entity properties into a JPQL constructor expression. Hibernate then selects only those columns and does not create managed entities for reads.
- `TodoService.getTodo` becomes `todoRepository.findTodoResponseById(id).orElseThrow(() -> new TodoNotFoundException(id))`
- `PageResponse.from(Page<T> page)` loses its mapper parameter
- `TodoResponse.from(Todo)` stays for `createTodo` (and for `updateTodo` in US-3.3), where you already hold the saved entity

**Validate:** the controller tests are green. Turn on `logging.level.org.hibernate.SQL=debug` and check that list and get queries select columns into the record (`select t.id, t.title, ...`), not whole entities. **This is the one change I could not run here** (Docker was off), so prove it with the test suite before keeping it. If Hibernate rejects the projection, fall back to `Page.map(TodoResponse::from)` in the service.

### Step 7: Validate once, at the boundary

- Remove `@NotBlank` and `@Size` from `Todo.title`. Keep `@Column(nullable = false, length = 200)`: the database stays the final guard
- Optional: Flyway `V2__todos_title_not_blank.sql` with `ALTER TABLE todos ADD CONSTRAINT todos_title_not_blank CHECK (btrim(title) <> '')`, if you want blank titles impossible even from SQL
- `TodoRepositoryTest`: replace the two `ConstraintViolationException` tests with one that saves a 201-character title and expects `DataIntegrityViolationException`, plus a matching blank-title test if you add the CHECK constraint. Keep the audit-timestamp tests
- `jakarta.validation` imports disappear from the entity

**Validate:** `make spring-build` is green; `rejectsBlankTitleWithProblemDetails` and `rejectsTitleOver200Characters` (controller tests) still return 422, because the request record still validates.

### Step 8: Error field names from Jackson (dropped)

Tried and reverted: Jackson 3 made `PropertyNamingStrategies.NamingBase.translate()` protected, so the configured strategy cannot be called from outside Jackson without passing `null`s into `nameForField`. The small regex helper in `GlobalExceptionHandler` stays, with a comment explaining why.

### Step 9: Tests use constructor injection

- `src/test/resources/junit-platform.properties`: `spring.test.constructor.autowire.mode=all`
- Test classes: `@RequiredArgsConstructor` with `private final MockMvcTester mockMvcTester;` and `private final TodoRepository todoRepository;`. Lombok is already on `testCompileOnly`/`testAnnotationProcessor`

**Validate:** `make spring-test` is green; `grep -rn "@Autowired" spring-todo/src` finds nothing.

### Step 10: Bring the learning plan in line

In `Todo-app-spring-boot-rails-earning Plan.md`:

- US-1.2 and the conventions section: "package by layer" becomes "package by feature (`todos`) with `controller`/`service`/`repository`/`dto`/`entity` inside, guarded by ArchUnit"
- US-2.2: the repository test no longer checks Bean Validation on the entity; validation is tested at the HTTP boundary
- US-3.3 (update), Spring track, so it follows the guidelines from the start:
  - `UpdateTodoRequest(@Size(max = 200) @Pattern(regexp = ".*\\S.*") String title, String description, LocalDate dueDate, Boolean completed)`. Every component is nullable, and `@Pattern` rejects a blank title only when one is sent
  - service method `updateTodo(Long id, UpdateTodoRequest request)`; dirty checking does the update; respond with `TodoResponse.from(todo)`
- US-3.4 (delete), Spring track: one query instead of `existsById` + `deleteById`:

```java
@Modifying
@Query("delete from Todo t where t.id = :id")
int deleteTodoById(Long id);
```

  `TodoService.deleteTodo(Long id)` throws `TodoNotFoundException` when it returns 0.

## Deliberately not changed

| Guideline | Why not here |
| --- | --- |
| Spring Cloud Function for events | The app is not event-driven. "Lean" says do not add Cloud Function, Stream or a broker without events; add them with the first real event |
| `@ConfigurationProperties` records | The only settings (paging) are Spring Data properties after step 5. Add a record, plus `spring-boot-configuration-processor`, when the app gets its own settings |
| `@Slf4j` logging | Nothing logs yet; add it where logging is actually needed |
| Removing `TodoApi` | It is the allowed interface exception: OpenAPI annotations would drown the controller |
| `@Validated` on the controller | Only needed once constraint annotations sit on `@RequestParam`/`@PathVariable`; after step 5 there are none |

Optional, outside the guideline list:

- `spring.threads.virtual.enabled: true` (Java 25), a one-line throughput win for a blocking MVC app
- `TodoRepositoryTest.updateChangesUpdatedAtButNotCreatedAt` uses `Thread.sleep(5)`. A `DateTimeProvider` bean backed by a test `Clock` (`@EnableJpaAuditing(dateTimeProviderRef = ...)`) would make it deterministic

## Done when

`make spring-format spring-build` is green with Docker running: Spotless clean, `-Werror` clean, `ArchitectureTest` green, all tests green, and no `@Autowired` field left anywhere in `spring-todo/src`.
