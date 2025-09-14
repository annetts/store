# Getting Started

## Store Service

RESTful store backend with Spring Boot 3, PostgreSQL, and Kafka.
Manages items (CRUD), sells items (decrement stock & emit Kafka event), and provides simple reporting. No UI.

## Features
* Items: create, read, update, delete.
* Sell flow: decrement stock; publish ItemSoldEvent to Kafka.
* Consumer: persist sales for reporting (idempotent via saleId).
* Reports: current stock and sales (time range filtering).
* Migrations: Flyway-managed schema.
* Tests: unit + integration (Testcontainers).

## Tech Stack
 
* Java 17+, Spring Boot 3.x (Web, Data JPA, Validation, Kafka)
* Docker
* PostgreSQL 16 + Flyway
* Kafka 3.x
* JUnit 5, Mockito, Spring Boot Test, Testcontainers
* springdoc-openapi for Swagger UI

## Prerequisites

* JDK 17+
* Docker & Docker Compose

### Start infrastructure

`docker compose up -d`

### Run application

```
# Using defaults (localhost for DB & Kafka)
./mvnw spring-boot:run
# or build a jar
./mvnw clean package && java -jar target/store-service-*.jar
```

### Health & Docs
* Health: GET http://localhost:8080/actuator/health
* OpenAPI (if included): http://localhost:8080/swagger-ui

### Configuration
Use env vars or `src/main/resources/application.properties`.

### Curl Examples
```
# Create
curl -s -X POST localhost:8080/api/items \
-H 'Content-Type: application/json' \
-d '{"name":"Pen","price":1.99,"quantity":50}'

# Get
curl -s localhost:8080/api/items/{ITEM_ID}

# Update
curl -s -X PUT localhost:8080/api/items/{ITEM_ID} \
-H 'Content-Type: application/json' \
-d '{"price":2.49,"quantity":60}'

# Sell 3
curl -s -X POST localhost:8080/api/items/{ITEM_ID}/sell \
-H 'Content-Type: application/json' \
-d '{"quantity":3}'

# Stock report
curl -s localhost:8080/api/reports/stock

# Sales report
curl -s "localhost:8080/api/reports/sales/summary"
```

### ID Generation
Default: app-generated UUIDs via Hibernate (e.g., @UuidGenerator).
Alternative: DB default gen_random_uuid(); if you choose this, remove generator annotations from the entity.

### Test

Requires Docker running for Testcontainers.

```
# Run unit + integration tests (uses Testcontainers for Postgres/Kafka)
 ./gradlew :test
```

### Troubleshooting

* Kafka connection refused: Check spring.kafka.bootstrap-servers matches Compose (localhost:9092 on host, kafka:9092 in container). 
* Flyway migration errors: Confirm DB credentials and schema; clear local volume if you changed the schema drastically.