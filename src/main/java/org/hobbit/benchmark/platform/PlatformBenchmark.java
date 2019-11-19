package org.hobbit.benchmark.platform;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hobbit.core.Commands;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.core.components.utils.SystemResourceUsageRequester;
import org.hobbit.core.data.usage.ResourceUsageInformation;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HobbitExperiments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatformBenchmark extends AbstractBenchmarkController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformBenchmark.class);

    private static final long RESOURCE_USAGE_REQUEST_INTERVAL = 5000;

    protected int numberOfQueries;
    protected SystemResourceUsageRequester resourceRequester = null;
    protected ResourceUsageInformation resourceUsage = new ResourceUsageInformation();
    protected int resourceUsageRequests = 0;

    @Override
    public void init() throws Exception {
        super.init();

        StmtIterator iterator = benchmarkParamModel.listStatements(HobbitExperiments.New, null, (RDFNode) null);
        while (iterator.hasNext()) {
            System.out.println(iterator.next().toString());
        }

        int numberOfDataGenerators, numberOfTaskGenerators;
        try {
            numberOfQueries = Integer.parseInt(RdfHelper.getStringValue(benchmarkParamModel, HobbitExperiments.New,
                    PlatformBenchmarkConstants.numberOfQueries));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + PlatformBenchmarkConstants.numberOfQueries
                    + "\" from the parameter model. Aborting.", e);
        }
        try {
            numberOfDataGenerators = Integer.parseInt(RdfHelper.getStringValue(benchmarkParamModel,
                    HobbitExperiments.New, PlatformBenchmarkConstants.numberOfDataGenerators));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + PlatformBenchmarkConstants.numberOfQueries
                    + "\" from the parameter model. Aborting.", e);
        }
        try {
            numberOfTaskGenerators = Integer.parseInt(RdfHelper.getStringValue(benchmarkParamModel,
                    HobbitExperiments.New, PlatformBenchmarkConstants.numberOfTaskGenerators));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + PlatformBenchmarkConstants.numberOfQueries
                    + "\" from the parameter model. Aborting.", e);
        }
        String seed = RdfHelper.getStringValue(benchmarkParamModel, HobbitExperiments.New,
                PlatformBenchmarkConstants.seed);
        if (seed == null) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + PlatformBenchmarkConstants.seed + "\" from the parameter model. Aborting.");
        }

        // Create the other components

        // Create data generators
        String[] envVariables = new String[] {
                PlatformBenchmarkConstants.CLASS_NAME_PARAMETER_KEY + "=" + DataGenerator.class.getName(),
                PlatformBenchmarkConstants.SEED_PARAMETER_KEY + "=" + seed,
                PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY + "=" + Integer.toString(numberOfQueries) };
        createDataGenerators(PlatformBenchmarkConstants.DATA_GENERATOR_IMAGE, numberOfDataGenerators, envVariables);

        // Create task generators
        envVariables = new String[] {
                PlatformBenchmarkConstants.CLASS_NAME_PARAMETER_KEY + "=" + TaskGenerator.class.getName() };
        createTaskGenerators(PlatformBenchmarkConstants.TASK_GENERATOR_IMAGE, numberOfTaskGenerators, envVariables);

        // Create evaluation storage
        createEvaluationStorage();

        resourceRequester = SystemResourceUsageRequester.create(this, getHobbitSessionId());

        System.out.println(this.getClass().getSimpleName() + " started");

        waitForComponentsToInitialize();
    }

    @Override
    protected void executeBenchmark() throws Exception {
        Timer timer = new Timer();
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        ResourceUsageInformation info = resourceRequester.getSystemResourceUsage();
                        if (info != null) {
                            resourceUsage.merge(info);
                            ++resourceUsageRequests;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Error while requesting resource usage information.", e);
                    }
                }
            }, 0, RESOURCE_USAGE_REQUEST_INTERVAL);
            // give the start signals
            sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
            sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);

            // wait for the data generators to finish their work
            waitForDataGenToFinish();

            // wait for the task generators to finish their work
            waitForTaskGenToFinish();

            // wait for the system to terminate
            waitForSystemToFinish();
        } finally {
            // terminate the timer that is regularly requesting system resource consumption
            // information
            timer.cancel();
        }

        // Create the evaluation module
        String[] envVariables = new String[] {
                PlatformBenchmarkConstants.CLASS_NAME_PARAMETER_KEY + "=" + EvaluationModule.class.getName(),
                PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY + "=" + numberOfQueries };
        createEvaluationModule(PlatformBenchmarkConstants.EVALUATION_MODULE_IMAGE, envVariables);

        // wait for the evaluation to finish
        waitForEvalComponentsToFinish();

        if (resourceUsageRequests > 0) {
            LOGGER.warn("Received {} resource usage statistics. The merged result is {}", resourceUsageRequests,
                    resourceUsage);
        } else {
            LOGGER.warn("Did not receive any resource usage request result.");
        }

        // the evaluation module should have sent an RDF model containing the
        // results. We should add the configuration of the benchmark to this
        // model.
        StmtIterator iterator = benchmarkParamModel.listStatements(HobbitExperiments.New, null, (RDFNode) null);
        Statement s;
        Resource experimentResource = resultModel.getResource(experimentUri);
        while (iterator.hasNext()) {
            s = iterator.next();
            resultModel.add(experimentResource, s.getPredicate(), s.getObject());
        }

        // Send the resultModul to the platform controller and terminate
        sendResultModel(resultModel);
    }

    @Override
    public void close() throws IOException {
        if (resourceRequester != null) {
            try {
                resourceRequester.close();
            } catch (Exception e) {
            }
        }
        super.close();
    }

}
