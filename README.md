# Distributed API Gateway

A production-grade API Gateway built with Spring Boot, implementing JWT authentication, Redis rate limiting, circuit breaking, and distributed tracing.

## Architecture

- Client sends request to API Gateway port 8080
- Gateway checks JWT token, applies rate limiting, circuit breaker
- Routes to Auth Service 8083, Order Service 8081, Product Service 8082

![Architecture Diagram](docs/architecture-diagram.svg)

## Features

- API Gateway and Routing via Spring Cloud Gateway
- JWT Authentication with JJWT 0.12.5
- Rate Limiting via Redis 10 requests per second per user
- Circuit Breaker via Resilience4j with automatic fallback
- Distributed Tracing via Zipkin and Micrometer
- Request Logging with Correlation ID
- Docker and Docker Compose for containerization
- Kubernetes manifests for production deployment
- GitHub Actions CI/CD pipeline

## Tech Stack

- Java 17 and Spring Boot 3.2.5
- Spring Cloud Gateway WebFlux Reactive
- Redis for Rate limiting
- Resilience4j for Circuit breaker
- Zipkin for Distributed tracing
- H2 Database for Auth service dev
- Docker and Kubernetes

## Prerequisites

Before running this project make sure you have installed:

- Java 17 or higher
- Maven 3.9 or higher
- Docker Desktop
- Git

Verify installations:

    java -version
    mvn -version
    docker --version
    git --version

## Quick Start

Step 1 - Clone the repository:

    git clone https://github.com/sumanshekhar435/distributed-api-gateway.git
    cd distributed-api-gateway

Step 2 - Start all services with Docker Compose:

    docker-compose up --build

Step 3 - Verify all services are running:

    docker-compose ps

All 6 services should show status UP.

## Environment Variables

These variables are pre-configured in docker-compose.yml for local development.
For production, set them as actual environment variables:

    JWT_SECRET        - Secret key for JWT signing (min 40 characters)
    REDIS_HOST        - Redis hostname (default: localhost)
    AUTH_SERVICE_URL  - Auth service URL (default: http://localhost:8083)
    ORDER_SERVICE_URL - Order service URL (default: http://localhost:8081)
    PRODUCT_SERVICE_URL - Product service URL (default: http://localhost:8082)
    ZIPKIN_URL        - Zipkin server URL (default: http://localhost:9411)

## Services and Ports

- API Gateway   : http://localhost:8080  (main entry point)
- Auth Service  : http://localhost:8083
- Order Service : http://localhost:8081
- Product Service: http://localhost:8082
- Redis         : localhost:6379
- Zipkin UI     : http://localhost:9411

## API Usage

### Step 1 - Register a user

    curl -X POST http://localhost:8080/api/auth/register \
      -H "Content-Type: application/json" \
      -d '{"username":"testuser","password":"Test@123","email":"test@test.com"}'

Response:

    {"message":"User registered successfully: testuser"}

### Step 2 - Login and get token

    curl -X POST http://localhost:8080/api/auth/login \
      -H "Content-Type: application/json" \
      -d '{"username":"testuser","password":"Test@123"}'

Response:

    {
      "accessToken": "eyJhbGci...",
      "refreshToken": "uuid-here",
      "tokenType": "Bearer",
      "expiresIn": 900,
      "username": "testuser",
      "role": "USER"
    }

### Step 3 - Use the token to access protected endpoints

    curl http://localhost:8080/api/orders \
      -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

    curl http://localhost:8080/api/products \
      -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

### Refresh token when expired

    curl -X POST http://localhost:8080/api/auth/refresh \
      -H "Content-Type: application/json" \
      -d '{"refreshToken":"your-refresh-token"}'

## API Endpoints

Auth endpoints (public - no token required):

    POST /api/auth/register   - Register new user
    POST /api/auth/login      - Login and get JWT tokens
    POST /api/auth/refresh    - Refresh access token

Order endpoints (JWT token required):

    GET  /api/orders          - Get all orders for logged in user
    POST /api/orders          - Create new order
    GET  /api/orders/{id}     - Get order by ID

Product endpoints (JWT token required):

    GET /api/products                    - Get all products
    GET /api/products/{id}               - Get product by ID
    GET /api/products/categories         - Get all categories
    GET /api/products?category=Electronics - Filter by category

## How JWT Auth Works

1. Client sends POST /api/auth/login and gets accessToken (valid 15 min)
2. Client sends request with header: Authorization: Bearer accessToken
3. Gateway validates JWT signature and expiry
4. Gateway extracts userId and username from token claims
5. Gateway adds X-User-Id and X-Username headers to request
6. Request is forwarded to downstream service with user context

## Rate Limiting

- 10 requests per second per user via Redis token bucket algorithm
- Burst capacity of 20 requests allowed
- Exceeding limit returns HTTP 429 Too Many Requests
- Rate limit key is JWT user ID (falls back to IP if no token)

## Circuit Breaker States

- CLOSED: normal operation, all requests go through
- OPEN: triggered when 50 percent of last 10 requests fail
- OPEN state: returns fallback response immediately (no downstream call)
- HALF-OPEN: after 10 seconds, allows 3 test requests
- Back to CLOSED if test requests succeed

## Monitoring Endpoints

    GET /actuator/health              - Overall health status
    GET /actuator/circuitbreakers     - Circuit breaker states
    GET /actuator/gateway/routes      - All configured routes
    GET /actuator/metrics             - Application metrics

Zipkin distributed tracing UI: http://localhost:9411

## Testing with Postman

Import the included postman-collection.json file into Postman:

1. Open Postman
2. Click Import
3. Select postman-collection.json from the project root
4. Run the Login User request first - access token is automatically saved
5. All other requests will use the saved token automatically

## Kubernetes Deployment

Apply all manifests:

    kubectl apply -f k8s/

Check deployment status:

    kubectl get pods
    kubectl get services

Individual service files in k8s/ folder:

    redis-deployment.yaml
    zipkin-deployment.yaml
    auth-deployment.yaml
    order-deployment.yaml
    product-deployment.yaml
    gateway-deployment.yaml
    secret.yaml

## Project Structure

    distributed-api-gateway/
    ├── gateway-service/
    │   └── src/main/java/com/gateway/gatewayservice/
    │       ├── config/         - GatewayConfig, RateLimiterConfig, SecurityConfig
    │       ├── filter/         - JwtAuthFilter, RequestLoggingFilter
    │       ├── fallback/       - FallbackController (circuit breaker responses)
    │       └── util/           - JwtUtil
    ├── auth-service/
    │   └── src/main/java/com/auth/authservice/
    │       ├── controller/     - AuthController
    │       ├── service/impl/   - AuthServiceImpl
    │       ├── model/entity/   - User, RefreshToken
    │       ├── model/dto/      - Request and Response DTOs
    │       ├── repository/     - UserRepository, RefreshTokenRepository
    │       └── util/           - JwtService
    ├── order-service/          - Order management service
    ├── product-service/        - Product catalog service
    ├── k8s/                    - Kubernetes deployment manifests
    ├── docs/                   - Architecture diagrams
    ├── .github/workflows/      - GitHub Actions CI/CD pipeline
    ├── docker-compose.yml      - Local development setup
    ├── postman-collection.json - API testing collection
    └── README.md

## Author

Shekhar Suman
Java Backend Engineer - 4 years experience
Spring Boot, Microservices, Distributed Systems
GitHub: https://github.com/sumanshekhar435