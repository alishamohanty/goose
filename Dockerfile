# Use the official Clojure image with OpenJDK and tools.deps
FROM clojure:openjdk-19-tools-deps-1.11.1.1113-bullseye

# Set the working directory in the container
WORKDIR /usr/src/app

# Copy the application code into the container
COPY . .

# Start the application
CMD ["clj", "-T:build" "uber"]
CMD ["java" "-jar" "target/goose-standalone.jar"]
