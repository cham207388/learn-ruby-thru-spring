# Todo App: .NET Learning Plan

<details>
<summary><span style="font-weight: bold; color: cyan;">Overview</span></summary>

Build the same Todo REST API a third time, now in C# on ASP.NET Core, story by story. The Spring and Rails apps in this repo are the reference: same endpoints, same JSON contract, same Postgres container. Each step maps a .NET idea to the Spring idea you already own.

**How we work:** one step at a time. Each step says what to do, why, and how to check it. You type the code yourself; every code block was tested on .NET SDK 10.0.402 before it went into this plan. Every step and every **Validate** table starts with a **Run in:** line naming the folder: repo root (`learn-ruby-thru-spring/`) or `dotnet-todo/`. Paths in "Code to type" headers are relative to repo root.

**Status:** Phase 1 (install .NET and create the app) is written out in full. Later phases are outlines; each gets full, tested steps when we reach it.

**Validate every story:** each story ends with a **Validate** table: commands and the result to expect. Commands use `command grep`, not `grep`: your shell aliases `grep` to ripgrep (`rg`), which prints nothing for a zero count and reads `-E` as an encoding. `command` skips the alias and runs the real `grep`.

**Tech stack (as of Oct 13, 2026)**

| Layer | Choice | Why this one |
| --- | --- | --- |
| Runtime + SDK | .NET 10 LTS: runtime 10.0.13, SDK 10.0.402 (released Oct 13, 2026) | LTS, supported to Nov 2028. .NET 11 is at RC 2 and ships in Nov 2026 as STS (shorter support), so stay on 10 |
| Language | C# 14 (default for .NET 10) | Extension members, `field` keyword, null-conditional assignment |
| Version manager | mise core `dotnet` plugin + `global.json` | Same tool you use for Ruby; `global.json` is the Gradle-toolchain equivalent |
| Web | ASP.NET Core 10 Minimal APIs, route groups, `TypedResults` | Microsoft's default for new APIs; MVC controllers are still supported but no longer the starting point |
| Validation | Built-in Minimal API validation (`AddValidation()`) with DataAnnotations | New in .NET 10; source-generated, no third-party library needed |
| Errors | RFC 9457 Problem Details (`AddProblemDetails()`) | Built in, same shape as Spring's `ProblemDetail` |
| Persistence | EF Core 10 + Npgsql provider 10.0.3 + `EFCore.NamingConventions` (snake_case) | EF Core is the JPA/Hibernate of .NET |
| Migrations | EF Core migrations (`dotnet ef`, pinned as a local tool) | Generated C# + SQL script, like Flyway with a generator |
| API docs | `Microsoft.AspNetCore.OpenApi` (OpenAPI 3.1) + Scalar UI | Swashbuckle left the templates in .NET 9; this is the built-in path |
| Tests | xUnit v3 4.0 on Microsoft.Testing.Platform, `WebApplicationFactory`, Testcontainers 4.16 | `@SpringBootTest` + MockMvc + Testcontainers, one for one |
| Build hygiene | `.slnx` solution, `Directory.Build.props`, Central Package Management, `TreatWarningsAsErrors`, analyzers, `dotnet format` | Gradle version catalog + Spotless + Checkstyle, built in |
| Security | NuGet Audit (on by default) + `dotnet package list --vulnerable` | OWASP dependency-check / bundler-audit |
| Database | PostgreSQL 18, the existing `todo-db` container | Shared with the other two apps |

Port: **5080** (Spring 8081, Rails 3000). Database: **`todo_dotnet`**. Tests start their own Postgres through Testcontainers, like the Spring app.

</details>

---

<details>
<summary><span style="font-weight: bold; color: cyan;">Concept map</span></summary>

.NET sits much closer to Spring than Rails does: static types, DI container, annotations (attributes), an ORM with change tracking. The big shifts are the build system (MSBuild) and the absence of a separate service/repository layer by default.

| You know (Spring / Java / Gradle) | .NET / C# equivalent | Key difference |
| --- | --- | --- |
| SDKMAN, Gradle toolchain | mise + `global.json` | `global.json` pins the SDK per folder; `rollForward` says how strict |
| `build.gradle.kts` | `*.csproj` (MSBuild XML) | One file per project; no build logic in it by default |
| `settings.gradle.kts` (multi-project) | `TodoApi.slnx` solution | `.slnx` is the new XML format, default since .NET 10 |
| Version catalog `libs.versions.toml` | `Directory.Packages.props` (Central Package Management) | `PackageReference` has no version; the props file owns all versions |
| `allprojects {}` / convention plugins | `Directory.Build.props` | MSBuild imports it into every project below that folder |
| Maven Central | NuGet (nuget.org) | `dotnet add package` edits the csproj and restores |
| `./gradlew build`, `test`, `bootRun` | `dotnet build`, `dotnet test`, `dotnet run` | One CLI for everything |
| Spotless + Checkstyle | `.editorconfig` + Roslyn analyzers + `dotnet format` | Style rules can fail the build (`EnforceCodeStyleInBuild`) |
| start.spring.io | `dotnet new webapi` | Templates are installable packages (`dotnet new install ...`) |
| `@SpringBootApplication` main class | `Program.cs` top-level statements | Build services, then build the pipeline, then `Run()` |
| `application.yaml` + profiles | `appsettings.json` + `appsettings.{Environment}.json` + env vars | `ASPNETCORE_ENVIRONMENT` is the active profile |
| `@Bean`, constructor injection | `builder.Services.Add...()`, constructor injection | Lifetimes are explicit: singleton, scoped (per request), transient |
| `@RestController` + `@GetMapping` | `app.MapGet(...)` in a route group | Handlers are methods; parameters bind from route, query, body and DI |
| `ResponseEntity<T>` | `TypedResults.Ok(...)`, `Results<Ok<T>, ProblemHttpResult>` | Union return types document every outcome for OpenAPI |
| `@Valid` + Bean Validation | `AddValidation()` + `[Required]`, `[MaxLength]` | Runs before the handler; failure is Problem Details |
| `@RestControllerAdvice`, `ProblemDetail` | `AddProblemDetails()`, `UseExceptionHandler()`, `IExceptionHandler` | Built in, same RFC |
| `@Entity` + `JpaRepository` | Entity class + `DbContext` with `DbSet<T>` | `DbContext` is already unit of work + repository; no repository layer on top |
| Hibernate dirty checking | EF Core change tracker | Same idea: load, change, `SaveChangesAsync()` |
| `@Transactional` | `SaveChangesAsync()` is one transaction; `BeginTransactionAsync()` for more | No annotation; scope is explicit |
| Flyway `V1__create_todos.sql` | `dotnet ef migrations add CreateTodos` | Generated C# with `Up` and `Down`; rollback is built in |
| `@CreatedDate` / `@LastModifiedDate` | `SaveChangesInterceptor` | You write the 20-line listener yourself |
| `Clock` bean | `TimeProvider` (+ `FakeTimeProvider` in tests) | Built into the base library since .NET 8 |
| springdoc-openapi + Swagger UI | `AddOpenApi()` + Scalar | Reads your `System.Text.Json` settings, so snake_case just works |
| Actuator `/actuator/health` | `AddHealthChecks()` + `MapHealthChecks("/health")` | Add checks (DB, ...) one by one |
| JUnit 5 | xUnit v3 | `[Fact]` = `@Test`; a new class instance per test |
| `@SpringBootTest` + MockMvc | `WebApplicationFactory<Program>` + `HttpClient` | Runs the real pipeline in memory |
| `Optional<T>`, `@Nullable` | Nullable reference types (`string?`), `?.`, `??` | The compiler tracks nulls; warnings fail the build here |
| Streams | LINQ (`Where`, `Select`) | On a `DbSet`, LINQ becomes SQL |
| `CompletableFuture`, virtual threads | `async` / `await`, `Task<T>` | Async all the way down; pass `CancellationToken` |
| Java `record` | C# `record` | Plus `with` expressions for copies |
| Lombok | Auto-properties, `required`, primary constructors | Language features, no annotation processor |

</details>

---

<details>
<summary><span style="font-weight: bold; color: cyan;"><b>Phase 1: Install .NET and create the app</b></span></summary>

Phase 1 ends when .NET 10 is installed through mise, `dotnet-todo/` builds with zero warnings, a smoke test passes, and `GET /health` answers on port 5080. Repo layout after this phase: `spring-todo/`, `rails-todo/`, `dotnet-todo/`, `compose.yml`, `Makefile` at the root.

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-1.1 Database for the .NET app</b></span></summary>

*As a developer, I want a `todo_dotnet` database in the shared Postgres 18 container so the .NET app is isolated from the other two.*

- [x] `todo_dotnet` exists in `todo-db`
- [x] The init script creates it on a fresh volume too

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 1: Add the database to the init script.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`). Edit `db/init/01-create-databases.sql`.

Add one line to `db/init/01-create-databases.sql`:

<details>
<summary>Code to type: <code>db/init/01-create-databases.sql</code></summary>

```sql
CREATE DATABASE todo_spring;
CREATE DATABASE todo_rails_development;
CREATE DATABASE todo_rails_test;
CREATE DATABASE todo_dotnet;
```

</details>

- Why: the script runs only on an empty volume. This line covers a future `make compose-reset`; Step 2 covers the volume you have now.
- No `todo_dotnet_test`: tests use Testcontainers, like the Spring app.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 2: Create it in the running container.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`).

```bash
make compose-up
docker exec todo-db sh -c 'createdb -U "$POSTGRES_USER" todo_dotnet'
make db-list
```

- Expect: `todo_dotnet` in the list.
- Why not `make compose-reset`: it deletes the Spring and Rails data.

</details>

<details>
<summary><span style="font-weight: bold; color: pink;"><b>Validate</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`).

| # | Command | Expect |
| --- | --- | --- |
| 1 | `make db-list` | `todo`, `todo_dotnet`, `todo_rails_development`, `todo_rails_test`, `todo_spring` |
| 2 | `command grep -c todo_dotnet db/init/01-create-databases.sql` | `1` |

</details>

</details>

---

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-1.2 .NET toolchain (mise)</b></span></summary>

*As a Java developer, I want a version-managed .NET 10 SDK so .NET works like SDKMAN-managed JDKs.*

.NET is not installed on this machine yet (`dotnet` is not on PATH). mise installs it.

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 1: Update mise.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`).

```bash
mise self-update
mise --version
```

- Expect: `2026.10.8` or newer. Your copy (2026.10.3) predates today's SDK release, so its version list may not show `10.0.402` yet.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 2: Install the SDK for this repo.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`). `mise use` writes to the `mise.toml` of the folder you are in.

```bash
mise ls-remote dotnet 10.0
mise use dotnet@10.0.402
cat mise.toml
```

- Expect: the list ends with `10.0.402` (or a newer `10.0.4xx`; use that instead). `mise.toml` gains `dotnet = "10.0.402"` under `[tools]`.
- **SDK vs runtime:** `10.0.402` is the SDK (compiler, CLI, templates). It bundles runtime `10.0.13`. The `4xx` is the SDK *feature band*; the runtime is what your app runs on. Java ships both in one JDK; .NET versions them apart.
- Where it went: mise installs every .NET SDK into one shared folder, `~/.local/share/mise/dotnet-root`, and sets `DOTNET_ROOT` to it. `dotnet --list-sdks` shows all of them; `global.json` (US-1.3) picks one per project.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 3: Turn off telemetry and the banner.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`). Edit `mise.toml` there.

Edit `mise.toml`:

<details>
<summary>Code to type: <code>mise.toml</code></summary>

```toml
[tools]
ruby = "4.0.7"
dotnet = "10.0.402"

[env]
_.file = ".env"
DOTNET_CLI_TELEMETRY_OPTOUT = "1"
DOTNET_NOLOGO = "1"
```

</details>

- Why: the `dotnet` CLI sends usage data to Microsoft by default. `DOTNET_NOLOGO` hides the first-run banner.
- `mise.toml` is committed, so the setting travels with the repo.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 4: Check the install.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`), in a new terminal.

```bash
dotnet --version
dotnet --list-sdks
dotnet --list-runtimes
which dotnet
echo $DOTNET_ROOT
```

- Expect: `10.0.402`; SDK `10.0.402`; runtimes `Microsoft.AspNetCore.App 10.0.13` and `Microsoft.NETCore.App 10.0.13`; paths under `~/.local/share/mise/`.
- Two runtimes: `NETCore.App` is the base library (the JRE). `AspNetCore.App` is the web framework, shipped as a shared framework instead of a dependency. Spring Boot is a dependency in your JAR; ASP.NET Core is not in your NuGet packages at all.
- If `dotnet` is not found: `mise doctor`, check `activated: yes` (US-1.3 of the Rails plan).

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 5: Install the xUnit v3 templates.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`).

```bash
dotnet new install xunit.v3.templates
dotnet new list xunit
```

- Expect: `xUnit.net v3 Test Project` with short name `xunit3`.
- Why: the SDK's built-in `xunit` template is still xUnit v2. v3 is the current major and runs on Microsoft.Testing.Platform.
- Templates are NuGet packages. `dotnet new install` is per user, like adding an initializr starter. It writes to your home folder (`~/.templateengine`), not the repo, so nothing here is committed.
- Why inside the repo: `dotnet` is on PATH only there, because `mise use` (without `-g`) pinned it in the repo's `mise.toml`. In another folder you would get `command not found`.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 6: Learn the CLI map.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`).

```bash
dotnet --help
dotnet new list web
```

| Task | Gradle | .NET |
| --- | --- | --- |
| New project | start.spring.io | `dotnet new webapi` |
| Add dependency | edit `build.gradle.kts` | `dotnet add package <Name>` |
| Restore deps | (part of build) | `dotnet restore` (part of build) |
| Build | `./gradlew build` | `dotnet build` |
| Test | `./gradlew test` | `dotnet test` |
| Run | `./gradlew bootRun` | `dotnet run --project src/TodoApi` |
| Hot reload | devtools | `dotnet watch --project src/TodoApi` |
| Format | `spotlessApply` | `dotnet format` |
| Dependency CVEs | dependency-check | `dotnet package list --vulnerable` |
| Project tools | Gradle plugins | `dotnet tool install` + `dotnet-tools.json` |

</details>

---

<details>
<summary><span style="font-weight: bold; color:rgb(153, 184, 255);"><b>Step 7: C# drill (20 minutes).</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`). The file is `playground/drill.cs`.

.NET 10 runs a single `.cs` file with no project: `dotnet run drill.cs`. Create `playground/drill.cs` and build it up one block at a time from the full file below. Before each run, predict the output.

- **Not a REPL.** Unlike `irb`, a bare `count.GetType();` prints nothing: it compiles and runs, but the value is thrown away. To see a value, pass it to `Console.WriteLine(...)`. Each block ends with one `Console.WriteLine` that prints one line, starting with the block number.
- **Where code goes.** Blocks 1-9 are top-level statements: add each one below the previous block. Blocks 4, 8 and 9 also need a declaration (`record`, method, `class`) at the **bottom** of the file, after all statements. Type declarations must come after the statements, or the compiler stops with `CS8803`.

| # | Expressions printed → value | Full output line | Java analogue / lesson | Also add at the bottom |
| --- | --- | --- | --- | --- |
| 1 | `count.GetType()` → `System.Int32`; `count % 2 == 0` → `False` | `1: System.Int32 False` | `var` works like Java 10. `int` is an alias for `System.Int32`. `False` because 5 is odd. Booleans print as `True`/`False` | none |
| 2 | `name.ToUpperInvariant()` → `TODO`; raw string `"""{"title": "x"}"""` → `{"title": "x"}` | `2: Hi TODO \| {"title": "x"}` | `$"..."` interpolates `{...}`. `"""` is a raw string: quotes inside need no escaping, like a Java text block | none |
| 3 | `description?.Length` → null (prints nothing); `description ?? "none"` → `none`; `description?.Length ?? 0` → `0` | `3:  \| none \| 0` (two spaces after `3:`) | `string?` marks a nullable reference. `?.` returns null instead of throwing; `??` supplies a fallback. `Optional.map(...).orElse(...)` in two operators | none |
| 4 | `a == new TodoItem("Buy milk", false)` → `True`; `b` → its generated `ToString()` | `4: True \| TodoItem { Title = Buy milk, Completed = True }` | A record compares by value, like a Java record. `a with { Completed = true }` copies `a` and changes one property | `record TodoItem(...)` |
| 5 | `tens` (evens times 10) → `20, 40` | `5: [20, 40]` | LINQ `Where`/`Select`/`ToList` = `stream().filter().map().toList()`. On EF Core the same calls become SQL | none |
| 6 | `Label(null)`, `Label(0)`, `Label(500)`, `Label(20)` → `default`, `too small`, `capped`, `ok` | `6: default too small capped ok` | Switch expression with `null`, relational (`< 1`, `> 100`) and discard (`_`) patterns. Java 21 pattern `switch`. `Label` is a local function, so it stays with block 6 | none |
| 7 | `todo.TryGetValue("due_date", out var due)` → `False`; `due is null` → `True` | `7: False True` | `todo["due_date"]` would throw `KeyNotFoundException`. `TryGetValue` returns `false` and sets `due` to null. `out var` declares the variable inline | none |
| 8 | `total` (awaited result) → `42` | `8: 42` | `async`/`await` on `Task<int>` = `CompletableFuture<Integer>` without callbacks. `await` works at top level in a file app | `static async Task<int> CountAsync()` |
| 9 | `"  ".IsBlank` → `True`; `"x".IsBlank` → `False` | `9: True False` | C# 14 extension members add a property to `string` without subclassing. Java has no equivalent | `static class StringExtensions` |

<details>
<summary>Code to type: <code>playground/drill.cs</code> (full file)</summary>

```csharp
// 1 var and type inference
var count = 5;
Console.WriteLine($"1: {count.GetType()} {count % 2 == 0}");

// 2 string interpolation and raw strings
var name = "todo";
Console.WriteLine($"2: Hi {name.ToUpperInvariant()} | {"""{"title": "x"}"""}");

// 3 nullable reference types and null operators
string? description = null;
Console.WriteLine($"3: {description?.Length} | {description ?? "none"} | {description?.Length ?? 0}");

// 4 records: value equality and with-expressions
var a = new TodoItem("Buy milk", false);
var b = a with { Completed = true };
Console.WriteLine($"4: {a == new TodoItem("Buy milk", false)} | {b}");

// 5 LINQ = streams
int[] numbers = [1, 2, 3, 4];
var tens = numbers.Where(n => n % 2 == 0).Select(n => n * 10).ToList();
Console.WriteLine($"5: [{string.Join(", ", tens)}]");

// 6 pattern matching and switch expressions
static string Label(int? size) => size switch
{
    null => "default",
    < 1 => "too small",
    > 100 => "capped",
    _ => "ok",
};
Console.WriteLine($"6: {Label(null)} {Label(0)} {Label(500)} {Label(20)}");

// 7 dictionaries: indexer throws, TryGetValue does not
var todo = new Dictionary<string, object?> { ["title"] = "Buy milk" };
Console.WriteLine($"7: {todo.TryGetValue("due_date", out var due)} {due is null}");

// 8 async/await
var total = await CountAsync();
Console.WriteLine($"8: {total}");

// 9 extension members (C# 14)
Console.WriteLine($"9: {"  ".IsBlank} {"x".IsBlank}");

static async Task<int> CountAsync()
{
    await Task.Delay(10);
    return 42;
}

record TodoItem(string Title, bool Completed);

static class StringExtensions
{
    extension(string text)
    {
        public bool IsBlank => string.IsNullOrWhiteSpace(text);
    }
}
```

</details>

```bash
dotnet run playground/drill.cs
```

- The compiler wraps all top-level statements in a generated `Main`. Local functions (`Label`, `CountAsync`) can sit anywhere among them. Types (`record`, `class`) must come last.
- The very first run can sit on `Restore (30s+)`: the SDK creates a hidden `drill.cs.csproj` and downloads its packages into `~/.nuget/packages`. Later runs take 1-2 seconds; a run after an edit recompiles in a few.

Done when you can read `public sealed record CreateTodoRequest([Required, MaxLength(200)] string Title, DateOnly? DueDate);` and name every part: an immutable record type, a primary constructor, two validation attributes (Java annotations), a non-nullable string and a nullable date.

</details>

---

<details>
<summary><span style="font-weight: bold; color: pink;"><b>Checklist</b></span></summary>

- [ ] mise updated; `dotnet --version` shows `10.0.402` from mise
- [ ] `mise.toml` pins `dotnet` and sets the two `DOTNET_*` vars; committed
- [ ] xUnit v3 templates installed
- [ ] Drill done; one note in `LEARNINGS.md`

</details>

---

<details>
<summary><span style="font-weight: bold; color: pink;"><b>Validate</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`).

| # | Command | Expect |
| --- | --- | --- |
| 1 | `dotnet --version` | `10.0.402` |
| 2 | `which dotnet` | a path under `~/.local/share/mise/` |
| 3 | `dotnet --list-runtimes` | `Microsoft.AspNetCore.App 10.0.13`, `Microsoft.NETCore.App 10.0.13` |
| 4 | `command grep -E 'dotnet\|DOTNET' mise.toml` | `dotnet = "10.0.402"`, both `DOTNET_*` lines (after US-2.1 also the `ConnectionStrings__Todo` line: it contains `todo_dotnet`) |
| 5 | `echo $DOTNET_CLI_TELEMETRY_OPTOUT` | `1` |
| 6 | `dotnet new list xunit3` | `xUnit.net v3 Test Project` |
| 7 | `dotnet run playground/drill.cs` | nine lines, `1:` to `9:`, as in the table |

</details>

</details>

---

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-1.3 .NET skeleton</b></span></summary>

*As a developer, I want an ASP.NET Core 10 API with a test project, pinned SDK, central package versions and strict analyzers, so I have a known-good baseline.*

Target layout:

```text
dotnet-todo/
├── TodoApi.slnx              # solution: lists the projects
├── global.json               # SDK pin + test runner
├── Directory.Build.props     # settings for every project
├── Directory.Packages.props  # every package version
├── dotnet-tools.json         # project-local CLI tools (dotnet-ef)
├── .editorconfig  .gitignore
├── src/TodoApi/              # the web app
└── tests/TodoApi.Tests/      # xUnit v3 tests
```

- `src/` and `tests/` folders are the .NET convention, like `src/main` and `src/test`, but as separate projects.

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 1: Pin the SDK.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`). The first command creates `dotnet-todo/` and moves into it. **Stay in `dotnet-todo/` for Steps 2 to 11.**

```bash
mkdir dotnet-todo && cd dotnet-todo
dotnet new globaljson --sdk-version 10.0.400 --roll-forward latestFeature
cat global.json
```

- Expect: `"version": "10.0.400"` and `"rollForward": "latestFeature"`.
- Why: `global.json` is the Gradle toolchain block. It decides which installed SDK runs in this folder.
- `10.0.400` + `latestFeature` means "any 10.0 SDK from 10.0.400 up, newest wins". mise pins the exact `10.0.402`; `global.json` only stops an older or a .NET 11 SDK from building this project.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 2: Create the solution and the two projects.</b></span></summary>

**Run in:** `dotnet-todo/`. Check with `pwd`: it must end in `/dotnet-todo`. Run from repo root and the solution lands in the wrong folder.

```bash
dotnet new sln -n TodoApi
dotnet new webapi -o src/TodoApi --no-https
dotnet new xunit3 -o tests/TodoApi.Tests -f net10.0
dotnet sln add src/TodoApi tests/TodoApi.Tests
cat TodoApi.slnx global.json
```

- `dotnet new sln` writes `TodoApi.slnx`, the XML solution format. The old `.sln` format is legacy.
- `webapi`: a Minimal API project with OpenAPI. `--no-https`: local HTTP only, like the other two apps. No `--use-controllers`: Minimal APIs are the current default.
- `-f net10.0`: the xUnit template still defaults to `net8.0`.
- Expect: `global.json` gained a `"test": { "runner": "Microsoft.Testing.Platform" }` block. The xUnit template added it. It tells `dotnet test` to use the new test platform instead of the legacy VSTest.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 3: Share settings with <code>Directory.Build.props</code>.</b></span></summary>

**Run in:** `dotnet-todo/`. Create the file next to `TodoApi.slnx`.

Create `Directory.Build.props` in `dotnet-todo/`:

<details>
<summary>Code to type: <code>dotnet-todo/Directory.Build.props</code></summary>

```xml
<Project>
  <PropertyGroup>
    <TargetFramework>net10.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
    <TreatWarningsAsErrors>true</TreatWarningsAsErrors>
    <AnalysisLevel>latest-recommended</AnalysisLevel>
    <EnforceCodeStyleInBuild>true</EnforceCodeStyleInBuild>
    <GenerateDocumentationFile>true</GenerateDocumentationFile>
    <NoWarn>$(NoWarn);CS1591</NoWarn>
  </PropertyGroup>
</Project>
```

</details>

- MSBuild imports this file into every project in this folder and below. Like `allprojects { }` in Gradle.
- `Nullable`: the compiler tracks `null`. `ImplicitUsings`: common `using` lines (`System`, `System.Linq`, ...) are added for you.
- `TreatWarningsAsErrors`: a nullable warning, an analyzer finding or a vulnerable package fails the build.
- `AnalysisLevel latest-recommended`: turns on the recommended set of Roslyn code-quality analyzers. Your Checkstyle/Error Prone.
- `EnforceCodeStyleInBuild`: `.editorconfig` style rules set to `warning` run during `dotnet build`, not only in the IDE.
- `GenerateDocumentationFile`: the compiler needs it to report unused `using` lines (`IDE0005`, Step 5) during a build. Without it the build fails with `EnableGenerateDocumentationFile`. Bonus: in .NET 10 your `///` comments flow into the OpenAPI document.
- `NoWarn CS1591`: the doc file would otherwise demand a `///` comment on every public member.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 4: Central package versions.</b></span></summary>

**Run in:** `dotnet-todo/`. All paths below are relative to it.

Create `Directory.Packages.props`, then remove `TargetFramework`, `Nullable`, `ImplicitUsings` and every `Version="..."` from both `.csproj` files.

<details>
<summary>Code to type: <code>dotnet-todo/Directory.Packages.props</code></summary>

```xml
<Project>
  <PropertyGroup>
    <ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>
  </PropertyGroup>
  <ItemGroup>
    <PackageVersion Include="Microsoft.AspNetCore.Mvc.Testing" Version="10.0.13" />
    <PackageVersion Include="Microsoft.AspNetCore.OpenApi" Version="10.0.13" />
    <PackageVersion Include="Scalar.AspNetCore" Version="2.18.0" />
    <PackageVersion Include="xunit.v3.mtp-v2" Version="4.0.2" />
  </ItemGroup>
</Project>
```

</details>

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/TodoApi.csproj</code></summary>

```xml
<Project Sdk="Microsoft.NET.Sdk.Web">

  <ItemGroup>
    <PackageReference Include="Microsoft.AspNetCore.OpenApi" />
    <PackageReference Include="Scalar.AspNetCore" />
  </ItemGroup>

</Project>
```

</details>

<details>
<summary>Code to type: <code>dotnet-todo/tests/TodoApi.Tests/TodoApi.Tests.csproj</code></summary>

```xml
<Project Sdk="Microsoft.NET.Sdk">

  <PropertyGroup>
    <OutputType>Exe</OutputType>
  </PropertyGroup>

  <ItemGroup>
    <Content Include="xunit.runner.json" CopyToOutputDirectory="PreserveNewest" />
  </ItemGroup>

  <ItemGroup>
    <Using Include="Xunit" />
  </ItemGroup>

  <ItemGroup>
    <PackageReference Include="Microsoft.AspNetCore.Mvc.Testing" />
    <PackageReference Include="xunit.v3.mtp-v2" />
  </ItemGroup>

  <ItemGroup>
    <ProjectReference Include="..\..\src\TodoApi\TodoApi.csproj" />
  </ItemGroup>

</Project>
```

</details>

- Central Package Management is the version catalog: `PackageReference` names the package, `PackageVersion` owns the version. A `Version` left in a `.csproj` fails restore with `NU1008`.
- From now on, `dotnet add package X` writes the version into `Directory.Packages.props` for you.
- `Sdk="Microsoft.NET.Sdk.Web"`: the web SDK references ASP.NET Core and adds `appsettings.json` handling. The test project uses the plain SDK.
- `OutputType Exe`: an xUnit v3 test project is a runnable program. `dotnet test` starts it.
- `Microsoft.AspNetCore.Mvc.Testing` brings `WebApplicationFactory`. The name says MVC, but it tests Minimal APIs too.
- `ProjectReference` is `testImplementation(project(":app"))`.
- `Scalar.AspNetCore` is new: the docs UI (Step 6).

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 5: Add <code>.editorconfig</code>, <code>.gitignore</code> and the tool manifest.</b></span></summary>

**Run in:** `dotnet-todo/`. All three files land next to `TodoApi.slnx`.

```bash
dotnet new gitignore
dotnet new tool-manifest
dotnet tool install dotnet-ef --version 10.0.13
cat dotnet-tools.json
```

- `.gitignore`: ignores `bin/`, `obj/`, IDE files.
- `dotnet-tools.json`: project-local CLI tools with pinned versions, like a Gradle plugin version. Teammates run `dotnet tool restore`. `dotnet-ef` (migrations) is used in Phase 2.
- Skip `dotnet new editorconfig`: its 390 lines include settings that make `dotnet format` fail on a fresh project. Write this short one instead.

<details>
<summary>Code to type: <code>dotnet-todo/.editorconfig</code></summary>

```ini
root = true

[*]
charset = utf-8
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true

[*.{json,csproj,props,slnx,xml}]
indent_size = 2

[*.cs]
dotnet_sort_system_directives_first = true
dotnet_separate_import_directive_groups = false
csharp_style_namespace_declarations = file_scoped
csharp_style_var_elsewhere = true
csharp_style_var_for_built_in_types = true
csharp_style_var_when_type_is_apparent = true

# Style rules that fail the build (EnforceCodeStyleInBuild)
dotnet_diagnostic.IDE0005.severity = warning # unnecessary using
dotnet_diagnostic.IDE0161.severity = warning # file-scoped namespace

# EF Core writes these files; do not lint them
[**/Migrations/*.cs]
generated_code = true
```

</details>

- `root = true`: stop looking in parent folders.
- `IDE0005` and `IDE0161` at `warning` + `TreatWarningsAsErrors` = build error. Pick a few rules to enforce; the rest stay IDE hints.
- The `Migrations` section is ready for Phase 2: EF Core generates code in its own style.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 6: Write <code>Program.cs</code>.</b></span></summary>

**Run in:** `dotnet-todo/`. All paths below are relative to it.

Replace the template's weather-forecast sample. Delete `src/TodoApi/TodoApi.http` and `tests/TodoApi.Tests/UnitTest1.cs` too.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Program.cs</code></summary>

```csharp
using Scalar.AspNetCore;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddProblemDetails();
builder.Services.AddOpenApi();
builder.Services.AddHealthChecks();

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    app.MapScalarApiReference();
}

app.MapHealthChecks("/health");

app.Run();
```

</details>

Read it in two halves:

- **Before `Build()`: services.** `builder.Services` is the DI container. Each `Add...` registers beans. Like `@Configuration` classes, but in one place and in code.
- **After `Build()`: the request pipeline.** Each `Use...` adds middleware (a servlet filter); each `Map...` adds an endpoint (a `@GetMapping`). Order matters: middleware runs top to bottom.
- `AddProblemDetails()` + `UseExceptionHandler()` + `UseStatusCodePages()`: unhandled exceptions and empty error responses (404, 405, ...) become RFC 9457 `application/problem+json`. Spring gives you this with `spring.mvc.problemdetails.enabled`.
- `AddOpenApi()` / `MapOpenApi()`: OpenAPI 3.1 JSON at `/openapi/v1.json`. `MapScalarApiReference()`: interactive docs at `/scalar`. Both only in `Development`: no public docs by default.
- `AddHealthChecks()` / `MapHealthChecks("/health")`: Actuator health. Phase 2 adds a database check.
- `Program.cs` has no class and no `Main`: top-level statements. The compiler generates `public partial class Program`, which the tests use.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 7: Set the port.</b></span></summary>

**Run in:** `dotnet-todo/`. All paths below are relative to it.

In `src/TodoApi/Properties/launchSettings.json`, set `applicationUrl` to `http://localhost:5080`.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Properties/launchSettings.json</code></summary>

```json
{
  "$schema": "https://json.schemastore.org/launchsettings.json",
  "profiles": {
    "http": {
      "commandName": "Project",
      "dotnetRunMessages": true,
      "launchBrowser": false,
      "applicationUrl": "http://localhost:5080",
      "environmentVariables": {
        "ASPNETCORE_ENVIRONMENT": "Development"
      }
    }
  }
}
```

</details>

- `launchSettings.json` is for local runs only (`dotnet run`, the IDE). It sets the port and the environment. It is not used in production, where you set `ASPNETCORE_URLS` / `ASPNETCORE_HTTP_PORTS` instead.
- `ASPNETCORE_ENVIRONMENT=Development` is `spring.profiles.active=dev`. It loads `appsettings.Development.json` on top of `appsettings.json`.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 8: Write a smoke test by hand.</b></span></summary>

**Run in:** `dotnet-todo/`. `dotnet test` finds `TodoApi.slnx` there and runs every test project in it.

<details>
<summary>Code to type: <code>dotnet-todo/tests/TodoApi.Tests/HealthTests.cs</code></summary>

```csharp
using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;

namespace TodoApi.Tests;

public sealed class HealthTests(WebApplicationFactory<Program> factory)
    : IClassFixture<WebApplicationFactory<Program>>
{
    [Fact]
    public async Task HealthReturnsHealthy()
    {
        var cancellationToken = TestContext.Current.CancellationToken;
        using var client = factory.CreateClient();

        var response = await client.GetAsync(new Uri("/health", UriKind.Relative), cancellationToken);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("Healthy", await response.Content.ReadAsStringAsync(cancellationToken));
    }
}
```

</details>

```bash
dotnet test
```

- Expect: `Test run summary: Passed!`, `total: 1`, `succeeded: 1`.
- `WebApplicationFactory<Program>` boots your whole app in memory with a test server. `CreateClient()` gives an `HttpClient` wired to it. `@SpringBootTest` + MockMvc in one.
- `IClassFixture<T>`: xUnit creates one factory and shares it with every test in this class. Like a `static @BeforeAll` resource. xUnit creates a new `HealthTests` instance per test.
- `(WebApplicationFactory<Program> factory)` after the class name is a primary constructor: constructor injection with no field boilerplate.
- `TestContext.Current.CancellationToken`: xUnit v3 cancels it when a test times out or the run is aborted. Its analyzer warns when you do not pass it on, and warnings fail this build.
- `new Uri(..., UriKind.Relative)`: the `CA2234` analyzer prefers `Uri` over a string URL.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 9: Build, format and audit.</b></span></summary>

**Run in:** `dotnet-todo/`. Each command works on `TodoApi.slnx`.

```bash
dotnet build
dotnet format --verify-no-changes
dotnet package list --vulnerable --include-transitive
```

- Expect: `Build succeeded` with `0 Warning(s)`; `dotnet format` prints nothing and exits 0; both projects `has no vulnerable packages`.
- If `dotnet format` reports issues: run `dotnet format` (no flag) to fix them, then check again. Same as `spotlessApply` then `spotlessCheck`.
- NuGet Audit already runs on every restore and reports known CVEs as `NU1901`-`NU1904` warnings, so with `TreatWarningsAsErrors` a vulnerable package fails the build. The `list` command shows the full report.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 10: Boot the server.</b></span></summary>

**Run in:** `dotnet-todo/`, terminal 1.

```bash
dotnet run --project src/TodoApi
```

In a second terminal (any folder):

```bash
curl -i localhost:5080/health
curl -s localhost:5080/openapi/v1.json | head -5
curl -i localhost:5080/nope
open http://localhost:5080/scalar
```

- Expect: `200` with body `Healthy`; JSON starting with `"openapi": "3.1.1"`; `404` with `Content-Type: application/problem+json`; the Scalar page with no endpoints yet.
- Read the 404 body: `type`, `title`, `status` and a `traceId`. Phase 3 trims it to the shared contract.
- Stop with `Ctrl-C`. For edit-and-reload while you code, use `dotnet watch --project src/TodoApi`.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 11: Walk the tree.</b></span></summary>

**Run in:** `dotnet-todo/`. Paths in the table are relative to it.

Open each path. Write one line per row in `LEARNINGS.md`.

| .NET path | Spring counterpart |
| --- | --- |
| `TodoApi.slnx` | `settings.gradle.kts` |
| `global.json` | Gradle toolchain block + `.sdkmanrc` |
| `Directory.Build.props` | `allprojects { }` / convention plugin |
| `Directory.Packages.props` | `gradle/libs.versions.toml` |
| `dotnet-tools.json` | Gradle plugin versions |
| `src/TodoApi/TodoApi.csproj` | `build.gradle.kts` of the app module |
| `src/TodoApi/Program.cs` | `@SpringBootApplication` class + `@Configuration` classes |
| `src/TodoApi/appsettings*.json` | `application.yaml`, `application-dev.yaml` |
| `src/TodoApi/Properties/launchSettings.json` | IDE run configuration |
| `tests/TodoApi.Tests/` | `src/test/java/` (as its own project) |
| `bin/`, `obj/` | `build/` |

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 12: Commit.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`). `cd ..` from `dotnet-todo/`.

```bash
git status
git add mise.toml db/init playground/drill.cs dotnet-todo
git commit -m "US-1.3 .NET skeleton"
```

- Check: `git status` shows no `bin/` or `obj/` folders. If it does, the `.gitignore` from Step 5 is missing.

**Checklist**

- [ ] SDK pinned in `global.json`; solution with `src/TodoApi` and `tests/TodoApi.Tests`
- [ ] `Directory.Build.props` with nullable, warnings as errors and analyzers; versions only in `Directory.Packages.props`
- [ ] `dotnet-ef` in the tool manifest; short `.editorconfig`
- [ ] Smoke test passes; `GET /health` returns 200 on port 5080; OpenAPI and Scalar load
- [ ] Build, format and audit clean
- [ ] Folder map written in `LEARNINGS.md`

**What to notice:** the template gave you less than `rails new` and about as much as start.spring.io. Everything strict (warnings as errors, analyzers, central versions) was your choice, made in two small XML files. The web framework is not a NuGet package at all: it ships with the runtime.

</details>

---

<details>
<summary><span style="font-weight: bold; color: pink;"><b>Validate</b></span></summary>

**Run in:** `dotnet-todo/`, except row 11 (repo root).

| # | Command | Expect |
| --- | --- | --- |
| 1 | `cat global.json` | `"version": "10.0.400"`, `latestFeature`, `Microsoft.Testing.Platform` runner |
| 2 | `command grep -c Version= src/TodoApi/TodoApi.csproj tests/TodoApi.Tests/TodoApi.Tests.csproj` | `0` for both: versions live only in `Directory.Packages.props` |
| 3 | `dotnet tool list` | `dotnet-ef 10.0.13` |
| 4 | `dotnet build` | `Build succeeded`, `0 Warning(s)`, `0 Error(s)` |
| 5 | `dotnet test` | `Passed!`, `total: 1` |
| 6 | `dotnet format --verify-no-changes; echo $?` | `0` |
| 7 | `dotnet package list --vulnerable --include-transitive` | `has no vulnerable packages` for both projects |
| 8 | `dotnet run --project src/TodoApi` (terminal 1) | `Now listening on: http://localhost:5080`, `Hosting environment: Development` |
| 9 | `curl -s localhost:5080/health` (terminal 2) | `Healthy` |
| 10 | `curl -si localhost:5080/nope \| command grep Content-Type` | `application/problem+json` |
| 11 | `git ls-files dotnet-todo \| command grep -cE '/(bin\|obj)/'` (repo root, after the commit) | `0` |

</details>

</details>

</details>

---

<details>
<summary><span style="font-weight: bold; color: cyan;"><b>Phase 2: Todo model and migration</b></span></summary>

Phase 2 creates the `todos` table through an EF Core migration and maps it to an entity, before any endpoint exists.

**Table `todos`:** `id` bigint identity PK · `title` varchar(200) not null · `description` text null · `completed` boolean not null default false · `due_date` date null · `created_at` / `updated_at` timestamptz not null. Same as the Spring and Rails tables.

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-2.1 Create the todos table</b></span></summary>

*As a developer, I want the schema defined in versioned migrations so every environment builds the same database.*

**What this story does, in plain terms.** By the end, Postgres has a `todos` table, and the app knows how to talk to it. Nothing serves todos over HTTP yet; that is Phase 3. Four moves:

1. **Tell the app where the database is** (Step 1): one line in `mise.toml` gives the app a connection string built from your `.env`.
2. **Add the database libraries** (Steps 2-3): EF Core (the .NET Hibernate) and the Postgres driver, then fix a version clash they cause.
3. **Describe the table in C#** (Steps 4-7): a `Todo` class (the row), a configuration class (column rules), a `TodoDbContext` (the connection + repository), and one registration line in `Program.cs`.
4. **Let EF Core write and run the SQL** (Steps 8-11): `dotnet ef` compares your C# to the database, writes a migration file, applies it, and can undo it.

Flow: `Todo.cs` + `TodoConfiguration.cs` → `dotnet ef migrations add` → migration file → `dotnet ef database update` → `todos` table in `todo_dotnet`.

- [x] Connection string comes from `.env` through mise; no password in any committed file
- [x] EF Core packages added; every project resolves the same EF Core version; 0 build warnings
- [x] `Todo` entity, `TodoConfiguration`, `TodoDbContext` registered with Npgsql and snake_case naming
- [x] Migration `CreateTodos` generated, read, applied; `\d todos` matches the spec
- [x] Rollback and re-apply practiced

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 1: Give the app its connection string.</b></span></summary>

**Run in:** repo root (`learn-ruby-thru-spring/`). Edit `mise.toml` there.

Add one line under `[env]`:

<details>
<summary>Code to type: <code>mise.toml</code></summary>

```toml
[tools]
dotnet = "10.0.402"
ruby = "4.0.7"

[env]
_.file = ".env"
DOTNET_CLI_TELEMETRY_OPTOUT = "1"
DOTNET_NOLOGO = "1"
ConnectionStrings__Todo = "Host=localhost;Port={{env.POSTGRES_PORT}};Database=todo_dotnet;Username={{env.POSTGRES_USER}};Password={{env.POSTGRES_PASSWORD}}"
```

</details>

```bash
echo $ConnectionStrings__Todo
```

- Expect: `Host=localhost;Port=5423;Database=todo_dotnet;Username=todo;Password=...` with your real password. Press Enter once first if it is empty: mise reloads env at each prompt.
- `{{env.POSTGRES_PORT}}` is a mise template. It reads values that `_.file = ".env"` loaded on the line above, so the password stays only in `.env` (gitignored).
- `__` (two underscores) in an environment variable name means `:` in .NET configuration. `ConnectionStrings__Todo` becomes the key `ConnectionStrings:Todo`, which `GetConnectionString("Todo")` reads. Spring's relaxed binding does the same with `SPRING_DATASOURCE_URL`.
- Why not `appsettings.Development.json`: it is committed, and a connection string with a password would go with it. .NET layers configuration like Spring: `appsettings.json`, then `appsettings.{Environment}.json`, then environment variables. Later sources win.
- The format is a Npgsql connection string (`Key=Value;`), not a JDBC URL.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 2: Add the EF Core packages.</b></span></summary>

**Run in:** `dotnet-todo/`.

```bash
dotnet add src/TodoApi package Npgsql.EntityFrameworkCore.PostgreSQL
dotnet add src/TodoApi package EFCore.NamingConventions
dotnet add src/TodoApi package Microsoft.EntityFrameworkCore.Design
git diff Directory.Packages.props src/TodoApi/TodoApi.csproj
```

- Expect: each command prints `PackageReference for package '...' added to '.../TodoApi.csproj' and PackageVersion added to central package management file`. `Directory.Packages.props` gains `EFCore.NamingConventions 10.0.1`, `Microsoft.EntityFrameworkCore.Design 10.0.13` and `Npgsql.EntityFrameworkCore.PostgreSQL 10.0.3`. The `.csproj` gets the three references with no versions.

| Package | Role | Spring counterpart |
| --- | --- | --- |
| `Npgsql.EntityFrameworkCore.PostgreSQL` | EF Core provider for Postgres; brings EF Core and the Npgsql driver | `spring-boot-starter-data-jpa` + `org.postgresql:postgresql` |
| `EFCore.NamingConventions` | Maps `DueDate` to column `due_date` | Hibernate's `CamelCaseToUnderscoresNamingStrategy` (Spring's default) |
| `Microsoft.EntityFrameworkCore.Design` | Design-time code that `dotnet ef` needs to create migrations | Flyway Gradle plugin |

- `Design` got `<PrivateAssets>all</PrivateAssets>` automatically. It is used to build migrations, not at run time, and does not flow to projects that reference `TodoApi`. Like a Gradle `compileOnly`.
- `git add` may warn `CRLF will be replaced by LF` for `TodoApi.csproj`: `dotnet add package` writes Windows line endings on the lines it adds. Harmless, because your git setting `core.autocrlf=input` stores LF. To clean the file on disk too: `sed -i '' $'s/\r$//' src/TodoApi/TodoApi.csproj`.
- `EFCore.NamingConventions` is a community package, not from Microsoft. It is widely used with Npgsql; its major version follows EF Core's.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 3: Fix the EF Core version conflict.</b></span></summary>

**Run in:** `dotnet-todo/`.

```bash
dotnet build
```

- Expect: `Build succeeded` **with 2 warnings**, `MSB3277: Found conflicts between different versions of "Microsoft.EntityFrameworkCore"`, in `TodoApi.Tests`.
- Why: Npgsql 10.0.3 depends on EF Core `10.0.4` (the oldest version it supports). `Design` 10.0.13 depends on EF Core `10.0.13`. `TodoApi` gets 10.0.13. The test project does not see `Design` (private assets), so it gets 10.0.4. Two versions of one library in one test run is the classic Maven "dependency mediation" problem.
- Why the build did not fail: `TreatWarningsAsErrors` covers compiler warnings (`CS...`, `CA...`, `IDE...`). `MSB...` warnings come from MSBuild and need their own switch.

Fix both in two files:

<details>
<summary>Code to type: <code>dotnet-todo/Directory.Packages.props</code> (full file)</summary>

```xml
<Project>
  <PropertyGroup>
    <ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>
    <CentralPackageTransitivePinningEnabled>true</CentralPackageTransitivePinningEnabled>
  </PropertyGroup>
  <ItemGroup>
    <PackageVersion Include="EFCore.NamingConventions" Version="10.0.1" />
    <PackageVersion Include="Microsoft.AspNetCore.Mvc.Testing" Version="10.0.13" />
    <PackageVersion Include="Microsoft.AspNetCore.OpenApi" Version="10.0.13" />
    <PackageVersion Include="Microsoft.EntityFrameworkCore.Design" Version="10.0.13" />
    <PackageVersion Include="Npgsql.EntityFrameworkCore.PostgreSQL" Version="10.0.3" />
    <PackageVersion Include="Scalar.AspNetCore" Version="2.18.0" />
    <PackageVersion Include="xunit.v3.mtp-v2" Version="4.0.2" />
  </ItemGroup>
  <!-- Transitive pins: Npgsql 10.0.3 asks for EF Core 10.0.4; lift it to the EF Core version above -->
  <ItemGroup>
    <PackageVersion Include="Microsoft.EntityFrameworkCore" Version="10.0.13" />
    <PackageVersion Include="Microsoft.EntityFrameworkCore.Relational" Version="10.0.13" />
  </ItemGroup>
</Project>
```

</details>

<details>
<summary>Code to type: <code>dotnet-todo/Directory.Build.props</code> (full file)</summary>

```xml
<Project>
  <PropertyGroup>
    <TargetFramework>net10.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
    <TreatWarningsAsErrors>true</TreatWarningsAsErrors>
    <AnalysisLevel>latest-recommended</AnalysisLevel>
    <EnforceCodeStyleInBuild>true</EnforceCodeStyleInBuild>
    <GenerateDocumentationFile>true</GenerateDocumentationFile>
    <MSBuildTreatWarningsAsErrors>true</MSBuildTreatWarningsAsErrors>
    <NoWarn>$(NoWarn);CS1591</NoWarn>
  </PropertyGroup>
</Project>
```

</details>

```bash
dotnet build
dotnet package list --include-transitive --project src/TodoApi | command grep EntityFrameworkCore
```

- Expect: `0 Warning(s)`; every `Microsoft.EntityFrameworkCore*` line shows `10.0.13`. The `Npgsql.EntityFrameworkCore.PostgreSQL` line also matches the `grep` and correctly stays `10.0.3`: that is the provider's own version.
- `CentralPackageTransitivePinningEnabled`: a `PackageVersion` now also pins packages you only get transitively. The two pins are never referenced directly; they only raise the version. Gradle's `constraints { }` block or Maven's `<dependencyManagement>`.
- `MSBuildTreatWarningsAsErrors`: the next version conflict fails the build instead of scrolling past. Checked: with the pins removed, the build fails with `error MSB3277`.
- When you upgrade EF Core later, change all three `10.0.13` lines together.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 4: Write the entity.</b></span></summary>

**Run in:** `dotnet-todo/`. Create the folder `src/TodoApi/Todos/`.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Todos/Todo.cs</code></summary>

```csharp
namespace TodoApi.Todos;

public sealed class Todo
{
    public long Id { get; set; }

    public required string Title { get; set; }

    public string? Description { get; set; }

    public bool Completed { get; set; }

    public DateOnly? DueDate { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public DateTimeOffset UpdatedAt { get; set; }
}
```

</details>

- A plain class with properties. No annotations: EF Core reads the model from names and types, plus a configuration class (Step 5).
- `{ get; set; }` is an auto-property: field, getter and setter in one line. Lombok `@Getter @Setter` as a language feature.
- `Id` of type `long` is the key by convention, like `@Id`. Npgsql makes it `GENERATED BY DEFAULT AS IDENTITY`, same as `@GeneratedValue(strategy = IDENTITY)`.
- Nullability decides `NOT NULL`: `string` → not null, `string?` → null; `bool` → not null; `DateOnly?` → null. In Java you need `@Column(nullable = false)`.
- `required`: every `new Todo { ... }` must set `Title`, or the code does not compile.
- `DateOnly` maps to `date`. `DateTimeOffset` maps to `timestamp with time zone`; Npgsql only accepts UTC values for it, so nothing stores a local time by accident.
- `sealed`: no subclasses, like `final`. A good default for classes not designed for inheritance.
- Folder `Todos/` is a feature folder. Endpoints and DTOs for todos land here in Phase 3. Spring's `todos/` package, without the layer sub-packages.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 5: Configure the columns.</b></span></summary>

**Run in:** `dotnet-todo/`.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Todos/TodoConfiguration.cs</code></summary>

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace TodoApi.Todos;

public sealed class TodoConfiguration : IEntityTypeConfiguration<Todo>
{
    public void Configure(EntityTypeBuilder<Todo> builder)
    {
        builder.Property(todo => todo.Title).HasMaxLength(200);
        builder.Property(todo => todo.Completed).HasDefaultValue(false);
    }
}
```

</details>

- Fluent API: mapping in code, next to the entity but not on it. `HasMaxLength(200)` is `@Column(length = 200)`; `HasDefaultValue(false)` writes `DEFAULT FALSE` in the table.
- `todo => todo.Title` is a lambda EF reads as an expression, not a function it calls. Rename `Title` and the compiler updates this line. Spring's string column names cannot do that.
- `[MaxLength(200)]` on the property also works. Fluent configuration keeps the entity a plain class and keeps all mapping in one place.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 6: Write the DbContext.</b></span></summary>

**Run in:** `dotnet-todo/`. Create the folder `src/TodoApi/Data/`.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Data/TodoDbContext.cs</code></summary>

```csharp
using Microsoft.EntityFrameworkCore;
using TodoApi.Todos;

namespace TodoApi.Data;

public sealed class TodoDbContext(DbContextOptions<TodoDbContext> options) : DbContext(options)
{
    public DbSet<Todo> Todos => Set<Todo>();

    protected override void OnModelCreating(ModelBuilder modelBuilder) =>
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(TodoDbContext).Assembly);
}
```

</details>

- `DbContext` is the `EntityManager` plus the transaction for one request. `DbSet<Todo>` is the `TodoRepository`: you query it with LINQ and add to it. No repository interface to write.
- `(DbContextOptions<TodoDbContext> options)` is a primary constructor; `: DbContext(options)` passes it to the base class. DI supplies the options (Step 7).
- `Todos => Set<Todo>()` is an expression-bodied property: a getter in one line.
- `ApplyConfigurationsFromAssembly` finds every `IEntityTypeConfiguration` in the project, so `TodoConfiguration` is picked up with no registration. Like component scanning.
- Table name `todos` comes from the `Todos` property name, lower-cased by the naming convention.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 7: Register the DbContext.</b></span></summary>

**Run in:** `dotnet-todo/`.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Program.cs</code> (full file)</summary>

```csharp
using Microsoft.EntityFrameworkCore;
using Scalar.AspNetCore;
using TodoApi.Data;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddDbContext<TodoDbContext>(options =>
    options
        .UseNpgsql(builder.Configuration.GetConnectionString("Todo"))
        .UseSnakeCaseNamingConvention());

builder.Services.AddProblemDetails();
builder.Services.AddOpenApi();
builder.Services.AddHealthChecks();

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    app.MapScalarApiReference();
}

app.MapHealthChecks("/health");

app.Run();
```

</details>

```bash
dotnet build
dotnet test
```

- Expect: `0 Warning(s)`; the health test still passes.
- `AddDbContext` registers `TodoDbContext` as **scoped**: one instance per HTTP request, disposed at the end. Same lifetime as a JPA `EntityManager` bound to a request. Never make it a singleton: it is not thread-safe.
- `GetConnectionString("Todo")` reads `ConnectionStrings:Todo` from Step 1.
- `UseSnakeCaseNamingConvention()` turns `DueDate` into `due_date`, `CreatedAt` into `created_at`, and the primary key name into `pk_todos`.
- The health test still passes without a database: nothing opens a connection yet. US-2.2 adds a database health check together with a Testcontainers database for tests.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 8: Generate the migration.</b></span></summary>

**Run in:** `dotnet-todo/`.

```bash
dotnet ef migrations add CreateTodos --project src/TodoApi
ls src/TodoApi/Migrations
```

- Expect: `Done. To undo this action, use 'ef migrations remove'`, and three files: `<timestamp>_CreateTodos.cs`, `<timestamp>_CreateTodos.Designer.cs`, `TodoDbContextModelSnapshot.cs`.
- `--project src/TodoApi`: the project that holds the `DbContext`. Without it, `dotnet ef` looks in the current folder and finds no project.
- `dotnet ef` not found (`Could not execute because the specified command or file was not found`): run `dotnet tool restore` to install the tools listed in `dotnet-tools.json` (US-1.3 Step 5).
- How it works: `dotnet ef` builds the app, runs `Program.cs` up to `Build()` to get the `DbContext`, compares your model with the last snapshot, and writes the difference as C#. Flyway makes you write the SQL; Rails generates a skeleton; EF Core diffs the model.

Open the three files:

| File | What it is |
| --- | --- |
| `<timestamp>_CreateTodos.cs` | `Up()` runs `CreateTable`; `Down()` runs `DropTable`. Reviewable, editable C#. The timestamp prefix orders migrations, like `V1__` |
| `<timestamp>_CreateTodos.Designer.cs` | The model as it was at this migration. Do not edit |
| `TodoDbContextModelSnapshot.cs` | The current model. The next `migrations add` diffs against it. Do not edit |

- The migration code uses block namespaces and `#nullable disable`. It still passes `dotnet format`: the `[**/Migrations/*.cs]` section in `.editorconfig` marks it as generated code.
- Made a mistake before applying? `dotnet ef migrations remove --project src/TodoApi` deletes the last migration and resets the snapshot. Never remove one that was applied or pushed.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 9: Read the SQL.</b></span></summary>

**Run in:** `dotnet-todo/`.

```bash
dotnet ef migrations script --project src/TodoApi
```

- Expect:

```sql
CREATE TABLE todos (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    title character varying(200) NOT NULL,
    description text,
    completed boolean NOT NULL DEFAULT FALSE,
    due_date date,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT pk_todos PRIMARY KEY (id)
);
```

- Compare it with your Flyway `V1__create_todos.sql`. Same table.
- The script also creates `"__EFMigrationsHistory"`: EF's `flyway_schema_history`.
- Production practice: generate this script (or `dotnet ef migrations bundle`), review it, and run it in the deploy pipeline. Do not migrate on app startup.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 10: Apply the migration.</b></span></summary>

**Run in:** `dotnet-todo/`, with the container up (`make compose-up` from repo root).

```bash
dotnet ef migrations list --project src/TodoApi
dotnet ef database update --project src/TodoApi
dotnet ef migrations list --project src/TodoApi
```

- Expect: `<timestamp>_CreateTodos (Pending)`, then `Applying migration '<timestamp>_CreateTodos'` and `Done.`, then the same name without `(Pending)`.
- `database update` also creates the database when it is missing. US-1.1 already created `todo_dotnet`.
- `An error occurred using the connection to database '' on server ''`: empty database and server names mean `ConnectionStrings__Todo` is not set. Redo Step 1 and check with `echo $ConnectionStrings__Todo`.
- `28P01: password authentication failed for user "todo"`: the connection string is set but the password does not match `.env`. Check `POSTGRES_PASSWORD` in `.env`.
- `Connection refused`: the container is down. From repo root: `make compose-up`.

Check the table. **Run in:** repo root (`learn-ruby-thru-spring/`).

```bash
docker exec todo-db sh -c 'psql -U "$POSTGRES_USER" -d todo_dotnet -c "\d todos"'
docker exec todo-db sh -c 'psql -U "$POSTGRES_USER" -d todo_dotnet -c "select * from \"__EFMigrationsHistory\""'
```

- Expect: seven columns matching the Phase 2 spec; `id` default `generated by default as identity`; index `pk_todos`. One history row with product version `10.0.13`.
- Same `id` as Spring (identity), unlike Rails (`bigserial`). Note it in `LEARNINGS.md`.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 11: Practice rollback.</b></span></summary>

**Run in:** `dotnet-todo/`.

```bash
dotnet ef database update 0 --project src/TodoApi
dotnet ef migrations list --project src/TodoApi
dotnet ef database update --project src/TodoApi
```

- Expect: `Reverting migration '<timestamp>_CreateTodos'` and `DROP TABLE todos;`, then the migration shows `(Pending)`, then it applies again.
- `0` means "before the first migration". `dotnet ef database update CreateTodos` moves to a named migration, up or down.
- Rollback runs `Down()`. Flyway Community has no undo; Rails and EF Core both do.
- Rule, same as Rails: roll back only migrations nobody else has applied. Once pushed, fix forward with a new migration.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 12: Check and commit.</b></span></summary>

**Run in:** `dotnet-todo/`.

```bash
dotnet build
dotnet test
dotnet format --verify-no-changes
dotnet package list --vulnerable --include-transitive
```

Then **Run in:** repo root (`learn-ruby-thru-spring/`).

```bash
git add mise.toml dot-net-todo.md dotnet-todo
git commit -m "US-2.1 todos table (EF Core migration)"
```

**Checklist**

- [x] `ConnectionStrings__Todo` set through mise; no password in a committed file
- [x] Packages added; EF Core pinned to 10.0.13 everywhere; MSBuild warnings fail the build
- [x] `Todo`, `TodoConfiguration`, `TodoDbContext` written; `AddDbContext` with Npgsql and snake_case
- [x] Migration generated, read, applied; rollback and re-apply done
- [x] Build, test, format and audit clean

**What to notice:** Flyway runs SQL you wrote; EF Core writes the migration from your C# model and keeps a snapshot to diff against next time. The C# type system carries the schema: `string?` vs `string` is `NULL` vs `NOT NULL`. Spring needs `@Column(nullable = false)` for that.

</details>

---

<details>
<summary><span style="font-weight: bold; color: pink;"><b>Validate</b></span></summary>

**Run in:** `dotnet-todo/` for rows 1-6. Rows 7-9 run in repo root (`learn-ruby-thru-spring/`).

| # | Command | Expect |
| --- | --- | --- |
| 1 | `echo $ConnectionStrings__Todo` | `Host=localhost;Port=5423;Database=todo_dotnet;Username=todo;Password=...` |
| 2 | `dotnet build` | `0 Warning(s)`, `0 Error(s)` |
| 3 | `dotnet package list --include-transitive --project src/TodoApi \| command grep EntityFrameworkCore` | every `Microsoft.EntityFrameworkCore*` line `10.0.13`; `Npgsql.EntityFrameworkCore.PostgreSQL` `10.0.3` |
| 4 | `dotnet test` | `Passed!`, `total: 1` |
| 5 | `dotnet format --verify-no-changes; echo $?` | `0` |
| 6 | `dotnet ef migrations list --project src/TodoApi` | `<timestamp>_CreateTodos`, no `(Pending)` |
| 7 | `docker exec todo-db sh -c 'psql -U "$POSTGRES_USER" -d todo_dotnet -c "\d todos"'` | 7 columns as in the spec, `generated by default as identity`, `pk_todos` |
| 8 | `command grep -c Password= dotnet-todo/src/TodoApi/appsettings*.json` | `...appsettings.Development.json:0` and `...appsettings.json:0` |
| 9 | `git ls-files dotnet-todo/src/TodoApi/Migrations` | the three migration files (after the commit) |

</details>

</details>

---

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-2.2 Map the Todo model</b></span></summary>

*As a developer, I want `created_at` / `updated_at` filled automatically and the schema covered by tests against real Postgres.*

**What this story does, in plain terms.** Four moves:

1. **Automatic timestamps** (Steps 2-5): a small class that EF Core calls before every save. It stamps `created_at` and `updated_at` from a clock you can fake in tests.
2. **Health check sees the database** (Steps 5-6): `/health` answers 503 when Postgres is down.
3. **A test database per run** (Steps 7-8): tests start their own Postgres in Docker (Testcontainers), apply the migrations to it, and never touch `todo_dotnet`.
4. **Tests for the table rules** (Step 9): timestamps, defaults, `NOT NULL`, `varchar(200)`.

No new migration and no endpoints: the table from US-2.1 is unchanged.

- [ ] Timestamps filled by an interceptor using `TimeProvider`
- [ ] `/health` includes a database check
- [ ] Testcontainers fixture shared by all tests; migrations applied to it
- [ ] Persistence tests for defaults, timestamps, `NOT NULL` and max length

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 1: Add the packages.</b></span></summary>

**Run in:** `dotnet-todo/`.

```bash
dotnet add src/TodoApi package Microsoft.Extensions.Diagnostics.HealthChecks.EntityFrameworkCore
dotnet add tests/TodoApi.Tests package Testcontainers.PostgreSql
dotnet add tests/TodoApi.Tests package Microsoft.Extensions.TimeProvider.Testing
sed -i '' $'s/\r$//' src/TodoApi/TodoApi.csproj tests/TodoApi.Tests/TodoApi.Tests.csproj
git diff Directory.Packages.props
```

- Expect: three new `PackageVersion` lines: `Microsoft.Extensions.Diagnostics.HealthChecks.EntityFrameworkCore 10.0.13`, `Microsoft.Extensions.TimeProvider.Testing 10.10.0`, `Testcontainers.PostgreSql 4.16.0`.
- The `sed` line removes the Windows line endings `dotnet add package` writes (US-2.1 Step 2). Running it now avoids the `CRLF will be replaced by LF` warning at commit time.

| Package | Project | Role | Spring counterpart |
| --- | --- | --- | --- |
| `Microsoft.Extensions.Diagnostics.HealthChecks.EntityFrameworkCore` | app | Health check that asks the `DbContext` "can you connect?" | Actuator's `db` health indicator |
| `Testcontainers.PostgreSql` | tests | Starts a throwaway Postgres in Docker for the test run | `org.testcontainers:postgresql` |
| `Microsoft.Extensions.TimeProvider.Testing` | tests | `FakeTimeProvider`: a clock tests can set and move forward | A fixed `Clock` bean / `Clock.offset` |

- `TimeProvider.Testing` is `10.10.0`, not `10.0.13`: it ships from a different Microsoft repo (`dotnet/extensions`) with its own version numbers. It still targets .NET 10.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 2: Mark entities that have timestamps.</b></span></summary>

**Run in:** `dotnet-todo/`.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Data/IHasTimestamps.cs</code></summary>

```csharp
namespace TodoApi.Data;

// Entities with created_at / updated_at columns; AuditTimestampsInterceptor fills them
public interface IHasTimestamps
{
    DateTimeOffset CreatedAt { get; set; }

    DateTimeOffset UpdatedAt { get; set; }
}
```

</details>

- An interface with two properties. Any entity that implements it gets its timestamps filled in Step 3. Today that is only `Todo`.
- C# interfaces can declare properties, not only methods. A class implements them with matching `{ get; set; }` properties.
- Interface names start with `I` in .NET (`IHasTimestamps`, `IDisposable`). A convention, not a rule, but universal.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 3: Write the timestamp interceptor.</b></span></summary>

**Run in:** `dotnet-todo/`.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Data/AuditTimestampsInterceptor.cs</code></summary>

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;

namespace TodoApi.Data;

// Fills CreatedAt and UpdatedAt on save, like Spring Data's AuditingEntityListener
public sealed class AuditTimestampsInterceptor(TimeProvider clock) : SaveChangesInterceptor
{
    public override InterceptionResult<int> SavingChanges(
        DbContextEventData eventData,
        InterceptionResult<int> result)
    {
        Stamp(eventData.Context);
        return base.SavingChanges(eventData, result);
    }

    public override ValueTask<InterceptionResult<int>> SavingChangesAsync(
        DbContextEventData eventData,
        InterceptionResult<int> result,
        CancellationToken cancellationToken = default)
    {
        Stamp(eventData.Context);
        return base.SavingChangesAsync(eventData, result, cancellationToken);
    }

    private void Stamp(DbContext? context)
    {
        if (context is null)
        {
            return;
        }

        // Postgres keeps microseconds; drop .NET's extra 100 ns digit so the saved row and the object agree
        var now = clock.GetUtcNow();
        now = now.AddTicks(-(now.Ticks % 10));

        foreach (var entry in context.ChangeTracker.Entries<IHasTimestamps>())
        {
            if (entry.State == EntityState.Added)
            {
                entry.Entity.CreatedAt = now;
                entry.Entity.UpdatedAt = now;
            }
            else if (entry.State == EntityState.Modified)
            {
                entry.Entity.UpdatedAt = now;
            }
        }
    }
}
```

</details>

- `SaveChangesInterceptor`: EF Core calls `SavingChanges` / `SavingChangesAsync` right before it writes to the database. Like a JPA `@PrePersist` / `@PreUpdate` listener, but for every entity in one place. This is what `AuditingEntityListener` does for you in Spring.
- Two overrides because `SaveChanges()` and `SaveChangesAsync()` are separate paths. The app only uses the async one; the sync override keeps the interceptor correct if anything calls the sync one.
- `ChangeTracker.Entries<IHasTimestamps>()`: every tracked entity that implements the interface, with its state. `Added` will become an `INSERT`, `Modified` an `UPDATE`. Hibernate's persistence context holds the same information.
- `(TimeProvider clock)`: the interceptor asks DI for a clock instead of calling `DateTimeOffset.UtcNow`. Tests swap in a fake clock (Step 7). Spring: inject a `Clock` bean.
- `AddTicks(-(now.Ticks % 10))`: a .NET tick is 100 ns; Postgres stores microseconds (10 ticks). Without this line the object in memory and the saved row differ in the last digit, and a later "did `created_at` change?" check fails. Step 9's tests catch it if you remove the line.
- `return base.SavingChanges(...)`: let EF Core continue with the save. Like `chain.doFilter(...)` in a servlet filter.
- `override`: C# requires the keyword to replace a base method. Java's `@Override` is optional; C# fails to compile without it.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 4: Give `Todo` the interface.</b></span></summary>

**Run in:** `dotnet-todo/`. Two changes: a `using` line at the top and `: IHasTimestamps` after the class name.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Todos/Todo.cs</code> (full file)</summary>

```csharp
using TodoApi.Data;

namespace TodoApi.Todos;

public sealed class Todo : IHasTimestamps
{
    public long Id { get; set; }

    public required string Title { get; set; }

    public string? Description { get; set; }

    public bool Completed { get; set; }

    public DateOnly? DueDate { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public DateTimeOffset UpdatedAt { get; set; }
}
```

</details>

- `: IHasTimestamps` is `implements IHasTimestamps`. C# uses `:` for both `extends` and `implements`; the base class, if any, comes first.
- `using TodoApi.Data;` imports the namespace, like a Java package import.
- No new migration: the columns already exist. Check with `dotnet ef migrations has-pending-model-changes --project src/TodoApi`. Expect: `No changes have been made to the model since the last migration.`

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 5: Register the clock, the interceptor and the database health check.</b></span></summary>

**Run in:** `dotnet-todo/`.

<details>
<summary>Code to type: <code>dotnet-todo/src/TodoApi/Program.cs</code> (full file)</summary>

```csharp
using Microsoft.EntityFrameworkCore;
using Scalar.AspNetCore;
using TodoApi.Data;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddSingleton(TimeProvider.System);
builder.Services.AddSingleton<AuditTimestampsInterceptor>();
builder.Services.AddDbContext<TodoDbContext>((services, options) =>
    options
        .UseNpgsql(builder.Configuration.GetConnectionString("Todo"))
        .UseSnakeCaseNamingConvention()
        .AddInterceptors(services.GetRequiredService<AuditTimestampsInterceptor>()));

builder.Services.AddProblemDetails();
builder.Services.AddOpenApi();
builder.Services.AddHealthChecks().AddDbContextCheck<TodoDbContext>();

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    app.MapScalarApiReference();
}

app.MapHealthChecks("/health");

app.Run();
```

</details>

```bash
dotnet build
```

- Expect: `0 Warning(s)`. Do not run `dotnet test` yet: the old health test now needs a database, and Steps 7-8 give it one.
- `AddSingleton(TimeProvider.System)`: one real clock for the whole app.
- `AddSingleton<AuditTimestampsInterceptor>()`: one interceptor; it holds no per-request state, so a singleton is safe. DI builds it and passes the `TimeProvider` to its constructor.
- `AddDbContext<TodoDbContext>((services, options) => ...)`: this overload also hands you the DI container (`services`), so you can fetch the interceptor and attach it with `AddInterceptors`. Like wiring a listener bean into an `EntityManagerFactory`.
- `AddDbContextCheck<TodoDbContext>()`: `/health` now runs a `SELECT 1` through the `DbContext`. Database down → `Unhealthy` with HTTP 503.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 6: Try the health check by hand.</b></span></summary>

**Run in:** `dotnet-todo/`, terminal 1.

```bash
dotnet run --project src/TodoApi
```

In terminal 2 (any folder):

```bash
curl -s -w ' %{http_code}\n' localhost:5080/health
docker stop todo-db
curl -s -w ' %{http_code}\n' localhost:5080/health
docker start todo-db
```

- Expect: `Healthy 200`, then `Unhealthy 503` (terminal 1 logs `Failed executing DbCommand ... SELECT 1`), then healthy again once the container is back (a few seconds).
- `docker stop` pauses the Spring and Rails apps' database too. `docker start` brings it back with all data.
- Stop the app with `Ctrl-C`.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 7: Write the test fixture.</b></span></summary>

**Run in:** `dotnet-todo/`. Docker must be running.

<details>
<summary>Code to type: <code>dotnet-todo/tests/TodoApi.Tests/TodoApiFactory.cs</code></summary>

```csharp
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Time.Testing;
using Testcontainers.PostgreSql;
using TodoApi.Data;

[assembly: AssemblyFixture(typeof(TodoApi.Tests.TodoApiFactory))]

namespace TodoApi.Tests;

// One Postgres container and one app for the whole test run, like a Spring test context with Testcontainers
public sealed class TodoApiFactory : WebApplicationFactory<Program>, IAsyncLifetime
{
    private readonly PostgreSqlContainer _postgres = new PostgreSqlBuilder("postgres:18-alpine").Build();

    // 7 ticks past a whole microsecond, like a real clock reading; Postgres stores only microseconds
    public FakeTimeProvider Clock { get; } = new(new DateTimeOffset(2026, 10, 1, 9, 0, 0, TimeSpan.Zero).AddTicks(7));

    public async ValueTask InitializeAsync()
    {
        await _postgres.StartAsync();

        await using var scope = Services.CreateAsyncScope();
        await scope.ServiceProvider.GetRequiredService<TodoDbContext>().Database.MigrateAsync();
    }

    public override async ValueTask DisposeAsync()
    {
        await base.DisposeAsync();
        await _postgres.DisposeAsync();
    }

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseSetting("ConnectionStrings:Todo", _postgres.GetConnectionString());
        builder.ConfigureTestServices(services => services.AddSingleton<TimeProvider>(Clock));
    }
}
```

</details>

- `WebApplicationFactory<Program>`: the in-memory app from US-1.3, now subclassed so you can change its setup. Spring: `@SpringBootTest` plus `@DynamicPropertySource`.
- `[assembly: AssemblyFixture(...)]`: xUnit v3 creates **one** `TodoApiFactory` for the whole test run and hands it to any test class that asks for it in its constructor. One container, one app, many test classes. Spring caches the test context the same way.
- `IAsyncLifetime`: `InitializeAsync` runs once before the first test, `DisposeAsync` after the last. `@BeforeAll` / `@AfterAll` that can `await`.
- `InitializeAsync`: start the container, then run the migrations from US-2.1 against it. Same SQL as your dev database, so tests check the real schema. Spring with Flyway does this on context start.
- `ConfigureWebHost` runs while the app is built:
  - `UseSetting("ConnectionStrings:Todo", ...)` points the app at the container. It wins over `ConnectionStrings__Todo` from mise. Checked: with the variable set to a nonexistent host, all tests still pass, so tests never touch `todo_dotnet`. Validate row 3 shows it on your machine.
  - `ConfigureTestServices(...)` replaces the real `TimeProvider` with `Clock`. Spring: `@TestConfiguration` with a `@Primary Clock`.
- `FakeTimeProvider` starts at a fixed time and only moves when a test calls `Advance`. The 7 extra ticks make it a realistic reading, so the microsecond trim in Step 3 is tested.
- `"postgres:18-alpine"`: the same image as `compose.yml`. The first run pulls it.
- `private readonly ... _postgres`: `_camelCase` is the .NET convention for private fields. `readonly` is `final`.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 8: Point the health test at the fixture.</b></span></summary>

**Run in:** `dotnet-todo/`.

<details>
<summary>Code to type: <code>dotnet-todo/tests/TodoApi.Tests/HealthTests.cs</code> (full file)</summary>

```csharp
using System.Net;

namespace TodoApi.Tests;

public sealed class HealthTests(TodoApiFactory factory)
{
    [Fact]
    public async Task HealthReturnsHealthy()
    {
        var cancellationToken = TestContext.Current.CancellationToken;
        using var client = factory.CreateClient();

        var response = await client.GetAsync(new Uri("/health", UriKind.Relative), cancellationToken);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("Healthy", await response.Content.ReadAsStringAsync(cancellationToken));
    }
}
```

</details>

- Two changes from US-1.3: the constructor takes `TodoApiFactory` instead of `WebApplicationFactory<Program>`, and `: IClassFixture<...>` is gone. The assembly fixture replaces the class fixture.
- `using Microsoft.AspNetCore.Mvc.Testing;` is gone too. Keeping it fails the build with `IDE0005: Using directive is unnecessary`: the `.editorconfig` rule from US-1.3 at work.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 9: Write the persistence tests.</b></span></summary>

**Run in:** `dotnet-todo/`.

<details>
<summary>Code to type: <code>dotnet-todo/tests/TodoApi.Tests/TodoPersistenceTests.cs</code></summary>

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Npgsql;
using TodoApi.Data;
using TodoApi.Todos;

namespace TodoApi.Tests;

public sealed class TodoPersistenceTests(TodoApiFactory factory) : IAsyncLifetime
{
    private readonly CancellationToken _cancellationToken = TestContext.Current.CancellationToken;
    private AsyncServiceScope _scope;
    private TodoDbContext _db = null!;

    public ValueTask InitializeAsync()
    {
        _scope = factory.Services.CreateAsyncScope();
        _db = _scope.ServiceProvider.GetRequiredService<TodoDbContext>();
        return ValueTask.CompletedTask;
    }

    public ValueTask DisposeAsync() => _scope.DisposeAsync();

    [Fact]
    public async Task SavesWithDefaultsAndTimestamps()
    {
        var todo = new Todo { Title = "Buy milk" };

        _db.Todos.Add(todo);
        await _db.SaveChangesAsync(_cancellationToken);

        var saved = await ReloadAsync(todo.Id);
        Assert.True(saved.Id > 0);
        Assert.False(saved.Completed);
        Assert.Null(saved.Description);
        Assert.Equal(todo.CreatedAt, saved.CreatedAt);
        Assert.Equal(factory.Clock.GetUtcNow(), saved.CreatedAt, TimeSpan.FromMicroseconds(1));
        Assert.Equal(saved.CreatedAt, saved.UpdatedAt);
    }

    [Fact]
    public async Task UpdateChangesUpdatedAtButNotCreatedAt()
    {
        var todo = new Todo { Title = "Buy milk" };
        _db.Todos.Add(todo);
        await _db.SaveChangesAsync(_cancellationToken);
        var createdAt = todo.CreatedAt;

        factory.Clock.Advance(TimeSpan.FromMinutes(1));
        todo.Title = "Buy oat milk";
        await _db.SaveChangesAsync(_cancellationToken);

        var saved = await ReloadAsync(todo.Id);
        Assert.Equal(createdAt, saved.CreatedAt);
        Assert.Equal(createdAt.AddMinutes(1), saved.UpdatedAt);
    }

    [Fact]
    public async Task DatabaseRejectsTitleOver200Characters()
    {
        _db.Todos.Add(new Todo { Title = new string('a', 201) });

        var error = await Assert.ThrowsAsync<DbUpdateException>(() => _db.SaveChangesAsync(_cancellationToken));

        var postgres = Assert.IsType<PostgresException>(error.InnerException);
        Assert.Equal(PostgresErrorCodes.StringDataRightTruncation, postgres.SqlState);
    }

    [Fact]
    public async Task DatabaseRejectsNullTitle()
    {
        _db.Todos.Add(new Todo { Title = null! });

        var error = await Assert.ThrowsAsync<DbUpdateException>(() => _db.SaveChangesAsync(_cancellationToken));

        var postgres = Assert.IsType<PostgresException>(error.InnerException);
        Assert.Equal(PostgresErrorCodes.NotNullViolation, postgres.SqlState);
    }

    [Fact]
    public async Task BlankTitleIsSavedBecauseValidationLivesAtTheHttpBoundary()
    {
        var todo = new Todo { Title = "   " };

        _db.Todos.Add(todo);
        await _db.SaveChangesAsync(_cancellationToken);

        Assert.Equal("   ", (await ReloadAsync(todo.Id)).Title);
    }

    // A fresh query, not the tracked object, so the asserts read what Postgres stored
    private Task<Todo> ReloadAsync(long id) =>
        _db.Todos.AsNoTracking().SingleAsync(todo => todo.Id == id, _cancellationToken);
}
```

</details>

```bash
dotnet test
```

- Expect: `Passed!`, `total: 6` (1 health + 5 persistence). The first run takes longer while Docker pulls `postgres:18-alpine`.
- These are repository-level tests: no HTTP, just the `DbContext` against real Postgres. Spring's `@DataJpaTest` with Testcontainers.
- `factory.Services.CreateAsyncScope()`: a `DbContext` is scoped (one per request). Outside a request, you open a scope yourself to get one. `DisposeAsync` closes it after each test.
- xUnit creates a **new instance of the test class for every test**, so `_scope` and `_db` are fresh each time. JUnit 5 does the same by default.
- `null!` tells the compiler "I know this is null"; it silences the nullable warning so the test can send a `NULL` to the database on purpose.
- `Assert.ThrowsAsync<DbUpdateException>(...)`: `assertThrows`. EF wraps the database error; `InnerException` is the `PostgresException` with the SQL state code (`22001` too long, `23502` not null).
- `AsNoTracking()` in `ReloadAsync`: a fresh read from Postgres, not the object EF already has in memory. Without it you would test the object, not the row.

| Test | What it proves | Rails / Spring parallel |
| --- | --- | --- |
| `SavesWithDefaultsAndTimestamps` | `id` generated, `completed` false, both timestamps from the clock, equal on insert | Rails `t.timestamps`; Spring `@CreatedDate` |
| `UpdateChangesUpdatedAtButNotCreatedAt` | `Clock.Advance(1 minute)` moves `updated_at` only | Rails `travel 1.minute` |
| `DatabaseRejectsTitleOver200Characters` | `varchar(200)` is enforced by Postgres | Same as both |
| `DatabaseRejectsNullTitle` | `NOT NULL` is enforced by Postgres | Rails `NotNullViolation` in the console (US-2.2 of the Rails plan) |
| `BlankTitleIsSavedBecauseValidationLivesAtTheHttpBoundary` | EF Core does **not** validate on save; `"   "` is stored | Spring behaves the same; Rails would reject it in the model |

- Checked: removing the `AddTicks` line or the `AddInterceptors` call makes the two timestamp tests fail.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 10: Check and commit.</b></span></summary>

**Run in:** `dotnet-todo/`.

```bash
dotnet build
dotnet test
dotnet format --verify-no-changes
dotnet package list --vulnerable --include-transitive
```

Then **Run in:** repo root (`learn-ruby-thru-spring/`).

```bash
git add dot-net-todo.md dotnet-todo
git commit -m "US-2.2 audit timestamps, DB health check, Testcontainers tests"
```

**Checklist**

- [ ] `IHasTimestamps` + `AuditTimestampsInterceptor` fill `created_at` / `updated_at` from `TimeProvider`
- [ ] `/health` checks the database: 200 when up, 503 when down
- [ ] One Postgres container per test run through an xUnit assembly fixture; migrations applied to it
- [ ] Persistence tests pass; tests ignore the dev database
- [ ] Build, test, format and audit clean

**What to notice:** Spring gives you auditing as a switch (`@EnableJpaAuditing`); EF Core gives you a hook (`SaveChangesInterceptor`) and you write the 30 lines. The entity has no validation at all: in .NET, as in Spring, rules like "title not blank" belong to the HTTP request type (Phase 3), and the database is the last line of defense.

</details>

---

<details>
<summary><span style="font-weight: bold; color: pink;"><b>Validate</b></span></summary>

**Run in:** `dotnet-todo/`. Docker running, `todo-db` up.

| # | Command | Expect |
| --- | --- | --- |
| 1 | `dotnet build` | `0 Warning(s)`, `0 Error(s)` |
| 2 | `dotnet test` | `Passed!`, `total: 6` |
| 3 | `docker exec todo-db sh -c 'psql -U "$POSTGRES_USER" -d todo_dotnet -Atc "select count(*) from todos"'` before and after `dotnet test` | the same number both times: the tests' inserts went to their own container, not `todo_dotnet` |
| 4 | `dotnet ef migrations has-pending-model-changes --project src/TodoApi` | `No changes have been made to the model since the last migration.` |
| 5 | `dotnet format --verify-no-changes; echo $?` | `0` |
| 6 | `dotnet run --project src/TodoApi` (terminal 1), then `curl -s -w ' %{http_code}\n' localhost:5080/health` (terminal 2) | `Healthy 200` |
| 7 | `docker stop todo-db`, the same `curl`, then `docker start todo-db` | `Unhealthy 503` |

</details>

</details>

</details>

---

<details>
<summary><span style="font-weight: bold; color: cyan;"><b>Phase 3: CRUD user stories</b> (outline: full steps when we get here)</span></summary>

Same contract as the other two apps under `/api/todos`, so the same `curl` commands test all three by switching the port (Spring 8081, Rails 3000, .NET 5080).

| Story | Method + path | Success | Errors |
| --- | --- | --- | --- |
| US-3.1 Create | `POST /api/todos` | 201 + body + `Location` header | 422 validation |
| US-3.2 View one | `GET /api/todos/{id}` | 200 | 404 |
| US-3.2 View list | `GET /api/todos?completed=&page=&size=` | 200, newest first | none |
| US-3.3 Update | `PATCH /api/todos/{id}` | 200 + body | 404, 422 |
| US-3.4 Delete | `DELETE /api/todos/{id}` | 204 | 404 |

**Shared JSON contract** (unchanged from the Spring/Rails plan)

- Keys are snake_case: `id`, `title`, `description`, `completed`, `due_date`, `created_at`, `updated_at`. .NET: `JsonNamingPolicy.SnakeCaseLower` in `ConfigureHttpJsonOptions`; OpenAPI picks it up automatically.
- Request bodies are flat: `{"title": "Buy milk"}`.
- List body: `{"items": [...], "page": 1, "size": 20, "total": 42}`, `page` starts at 1.
- Errors: RFC 9457 Problem Details, `application/problem+json`, keys `title`, `status`, `detail`, `instance`, plus `errors` for validation. No `type`, no `traceId`: one `CustomizeProblemDetails` callback trims ASP.NET's defaults to this shape.

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-3.1 Create a todo</b></span></summary>

- [ ] Valid request returns 201, the saved todo and a `Location` header ending in `/api/todos/{id}`
- [ ] Blank title or title over 200 characters returns 422 with field-level errors
- [ ] Unknown fields (`id`, `completed`) are ignored
- [ ] Tests cover success and both validation failures

.NET pieces: `TodoEndpoints.MapTodoEndpoints()` with `MapGroup("/api/todos")`; `CreateTodoRequest` record with `[Required, MaxLength(200)]`; `AddValidation()`; built-in validation answers 400, so `CustomizeProblemDetails` turns it into the contract's 422; `TypedResults.CreatedAtRoute`; OpenAPI metadata with `WithSummary`, `WithTags`, `ProducesValidationProblem(422)`.

</details>

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-3.2 View todos</b></span></summary>

- [ ] List newest first; `?completed=true|false` filters; paginated (default 20, max 100) with page metadata
- [ ] Unknown id returns 404 Problem Details
- [ ] Tests cover list, filter, pagination, show and 404

.NET pieces: `AsNoTracking()` queries, `CountAsync` + `Skip`/`Take` (the two queries Spring Data's `Page` runs), `Results<Ok<TodoResponse>, ProblemHttpResult>` instead of exceptions for "not found", `{id:long}` route constraint. `?completed=abc` is a 400, as in Spring.

</details>

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-3.3 Update a todo</b></span></summary>

- [ ] `PATCH` updates only the fields sent; `updated_at` changes, `created_at` does not
- [ ] Same validation as create (422); unknown id returns 404
- [ ] Tests cover partial update, mark complete, timestamps, 404 and 422

.NET pieces: `UpdateTodoRequest` with nullable fields; a custom `[NotBlank]` attribute (null passes, whitespace fails, like Spring's `@Pattern` trick); change tracker writes the `UPDATE`; `FakeTimeProvider.Advance()` in tests, like Rails' `travel`.

</details>

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-3.4 Delete a todo</b></span></summary>

- [ ] Delete returns 204 with no body; a later `GET` returns 404; unknown id returns 404
- [ ] Tests cover success and 404

.NET pieces: `ExecuteDeleteAsync()` returns the row count in one `DELETE`, the same trade-off as Spring's `@Modifying` JPQL delete (no change tracker, no callbacks).

</details>

</details>

---

<details>
<summary><span style="font-weight: bold; color: cyan;"><b>Definition of done and conventions</b></span></summary>

**Definition of done (every story)**

- [ ] Acceptance criteria covered by HTTP-level tests against real Postgres (Testcontainers)
- [ ] `dotnet build` (0 warnings), `dotnet test`, `dotnet format --verify-no-changes` and the vulnerability check clean
- [ ] Schema changes only through a new migration, never by editing an applied one
- [ ] Same request returns the same status and JSON shape as the Spring and Rails apps
- [ ] One short note per story in `LEARNINGS.md`

**Clean code conventions**

- Feature folders (`Todos/`, `Data/`), not layer folders: endpoints, requests, responses and entity for one feature sit together
- Minimal APIs in a static `TodoEndpoints` class with `MapTodoEndpoints()`; handlers are small static methods, not lambdas in `Program.cs`
- `TypedResults` and `Results<...>` return types; no exceptions for expected outcomes (404, 422)
- `DbContext` used directly in handlers; no repository wrapper around it; add a service class only when logic outgrows a handler
- Records for requests and responses; the entity never leaves the handler
- `async` all the way with `CancellationToken` passed through
- `TimeProvider` instead of `DateTime.UtcNow`, so tests control the clock
- Nullable enabled, warnings as errors, versions only in `Directory.Packages.props`

**After this plan:** add Makefile targets (`dotnet-build`, `dotnet-run`, `APP=dotnet` for the `api-*` helpers); try .NET Aspire for local orchestration (Postgres + app + dashboard with OpenTelemetry); add authentication with ASP.NET Core Identity and compare it with Spring Security and Rails 8's generator.

</details>

---
