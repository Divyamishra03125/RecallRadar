# RecallRadar 🛡️ Enterprise Production Deployment & Operations Guide

> **System:** RecallRadar — AI-Powered Product Recall Intelligence & Risk Analysis Platform  
> **Release Target:** Production / High-Availability  
> **Supported Runtime:** Java 21+ LTS, Node.js 18+ LTS, PostgreSQL 15+ (Verified on PostgreSQL 18.1)

---

## Table of Contents
1. [Architecture & Topology Overview](#1-architecture--topology-overview)
2. [Prerequisites & System Requirements](#2-prerequisites--system-requirements)
3. [Configuration & Environment Reference](#3-configuration--environment-reference)
4. [Database Provisioning & Flyway Migrations](#4-database-provisioning--flyway-migrations)
5. [Database Backup, Restore & Disaster Recovery](#5-database-backup-restore--disaster-recovery)
6. [Backend Deployment Options](#6-backend-deployment-options)
   - [Option A: Linux Systemd Service](#option-a-linux-systemd-service)
   - [Option B: Production Docker Containerization](#option-b-production-docker-containerization)
   - [Option C: Docker Compose (Full Stack)](#option-c-docker-compose-full-stack)
7. [Frontend Deployment Options](#7-frontend-deployment-options)
   - [Nginx Production Reverse Proxy & Static Host](#nginx-production-reverse-proxy--static-host)
   - [Cloud Hosting (Vercel / Netlify / AWS S3 + CloudFront)](#cloud-hosting-vercel--netlify--aws-s3--cloudfront)
8. [Production Security Hardening](#8-production-security-hardening)
9. [Verification & Smoke Test Runbook](#9-verification--smoke-test-runbook)
10. [Troubleshooting & Operational Runbook](#10-troubleshooting--operational-runbook)

---

## 1. Architecture & Topology Overview

RecallRadar is architected as a high-performance, decoupled three-tier system:

```
[ Web Clients / Browsers ]
          │ (HTTPS)
          ▼
   ┌──────────────┐
   │ Nginx Reverse│  <-- SSL Termination, Static Asset Serving, Rate Limiting
   │    Proxy     │
   └──────┬───────┘
          │ (HTTP /api/v1/*)
          ▼
┌─────────────────────────┐
│ Spring Boot Backend     │  <-- Java 21 LTS, Spring Web, Spring Security, Flyway
│ Port: 8080              │      Stateless JWT Auth, Hibernate/JPA, Database-First AI
└─────┬──────────────┬────┘
      │              │
      ▼              ▼
┌──────────────┐   ┌───────────────────────┐
│  PostgreSQL  │   │  Google Gemini Flash  │
│  18.1 RDBMS  │   │  AI Inference Engine  │
│  Port: 5432  │   │  (gemini-3.5-flash /  │
└──────────────┘   │   gemini-flash-lite)  │
                   └───────────────────────┘
```

### Component Breakdown
- **Frontend SPA**: React 18, Vite build tool, Tailwind-compatible modern CSS, Recharts for dynamic visual intelligence, Lucide icons, responsive design supporting desktop to mobile viewports.
- **Backend Core**: Java 21, Spring Boot 4.1.2, Spring Security with BCrypt password hashing (work factor 10) and stateless HMAC-SHA256 JWT tokens, ModelMapper DTO translation, Flyway automated versioned migrations.
- **Primary Data Layer**: PostgreSQL 18.1 storing registered product catalogue, recall safety notices, immutable severity audit log, and role-based user accounts.
- **AI Intelligence Layer**: Database-first hybrid search architecture. Direct deterministic database searches execute in 15–130ms. Complex natural language queries route to Google Gemini Flash with bounded timeouts and failover cascade.

---

## 2. Prerequisites & System Requirements

### Hardware Sizing Guidelines
| Environment | vCPU | RAM | Disk Storage | Target Concurrent Users |
|---|---|---|---|---|
| **Development** | 2 | 4 GB | 20 GB SSD | 1–5 |
| **Production (Standard)** | 4 | 8 GB | 50 GB NVMe | 100–500 |
| **Production (High-Scale)** | 8 | 16 GB | 100+ GB NVMe | 1,000+ |

### Software Prerequisites
- **Operating System**: Ubuntu 22.04 / 24.04 LTS, Debian 12, RHEL 9, or Windows Server 2022.
- **Java Development Kit (JDK)**: OpenJDK 21 or Eclipse Temurin 21+.
- **Node.js Runtime**: Node.js 18.x or 20.x LTS with `npm` 9+.
- **PostgreSQL**: PostgreSQL 15.x, 16.x, 17.x, or 18.x.
- **Build Utilities**: Apache Maven 3.9+ or packaged `./mvnw`.
- **API Credentials**: Active Google Gemini API Key from Google AI Studio.

---

## 3. Configuration & Environment Reference

All configurable properties are parameterized via environment variables or loaded from root `.env` / `recallradar-backend/.env`.

### 3.1 Backend Environment Variables

| Variable Name | Required | Default Value | Production Recommendation | Description |
|---|---|---|---|---|
| `DB_URL` | **Yes** | `jdbc:postgresql://localhost:5432/recallradar` | `jdbc:postgresql://<db-host>:5432/<dbname>?sslmode=require` | JDBC connection string |
| `DB_USERNAME` | **Yes** | `postgres` | Dedicated unprivileged user e.g. `recallradar_app` | Database username |
| `DB_PASSWORD` | **Yes** | `postgres` | Strong random password (>= 24 chars) | Database user password |
| `JWT_SECRET` | **Yes** | Built-in fallback | Base64 or 64+ char random string | Secret key for signing HMAC-SHA256 tokens |
| `JWT_EXPIRATION_MS` | No | `86400000` (24h) | `28800000` (8h) or `86400000` | JWT token validity in milliseconds |
| `CORS_ALLOWED_ORIGINS` | **Yes** | `http://localhost:5173,...` | `https://yourdomain.com` | Comma-separated list of allowed web origins |
| `RECALLRADAR_GEMINI_API_KEY` | **Yes** | None | Secure API key | Google AI Gemini API secret key |
| `GEMINI_MODEL` | No | `gemini-3.5-flash` | `gemini-3.5-flash` | Primary low-latency AI inference model |
| `GEMINI_FALLBACK_MODEL` | No | `gemini-flash-lite-latest`| `gemini-flash-lite-latest` | Fallback model in case of rate limits or 503 |
| `GEMINI_MAX_RETRIES` | No | `3` | `3` | Maximum retry attempts per model |
| `GEMINI_RETRY_DELAY_MS` | No | `1000` | `1000` | Exponential backoff base interval in ms |
| `SERVER_PORT` | No | `8080` | `8080` | HTTP listening port for Spring Boot |

### 3.2 Frontend Environment Variables (`frontend/.env`)

| Variable Name | Required | Default Value | Production Recommendation | Description |
|---|---|---|---|---|
| `VITE_API_URL` | **Yes** | `http://localhost:8080/api/v1` | `/api/v1` (behind reverse proxy) | Base REST API URL |

---

## 4. Database Provisioning & Flyway Migrations

### 4.1 Create Database and User (PostgreSQL CLI)

Log into PostgreSQL as `postgres` superuser:

```bash
psql -U postgres
```

Execute initialization script:

```sql
-- 1. Create dedicated application database
CREATE DATABASE recallradar
    WITH OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'C'
    LC_CTYPE = 'C'
    TEMPLATE = template0;

-- 2. Connect to the database
\c recallradar

-- 3. Create production app user with strict privileges
CREATE USER recallradar_app WITH ENCRYPTED PASSWORD 'ReplaceWithStrongProductionPassword!';
GRANT CONNECT ON DATABASE recallradar TO recallradar_app;
GRANT USAGE, CREATE ON SCHEMA public TO recallradar_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO recallradar_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO recallradar_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO recallradar_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO recallradar_app;
```

### 4.2 Flyway Versioned Migrations

Flyway runs automatically on backend startup (`spring.flyway.enabled=true`). The migrations execute in strict order:

1. **`V1__init_schema.sql`**: Initial tables (`product`, `recall`, `recall_severity_audit`, `app_user`).
2. **`V2__seed_demo_data.sql`**: Initial baseline products and regulatory notices.
3. **`V3__production_rbac_and_evidence.sql`**: Sets up BCrypt RBAC credentials (`admin`, `analyst`, `viewer`).
4. **`V4__severity_evidence_and_catalog_expansion.sql`**: Adds `severity_evidence`, `severity_classification_source` columns to `recall` and `recall_severity_audit`, expands catalogue to 17 registered products.

#### Manual Flyway Verification (Command Line)
To verify or repair Flyway status manually using Maven:

```bash
cd recallradar-backend
mvn flyway:info
```

To repair schema history in case of a failed step:
```bash
mvn flyway:repair
```

---

## 5. Database Backup, Restore & Disaster Recovery

### 5.1 Automated Production Backup Script (Linux Bash)

Create `/opt/recallradar/backup_db.sh`:

```bash
#!/bin/bash
set -eo pipefail

BACKUP_DIR="/var/backups/recallradar"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/recallradar_backup_${TIMESTAMP}.dump"
LOG_FILE="${BACKUP_DIR}/backup.log"
RETENTION_DAYS=14

mkdir -p "${BACKUP_DIR}"

export PGPASSWORD="${DB_PASSWORD:-postgres}"
DB_HOST="${DB_HOST:-localhost}"
DB_USER="${DB_USERNAME:-postgres}"
DB_NAME="${DB_NAME:-recallradar}"

echo "[$(date)] Starting database backup..." >> "${LOG_FILE}"

pg_dump -h "${DB_HOST}" -U "${DB_USER}" -d "${DB_NAME}" \
    -F c -b -v -f "${BACKUP_FILE}" 2>> "${LOG_FILE}"

echo "[$(date)] Backup completed successfully: ${BACKUP_FILE}" >> "${LOG_FILE}"

# Enforce 14-day retention policy
find "${BACKUP_DIR}" -type f -name "recallradar_backup_*.dump" -mtime +${RETENTION_DAYS} -delete
echo "[$(date)] Cleaned up backups older than ${RETENTION_DAYS} days." >> "${LOG_FILE}"
```

Make executable and add to crontab for daily execution at 02:00 AM:
```bash
chmod +x /opt/recallradar/backup_db.sh
crontab -e
# Add line:
0 2 * * * /opt/recallradar/backup_db.sh
```

### 5.2 PowerShell Backup Script (Windows)

Create `C:\RecallRadar\backup_db.ps1`:

```powershell
$Date = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupDir = "C:\RecallRadar\Backups"
$BackupFile = "$BackupDir\recallradar_backup_$Date.dump"
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

$env:PGPASSWORD = "postgres"
& "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -h localhost -U postgres -d recallradar -F c -b -v -f $BackupFile

# Retention: delete dumps older than 14 days
Get-ChildItem -Path $BackupDir -Filter "recallradar_backup_*.dump" | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } | Remove-Item -Force
Write-Host "Backup completed: $BackupFile"
```

### 5.3 Database Restoration Procedure

To restore a `.dump` archive to a fresh database:

```bash
# 1. Terminate active backend connections to database
psql -U postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'recallradar' AND pid <> pg_backend_pid();"

# 2. Drop and recreate clean database
dropdb -U postgres recallradar
createdb -U postgres recallradar

# 3. Restore from custom format archive
pg_restore -U postgres -d recallradar -v /var/backups/recallradar/recallradar_backup_YYYYMMDD_HHMMSS.dump
```

---

## 6. Backend Deployment Options

### Option A: Linux Systemd Service

1. Build production JAR:
```bash
cd recallradar-backend
mvn clean package -DskipTests
```
The output is at `recallradar-backend/target/recallradar-backend-0.0.1-SNAPSHOT.jar`.

2. Copy JAR to deployment directory:
```bash
sudo mkdir -p /opt/recallradar
sudo cp target/recallradar-backend-0.0.1-SNAPSHOT.jar /opt/recallradar/recallradar.jar
sudo useradd -r -s /bin/false recallradar
sudo chown -R recallradar:recallradar /opt/recallradar
```

3. Create environment file `/opt/recallradar/recallradar.env`:
```ini
DB_URL=jdbc:postgresql://localhost:5432/recallradar
DB_USERNAME=recallradar_app
DB_PASSWORD=YourSecureDatabasePassword
JWT_SECRET=YourSuperSecretHMACSHA256KeyWithAtLeast64RandomCharacters!
CORS_ALLOWED_ORIGINS=https://recallradar.yourdomain.com
RECALLRADAR_GEMINI_API_KEY=AIzaSyYourGeminiApiKey
GEMINI_MODEL=gemini-3.5-flash
GEMINI_FALLBACK_MODEL=gemini-flash-lite-latest
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError
```

4. Create systemd unit `/etc/systemd/system/recallradar.service`:
```ini
[Unit]
Description=RecallRadar Backend Service
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=recallradar
Group=recallradar
WorkingDirectory=/opt/recallradar
EnvironmentFile=/opt/recallradar/recallradar.env
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/recallradar/recallradar.jar
Restart=always
RestartSec=10
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

5. Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable recallradar
sudo systemctl start recallradar
sudo systemctl status recallradar
```

---

### Option B: Production Docker Containerization

Create `Dockerfile` in `recallradar-backend/Dockerfile`:

```dockerfile
# Stage 1: Build JAR with Maven & OpenJDK 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal hardened runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-privileged system user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build --chown=appuser:appgroup /app/target/recallradar-backend-0.0.1-SNAPSHOT.jar app.jar

ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

Build and test Docker image:
```bash
docker build -t recallradar-backend:latest ./recallradar-backend
docker run -d --name recallradar-api -p 8080:8080 \
  --env-file ./recallradar-backend/.env \
  recallradar-backend:latest
```

---

### Option C: Docker Compose (Full Stack)

Create `docker-compose.yml` in root directory:

```yaml
version: '3.8'

services:
  db:
    image: postgres:16-alpine
    container_name: recallradar-db
    restart: always
    environment:
      POSTGRES_DB: recallradar
      POSTGRES_USER: ${DB_USERNAME:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-postgres}"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./recallradar-backend
      dockerfile: Dockerfile
    container_name: recallradar-backend
    restart: always
    depends_on:
      db:
        condition: service_healthy
    environment:
      DB_URL: jdbc:postgresql://db:5432/recallradar
      DB_USERNAME: ${DB_USERNAME:-postgres}
      DB_PASSWORD: ${DB_PASSWORD:-postgres}
      JWT_SECRET: ${JWT_SECRET:-DefaultEnterpriseProductionJwtSigningSecretKey2026!}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost,http://localhost:80,http://localhost:5173}
      RECALLRADAR_GEMINI_API_KEY: ${RECALLRADAR_GEMINI_API_KEY}
      GEMINI_MODEL: gemini-3.5-flash
      GEMINI_FALLBACK_MODEL: gemini-flash-lite-latest
    ports:
      - "8080:8080"

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: recallradar-frontend
    restart: always
    depends_on:
      - backend
    ports:
      - "80:80"

volumes:
  postgres_data:
```

Launch full stack:
```bash
docker compose up -d
```

---

## 7. Frontend Deployment Options

### Nginx Production Reverse Proxy & Static Host

1. Build static production bundle:
```bash
cd frontend
npm ci
npm run build
```
The optimized bundle is generated in `frontend/dist/`.

2. Copy files to webroot:
```bash
sudo mkdir -p /var/www/recallradar
sudo cp -r dist/* /var/www/recallradar/
sudo chown -R www-data:www-data /var/www/recallradar
```

3. Nginx Server Block `/etc/nginx/sites-available/recallradar.conf`:
```nginx
server {
    listen 80;
    server_name recallradar.yourdomain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name recallradar.yourdomain.com;

    # SSL Certificates
    ssl_certificate /etc/letsencrypt/live/recallradar.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/recallradar.yourdomain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Security Headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; connect-src 'self' https://generativelanguage.googleapis.com;" always;

    root /var/www/recallradar;
    index index.html;

    # Static file caching
    location ~* \.(?:css|js|woff2?|svg|png|jpg|ico)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # SPA client-side routing fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Reverse proxy API requests to Spring Boot backend
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
        proxy_connect_timeout 10s;
    }

    # Health check passthrough
    location /actuator/health {
        proxy_pass http://127.0.0.1:8080/actuator/health;
        access_log off;
    }
}
```

Enable site and restart Nginx:
```bash
sudo ln -s /etc/nginx/sites-available/recallradar.conf /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

### Cloud Hosting (Vercel / Netlify / AWS S3 + CloudFront)

#### Vercel (`frontend/vercel.json`):
```json
{
  "rewrites": [
    { "source": "/api/(.*)", "destination": "https://api.yourdomain.com/api/$1" },
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

#### Netlify (`frontend/public/_redirects`):
```text
/api/*  https://api.yourdomain.com/api/:splat  200
/*      /index.html                            200
```

---

## 8. Production Security Hardening

1. **Principle of Least Privilege**:
   - Never run Spring Boot backend or PostgreSQL as `root`.
   - Dedicated database role `recallradar_app` should not have `SUPERUSER` or `CREATEDB` permissions.
2. **Stateless JWT Rotation**:
   - Set `JWT_SECRET` using a minimum 256-bit cryptographically random key.
   - Recommended generator:
     ```bash
     openssl rand -base64 48
     ```
3. **Database Connection Safety**:
   - Append `?sslmode=require` to `DB_URL` in cloud environments (e.g. AWS RDS, Azure Database for PostgreSQL).
4. **CORS Isolation**:
   - Set `CORS_ALLOWED_ORIGINS` to exact trusted origins. Never use `*` with credentials enabled.
5. **AI Prompt Injection & Grounding Safeguards**:
   - RecallRadar features strict grounding and hallucination detection in `OpenAiService.java`. The AI output is vetted against database context IDs before returning to clients.

---

## 9. Verification & Smoke Test Runbook

Perform the following sequential smoke tests to certify production readiness:

### 1. Actuator Health Verification
```bash
curl -I http://localhost:8080/actuator/health
```
**Expected:** `HTTP/1.1 200 OK`, `{"status":"UP"}`

### 2. Public Catalog Read Test
```bash
curl -s http://localhost:8080/api/v1/products | jq '.[0]'
```
**Expected:** Returns first product record without requiring authorization header.

### 3. Unauthorized Mutation Rejection Test
```bash
curl -i -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Hacker Item","brand":"Fake","category":"Test","modelNumber":"HK-01"}'
```
**Expected:** `HTTP/1.1 403 Forbidden`

### 4. Admin Authentication & Protected Operation
```bash
# 1. Login as Admin
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"adminpassword"}' | jq -r '.token')

echo "Acquired JWT: ${TOKEN}"

# 2. Authenticated Product Creation
curl -i -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Safety Certified Helmet","brand":"ApexShield","category":"Sports","modelNumber":"AS-HLM-2026","batchNumber":"B2026-A"}'
```
**Expected:** `HTTP/1.1 201 Created`

### 5. Chatbot Database-First Latency Benchmark (< 100ms)
```bash
curl -w "\nTime: %{time_total}s\n" -s -X POST http://localhost:8080/api/v1/ai/assistant \
  -H "Content-Type: application/json" \
  -d '{"question":"Find model SM-S921B"}'
```
**Expected:** Sub-100ms response with direct database payload and source status badge.

### 6. Severity Review with Evidence Requirement Check
```bash
# Attempt CRITICAL without evidence (should fail 400):
curl -i -X PATCH http://localhost:8080/api/v1/recalls/14/severity \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"severity":"CRITICAL","reason":"short"}'
# Expected: HTTP 400 Bad Request

# Submit CRITICAL with evidence and classification source:
curl -i -X PATCH http://localhost:8080/api/v1/recalls/14/severity \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"severity":"CRITICAL","reason":"Hazard analysis confirmed","evidence":"CPSC Notice #24-105 with fatal infant entrapment","classificationSource":"CPSC Official Notice","changedBy":"admin"}'
# Expected: HTTP 200 OK
```

---

## 10. Troubleshooting & Operational Runbook

### Issue 1: AI Assistant Takes > 10 Seconds or Fails with HTTP 503 / 429
- **Cause**: Outdated Gemini model configured (`gemini-2.5-flash` exhausted, or model quota limits reached).
- **Remedy**: Verify `.env` models:
  ```ini
  GEMINI_MODEL=gemini-3.5-flash
  GEMINI_FALLBACK_MODEL=gemini-flash-lite-latest
  ```
  The system will automatically fail over from primary to fallback in 1–2 seconds.

### Issue 2: Flyway Migration Checksum Mismatch
- **Cause**: A previously applied SQL migration file was edited after application.
- **Remedy**: Run `mvn flyway:repair` to recalculate checksums in `flyway_schema_history`.

### Issue 3: HikariCP Connection Pool Timeout (`Connection is not available`)
- **Cause**: PostgreSQL connection limit exceeded or database slow to respond.
- **Remedy**: Tune HikariCP pool in `application.properties`:
  ```properties
  spring.datasource.hikari.maximum-pool-size=20
  spring.datasource.hikari.minimum-idle=5
  spring.datasource.hikari.connection-timeout=20000
  spring.datasource.hikari.idle-timeout=300000
  spring.datasource.hikari.max-lifetime=1200000
  ```

### Issue 4: Frontend Returns 404 on Browser Page Refresh
- **Cause**: Web server (Nginx or Apache) is attempting to locate `/products` or `/analytics` as a physical filesystem directory.
- **Remedy**: Ensure `try_files $uri $uri/ /index.html;` is active in Nginx configuration.

---

*RecallRadar Operations Guide — Antigravity Engineering*
