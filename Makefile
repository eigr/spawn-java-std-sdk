# Variables
VERSION := 1.0-SNAPSHOT
PACKAGE := target/spawn-java-demo-${VERSION}-shaded.jar

clean:
	mvn clean

build: clean
	mvn compile

install:
	mvn install

test: run-dependencies
	mvn test

run-dependencies:
	docker-compose up -d && docker-compose logs -f

stop-dependencies:
	docker-compose down
