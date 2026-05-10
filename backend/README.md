# Mortgage Application Microservice Starter

A production-ready Spring Boot microservice for processing mortgage loan applications with Kafka event streaming, PostgreSQL persistence, and JWT security.

## Features

- **Spring Boot 3.3.5** with Java 21
- **RESTful API** for application management
- **Kafka Integration** for event streaming
- **PostgreSQL** with Flyway migrations
- **JWT Authentication** with security filters
- **OpenAPI/Swagger** documentation
- **OpenTelemetry** instrumentation
- **Docker & Docker Compose** for containerization
- **Lombok** for boilerplate reduction
- **MapStruct** for entity mapping

## Project Structure

```
backend/
├── src/main/java/com/bank/mortgage
│   ├── config/              # Configuration classes
│   ├── controller/          # REST endpoints
│   ├── domain/              # JPA entities
│   ├── dto/                 # Request/response DTOs
│   ├── events/              # Kafka event handling
│   ├── exception/           # Exception handlers
│   ├── mapper/              # Entity mappers
│   ├── repository/          # Data repositories
│   ├── security/            # JWT & auth
│   ├── service/             # Business logic
│   └── util/                # Utilities
├── src/main/resources
│   ├── application.yml      # Base configuration
│   ├── application-dev.yml  # Dev profile
│   ├── application-prod.yml # Production profile
│   └── db/migration/        # Flyway migrations
└── pom.xml
```

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9.8+
- Docker & Docker Compose

### Local Development

1. Clone the repository
```bash
cd backend
```

2. Build the project
```bash
mvn clean package
```

3. Start services (from project root)
```bash
cd ../infra
docker-compose up -d
```

4. Run the application
```bash
java -jar target/mortgage-service-1.0.0.jar
```

Access Swagger UI at `http://localhost:8080/swagger-ui.html`

### Docker Deployment

```bash
cd infra
docker-compose up --build
```

The application will be available at `http://localhost:8080`

## API Endpoints

### Create Application
```bash
curl -X POST http://localhost:8080/api/v1/applications \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678",
    "loanAmount": 500000,
    "tenureMonths": 240,
    "income": 150000
  }'
```

### Get Application
```bash
curl http://localhost:8080/api/v1/applications/{id}
```

## Configuration

### Environment Variables

```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_HOST=postgres
DB_NAME=mortgage

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Security
JWT_SECRET=your-secret-key

# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
```

### Profiles

- **default**: Production-ready defaults
- **dev**: Debug logging, verbose SQL, test fixtures
- **prod**: Optimized for production with warnings-only logging

## Database

Flyway manages migrations:
- `V1__init_schema.sql` - Creates tables for users, applications, decisions, documents
- `V2__add_indexes.sql` - Performance indexes

Run migrations automatically on startup or manually:
```bash
mvn flyway:migrate
```

## Security

JWT tokens expected in Authorization header:
```
Authorization: Bearer <token>
```

Default secret (change in production):
```
jwt.secret=secret-key-for-jwt-token-signing-change-in-production
```

## Kafka Topics

- `loan.applications` - Application creation events

## Testing

Run tests:
```bash
mvn test
```

Integration tests use Testcontainers for PostgreSQL:
```bash
mvn verify
```

## Monitoring

OpenTelemetry integration provides distributed tracing. Logs include trace IDs:
```
[2026-05-10 10:30:45] [main] INFO com.bank.mortgage.service - Published application created event [traceId: 550e8400-e29b-41d4-a716-446655440000]
```

## Next Steps (Senior Enhancements)

- [ ] Outbox pattern for transactional event publishing
- [ ] Dead Letter Queue (DLQ) for failed Kafka messages
- [ ] Circuit breaker with Resilience4j
- [ ] CQRS read models
- [ ] Async document verification
- [ ] Kubernetes deployment manifests
- [ ] GitHub Actions CI/CD pipeline
- [ ] Performance tuning with caching
- [ ] Advanced rate limiting

## License

Apache 2.0
