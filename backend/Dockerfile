# Этап сборки
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /app
COPY . .

# Даем права на запуск Gradle Wrapper и собираем проект (пропуская тесты для скорости)
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Этап запуска
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем собранный jar-файл из папки build/libs
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
EXPOSE 44444
ENTRYPOINT ["java", "-jar", "app.jar"]