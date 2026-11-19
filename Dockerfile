# Start from an image that already has Java 21 installed
FROM eclipse-temurin:21-jdk

# All the next commands run inside the /app folder
WORKDIR /app

# Copy our files into the image
COPY RainAlertServer.java RainAlert.html ./
COPY lib ./lib

# Compile the Java code (same as run.bat does on Windows)
RUN javac -cp lib/json-20240303.jar -d out RainAlertServer.java

# The app listens on port 8080 (Render can override it with PORT)
EXPOSE 8080

# Start the server
CMD ["java", "-cp", "out:lib/json-20240303.jar", "RainAlertServer"]
