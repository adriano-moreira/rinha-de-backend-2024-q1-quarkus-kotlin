test: build-jvm up-d benchmark stress-test down

build-native:
	quarkus build --native
	docker build -f src/main/docker/Dockerfile.native-micro -t docker.io/adrianomoreira86/rinha2024q1quarkus .


build-jvm:
	quarkus build	
	docker build -f src/main/docker/Dockerfile.jvm -t docker.io/adrianomoreira86/rinha2024q1quarkus-jvm .

up:
	cd infra2 && docker-compose up

down:
	cd infra2 && docker-compose down

my-benchmark:
	docker run --name k6 --network host --rm -it --user ${UID}:12345 -v ${PWD}:/app -w /app grafana/k6 run load-test/benchmark.js
