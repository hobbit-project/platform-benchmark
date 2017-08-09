package org.hobbit.benchmark.platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.utils.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatformBenchmarkingSystem extends AbstractSystemAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformBenchmarkingSystem.class);

    private static final long DEFAULT_SEED = 31;

    private static final String NOT_MASTER_NODE_KEY = "NOT_MASTER_NODE";

    private StringGenerator generator;
    private Set<String> slaveNodes = null;
    private Semaphore slaveTerminationSemaphore = new Semaphore(0);

    public PlatformBenchmarkingSystem() {
        super(100);
    }

    @Override
    public void init() throws Exception {
        // Always init the super class first!
        super.init();

        Map<String, String> env = System.getenv();
        // Check whether this node is the master or not
        if (env.containsKey(NOT_MASTER_NODE_KEY)) {
            slaveNodes = null;
        } else {
            slaveNodes = new HashSet<>();
        }

        // Read data from file
        String data = null;
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader().getResourceAsStream(PlatformBenchmarkConstants.DATA_RESOURCE_NAME);
            data = IOUtils.toString(is, Charsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't load data from resource \""
                    + PlatformBenchmarkConstants.DATA_RESOURCE_NAME + "\". Aborting.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        // If this is the master, create the additional adapters if they are
        // needed
        if (slaveNodes != null) {
            createSlaveNodes();
        }

        generator = new StringGenerator(data, DEFAULT_SEED);
        System.out.println(this.getClass().getSimpleName() + " started");
    }

    @Override
    public void receiveGeneratedData(byte[] data) {
        // nothing to do
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        try {
            sendResultToEvalStorage(taskId,
                    RabbitMQUtils.writeString(generator.generateString(PlatformBenchmarkConstants.RESULT_SET_LENGTH)));
        } catch (IOException e) {
            LOGGER.error("Exception while sending results.", e);
        }
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        if (command == Commands.DOCKER_CONTAINER_TERMINATED) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            String containerName = RabbitMQUtils.readString(buffer);
            int exitCode = buffer.get();
            if ((slaveNodes != null) && (slaveNodes.contains(containerName))) {
                slaveTerminationSemaphore.release();
                LOGGER.error("One of the slaves terminated with exit code {}.", exitCode);
            }
        }
        super.receiveCommand(command, data);
    }

    private void createSlaveNodes() throws Exception {
        if (systemParamModel.contains(null, PlatformBenchmarkConstants.numberOfSystemAdapters)) {
            try {
                int numberOfInstances = Integer.parseInt(RdfHelper.getStringValue(systemParamModel, null,
                        PlatformBenchmarkConstants.numberOfSystemAdapters));
                if (numberOfInstances > 1) {
                    for (int i = 1; i < numberOfInstances; ++i) {
                        createSlaveNode();
                    }
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Couldn't load number of instances that should be created. Assuming 1.", e);
            }
        } else {
            LOGGER.error("Couldn't load number of instances that should be created. Assuming 1.");
        }
    }

    private void createSlaveNode() throws Exception {
        String containerName = createContainer(PlatformBenchmarkConstants.SYSTEM_IMAGE, Constants.CONTAINER_TYPE_SYSTEM,
                new String[] { NOT_MASTER_NODE_KEY + "=true", Constants.SYSTEM_PARAMETERS_MODEL_KEY + "="
                        + System.getenv().get(Constants.SYSTEM_PARAMETERS_MODEL_KEY) });
        if (containerName != null) {
            slaveNodes.add(containerName);
        } else {
            throw new Exception("Couldn't create slave node. Aborting.");
        }
    }
}
