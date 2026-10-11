FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system budget && useradd --system --gid budget budget
COPY --chown=budget:budget build/libs/budget.war /app/budget.war
USER budget
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/budget.war"]
