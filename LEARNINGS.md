# Learning Notes

## User Stories

## Extras

<details>
<summary><span style="font-weight: bold; color: rgb(153, 184, 255);"><b>Spring Boot Dependency: e.g webmvc</b></span></summary>

**Key fact first:** `spring-boot-starter-webmvc` does not include Thymeleaf. Thymeleaf comes only from its own starter, `spring-boot-starter-thymeleaf`. A REST-only app needs no exclusion: do not add the Thymeleaf starter. The plan line "webmvc without Thymeleaf" means exactly that.

Check in this project (prints `0`):

```bash
cd spring-todo && ./gradlew -q dependencies --configuration runtimeClasspath | grep -ci thymeleaf
```

### 1. See transitive dependencies

**Full tree.** Run from the app folder (`spring-todo/`):

```bash
./gradlew dependencies --configuration runtimeClasspath
```

Pick the configuration by question:

| Configuration | Answers |
| --- | --- |
| `compileClasspath` | What can my code import? |
| `runtimeClasspath` | What ships in the JAR and runs? |
| `testRuntimeClasspath` | What runs during tests? |

**Read the tree:**

| Marker | Meaning |
| --- | --- |
| `+---` / `\---` | Child dependency (indent = depth) |
| `-> 4.1.1` | Version picked by resolution (Spring Boot BOM or conflict win) |
| `(*)` | Subtree already printed above; not expanded again |
| `(c)` | Version constraint only, not a real dependency |

**One starter only.** Cut the tree at the starter you care about:

```bash
./gradlew -q dependencies --configuration runtimeClasspath | grep -A 45 'spring-boot-starter-webmvc'
```

**Why is library X here?** Reverse lookup, from the library up to the starter that pulled it:

```bash
./gradlew -q dependencyInsight --dependency tomcat-embed-websocket --configuration runtimeClasspath
```

Output ends with the path: `tomcat-embed-websocket` ← `spring-boot-starter-tomcat-runtime` ← `spring-boot-starter-tomcat`.

**Visual options:**

- IDE: Gradle panel > `spring-todo` > Dependencies. Click to expand.
- `./gradlew dependencies --scan`: publishes a searchable web report to scans.gradle.com. **Warning:** it uploads build data to a public service. Skip for private or work projects.

**What `spring-boot-starter-webmvc` brings (Spring Boot 4.1.1, from this project):**

| Child | Role | Brings |
| --- | --- | --- |
| `spring-boot-starter` | Core Boot | auto-config, logging (Logback), YAML |
| `spring-boot-starter-jackson` | JSON | `tools.jackson.core:jackson-databind` 3.x |
| `spring-boot-starter-tomcat` | Embedded server | `tomcat-embed-core`, `-el`, `-websocket` 11.x |
| `spring-boot-http-converter` | Message converters | JSON ↔ objects for request and response bodies |
| `spring-boot-webmvc` | MVC auto-config | `spring-webmvc`, `spring-web` 7.x |

No template engine. Starter = curated bundle plus auto-config, nothing hidden.

**Other starters:** same commands, swap the name (`spring-boot-starter-data-jpa`, `-actuator`, ...). To read a starter before adding it, open its `.pom` on Maven Central (`org/springframework/boot/<starter>/<version>/`): the `<dependencies>` block lists its direct children.

**Maven projects:**

```bash
./mvnw dependency:tree
./mvnw dependency:tree -Dincludes=org.apache.tomcat.embed
```

`-Dincludes` filters like `dependencyInsight`.

### 2. Exclude what you do not need

**Rule:** exclude only a library that is on the classpath and unused. Spring Boot auto-config switches on by classpath presence: an unused library can still start beans, open features or grow the JAR.

**Order of options:**

1. **Remove the starter line.** Best fix when you declared the thing yourself (e.g. `spring-boot-starter-thymeleaf` in a REST app). No exclusion needed.
2. **Exclude a transitive dependency.** When a starter you need drags in a piece you do not.
3. **Exclude auto-config only.** When the library must stay (another library uses it) but its Boot behavior must not start:

```yaml
spring:
  autoconfigure:
    exclude: org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration
```

Find the exact class name with `--debug` at startup (conditions report) or in the IDE.

**Exclude from one dependency** (`build.gradle.kts`):

```kotlin
implementation(libs.spring.boot.starter.webmvc) {
    exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-websocket")
}
```

**Exclude everywhere** (every configuration, every path):

```kotlin
configurations.all {
    exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-websocket")
}
```

**Maven:**

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webmvc</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.apache.tomcat.embed</groupId>
      <artifactId>tomcat-embed-websocket</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

**Common real cases:**

| Goal | Exclude | Add |
| --- | --- | --- |
| No WebSockets in a REST API | `tomcat-embed-websocket` | nothing |
| Jetty instead of Tomcat | `spring-boot-starter-tomcat` from `spring-boot-starter-webmvc` | `spring-boot-starter-jetty` |
| Log4j2 instead of Logback | `spring-boot-starter-logging` with `configurations.all` | `spring-boot-starter-log4j2` |

Jetty swap, tested on this project:

```kotlin
implementation(libs.spring.boot.starter.webmvc) {
    exclude(module = "spring-boot-starter-tomcat")
}
implementation("org.springframework.boot:spring-boot-starter-jetty")
```

**Verify every exclusion:**

1. `./gradlew -q dependencyInsight --dependency <name> --configuration runtimeClasspath` prints no match.
2. `./gradlew build` passes.
3. `./gradlew bootRun` starts. A wrong exclusion shows up here as `ClassNotFoundException` or `NoClassDefFoundError`.

**Cautions:**

- Do not exclude core Spring modules (`spring-core`, `spring-web`, ...). Starters need them.
- Small win per jar. Exclude for a reason (security scan finding, conflict, server swap, size), not by habit.
- Exclusions hide future versions too. Recheck after each Spring Boot upgrade.

**Your other projects with unused Thymeleaf:**

```bash
./gradlew -q dependencyInsight --dependency thymeleaf --configuration runtimeClasspath
```

- Path ends at `spring-boot-starter-thymeleaf` declared by you: delete that line (option 1).
- Path goes through another library: exclude it from that library (option 2).
- No match: Thymeleaf is not there. Nothing to do.


</details>