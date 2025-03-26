FROM openjdk:8

ADD target/platform-benchmark.jar /hobbit/platform-benchmark.jar

WORKDIR /hobbit

CMD java -cp platform-benchmark.jar org.hobbit.core.run.ComponentStarter ${CLASS:=org.hobbit.benchmark.platform.PlatformBenchmark}
