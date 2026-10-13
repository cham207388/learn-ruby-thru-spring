# Todo App: .NET Learning Plan

<details>
<summary><span style="font-weight: bold; color: cyan;">Overview</span></summary>

Build the same Todo REST API a third time, now in C# on ASP.NET Core, story by story. The Spring and Rails apps in this repo are the reference: same endpoints, same JSON contract, same Postgres container. Each step maps a .NET idea to the Spring idea you already own.

**How we work:** one step at a time. Each step says what to do, why, and how to check it. You type the code yourself; every code block was tested on .NET SDK 10.0.402 before it went into this plan. Run commands from `dotnet-todo/` unless a step says repo root.

**Status:** Phase 1 (install .NET and create the app) is written out in full. Later phases are outlines; each gets full, tested steps when we reach it.

**Validate every story:** each story ends with a **Validate** table: commands and the result to expect.

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

From repo root:

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

Run from repo root.

| # | Command | Expect |
| --- | --- | --- |
| 1 | `make db-list` | `todo`, `todo_dotnet`, `todo_rails_development`, `todo_rails_test`, `todo_spring` |
| 2 | `grep -c todo_dotnet db/init/01-create-databases.sql` | `1` |

</details>

</details>

---

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-1.2 .NET toolchain (mise)</b></span></summary>

*As a Java developer, I want a version-managed .NET 10 SDK so .NET works like SDKMAN-managed JDKs.*

.NET is not installed on this machine yet (`dotnet` is not on PATH). mise installs it.

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 1: Update mise.</b></span></summary>

```bash
mise self-update
mise --version
```

- Expect: `2026.10.8` or newer. Your copy (2026.10.3) predates today's SDK release, so its version list may not show `10.0.402` yet.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 2: Install the SDK for this repo.</b></span></summary>

From repo root:

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

Open a new terminal in the repo, then:

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

```bash
dotnet new install xunit.v3.templates
dotnet new list xunit
```

- Expect: `xUnit.net v3 Test Project` with short name `xunit3`.
- Why: the SDK's built-in `xunit` template is still xUnit v2. v3 is the current major and runs on Microsoft.Testing.Platform.
- Templates are NuGet packages. `dotnet new install` is per user, like adding an initializr starter.

</details>

---

<details>
<summary><span style="font-weight: bold; color: #9999FF;"><b>Step 6: Learn the CLI map.</b></span></summary>

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

.NET 10 runs a single `.cs` file with no project: `dotnet run drill.cs`. Use it like `irb` or `jshell`. Create `playground/drill.cs`, add one block at a time, run it, and predict the output first.

| # | Topic | Expect | Java analogue / lesson |
| --- | --- | --- | --- |
| 1 | `var count = 5;` then `count.GetType()` | `System.Int32 False` | `var` like Java 10; `int` is an alias for `System.Int32` |
| 2 | `$"Hi {name.ToUpperInvariant()}"` and a raw string `"""{"title": "x"}"""` | `Hi TODO \| {"title": "x"}` | `$` interpolates; `"""` is a text block |
| 3 | `string? description = null;` then `?.`, `??` | ` \| none \| 0` | `string?` is a nullable reference; the compiler warns when you dereference it |
| 4 | `record TodoItem(...)` and `a with { Completed = true }` | `True \| TodoItem { Title = Buy milk, Completed = True }` | Like a Java record, plus `with` for copies |
| 5 | `numbers.Where(...).Select(...).ToList()` | `[20, 40]` | LINQ is Streams; on EF Core it becomes SQL |
| 6 | `size switch { null => ..., < 1 => ..., > 100 => ..., _ => ... }` | `default too small capped ok` | Switch expression with relational patterns; Java 21 pattern switch |
| 7 | `Dictionary.TryGetValue("due_date", out var due)` | `False True` | The indexer throws on a missing key; `TryGetValue` does not |
| 8 | `await CountAsync()` | `42` | `async`/`await` on `Task<T>`; top-level `await` works in a file app |
| 9 | `extension(string text) { public bool IsBlank => ... }` | `True False` | C# 14 extension members: add a property to `string` |

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

- Order rule: top-level statements first, then local functions, then type declarations (`record`, `class`). The compiler wraps the statements in a generated `Main`.
- The first run takes a few seconds: the SDK builds a hidden project and caches it.

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

Run from repo root.

| # | Command | Expect |
| --- | --- | --- |
| 1 | `dotnet --version` | `10.0.402` |
| 2 | `which dotnet` | a path under `~/.local/share/mise/` |
| 3 | `dotnet --list-runtimes` | `Microsoft.AspNetCore.App 10.0.13`, `Microsoft.NETCore.App 10.0.13` |
| 4 | `grep -E 'dotnet\|DOTNET' mise.toml` | `dotnet = "10.0.402"`, both `DOTNET_*` lines |
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

From repo root:

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

Create `Directory.Build.props` in `dotnet-todo/`:

<details>
<summary>Code to type: <code>Directory.Build.props</code></summary>

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

Create `Directory.Packages.props`, then remove `TargetFramework`, `Nullable`, `ImplicitUsings` and every `Version="..."` from both `.csproj` files.

<details>
<summary>Code to type: <code>Directory.Packages.props</code></summary>

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
<summary>Code to type: <code>src/TodoApi/TodoApi.csproj</code></summary>

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
<summary>Code to type: <code>tests/TodoApi.Tests/TodoApi.Tests.csproj</code></summary>

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
<summary>Code to type: <code>.editorconfig</code></summary>

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

Replace the template's weather-forecast sample. Delete `src/TodoApi/TodoApi.http` and `tests/TodoApi.Tests/UnitTest1.cs` too.

<details>
<summary>Code to type: <code>src/TodoApi/Program.cs</code></summary>

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

In `src/TodoApi/Properties/launchSettings.json`, set `applicationUrl` to `http://localhost:5080`.

<details>
<summary>Code to type: <code>src/TodoApi/Properties/launchSettings.json</code></summary>

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

<details>
<summary>Code to type: <code>tests/TodoApi.Tests/HealthTests.cs</code></summary>

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

```bash
dotnet run --project src/TodoApi
```

In a second terminal:

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

From repo root:

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

Run from `dotnet-todo/`.

| # | Command | Expect |
| --- | --- | --- |
| 1 | `cat global.json` | `"version": "10.0.400"`, `latestFeature`, `Microsoft.Testing.Platform` runner |
| 2 | `grep -c Version= src/TodoApi/TodoApi.csproj tests/TodoApi.Tests/TodoApi.Tests.csproj` | `0` for both: versions live only in `Directory.Packages.props` |
| 3 | `dotnet tool list` | `dotnet-ef 10.0.13` |
| 4 | `dotnet build` | `Build succeeded`, `0 Warning(s)`, `0 Error(s)` |
| 5 | `dotnet test` | `Passed!`, `total: 1` |
| 6 | `dotnet format --verify-no-changes; echo $?` | `0` |
| 7 | `dotnet package list --vulnerable --include-transitive` | `has no vulnerable packages` for both projects |
| 8 | `dotnet run --project src/TodoApi` (terminal 1) | `Now listening on: http://localhost:5080`, `Hosting environment: Development` |
| 9 | `curl -s localhost:5080/health` (terminal 2) | `Healthy` |
| 10 | `curl -si localhost:5080/nope \| grep Content-Type` | `application/problem+json` |
| 11 | `git ls-files dotnet-todo \| grep -cE '/(bin\|obj)/'` (repo root, after the commit) | `0` |

</details>

</details>

</details>

---

<details>
<summary><span style="font-weight: bold; color: cyan;"><b>Phase 2: Todo model and migration</b> (outline: full steps when we get here)</span></summary>

Phase 2 creates the `todos` table through an EF Core migration and maps it to an entity, before any endpoint exists.

**Table `todos`:** `id` bigint identity PK · `title` varchar(200) not null · `description` text null · `completed` boolean not null default false · `due_date` date null · `created_at` / `updated_at` timestamptz not null. Same as the Spring and Rails tables.

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-2.1 Create the todos table</b></span></summary>

*As a developer, I want the schema defined in versioned migrations so every environment builds the same database.*

- [ ] `dotnet add package` for `Npgsql.EntityFrameworkCore.PostgreSQL`, `EFCore.NamingConventions`, `Microsoft.EntityFrameworkCore.Design`, `Microsoft.Extensions.Diagnostics.HealthChecks.EntityFrameworkCore`
- [ ] Connection string `ConnectionStrings:Todo` built from the `.env` values, never committed with a password
- [ ] `Todo` entity, `TodoDbContext`, `IEntityTypeConfiguration<Todo>`; `UseSnakeCaseNamingConvention()` so columns are `due_date`, `created_at`
- [ ] `dotnet ef migrations add CreateTodos`; read the generated `Up`/`Down` and `dotnet ef migrations script`
- [ ] `dotnet ef database update`; `\d todos` matches the spec; `id` is `GENERATED BY DEFAULT AS IDENTITY` (like Spring, unlike Rails' `bigserial`)
- [ ] Rollback practiced: `dotnet ef database update 0`, then forward again
- [ ] `/health` also checks the database

</details>

<details>
<summary><span style="font-weight: bold; color: green;"><b>US-2.2 Map the Todo model</b></span></summary>

*As a developer, I want a Todo entity with audit timestamps so `created_at` / `updated_at` fill themselves.*

- [ ] `AuditTimestampsInterceptor : SaveChangesInterceptor` using `TimeProvider` (the `@EnableJpaAuditing` equivalent)
- [ ] Testcontainers fixture: `PostgreSqlContainer` + `WebApplicationFactory`, migrations applied once per run
- [ ] Persistence tests: saves with defaults and timestamps; null title and 201-character title rejected by the database
- [ ] What to notice: EF Core does not run DataAnnotations on save. Validation happens at the HTTP boundary, as in Spring, not in the model as in Rails

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
