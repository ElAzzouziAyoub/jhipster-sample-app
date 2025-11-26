# Docker Deployment Guide for JHipster App

## Quick Summary: What We Fixed

1. **Dockerfile** - Added `prod` profile to use PostgreSQL
2. **Database Configuration** - Used correct database name and credentials
3. **Port Mapping** - Exposed port 8080 for external access

## Step-by-Step Guide

### 1. Build the Docker Image

```bash
# Make sure you have the JAR file built
mvn package -DskipTests

# Build the Docker image
docker build -t jhipster-app:latest .
```

### 2. Start PostgreSQL Database

```bash
docker run -d --name postgresql \
  -e POSTGRES_DB=jhipsterSampleApplication \
  -e POSTGRES_USER=jhipsterSampleApplication \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:15
```

### 3. Start the JHipster App

```bash
docker run -d --name jhipster-app \
  --link postgresql:postgresql \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgresql:5432/jhipsterSampleApplication \
  -e SPRING_DATASOURCE_USERNAME=jhipsterSampleApplication \
  -e SPRING_DATASOURCE_PASSWORD=password \
  jhipster-app:latest
```

### 4. Verify It's Working

```bash
# Check container status
docker ps

# Check logs
docker logs jhipster-app

# Test health endpoint
curl http://localhost:8080/management/health
```

### 5. Access the App

- **Local:** http://localhost:8080
- **From another machine:** http://YOUR_SERVER_IP:8080

## Important Notes

### Dockerfile Requirements

Your `Dockerfile` should include:

```dockerfile
FROM eclipse-temurin:17-jre-focal
COPY target/jhipster-sample-application-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
```

**Key point:** The `-Dspring.profiles.active=prod` is crucial! Without it, the app uses `dev` profile which expects H2 database, not PostgreSQL.

### Database Configuration

- **Database name:** `jhipsterSampleApplication` (must match what's in your PostgreSQL container)
- **Username:** `jhipsterSampleApplication`
- **Password:** `password`
- **Connection URL:** `jdbc:postgresql://postgresql:5432/jhipsterSampleApplication`

The `--link postgresql:postgresql` allows the app container to resolve `postgresql` hostname to the database container.

### Troubleshooting

**Container exits immediately:**

```bash
docker logs jhipster-app  # Check the error
```

**Common issues:**

- Wrong database name → Check PostgreSQL container environment variables
- Missing `prod` profile → Rebuild image with updated Dockerfile
- Port already in use → Stop other containers using port 8080
- Database not ready → Wait 10-15 seconds after starting PostgreSQL

**Stop and clean up:**

```bash
docker stop jhipster-app postgresql
docker rm jhipster-app postgresql
```

## Using Docker Compose (Easier Alternative)

Create a `docker-compose.yml`:

```yaml
version: '3.8'
services:
  postgresql:
    image: postgres:15
    environment:
      POSTGRES_DB: jhipsterSampleApplication
      POSTGRES_USER: jhipsterSampleApplication
      POSTGRES_PASSWORD: password
    ports:
      - '5432:5432'
    healthcheck:
      test: ['CMD-SHELL', 'pg_isready -U jhipsterSampleApplication']
      interval: 10s
      timeout: 5s
      retries: 5

  jhipster-app:
    build: .
    image: jhipster-app:latest
    ports:
      - '8080:8080'
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgresql:5432/jhipsterSampleApplication
      SPRING_DATASOURCE_USERNAME: jhipsterSampleApplication
      SPRING_DATASOURCE_PASSWORD: password
    depends_on:
      postgresql:
        condition: service_healthy
```

Then simply run:

```bash
docker-compose up -d
```
