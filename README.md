# Microservices-Based Object Storage Service  (In Development)

A scalable, high-performance distributed storage service designed to handle large-scale file uploads, metadata management, and asset retrieval efficiently. 

This project focuses on implementing robust backend architecture patterns—such as centralized routing, asynchronous event processing, and distributed caching—to ensure high availability and low latency.





# Auth Service — Complete Deployment Guide

> **Who this is for:** Anyone deploying this service — whether it's your first time
> touching Docker or you're an experienced engineer looking for the specifics of this project.
> Every concept is explained from scratch. Nothing is assumed.

---

## Table of Contents

1. [What is Deployment?](#1-what-is-deployment)
2. [Understanding the Stack](#2-understanding-the-stack)
3. [Prerequisites](#3-prerequisites)
4. [Project Structure](#4-project-structure)
5. [Environment Variables — The Foundation](#5-environment-variables--the-foundation)
6. [Understanding Docker](#6-understanding-docker)
7. [Understanding Docker Compose](#7-understanding-docker-compose)
8. [The Dockerfile — How the App is Packaged](#8-the-dockerfile--how-the-app-is-packaged)
9. [Local Deployment — Step by Step](#9-local-deployment--step-by-step)
10. [Verifying Everything Works](#10-verifying-everything-works)
11. [Common Issues and Fixes](#11-common-issues-and-fixes)
12. [Understanding Spring Profiles](#12-understanding-spring-profiles)
13. [Production Deployment Concepts](#13-production-deployment-concepts)
14. [Useful Commands Reference](#14-useful-commands-reference)
15. [How Services Communicate Inside Docker](#15-how-services-communicate-inside-docker)
16. [Security Checklist Before Going Live](#16-security-checklist-before-going-live)

---

## 1. What is Deployment?

When you write code on your laptop and run it with `./gradlew bootRun`, only you can use it.
Nobody else on the internet can reach it. It lives only on your machine.

**Deployment** is the process of taking your code and putting it somewhere that other
people can actually use it.

```
Before Deployment:
  Your code → runs on YOUR laptop → only YOU can use it

After Deployment:
  Your code → runs on a SERVER → ANYONE can use it
              (or inside Docker → runs identically everywhere)
```

There are different levels of deployment:

```
Level 1 — Local Deployment
  Everything runs in Docker on your laptop
  Only you can access it (localhost)
  Used for: development and testing

Level 2 — Server Deployment
  Everything runs on a cloud server (AWS, GCP, DigitalOcean)
  Anyone on the internet can access it
  Used for: production (real users)

Level 3 — Kubernetes Deployment
  Multiple copies of your app run across multiple servers
  Automatically scales up/down based on traffic
  Used for: large-scale production (millions of users)
```

This guide covers **Level 1** in full detail and explains the concepts behind **Level 2 and 3**.

---

## 2. Understanding the Stack

This service uses three components that must all run together:

```
┌──────────────────────────────────────────────────────────┐
│                    Auth Service Stack                     │
│                                                          │
│  ┌─────────────────┐                                     │
│  │   Auth Service  │  Spring Boot App (Java 21)          │
│  │   Port: 8080    │  Handles: login, register,          │
│  │                 │  JWT, OAuth2, RBAC                  │
│  └────────┬────────┘                                     │
│           │ reads/writes users, tokens, roles            │
│           ▼                                              │
│  ┌─────────────────┐                                     │
│  │     MySQL       │  Relational Database                │
│  │   Port: 3308    │  Stores: users, roles,              │
│  │                 │  permissions, refresh tokens        │
│  └─────────────────┘                                     │
│           │ blacklists tokens, rate limiting             │
│           ▼                                              │
│  ┌─────────────────┐                                     │
│  │     Redis       │  In-Memory Cache                    │
│  │   Port: 6379    │  Stores: token blacklist,           │
│  │                 │  rate limit counters                │
│  └─────────────────┘                                     │
└──────────────────────────────────────────────────────────┘
```

**Why all three?**

- **MySQL** — permanent storage. User data survives restarts.
- **Redis** — fast temporary storage. Blacklisted tokens, rate limits. Auto-expires old data.
- **Auth Service** — the brain. Handles all business logic, talks to MySQL and Redis.

All three must be running for the service to work correctly.

---

## 3. Prerequisites

Before you can deploy, you need these installed on your machine:

### Docker Desktop

Docker is what runs everything in containers.

**Install:**
- Windows: [docs.docker.com/desktop/install/windows-install](https://docs.docker.com/desktop/install/windows-install/)
- Mac: [docs.docker.com/desktop/install/mac-install](https://docs.docker.com/desktop/install/mac-install/)
- Linux: [docs.docker.com/desktop/install/linux-install](https://docs.docker.com/desktop/install/linux-install/)

**Verify installation:**
```bash
docker --version
# Should print: Docker version 24.x.x

docker-compose --version
# Should print: Docker Compose version v2.x.x
```

**Recommended Docker Desktop settings:**

Go to Docker Desktop → Settings → Resources:
```
Memory: minimum 4GB (Spring Boot needs ~512MB, MySQL ~512MB, Redis ~100MB)
CPUs:   minimum 2
Disk:   minimum 20GB
```

### Java 21

Only needed if you want to run the app locally (outside Docker).

```bash
java --version
# Should print: openjdk 21.x.x
```

### Git

```bash
git --version
# Should print: git version 2.x.x
```

---

## 4. Project Structure

Understanding where every file lives before deploying:

```
auth-service/
│
├── Dockerfile                  ← Instructions to build the app image
├── docker-compose.yml          ← Defines all services (app + MySQL + Redis)
├── .env                        ← Secret values (NEVER commit this to Git)
├── .dockerignore               ← Files Docker should NOT copy
├── .gitignore                  ← Files Git should NOT track
│
├── build.gradle                ← Java dependencies
├── settings.gradle             ← Project name
│
└── src/
    └── main/
        └── resources/
            ├── application.yml         ← Base config (all environments)
            ├── application-dev.yml     ← Development overrides
            ├── application-prod.yml    ← Production overrides
            └── db/
                └── migration/          ← Flyway SQL files
                    ├── V1__create_users_table.sql
                    ├── V2__create_roles_permissions_tables.sql
                    ├── V3__create_token_tables.sql
                    ├── V4__seed_default_roles_permissions.sql
                    ├── V5__add_login_security_columns.sql
                    ├── V6__add_storage_permissions.sql
                    └── V7__verify_indexes.sql
```

---

## 5. Environment Variables — The Foundation

### What Are Environment Variables?

Imagine you're building a house. The blueprint (your code) is the same.
But the address, the key to the door, the alarm code — those are different for every house.

Environment variables are the "address and keys" for your application.
They hold values that change between environments (dev vs prod) and
values that are secret (passwords, API keys).

```
Code (same everywhere):
  String dbPassword = System.getenv("DB_PASSWORD");

.env file on your laptop (dev):
  DB_PASSWORD=localpassword123

Environment variable on production server:
  DB_PASSWORD=super-strong-production-password-nobody-knows
```

The code never changes. Only the values change per environment.

### Why Not Just Hardcode Values?

```java
// NEVER do this:
String dbPassword = "mypassword123";

Problems:
  1. If you push to GitHub → everyone sees your password
  2. Can't change password without changing and redeploying code
  3. Dev and prod use same password → security nightmare
  4. Teammate clones repo → has your production credentials
```

### The `.env` File

Create this file in your project root. This is for LOCAL DEVELOPMENT ONLY.

```bash
# ─────────────────────────────────────────────────────
# AUTH SERVICE — LOCAL DEVELOPMENT ENVIRONMENT
# ─────────────────────────────────────────────────────
# IMPORTANT: Never commit this file to Git
# Add .env to your .gitignore
# ─────────────────────────────────────────────────────

# Spring Profile — which set of configs to use
SPRING_PROFILES_ACTIVE=dev

# ── Database ──────────────────────────────────────────
DB_ROOT_PASSWORD=rootpassword123
DB_NAME=auth_service_db
DB_USERNAME=authuser
DB_PASSWORD=authpassword123

# ── Redis ─────────────────────────────────────────────
REDIS_PASSWORD=redispassword123

# ── JWT ───────────────────────────────────────────────
# Must be at least 32 characters (256 bits)
# Generate a random one: openssl rand -base64 32
JWT_SECRET=your-super-secret-jwt-key-must-be-at-least-32-chars

# ── Email (Mailtrap for dev — fake inbox) ─────────────
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your_mailtrap_username
MAIL_PASSWORD=your_mailtrap_password

# ── OAuth2 (GitHub) ───────────────────────────────────
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# ── Frontend ──────────────────────────────────────────
FRONTEND_URL=*
```

### Protecting Your Secrets

**Step 1:** Make sure `.gitignore` contains:

```
# Environment files — NEVER commit these
.env
*.env
.env.local
.env.production
```

**Step 2:** Verify `.env` is not tracked:

```bash
git status
# .env should NOT appear in the output
```

**Step 3:** If you accidentally committed `.env` already:

```bash
# Remove from Git tracking (but keep the file)
git rm --cached .env
git commit -m "remove .env from tracking"
```

### Generating a Secure JWT Secret

```bash
# On Mac/Linux:
openssl rand -base64 32

# On Windows (PowerShell):
[System.Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))

# Output example:
# K7mX2pL9nQ4vR8wE1yT5uO6iP3sA0dF7
```

---

## 6. Understanding Docker

### What is Docker?

Before Docker, deploying an app meant:

```
1. Buy a server
2. Install the right version of Java on it
3. Install MySQL
4. Install Redis
5. Configure everything
6. Hope it matches your laptop's setup
7. Debug for hours when it doesn't
```

Docker packages your app + all its dependencies into a single unit called a **container**.

```
Container = Your App + Java 21 + All Libraries + Config

One container. Runs identically on:
  - Your Windows laptop
  - Your teammate's Mac
  - A Linux server in AWS
  - A Kubernetes cluster in GCP
```

### Images vs Containers

This is the most important Docker concept to understand:

```
Image = Recipe (blueprint, template)
  → Read-only
  → Stored in Docker Hub or your local machine
  → Can be shared, versioned, pulled

Container = Running instance of an image (the actual thing)
  → Created FROM an image
  → Has its own filesystem, network, processes
  → Can be started, stopped, deleted

Analogy:
  Image     = Cookie cutter
  Container = The actual cookie

You can make 100 cookies from one cookie cutter.
You can run 100 containers from one image.
```

```bash
# Pull an image from Docker Hub
docker pull mysql:8.0

# Run a container from that image
docker run mysql:8.0

# List running containers
docker ps

# List all images on your machine
docker images
```

### Docker Layers

Every line in a Dockerfile creates a **layer**. Layers are cached.

```dockerfile
FROM eclipse-temurin:21-jre-alpine    # Layer 1 — base image
RUN adduser appuser                    # Layer 2 — create user
COPY app.jar .                         # Layer 3 — copy JAR
```

If Layer 1 and 2 haven't changed, Docker reuses them from cache.
Only Layer 3 rebuilds. This makes builds very fast after the first one.

---

## 7. Understanding Docker Compose

### What is Docker Compose?

Running one container is simple. But our service needs three containers
(app + MySQL + Redis) that:
- Start in the right order (MySQL before the app)
- Can talk to each other
- Share the same network
- Have their volumes persisted

Doing all this manually with `docker run` commands is painful:

```bash
# Without Docker Compose (painful):
docker network create auth_network
docker run -d --name mysql --network auth_network -e MYSQL_ROOT_PASSWORD=... mysql:8.0
docker run -d --name redis --network auth_network redis:7.2-alpine
docker run -d --name app --network auth_network -p 8080:8080 auth-service
```

Docker Compose lets you describe all of this in ONE file:

```yaml
# With Docker Compose (simple):
docker-compose up
```

One command. Everything starts in the right order with the right config.

### The `docker-compose.yml` File Explained

```yaml
version: '3.8'      # Docker Compose file format version

services:           # All the containers we want to run

  mysql:            # Service name — used to reference this container
    image: mysql:8.0                # Use this Docker image
    container_name: auth_mysql      # Name of the running container
    restart: unless-stopped         # Restart if it crashes (not if manually stopped)
    environment:                    # Environment variables inside the container
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}   # Read from .env file
      MYSQL_DATABASE: ${DB_NAME}
    ports:
      - "3308:3306"    # host_port:container_port
                       # 3306 is MySQL's port inside container
                       # 3308 is what you use from outside (your laptop)
    volumes:
      - mysql_data:/var/lib/mysql   # Persist data outside container
    networks:
      - auth_network   # Which network this container joins
    healthcheck:       # How Docker checks if this is healthy
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s    # Check every 10 seconds
      timeout: 5s      # Wait 5 seconds for response
      retries: 5       # Mark unhealthy after 5 failures

volumes:             # Named volumes — data survives container restarts
  mysql_data:        # MySQL data persists here
  redis_data:        # Redis data persists here

networks:            # Custom networks for service communication
  auth_network:
    driver: bridge   # Standard bridge network
```

### Ports Explained

```
ports:
  - "3308:3306"

Left side (3308)  = PORT ON YOUR LAPTOP
Right side (3306) = PORT INSIDE THE CONTAINER

MySQL always runs on 3306 inside the container.
We map it to 3308 on your laptop to avoid conflicts
(in case you have MySQL already installed on 3306).

Your laptop:          Container:
  localhost:3308  →  mysql-container:3306
```

### Volumes Explained

```
volumes:
  - mysql_data:/var/lib/mysql

Without this:
  Container deleted → ALL MySQL data gone
  Container restarted → ALL MySQL data gone

With this:
  Docker stores MySQL data in a "named volume" called mysql_data
  Container can be deleted/recreated — data survives
  mysql_data volume persists on your machine

Where is it actually stored?
  On Mac/Linux: /var/lib/docker/volumes/mysql_data/
  On Windows:   \\wsl$\docker-desktop-data\version-pack-data\community\docker\volumes\
```

---

## 8. The Dockerfile — How the App is Packaged

```dockerfile
# ════════════════════════════════════════════════════════════
# STAGE 1: BUILD
# ════════════════════════════════════════════════════════════
FROM gradle:8.5-jdk21 AS builder
```

`FROM` — start from this base image. `gradle:8.5-jdk21` has Gradle and JDK 21 already installed.
`AS builder` — name this stage "builder" so we can reference it later.

```dockerfile
WORKDIR /app
```

Set the working directory inside the container. All following commands run from here.

```dockerfile
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon 2>/dev/null || true
```

Copy Gradle files FIRST and download dependencies BEFORE copying source code.

**Why this order?**

```
If you copy source code first:
  ANY code change → re-download ALL dependencies → slow (2-5 minutes)

If you copy Gradle files first:
  Gradle files unchanged → dependencies layer is CACHED
  Only source code changes → dependency download SKIPPED → fast (10 seconds)

This is one of the most important Dockerfile optimization techniques.
```

```dockerfile
COPY src ./src
RUN gradle bootJar --no-daemon -x test
```

Now copy source code and build. `-x test` skips tests during build (tests run in CI separately).

```dockerfile
# ════════════════════════════════════════════════════════════
# STAGE 2: RUNTIME
# ════════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jre-alpine
```

Start a brand new stage with only the JRE (Java Runtime Environment), not the full JDK.

```
JDK (Java Development Kit) — what you need to COMPILE code
  → 500MB+, includes compiler, debugger, profiler

JRE (Java Runtime Environment) — what you need to RUN code
  → 150MB, includes only what's needed to execute

In production, you don't need to compile anything.
Use JRE → smaller image → faster startup → smaller attack surface.

alpine = minimal Linux (5MB vs Ubuntu's 200MB)
```

```dockerfile
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
```

Create a non-root user. **Never run production apps as root.**

```dockerfile
COPY --from=builder /app/build/libs/*.jar app.jar
```

Copy ONLY the JAR from the builder stage. Everything else (source code, Gradle files, build cache) stays in the builder and is discarded.

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

Docker checks this every 30 seconds. If it fails 3 times → container is marked `unhealthy`.
`start-period=60s` → give Spring Boot 60 seconds to start before checking.

```dockerfile
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

- `-XX:+UseContainerSupport` — JVM respects Docker memory limits (not host machine RAM)
- `-XX:MaxRAMPercentage=75.0` — use max 75% of container's memory limit
- `-Djava.security.egd=file:/dev/./urandom` — faster random number generation (faster startup)

---

## 9. Local Deployment — Step by Step

### Option A: Development Mode (Recommended for Active Development)

Run MySQL and Redis in Docker, but run the Spring Boot app directly on your machine.

**Why?** Faster feedback loop. No rebuilding Docker image on every code change.

```bash
# Step 1: Start only the infrastructure
docker-compose up -d mysql redis

# Step 2: Wait for MySQL to be ready (check health)
docker-compose ps
# Wait until mysql shows: healthy

# Step 3: Run the Spring Boot app locally
./gradlew bootRun

# App is now running at: http://localhost:8080
```

To stop:

```bash
# Stop the app: Ctrl+C in the terminal running bootRun

# Stop MySQL and Redis
docker-compose down
```

---

### Option B: Full Docker Deployment (Everything Containerized)

Run everything — app, MySQL, Redis — inside Docker.

**Step 1: Verify your `.env` file exists and has all values**

```bash
cat .env
# Should show all variables with values (not empty)
```

**Step 2: Build the Docker image**

```bash
docker-compose build auth-service

# Watch the output — you'll see each Dockerfile step
# First build takes 3-5 minutes (downloading dependencies)
# Subsequent builds take 30-60 seconds (cached layers)
```

**Step 3: Start everything**

```bash
docker-compose up -d

# -d means "detached" — runs in background
# Without -d, logs stream to your terminal (Ctrl+C stops everything)
```

**Step 4: Watch the startup logs**

```bash
docker-compose logs -f auth-service

# You should see:
# Flyway: Migrating schema to version 1 - create users table
# Flyway: Migrating schema to version 2 - create roles permissions tables
# ...
# Flyway: Successfully applied 7 migrations
# Started AuthServiceApplication in 8.3 seconds
```

Press `Ctrl+C` to stop watching logs (containers keep running).

**Step 5: Verify everything is healthy**

```bash
docker-compose ps

# Expected output:
# NAME           STATUS          PORTS
# auth_mysql     Up (healthy)    0.0.0.0:3308->3306/tcp
# auth_redis     Up (healthy)    0.0.0.0:6379->6379/tcp
# auth_service   Up (healthy)    0.0.0.0:8080->8080/tcp
```

All three should show `healthy`. If any shows `starting` — wait 30 seconds and check again.

---

### What Happens During Startup (In Order)

Understanding this helps you debug startup failures:

```
1. Docker Compose reads docker-compose.yml

2. MySQL container starts
   → Docker pulls mysql:8.0 image (first time only)
   → MySQL initializes the database
   → Creates auth_service_db database
   → Creates authuser with authpassword123
   → MySQL is ready → healthcheck passes

3. Redis container starts
   → Docker pulls redis:7.2-alpine image (first time only)
   → Redis starts with password protection
   → Redis is ready → healthcheck passes

4. auth-service container starts (only after MySQL + Redis are healthy)
   → Java JVM starts
   → Spring Boot context loads
   → Flyway connects to MySQL
   → Flyway checks flyway_schema_history table
   → Flyway runs any pending migrations (V1, V2, V3... in order)
   → Spring Security configures filter chain
   → Application starts → listening on port 8080
   → Health endpoint returns UP
```

---

## 10. Verifying Everything Works

### Check 1: Health Endpoint

```bash
curl http://localhost:8080/actuator/health

# Expected response:
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

If `db` or `redis` shows `DOWN` → check that those containers are healthy.

### Check 2: Register a User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Brijesh",
    "lastName": "Mourya",
    "email": "brijesh@example.com",
    "password": "password123"
  }'

# Expected response:
{
  "message": "Registration successful. Please check your email to verify your account."
}
```

### Check 3: Verify Email (from Mailtrap)

1. Go to [mailtrap.io](https://mailtrap.io)
2. Open your inbox
3. Find the verification email
4. Copy the token from the link

```bash
curl "http://localhost:8080/api/v1/auth/verify-email?token=PASTE_TOKEN_HERE"

# Expected:
{ "message": "Email verified successfully. You can now login." }
```

### Check 4: Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "brijesh@example.com",
    "password": "password123"
  }'

# Expected:
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "uuid": "...",
    "email": "brijesh@example.com",
    "firstName": "Brijesh",
    "lastName": "Mourya"
  }
}
```

### Check 5: Access Protected Endpoint

```bash
# Replace TOKEN with your actual accessToken from login
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer TOKEN"

# Expected: your user details
```

### Check 6: Verify Database Has Data

```bash
docker exec -it auth_mysql mysql -u authuser -pauthpassword123 auth_service_db

# Inside MySQL:
SELECT uuid, email, is_enabled FROM users;
SELECT name FROM roles;
SELECT name FROM permissions;
SHOW TABLES;
exit
```

### Check 7: Verify Redis Blacklist Works

```bash
# 1. Login and get tokens
# 2. Logout with those tokens
# 3. Check Redis

docker exec -it auth_redis redis-cli -a redispassword123

# Inside Redis CLI:
KEYS blacklist:jti:*
# Should show the blacklisted token jti

TTL blacklist:jti:YOUR_JTI
# Should show remaining seconds until auto-deletion

GET blacklist:jti:YOUR_JTI
# Should return "revoked"
```

---

## 11. Common Issues and Fixes

### Issue: App Can't Connect to MySQL

```
Error: Communications link failure
       Connection refused: mysql/172.x.x.x:3306
```

**Cause:** App started before MySQL was ready, or MySQL container is unhealthy.

**Fix:**
```bash
# Check MySQL health
docker-compose ps mysql

# See MySQL logs
docker-compose logs mysql

# If MySQL is still starting, wait 30 seconds and restart the app
docker-compose restart auth-service
```

**Prevention:** The `depends_on` with `condition: service_healthy` in `docker-compose.yml`
makes the app wait for MySQL's healthcheck to pass before starting.

---

### Issue: Flyway Checksum Mismatch

```
Error: Validate failed: Migration checksum mismatch for migration version 2
       → Applied to database: 123456789
       → Resolved locally: 987654321
```

**Cause:** You edited an existing migration file after it already ran.

**Why this happens:** Flyway stores a checksum (fingerprint) of each migration file
when it runs it. If you change the file later, the checksum changes, and Flyway
refuses to start because it can't trust the database is in a consistent state.

**Fix for development (wipes all data):**
```bash
docker-compose down -v    # -v deletes volumes (ALL data gone)
docker-compose up -d
```

**Fix for production (never wipe production data):**
Create a NEW migration file to fix the issue:
```sql
-- V8__fix_something.sql
ALTER TABLE users ADD COLUMN new_column VARCHAR(100) NULL;
```

**Golden rule:** Never edit a migration file after it has run anywhere.

---

### Issue: Port Already in Use

```
Error: Bind for 0.0.0.0:8080 failed: port is already allocated
```

**Cause:** Something else is using port 8080.

**Fix on Windows:**
```bash
# Find what's using port 8080
netstat -ano | findstr :8080

# Kill it (replace PID with the number you see)
taskkill /PID YOUR_PID /F
```

**Fix on Mac/Linux:**
```bash
lsof -i :8080
kill -9 YOUR_PID
```

**Alternative fix:** Change the port in `docker-compose.yml`:
```yaml
ports:
  - "8081:8080"    # use 8081 instead of 8080
```

---

### Issue: Container Exits Immediately

```bash
docker-compose ps
# auth_service shows: Exit 1
```

**Fix:**
```bash
# See why it crashed
docker-compose logs auth-service

# Common causes:
# 1. Missing environment variable (JWT_SECRET not set)
# 2. MySQL not ready yet
# 3. Flyway migration failed
# 4. Port conflict
```

---

### Issue: Out of Memory

```
Error: Container auth_service killed (OOMKilled)
```

**Cause:** Docker Desktop doesn't have enough memory allocated.

**Fix:**
Docker Desktop → Settings → Resources → Memory → set to at least 4GB.

---

### Issue: `./gradlew: Permission Denied` (Mac/Linux)

```bash
chmod +x gradlew
./gradlew bootRun
```

---

### Issue: Redis Authentication Failed

```
Error: NOAUTH Authentication required
```

**Cause:** App is connecting to Redis without the password.

**Fix:** Make sure `REDIS_PASSWORD` in your `.env` matches the password in `docker-compose.yml`.

---

## 12. Understanding Spring Profiles

### What is a Profile?

A profile is a named set of configuration overrides.

```
Base config:        application.yml       → shared by everyone
Dev overrides:      application-dev.yml   → for your laptop
Prod overrides:     application-prod.yml  → for real server
Test overrides:     application-test.yml  → for automated tests
```

Spring loads the base config first, then overlays the active profile on top.
Profile settings win over base settings.

### Dev Profile (`application-dev.yml`)

```yaml
spring:
  jpa:
    show-sql: true          # See every SQL query in console

logging:
  level:
    com.brijesh.authservice: DEBUG    # Very detailed logs
    org.springframework.security: DEBUG

app:
  jwt:
    access-token-expiry: 3600000    # 1 hour in dev (not 15 min)
                                    # Less annoying when testing
```

### Prod Profile (`application-prod.yml`)

```yaml
spring:
  jpa:
    show-sql: false          # No SQL logs in production

logging:
  level:
    root: WARN               # Only warnings and errors
    com.brijesh.authservice: INFO

server:
  error:
    include-message: never   # Never expose error details
    include-stacktrace: never

app:
  jwt:
    access-token-expiry: 900000    # 15 min in production (strict)
```

### Activating a Profile

**Method 1 — Environment variable (recommended):**
```bash
# In .env for local:
SPRING_PROFILES_ACTIVE=dev

# On production server:
SPRING_PROFILES_ACTIVE=prod
```

**Method 2 — application.yml default:**
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}    # dev if env var not set
```

**Method 3 — Command line:**
```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Verify Active Profile

Look for this line in startup logs:

```
The following 1 profile is active: "dev"
```

---

## 13. Production Deployment Concepts

This section explains what happens when you deploy to a REAL server.
You don't need to do this now, but understanding it completes the picture.

### What is a Cloud Server?

A cloud server is a computer that runs 24/7, has a real IP address,
and is accessible from the internet.

Popular options:
```
DigitalOcean Droplet    → simple, cheap ($6/month), good for learning
AWS EC2                 → industry standard, complex, powerful
Google Cloud Compute    → similar to AWS
Hetzner                 → cheap European option
```

### The Basic Production Setup

```
Internet
   │
   ▼
Your Server (example: DigitalOcean Droplet, Ubuntu 22.04)
   ├── Nginx (reverse proxy, handles HTTPS)
   │     → receives traffic on port 80 (HTTP) and 443 (HTTPS)
   │     → forwards to Spring Boot on port 8080
   │
   ├── Spring Boot App (port 8080)
   │     → same Docker setup as local
   │
   ├── MySQL (port 3306, not exposed to internet)
   │
   └── Redis (port 6379, not exposed to internet)
```

### Setting Up a Production Server — Conceptual Steps

```
1. Create a server (DigitalOcean, AWS, etc.)
   → You get an IP address: 123.456.789.000

2. Connect via SSH
   ssh root@123.456.789.000

3. Install Docker and Docker Compose
   apt update && apt install docker.io docker-compose

4. Copy your project to the server
   git clone your-repo
   OR
   scp -r ./auth-service root@123.456.789.000:/app

5. Create .env file on server with PRODUCTION values
   nano /app/auth-service/.env
   # Set SPRING_PROFILES_ACTIVE=prod
   # Set real DB passwords (strong, random)
   # Set real JWT secret (openssl rand -base64 32)
   # Set real mail credentials

6. Start everything
   docker-compose up -d

7. Point your domain to the server's IP
   → In your domain registrar (GoDaddy, Namecheap, etc.)
   → Add an A record: auth.yourdomain.com → 123.456.789.000

8. Set up HTTPS with Let's Encrypt (free SSL)
   → Install Nginx + Certbot
   → Certbot automatically gets SSL certificate
   → Your API is now at: https://auth.yourdomain.com
```

### Production Security Rules

```
1. Never expose MySQL to the internet
   → Remove "ports: 3306:3306" from docker-compose.yml
   → Only your app needs to talk to MySQL
   → MySQL should NOT be accessible from outside the server

2. Never expose Redis to the internet
   → Same as MySQL — internal only

3. Only expose port 8080 (or 80/443 via Nginx)
   → The outside world only needs to reach your Spring Boot app

4. Use strong random passwords for everything
   openssl rand -base64 32   → for each password

5. Set SPRING_PROFILES_ACTIVE=prod
   → Enables production logging, security settings

6. Keep your server updated
   apt update && apt upgrade -y   → run weekly

7. Set up firewall (UFW on Ubuntu)
   ufw allow 22    → SSH
   ufw allow 80    → HTTP
   ufw allow 443   → HTTPS
   ufw enable
```

### Environment Variables on a Real Server

On a real server, you don't use a `.env` file.
Instead, you set variables directly in the server's environment
or use a secrets manager:

```bash
# Option 1: Export in shell session
export JWT_SECRET="your-real-secret"
export DB_PASSWORD="your-real-password"

# Option 2: Set in /etc/environment (persists across reboots)
echo "JWT_SECRET=your-real-secret" >> /etc/environment

# Option 3: Use Docker secrets (more secure)
echo "your-real-secret" | docker secret create jwt_secret -

# Option 4: AWS Secrets Manager / HashiCorp Vault (enterprise)
# App fetches secrets from a secure vault at runtime
# Even the server admin can't see the secrets in plaintext
```

---

## 14. Useful Commands Reference

### Docker Compose Commands

```bash
# Start all services (detached)
docker-compose up -d

# Start and rebuild images
docker-compose up --build -d

# Start specific service
docker-compose up -d mysql

# Stop all services (data preserved)
docker-compose down

# Stop and delete all data (fresh start)
docker-compose down -v

# See running containers and status
docker-compose ps

# See logs of all services
docker-compose logs

# Follow logs of specific service
docker-compose logs -f auth-service

# Restart specific service
docker-compose restart auth-service

# Rebuild specific service image
docker-compose build auth-service

# Execute command inside running container
docker exec -it auth_service sh

# Scale a service (run multiple instances)
docker-compose up -d --scale auth-service=3
```

### Docker Commands

```bash
# List all running containers
docker ps

# List all containers (including stopped)
docker ps -a

# List all images
docker images

# Remove an image
docker rmi image-name

# Remove all stopped containers
docker container prune

# Remove unused images
docker image prune

# See container resource usage (CPU, memory)
docker stats

# Inspect container details (networking, volumes, config)
docker inspect auth_service

# Copy file from container to host
docker cp auth_service:/app/logs/app.log ./app.log
```

### MySQL Commands (Inside Container)

```bash
# Connect to MySQL
docker exec -it auth_mysql mysql -u authuser -pauthpassword123 auth_service_db

# Inside MySQL:
SHOW TABLES;
SELECT * FROM users;
SELECT * FROM roles;
SELECT * FROM flyway_schema_history;
DESCRIBE users;
exit
```

### Redis Commands (Inside Container)

```bash
# Connect to Redis CLI
docker exec -it auth_redis redis-cli -a redispassword123

# Inside Redis CLI:
KEYS *                          # all keys
KEYS blacklist:jti:*            # blacklisted tokens
KEYS rate_limit:*               # rate limit counters
GET key-name                    # get value
TTL key-name                    # time until expiry (seconds)
DEL key-name                    # delete a key
FLUSHALL                        # delete everything (dev only!)
DBSIZE                          # count all keys
INFO memory                     # memory usage
exit
```

### Gradle Commands

```bash
# Build the project
./gradlew build

# Run the app
./gradlew bootRun

# Run tests
./gradlew test

# Build just the JAR (skip tests)
./gradlew bootJar -x test

# Clean build output
./gradlew clean

# See all dependencies
./gradlew dependencies

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=prod'
```

---

## 15. How Services Communicate Inside Docker

This is one of the most confusing concepts for beginners. Let me explain clearly.

### The Problem

```
On your laptop:
  MySQL is at: localhost:3308
  Redis is at: localhost:6379

Inside the auth-service container:
  "localhost" means the auth-service container ITSELF
  NOT your laptop
  NOT MySQL
  NOT Redis
```

This is why `DB_HOST=localhost` fails inside Docker.

### The Solution: Docker Networks

When you put all services on the same Docker network (`auth_network`),
they can reach each other using their **service name** as the hostname.

```yaml
services:
  mysql:                    # ← this is the hostname
    networks:
      - auth_network

  auth-service:
    environment:
      DB_HOST: mysql        # ← use service name, not localhost
    networks:
      - auth_network
```

```
Inside auth-service container:
  "mysql" resolves to MySQL container's IP address
  "redis" resolves to Redis container's IP address

Docker handles the DNS resolution automatically.
You never need to know the actual IP addresses.
```

### Visual Network Diagram

```
┌─────────────────── auth_network ──────────────────────┐
│                                                        │
│  ┌──────────────┐    mysql:3306    ┌───────────────┐  │
│  │ auth-service │ ──────────────► │     MySQL      │  │
│  │   :8080      │                  │    :3306       │  │
│  │              │    redis:6379    │                │  │
│  │              │ ──────────────► │               │  │
│  └──────────────┘                  └───────────────┘  │
│                                                        │
│                                    ┌───────────────┐  │
│                                    │     Redis      │  │
│                                    │    :6379       │  │
│                                    └───────────────┘  │
└────────────────────────────────────────────────────────┘
         ▲
         │ port 8080 exposed to your laptop
         │
  localhost:8080 (your browser/Postman)
```

Services inside `auth_network` can talk to each other freely.
Only port 8080 is exposed outside (to your laptop).
MySQL and Redis are NOT exposed outside the network.

---

## 16. Security Checklist Before Going Live

Run through this before deploying to a real server:

### Secrets

- [ ] `.env` is in `.gitignore`
- [ ] No hardcoded passwords anywhere in source code
- [ ] `JWT_SECRET` is at least 32 random characters
- [ ] All passwords are strong and unique (not "password123")
- [ ] OAuth2 client secrets are stored as environment variables

### Configuration

- [ ] `SPRING_PROFILES_ACTIVE=prod` on production server
- [ ] `show-sql: false` in prod profile
- [ ] `include-message: never` in prod server error config
- [ ] `include-stacktrace: never` in prod server error config

### Database

- [ ] MySQL not exposed to internet (no public ports)
- [ ] Redis not exposed to internet (no public ports)
- [ ] Database user has only necessary permissions (not root)
- [ ] Strong database passwords

### Application

- [ ] HTTPS enabled (SSL certificate from Let's Encrypt)
- [ ] Rate limiting active on auth endpoints
- [ ] CORS configured to your actual frontend URL (not `*`)
- [ ] Security headers configured (X-Frame-Options, HSTS, etc.)

### Docker

- [ ] App runs as non-root user inside container
- [ ] Multi-stage build used (smaller attack surface)
- [ ] Healthchecks configured for all services
- [ ] `.dockerignore` excludes `.env` and sensitive files

---

## Quick Start Summary

For someone who just cloned the repo and wants to run it:

```bash
# 1. Install Docker Desktop (if not installed)

# 2. Clone the project
git clone https://github.com/123Brijesh44aa/auth-service
cd auth-service

# 3. Create your .env file
cp .env.example .env     # if .env.example exists
# OR create .env manually and fill in all values

# 4. Start everything
docker-compose up --build -d

# 5. Wait ~60 seconds, then check health
curl http://localhost:8080/actuator/health

# 6. Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"test@example.com","password":"password123"}'

# 7. Check email in Mailtrap, verify, then login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Done! You have a running JWT-based auth service.
```

---

*This guide covers the Auth Service component of the Object Storage System.*
*For the full system architecture, see the main project README.*

*Built by Brijesh Mourya — github.com/123Brijesh44aa*
