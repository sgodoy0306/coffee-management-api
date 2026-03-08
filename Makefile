.PHONY: db compile test package run all stop clean

## Start the PostgreSQL database
db:
	docker compose up -d

## Compile source code
compile:
	mvn compile

## Run tests
test:
	mvn test

## Package into a JAR (skips tests)
package:
	mvn package -DskipTests

## Run the application
run:
	mvn spring-boot:run

## Stop the database
stop:
	docker compose down

## Remove compiled artifacts
clean:
	mvn clean

## Build and run everything: db → compile → test → package → run
all: db compile test package run
