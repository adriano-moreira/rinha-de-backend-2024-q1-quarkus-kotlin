test: build-jvm up-d benchmark stress-test down

build-native:
	quarkus build --native
	docker build -f src/main/docker/Dockerfile.native-micro -t rinha2024q1quarkus .

build-jvm:
	quarkus build
	docker build -f src/main/docker/Dockerfile.jvm -t rinha2024q1quarkus .

up-d:
	cd infra && docker-compose up -d

up:
	cd infra && docker-compose up

down:
	cd infra && docker-compose down

benchmark:
	docker run --name k6 --network host --rm -it --user ${UID}:12345 -v ${PWD}:/app -w /app grafana/k6 run load-test/benchmark.js

stress-test:
	k6 run load-test/benchmark

build-restart-jvm: build-jvm down up
