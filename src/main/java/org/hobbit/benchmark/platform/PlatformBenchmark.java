package org.hobbit.benchmark.platform;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.utils.rdf.RdfHelper;

public class PlatformBenchmark extends AbstractBenchmarkController {

    protected int numberOfQueries;

    @Override
    public void init() throws Exception {
        super.init();

        Resource expResourc = benchmarkParamModel.getResource(Constants.NEW_EXPERIMENT_URI);
        StmtIterator iterator = benchmarkParamModel.listStatements(expResourc, null, (RDFNode) null);
        while (iterator.hasNext()) {
            System.out.println(iterator.next().toString());
        }

        int numberOfDataGenerators, numberOfTaskGenerators;
        try {
            numberOfQueries = Integer.parseInt(RdfHelper.getStringValue(benchmarkParamModel, expResourc,
                    PlatformBenchmarkConstants.numberOfQueries));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + PlatformBenchmarkConstants.numberOfQueries
                    + "\" from the parameter model. Aborting.", e);
        }
        try {
            numberOfDataGenerators = Integer.parseInt(RdfHelper.getStringValue(benchmarkParamModel, expResourc,
                    PlatformBenchmarkConstants.numberOfDataGenerators));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + PlatformBenchmarkConstants.numberOfQueries
                    + "\" from the parameter model. Aborting.", e);
        }
        try {
            numberOfTaskGenerators = Integer.parseInt(RdfHelper.getStringValue(benchmarkParamModel, expResourc,
                    PlatformBenchmarkConstants.numberOfTaskGenerators));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + PlatformBenchmarkConstants.numberOfQueries
                    + "\" from the parameter model. Aborting.", e);
        }
        String seed = RdfHelper.getStringValue(benchmarkParamModel, expResourc, PlatformBenchmarkConstants.seed);
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

        System.out.println(this.getClass().getSimpleName() + " started");

        waitForComponentsToInitialize();
    }

    @Override
    protected void executeBenchmark() throws Exception {
        // give the start signals
        sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
        sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);

        // wait for the data generators to finish their work
        waitForDataGenToFinish();

        // wait for the task generators to finish their work
        waitForTaskGenToFinish();

        // wait for the system to terminate
        waitForSystemToFinish();

        // Create the evaluation module
        String[] envVariables = new String[] {
                PlatformBenchmarkConstants.CLASS_NAME_PARAMETER_KEY + "=" + EvaluationModule.class.getName(),
                PlatformBenchmarkConstants.NUMBER_OF_QUERIES_PARAMETER_KEY + "=" + numberOfQueries };
        createEvaluationModule(PlatformBenchmarkConstants.EVALUATION_MODULE_IMAGE, envVariables);

        // wait for the evaluation to finish
        waitForEvalComponentsToFinish();

        // the evaluation module should have sent an RDF model containing the
        // results. We should add the configuration of the benchmark to this
        // model.
        StmtIterator iterator = benchmarkParamModel
                .listStatements(benchmarkParamModel.getResource(Constants.NEW_EXPERIMENT_URI), null, (RDFNode) null);
        Statement s;
        Resource experimentResource = resultModel.getResource(experimentUri);
        while (iterator.hasNext()) {
            s = iterator.next();
            resultModel.add(experimentResource, s.getPredicate(), s.getObject());
        }

        // Send the resultModul to the platform controller and terminate
        sendResultModel(resultModel);
    }

}
