.PHONY: infra infra-down \
        dev-riot dev-server dev-ai dev-broadcast dev-frontend \
        setup-ai setup-broadcast setup-frontend \
        build build-riot build-server build-ai build-broadcast build-frontend build-frontend-dev \
        test test-riot test-server test-ai test-broadcast test-frontend

## Local infrastructure (Postgres + RabbitMQ)
infra:
	docker compose up -d

infra-down:
	docker compose down

## Module dev servers
dev-riot:
	cd riot-api && mvn spring-boot:run -Dspring-boot.run.profiles=local

dev-server:
	cd server && mvn spring-boot:run

dev-ai:
	cd ai && uvicorn main:app --reload --port 8001

dev-broadcast:
	cd broadcast && python worker.py

dev-frontend:
	cd frontend && bun run dev

## Install deps (run once per module)
setup-frontend:
	cd frontend && bun install

setup-ai:
	python -m venv ai/venv
	ai/venv/bin/pip install -r ai/requirements-test.txt

setup-broadcast:
	python -m venv broadcast/venv
	broadcast/venv/bin/pip install -r broadcast/requirements-test.txt

## Builds
build: build-riot build-server build-ai build-broadcast build-frontend

build-riot:
	cd riot-api && mvn package -DskipTests

build-server:
	cd server && mvn package -DskipTests

build-ai:
	cd ai && python -m build

build-broadcast:
	cd broadcast && python -m build

build-frontend:
	cd frontend && bun run build

build-frontend-dev: export MSYS_NO_PATHCONV := 1
build-frontend-dev: export NEXT_PUBLIC_STATIC_EXPORT := true
build-frontend-dev: export NEXT_PUBLIC_BASE_PATH := /ScoutGG/dev
build-frontend-dev: export NEXT_PUBLIC_USE_MOCKS := true
build-frontend-dev:
	cd frontend && bun run build

## Tests
test: test-riot test-server test-ai test-broadcast test-frontend

test-riot:
	cd riot-api && mvn verify

test-server:
	cd server && mvn verify

test-ai:
	cd ai && venv/bin/python -m pytest --cov --cov-fail-under=85

test-broadcast:
	cd broadcast && venv/bin/python -m pytest --cov --cov-fail-under=85

test-frontend:
	cd frontend && bun run test:run
