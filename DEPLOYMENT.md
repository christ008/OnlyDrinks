# OnlyDrinks Deployment Guide

## Overview

OnlyDrinks is a Progressive Web App (PWA) built with Java 25, Spring Boot 4, and Vaadin 25. It supports two deployment modes:

- **Development**: H2 in-memory database
- **Production**: PostgreSQL + Valkey (Redis) with Docker Compose

## Prerequisites

- Java 25 JDK
- Maven 3.9+
- Docker & Docker Compose (for production)
- Node.js 24+ (automatically managed by Vaadin)

## Development Mode

### Running Locally

```bash
# Using Maven
./mvnw spring-boot:run

# Or with profile explicitly
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on http://localhost:8080

**Default credentials:**
- User: `user` / Password: `user`
- Admin: `admin` / Password: `admin`

**H2 Console:** http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:onlydrinks`
- Username: `sa`
- Password: (empty)

## Production Deployment

### Option 1: Docker Compose (Recommended)

This is the easiest way to deploy on commodity hardware.

1. **Copy environment template:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` and set your database password:**
   ```bash
   DB_PASSWORD=your_secure_password_here
   ```

3. **Start all services:**
   ```bash
   docker-compose up -d
   ```

4. **View logs:**
   ```bash
   docker-compose logs -f app
   ```

5. **Stop services:**
   ```bash
   docker-compose down
   ```

**Services:**
- App: http://localhost:8080
- PostgreSQL: localhost:5432
- Valkey: localhost:6379

### Option 2: Spring Boot Buildpacks

Build a hardened Docker image using Cloud Native Buildpacks (Alpaquita Linux `musl` + BellSoft Liberica JRE):

```bash
# Build the image (no Dockerfile needed)
./mvnw spring-boot:build-image

# Run the container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your-db-host \
  -e DB_PASSWORD=your-password \
  onlydrinks/only-drinks:1.0-SNAPSHOT
```

### Option 3: Traditional Docker Build

```bash
# Build the image
docker build -t onlydrinks:latest .

# Run the container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your-db-host \
  -e DB_PASSWORD=your-password \
  onlydrinks:latest
```

### Option 4: Standalone JAR

```bash
# Build the JAR
./mvnw clean package

# Run with production profile
java -jar target/only-drinks-1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --DB_PASSWORD=your-password
```

## Production Configuration

### Database Schema

On first run, you need to initialize the database schema:

```bash
# Set ddl-auto to create for first run
docker-compose exec app sh
# Then change application-prod.yml ddl-auto from 'validate' to 'create'
# Restart the app, then change it back to 'validate'
```

**Or use Flyway/Liquibase for migrations (recommended for production).**

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile (dev/prod) | dev |
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | onlydrinks |
| `DB_USERNAME` | Database user | onlydrinks |
| `DB_PASSWORD` | Database password | (required) |
| `REDIS_HOST` | Valkey/Redis host | localhost |
| `REDIS_PORT` | Valkey/Redis port | 6379 |
| `REDIS_PASSWORD` | Valkey/Redis password | (empty) |
| `APP_PORT` | Application port | 8080 |

### JVM Tuning for Commodity Hardware

With Buildpacks, JVM options are set through environment variables:

```bash
# Example: override the image build-time defaults
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.environment.BPE_APPEND_JAVA_TOOL_OPTIONS="-Xmx768m -Xms384m -XX:+ExitOnOutOfMemoryError"
```

Virtual threads are enabled by default for better concurrency with less memory.

## PWA Features

OnlyDrinks is a Progressive Web App:

- ✅ Installable on mobile and desktop
- ✅ Offline page when no connection
- ✅ App-like experience
- ✅ Responsive design

### Installing the PWA

1. Open the app in a browser
2. Look for "Install" or "Add to Home Screen" prompt
3. Follow the installation wizard

**Note:** You need to provide a 512x512 icon at:
`src/main/resources/META-INF/resources/icons/icon.png`

## Security Considerations

### Before Production:

1. **Change default passwords** in `Application.java` CommandLineRunner
2. **Use environment variables** for all secrets
3. **Enable HTTPS** (reverse proxy with Nginx/Traefik)
4. **Set strong database passwords**
5. **Configure firewall rules** (only expose port 8080)
6. **Enable Spring Security CSRF** (already enabled by Vaadin)
7. **Add rate limiting** (e.g., with Nginx)

### Reverse Proxy Example (Nginx)

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Monitoring

Add Spring Boot Actuator for production monitoring:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Access metrics at: `/actuator/health`, `/actuator/metrics`  
Probe endpoints: `/actuator/health/liveness`, `/actuator/health/readiness`

## Backup

### PostgreSQL Backup

```bash
# Backup
docker-compose exec postgres pg_dump -U onlydrinks onlydrinks > backup.sql

# Restore
docker-compose exec -T postgres psql -U onlydrinks onlydrinks < backup.sql
```

### Valkey Persistence

Valkey is configured with RDB persistence. Data is saved to Docker volume `valkey_data`.

## Troubleshooting

### App won't start

Check logs: `docker-compose logs -f app`

### Database connection errors

Verify PostgreSQL is running: `docker-compose ps postgres`

### Session issues

Check Valkey: `docker-compose exec valkey valkey-cli ping`

### Port conflicts

Change ports in `.env` file before starting

## Performance Tips

1. Use virtual threads (already enabled)
2. Enable JPA batch inserts (already configured)
3. Use connection pooling (HikariCP, already configured)
4. Add Redis caching for frequent queries
5. Enable Vaadin production mode (automatic with `mvn package`)

## Support

For issues and questions, refer to:
- Spring Boot 4: https://docs.spring.io/spring-boot/
- Vaadin 25: https://vaadin.com/docs
- PostgreSQL: https://www.postgresql.org/docs/
