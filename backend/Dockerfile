# ==========================================
# 阶段 1：Maven 构建
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# 先下载依赖（利用 Docker 层缓存，改源码不需要重新下载）
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn -q -DskipTests package

# ==========================================
# 阶段 2：JRE 运行
# ==========================================
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
