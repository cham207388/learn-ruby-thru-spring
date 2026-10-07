# Shortcuts for the Spring and Rails todo apps. Run `make` to list targets.
# Recipes run from the repo root unless they cd into an app folder.

# Load POSTGRES_* from .env so Rails targets work even when mise is not activated
-include .env
export POSTGRES_USER POSTGRES_PASSWORD POSTGRES_PORT

SPRING_DIR := spring-todo
RAILS_DIR  := rails-todo
DB_EXEC    := docker exec todo-db sh -c

# API helpers: APP=spring (default) or APP=rails
APP   ?= spring
ID    ?= 1
BODY  ?= {"title":"Buy milk"}
QUERY ?=
ifeq ($(APP),rails)
BASE_URL := http://localhost:3000
else
BASE_URL := http://localhost:8081
endif

.DEFAULT_GOAL := help
.PHONY: help tools-check check \
	compose-up compose-down compose-reset compose-ps compose-logs \
	db-version db-list db-todos-spring db-todos-rails db-compare db-flyway-history psql-spring psql-rails \
	spring-build spring-clean spring-test spring-lint spring-format spring-run spring-health spring-db \
	rails-exists rails-install rails-db-prepare rails-db-migrate rails-db-rollback rails-db-status rails-db \
	rails-test rails-lint rails-lint-fix rails-security rails-run rails-console rails-routes rails-health rails-check \
	api-create api-list api-get api-update api-delete api-shape api-list-shape

help: ## List targets
	@grep -hE '^[a-z0-9-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

## ---------- Toolchain ----------

tools-check: ## Print versions of every tool the plan needs
	@echo "mise:    $$(mise --version 2>/dev/null || echo MISSING)"
	@echo "java:    $$(java -version 2>&1 | head -1)"
	@echo "ruby:    $$(ruby -v 2>/dev/null || echo MISSING)  ($$(command -v ruby))"
	@echo "gem:     $$(gem -v 2>/dev/null || echo MISSING)"
	@echo "bundler: $$(bundle -v 2>/dev/null || echo MISSING)"
	@echo "rails:   $$(rails -v 2>/dev/null || echo MISSING)"
	@echo "libpq:   $$(brew list libpq >/dev/null 2>&1 && echo ok || echo MISSING)"
	@echo "docker:  $$(docker --version 2>/dev/null || echo MISSING)"

check: spring-build rails-check ## Full gate: Spring build plus Rails tests, lint and security

## ---------- Database (Docker Compose) ----------

compose-up: ## Start Postgres and wait until healthy
	docker compose up -d --wait

compose-down: ## Stop Postgres (keeps data)
	docker compose down

compose-reset: ## Delete the data volume and start fresh (re-runs db/init)
	docker compose down -v
	docker compose up -d --wait

compose-ps: ## Show container status and health
	docker compose ps

compose-logs: ## Follow Postgres logs
	docker compose logs -f todo-db

db-version: ## Print the Postgres server version
	@$(DB_EXEC) 'psql -U "$$POSTGRES_USER" -d postgres -Atc "select version()"'

db-list: ## List the todo databases
	@$(DB_EXEC) 'psql -U "$$POSTGRES_USER" -d postgres -Atc "select datname from pg_database where datname like '"'"'todo%'"'"' order by 1"'

db-todos-spring: ## Describe the todos table in todo_spring
	@$(DB_EXEC) 'psql -U "$$POSTGRES_USER" -d todo_spring -c "\d todos"'

db-todos-rails: ## Describe the todos table in todo_rails_development
	@$(DB_EXEC) 'psql -U "$$POSTGRES_USER" -d todo_rails_development -c "\d todos"'

db-compare: ## Compare todos columns in both databases side by side
	@for db in todo_spring todo_rails_development; do \
		echo "== $$db"; \
		$(DB_EXEC) "psql -U \"\$$POSTGRES_USER\" -d $$db -c \"select column_name, data_type, character_maximum_length as max_len, is_nullable, column_default from information_schema.columns where table_name = 'todos' order by column_name\""; \
	done

db-flyway-history: ## Show Flyway migrations applied to todo_spring
	@$(DB_EXEC) 'psql -U "$$POSTGRES_USER" -d todo_spring -c "select version, description, success from flyway_schema_history order by installed_rank"'

psql-spring: ## Open psql on todo_spring
	docker exec -it todo-db sh -c 'psql -U "$$POSTGRES_USER" -d todo_spring'

psql-rails: ## Open psql on todo_rails_development
	docker exec -it todo-db sh -c 'psql -U "$$POSTGRES_USER" -d todo_rails_development'

## ---------- Spring ----------

spring-build: ## Compile, Spotless check and tests (./gradlew build)
	cd $(SPRING_DIR) && ./gradlew build

spring-clean: ## Delete build output (./gradlew clean)
	cd $(SPRING_DIR) && ./gradlew clean

spring-test: ## Run tests only
	cd $(SPRING_DIR) && ./gradlew test

spring-lint: ## Check formatting (spotlessCheck)
	cd $(SPRING_DIR) && ./gradlew spotlessCheck

spring-format: ## Fix formatting (spotlessApply)
	cd $(SPRING_DIR) && ./gradlew spotlessApply

spring-run: ## Start the app on port 8081 (Ctrl-C to stop)
	cd $(SPRING_DIR) && ./gradlew bootRun

spring-health: ## Call /actuator/health
	@curl -s localhost:8081/actuator/health; echo

## ---------- Rails ----------

rails-exists:
	@test -d $(RAILS_DIR) || { echo "$(RAILS_DIR)/ not found: finish US-1.4 Step 1 first"; exit 1; }

rails-install: rails-exists ## Install gems (bundle install)
	cd $(RAILS_DIR) && bundle install

rails-db-prepare: rails-exists ## Create and migrate the dev DB if needed
	cd $(RAILS_DIR) && bin/rails db:prepare

rails-db-migrate: rails-exists ## Run pending migrations
	cd $(RAILS_DIR) && bin/rails db:migrate

rails-db-rollback: rails-exists ## Roll back the last migration
	cd $(RAILS_DIR) && bin/rails db:rollback

rails-db-status: rails-exists ## Show migration status (up/down)
	cd $(RAILS_DIR) && bin/rails db:migrate:status

rails-db: rails-exists ## Print the database Rails connects to
	@cd $(RAILS_DIR) && bin/rails runner 'puts ActiveRecord::Base.connection.current_database'

rails-test: rails-exists ## Run tests; one file with T=test/models/todo_test.rb
	cd $(RAILS_DIR) && bin/rails test $(T)

rails-lint: rails-exists ## RuboCop
	cd $(RAILS_DIR) && bin/rubocop

rails-lint-fix: rails-exists ## RuboCop with safe autocorrect
	cd $(RAILS_DIR) && bin/rubocop -a

rails-security: rails-exists ## Brakeman and bundler-audit
	cd $(RAILS_DIR) && bin/brakeman --no-pager -q && bin/bundler-audit

rails-run: rails-exists ## Start the server on port 3000 (Ctrl-C to stop)
	cd $(RAILS_DIR) && bin/rails server

rails-console: rails-exists ## Open the Rails console
	cd $(RAILS_DIR) && bin/rails console

rails-routes: rails-exists ## Show the todo routes
	cd $(RAILS_DIR) && bin/rails routes -g todos

rails-health: ## Call /up
	@curl -s -o /dev/null -w "GET /up -> %{http_code}\n" localhost:3000/up

rails-check: rails-test rails-lint rails-security ## Tests, lint and security scans

rails-dep: rails-exists ## Deploy the app to the cloud
	cd $(RAILS_DIR) && bin/rails -T

ruby-drill: ## Run the Ruby drill
	ruby ./playground/irb_drill.rb

## ---------- API (APP=spring|rails, ID=, BODY=, QUERY=) ----------

api-create: ## POST /api/todos with BODY
	@curl -si -X POST $(BASE_URL)/api/todos -H 'Content-Type: application/json' -d '$(BODY)'; echo

api-list: ## GET /api/todos?QUERY
	@curl -si "$(BASE_URL)/api/todos?$(QUERY)"; echo

api-get: ## GET /api/todos/ID
	@curl -si $(BASE_URL)/api/todos/$(ID); echo

api-update: ## PATCH /api/todos/ID with BODY
	@curl -si -X PATCH $(BASE_URL)/api/todos/$(ID) -H 'Content-Type: application/json' -d '$(BODY)'; echo

api-delete: ## DELETE /api/todos/ID
	@curl -si -X DELETE $(BASE_URL)/api/todos/$(ID); echo

api-shape: ## Print JSON keys of POST /api/todos (with BODY) from both apps
	@for url in http://localhost:8081 http://localhost:3000; do \
		echo "== $$url"; \
		curl -s -X POST $$url/api/todos -H 'Content-Type: application/json' -d '$(BODY)' | jq -c 'keys'; \
	done

api-list-shape: ## Print JSON keys of GET /api/todos (page and item) from both apps
	@for url in http://localhost:8081 http://localhost:3000; do \
		echo "== $$url"; \
		curl -s "$$url/api/todos?$(QUERY)" | jq -c 'keys, (.items[0] // {} | keys)'; \
	done
