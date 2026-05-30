.PHONY: infra infra-down dev-riot dev-server dev-ai dev-broadcast dev-frontend

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
