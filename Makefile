.PHONY: help up down restart nuke ps logs build test lint lint-fix clean docker-build

COMPOSE_FILE := infra/docker/docker-compose.yml
GRADLE_DOCKER := docker run --rm -v "$(CURDIR):/project" -v gradle-cache:/gradle-cache -w /project eclipse-temurin:21-jdk ./gradlew --no-daemon

help: ## Lista todos os comandos disponiveis
	@powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-Content Makefile | Select-String '^\w+:.*##' | ForEach-Object { $$line = $$_.Line -split '##'; '{0,-20} {1}' -f $$line[0].TrimEnd(': '), $$line[1].Trim() }"

# --- Infra ---

up: ## Sobe toda infra (Postgres, Redis, Kafka, ES, EMQX, monitoring)
	powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev.ps1 up

down: ## Para toda infra
	docker compose -f $(COMPOSE_FILE) down

restart: ## Reinicia toda infra
	powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev.ps1 restart

nuke: ## Remove TUDO (containers, volumes, dados)
	powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev.ps1 nuke

ps: ## Status dos containers
	docker compose -f $(COMPOSE_FILE) ps

logs: ## Logs de todos os containers (follow)
	docker compose -f $(COMPOSE_FILE) logs -f

# --- Build (zero dependencia no host - roda em Docker) ---

build: ## Compila todos os modulos via Docker
	$(GRADLE_DOCKER) build

build-shared: ## Compila apenas modulos shared
	$(GRADLE_DOCKER) :shared:shared-domain:build :shared:shared-infra:build

# --- Qualidade (zero dependencia no host) ---

test: ## Roda todos os testes via Docker
	$(GRADLE_DOCKER) test

test-v: ## Testes com output verbose
	$(GRADLE_DOCKER) test --info

lint: ## Verifica formatacao (ktlint) via Docker
	$(GRADLE_DOCKER) ktlintCheck

lint-fix: ## Corrige formatacao automaticamente
	$(GRADLE_DOCKER) ktlintFormat

check: lint test ## Lint + testes

# --- Docker Build (imagens de servico) ---

docker-build: ## Build de imagem Docker de um servico. Uso: make docker-build SERVICE=tenant-service
	docker build -f infra/docker/Dockerfile.build --build-arg SERVICE=$(SERVICE) -t rappiclone-$(SERVICE):latest .

# --- Limpeza ---

clean: ## Limpa build artifacts
	$(GRADLE_DOCKER) clean

# --- Geo (OSRM) ---

prepare-osrm: ## Prepara dados OSRM (executar uma vez)
	powershell -NoProfile -ExecutionPolicy Bypass -File scripts/dev.ps1 prepare-osrm
