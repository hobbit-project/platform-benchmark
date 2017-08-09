default: build dockerize

build:
	mvn clean package -U -Dmaven.test.skip=true

dockerize:
	docker build -t git.project-hobbit.eu:4567/gitadmin/platform-benchmark .
	docker build -f Dockerfile.System -t git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system .

push:
	docker push git.project-hobbit.eu:4567/gitadmin/platform-benchmark
	docker push git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system