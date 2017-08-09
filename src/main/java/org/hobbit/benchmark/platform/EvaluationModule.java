package org.hobbit.benchmark.platform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.hobbit.core.components.AbstractEvaluationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluationModule extends AbstractEvaluationModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationModule.class);

    private File tempDataFile;
    private OutputStream os;
    // private int errorCount = 0;
    private long tsFirstSent = Long.MAX_VALUE;
    private long tsLastSent = 0;
    private long tsFirstReceived = Long.MAX_VALUE;
    private long tsLastReceived = 0;
    private long diffSums = 0;
    private long successfulQueries = 0;
    private int expectedNumberOfQueries;

    public EvaluationModule() {
    }

    /**
     * Constructor for testing purposes.
     */
    public EvaluationModule(File tempDataFile, OutputStream os, String experimentUri) {
        this.tempDataFile = tempDataFile;
        this.os = os;
        this.experimentUri = experimentUri;
    }

    @Override
    public void init() throws Exception {
        super.init();

        // get number of queries
        Map<String, String> env = System.getenv();
        if (!env.containsKey(PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY)) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY
                            + "\" from the environment. Aborting.");
        }
        try {
            expectedNumberOfQueries = Integer
                    .parseInt(env.get(PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \""
                    + PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY + "\" from the environment. Aborting.",
                    e);
        }

        tempDataFile = File.createTempFile("eval", "data");
        os = new BufferedOutputStream(new FileOutputStream(tempDataFile));

        System.out.println(this.getClass().getSimpleName() + " started");
    }

    @Override
    protected void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp,
            long responseReceivedTimestamp) throws Exception {
        if ((expectedData != null) && (receivedData != null)) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2);
            buffer.putLong(taskSentTimestamp);
            buffer.putLong(responseReceivedTimestamp);
            os.write(buffer.array());
            // process what we already can process
            diffSums += responseReceivedTimestamp - taskSentTimestamp;
            if (responseReceivedTimestamp > tsLastReceived) {
                tsLastReceived = responseReceivedTimestamp;
            }
            if (responseReceivedTimestamp < tsFirstReceived) {
                tsFirstReceived = responseReceivedTimestamp;
            }
            if (taskSentTimestamp > tsLastSent) {
                tsLastSent = taskSentTimestamp;
            }
            if (taskSentTimestamp < tsFirstSent) {
                tsFirstSent = taskSentTimestamp;
            }
            ++successfulQueries;
            // } else {
            // ++errorCount;
        }
    }

    @Override
    protected Model summarizeEvaluation() throws Exception {
        IOUtils.closeQuietly(os);

        double avgQueryRunTime = (double) diffSums / (double) successfulQueries;
        double stdDev = 0;

        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(tempDataFile));
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2);
            int length = is.read(buffer.array());
            long taskSentTimestamp, responseReceivedTimestamp;
            int pairsCount = 0;
            double diff;
            while (length == (Long.BYTES * 2)) {
                buffer.position(0);
                taskSentTimestamp = buffer.getLong();
                responseReceivedTimestamp = buffer.getLong();
                diff = responseReceivedTimestamp - taskSentTimestamp;
                diff = avgQueryRunTime - diff;
                stdDev += Math.pow(diff, 2);
                ++pairsCount;
                length = is.read(buffer.array());
            }
            if (length > 0) {
                LOGGER.error("The last part read from the temp data file has {} bits while 0 have been expected.",
                        length);
            }
            // Make sure that we have seen all pairs
            if (pairsCount != successfulQueries) {
                throw new IllegalStateException(successfulQueries + " successful queries have been received but "
                        + pairsCount + " have been read from file.");
            }

            stdDev = Math.sqrt(stdDev / successfulQueries);
        } finally {
            IOUtils.closeQuietly(is);
        }

        Model model = createDefaultModel();
        Resource experimentResource = model.getResource(experimentUri);
        // avg query runtime
        model.addLiteral(experimentResource, PlatformBenchmarkConstants.msPerQuery, avgQueryRunTime);
        // std deviation of query runtime
        model.addLiteral(experimentResource, PlatformBenchmarkConstants.queryRuntimeStdDev, stdDev);
        // overall runtime
        model.addLiteral(experimentResource, PlatformBenchmarkConstants.runtime, tsLastReceived - tsFirstSent);
        // responses per second
        model.addLiteral(experimentResource, PlatformBenchmarkConstants.responsesPerSecond,
                (successfulQueries * 1000.0) / ((double) (tsLastReceived - tsFirstReceived)));
        // error count
        model.addLiteral(experimentResource, PlatformBenchmarkConstants.errorCount,
                expectedNumberOfQueries - successfulQueries);
        return model;
    }

}
