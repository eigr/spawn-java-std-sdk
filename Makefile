# Variables
VERSION := 1.3.0
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
