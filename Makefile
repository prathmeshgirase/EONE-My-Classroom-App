.PHONY: up down logs backend-test mobile-check
up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f backend

backend-test:
	cd backend && mvn test

mobile-check:
	cd mobile && flutter pub get && flutter analyze && flutter test
