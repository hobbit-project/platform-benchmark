package org.hobbit.benchmark.platform;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.hobbit.core.components.AbstractTaskGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;

public class TaskGenerator extends AbstractTaskGenerator {

    public TaskGenerator() {
        super(20);
    }

    @Override
    public void init() throws Exception {
        // Always init the super class first!
        super.init();

        System.out.println(this.getClass().getSimpleName() + " started");
    }

    @Override
    protected void generateTask(byte[] data) throws Exception {
        // Create tasks based on the incoming data inside this method.
        // You might want to use the id of this task generator and the
        // number of all task generators running in parallel.
        // int dataGeneratorId = getGeneratorId();
        // int numberOfGenerators = getNumberOfGenerators();

        // Create an ID for the task
        String taskId = getNextTaskId();

        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] generationTimeStamp = new byte[Long.BYTES];
        buffer.get(generationTimeStamp);
        byte[] sparqlQuery = RabbitMQUtils.readByteArray(buffer);
        byte[] resultSet = RabbitMQUtils.readByteArray(buffer);

        // Send the task to the system (and store the timestamp)
        long timestamp = System.currentTimeMillis();
        sendTaskToSystemAdapter(taskId, sparqlQuery);

        // Send the expected answer to the evaluation store
        sendTaskToEvalStorage(taskId, timestamp, resultSet);
        System.out.println("Handled " + taskId);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

}
