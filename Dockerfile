# PanoCerto num serviço só: o painel (React) vai embutido no backend (Spring Boot).
# Usado pelo Render. Banco H2 em arquivo: volta aos dados de exemplo a cada reinício.

# 1) Painel
FROM node:22-bookworm-slim AS painel
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
# npm install (e não npm ci): o lock gerado no Windows não traz os pacotes que só o Linux usa
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# 2) Backend, com o painel dentro de static/
FROM maven:3.9-eclipse-temurin-21 AS servidor
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=painel /app/frontend/dist ./src/main/resources/static
RUN mvn -q -B package -DskipTests

# 3) Só o necessário para rodar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=servidor /app/backend/target/panocerto-*.jar app.jar
# O plano grátis tem 512 MB: memória contida e inicialização mais leve
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
CMD ["java", "-jar", "app.jar", "--spring.profiles.active=dev"]
