# Use the official Clojure image with OpenJDK and tools.deps
FROM clojure:openjdk-19-tools-deps-1.11.1.1113-bullseye

# Set the working directory in the container
WORKDIR /usr/src/app

# Expose port 3000
EXPOSE 3000

# Copy the application code and entrypoint script into the container
COPY . .

# Ensure the entrypoint script is executable
RUN chmod +x /usr/src/app/entrypoint.sh

# Start the application using the entrypoint script
CMD ["/usr/src/app/entrypoint.sh"]
