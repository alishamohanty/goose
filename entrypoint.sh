#!/bin/sh

# Run the build
clj -T:build uber

# Run the JAR
exec java -jar target/goose-standalone.jar
