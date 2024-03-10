test: build-jvm up-d benchmark stress-test down

build-native:
	quarkus build --native
	docker build -f src/main/docker/Dockerfile.native-micro -t docker.io/adrianomoreira86/rinha2024q1quarkus .

up:
	cd infra && docker-compose up

down:
	cd infra && docker-compose down

my-benchmark:
	docker run --name k6 --network host --rm -it --user ${UID}:12345 -v ${PWD}:/app -w /app grafana/k6 run load-test/benchmark.js
