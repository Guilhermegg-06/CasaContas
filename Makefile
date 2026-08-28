.PHONY: help setup up down logs verify verify-backend verify-frontend test-e2e security clean

help:
	@echo "CasaContas"
	@echo "  make setup            instala dependências do frontend"
	@echo "  make up               sobe a aplicação completa"
	@echo "  make verify           roda verificações do backend e frontend"
	@echo "  make test-e2e         roda a jornada no navegador"
	@echo "  make security         audita dependências"

setup:
	cd frontend && npm ci

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f --tail=200

verify: verify-backend verify-frontend

verify-backend:
	cd backend && ./mvnw -B -ntp verify

verify-frontend:
	cd frontend && npm ci && npm run verify

test-e2e:
	cd frontend && npm run test:e2e

security:
	cd backend && ./mvnw -B -ntp -Psecurity verify
	cd frontend && npm audit --audit-level=high

clean:
	cd backend && ./mvnw clean
	cd frontend && npm run build -- --emptyOutDir
