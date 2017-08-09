package org.hobbit.benchmark.platform;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;

public class DataGenerator extends AbstractDataGenerator {

    private int numberOfQueries;
    private StringGenerator generator;

    @Override
    public void init() throws Exception {
        // Always init the super class first!
        super.init();

        // Read data from file
        String data = null;
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(PlatformBenchmarkConstants.DATA_RESOURCE_NAME);
            data = IOUtils.toString(is, Charsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Couldn't load data from resource \"" + PlatformBenchmarkConstants.DATA_RESOURCE_NAME + "\". Aborting.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        Map<String, String> env = System.getenv();
        // Get seed
        long seed = getGeneratorId();
        if (!env.containsKey(PlatformBenchmarkConstants.SEED_PARAMETER_KEY)) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + PlatformBenchmarkConstants.SEED_PARAMETER_KEY + "\" from the environment. Aborting.");
        }
        try {
            seed += Long.parseLong(env.get(PlatformBenchmarkConstants.SEED_PARAMETER_KEY));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + PlatformBenchmarkConstants.SEED_PARAMETER_KEY + "\" from the environment. Aborting.", e);
        }
        // get number of queries
        int globalQueries;
        if (!env.containsKey(PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY)) {
            throw new IllegalArgumentException("Couldn't get \"" + PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY
                    + "\" from the environment. Aborting.");
        }
        try {
            globalQueries = Integer.parseInt(env.get(PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY
                    + "\" from the environment. Aborting.", e);
        }
        // determine the number of queries that have to be created by this
        // generator
        numberOfQueries = globalQueries / getNumberOfGenerators();
        if ((globalQueries % getNumberOfGenerators()) > getGeneratorId()) {
            ++numberOfQueries;
        }

        generator = new StringGenerator(data, seed);
        
        System.out.println(this.getClass().getSimpleName() + " started");
    }

    @Override
    protected void generateData() throws Exception {
        System.out.println("Generating data...");
        String query, resultSet;
        for (int i = 0; i < numberOfQueries; ++i) {
            query = generator.generateString(PlatformBenchmarkConstants.SPARQL_QUERY_LENGTH);
            resultSet = generator.generateString(PlatformBenchmarkConstants.RESULT_SET_LENGTH);
            sendDataToTaskGenerator(RabbitMQUtils.writeByteArrays(RabbitMQUtils.writeLong(System.currentTimeMillis()),
                    new byte[][] { RabbitMQUtils.writeString(query), RabbitMQUtils.writeString(resultSet) }, null));
        }
        System.out.println("Finished generation...");
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

}
