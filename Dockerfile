FROM amazoncorretto:21

WORKDIR /app

COPY target/*.jar app.jar

ENV PORT=8080
ENV APP_ENV=production
ENV THREAD_POOL_SIZE=20

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
