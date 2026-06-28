.PHONY: infra infra-down dev-riot dev-server dev-ai dev-broadcast dev-frontend \
        test test-riot test-server test-ai test-broadcast test-frontend \
        setup-ai setup-broadcast

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

## Create venvs and install Python deps (run once per module)
setup-ai:
	python -m venv ai/venv
	ai/venv/bin/pip install -r ai/requirements-test.txt

setup-broadcast:
	python -m venv broadcast/venv
	broadcast/venv/bin/pip install -r broadcast/requirements-test.txt

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
	cd frontend && npm run test:run
