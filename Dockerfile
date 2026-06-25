# ============================================================
# 影院购票管理系统 — Docker 多阶段构建
# ============================================================

# ---- Stage 1: Maven 构建 ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY xm_film/springboot/pom.xml .
RUN mvn dependency:go-offline -q
COPY xm_film/springboot/src ./src
RUN mvn clean package -DskipTests -q

# ---- Stage 2: JRE 运行 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# Seed placeholder files for data.sql references (47 JPG, 4 PNG, 10 MP4)
COPY xm_film/sql/seed-uploads /app/seed-uploads

# Entrypoint script: seeds uploads dir on first start, then launches app
COPY scripts/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/uploads

EXPOSE 9090

ENV SERVER_PORT=9090 \
    DB_HOST=localhost \
    DB_PASSWORD=123456 \
    JWT_SECRET=docker-secret-key-for-cinema-system \
    FILE_UPLOAD_DIR=/app/uploads \
    SEED_DIR=/app/seed-uploads \
    MYBATIS_LOG_LEVEL=INFO

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -s http://localhost:9090/api/v1/health > /dev/null || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
