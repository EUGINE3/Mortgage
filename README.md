# Mortgage Application Microservice

Complete Spring Boot 3.3.5 starter project for mortgage loan processing with production-ready features.

## Quick Start

```bash
# Build
cd backend && mvn clean package

# Run with Docker
cd ../infra && docker-compose up --build

# Access
- API: http://localhost:8080/api/v1/applications
- Swagger: http://localhost:8080/swagger-ui.html
- Kafka UI: http://localhost:8085
```

## Key Technologies

✅ Spring Boot 3.3.5 | Spring Security | Spring Data JPA | Spring Kafka
✅ PostgreSQL | Flyway Migrations
✅ JWT Authentication | OpenAPI/Swagger
✅ Docker & Docker Compose
✅ OpenTelemetry Instrumentation
✅ MapStruct | Lombok

## Project Structure

- `backend/` - Spring Boot application
- `infra/` - Docker Compose configuration
- `docs/` - API documentation

## Documentation

See `backend/README.md` for detailed configuration, API examples, and development guide.

## Example Request

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

## Next Steps

1. Update JWT secret in `.env.example`
2. Configure database connection for your environment
3. Customize Kafka topics and event handling
4. Add business logic to service implementations
5. Create comprehensive unit/integration tests
6. Set up CI/CD pipeline (GitHub Actions recommended)

---

**Ready for production development!**
