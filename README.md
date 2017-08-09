This project contains a simple benchmark that measure the throughput of a [HOBBIT benchmarking platform](https://github.com/MichaelRoeder/platform) instance. It simulates the benchmarking of a SPARQL endpoint using random strings that are generated in fast way but make sure that caching of messages wouldn't be very helpful.

The benchmark relies on the following assumptions:

* A SPARQL query has an average length of [545 characters](https://github.com/MichaelRoeder/platform-benchmark/blob/master/src/main/java/org/hobbit/benchmark/platform/PlatformBenchmarkConstants.java#L32).
* A SPARQL result set comprises 122 results. Each result has an average length of 100 characters leading to a length of [12200 characters](https://github.com/MichaelRoeder/platform-benchmark/blob/master/src/main/java/org/hobbit/benchmark/platform/PlatformBenchmarkConstants.java#L33) for a single result set.