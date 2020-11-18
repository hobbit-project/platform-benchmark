package org.hobbit.benchmark.platform;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.utils.test.ModelComparisonHelper;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitExperiments;
import org.junit.Assert;
import org.junit.Test;

public class EvaluationModuleTest {

    @Test
    public void test() throws Exception {
        OutputStream os = null;
        try {
            File tempDataFile = File.createTempFile("eval", "data");
            os = new BufferedOutputStream(new FileOutputStream(tempDataFile));
            String experimentUri = HobbitExperiments.getExperimentURI("123");
            // create evaluation module (do not init it!)
            @SuppressWarnings("resource")
            EvaluationModule module = new EvaluationModule(tempDataFile, os, experimentUri);
            module.setExpectedNumberOfQueries(4);

            // insert data
            module.evaluateResponse(new byte[10], new byte[10], 10, 15);
            module.evaluateResponse(new byte[10], new byte[10], 11, 15);
            module.evaluateResponse(new byte[10], new byte[10], 12, 17);
            module.evaluateResponse(new byte[10], new byte[10], 13, 19);

            Model model = module.summarizeEvaluation();
            System.out.println(model.toString());

            Model expectedModel = ModelFactory.createDefaultModel();
            Resource experimentResource = expectedModel.getResource(experimentUri);
            expectedModel.add(experimentResource, RDF.type, HOBBIT.Experiment);
            expectedModel.addLiteral(experimentResource, PlatformBenchmarkConstants.errorCount, 0);
            expectedModel.addLiteral(experimentResource, PlatformBenchmarkConstants.responsesPerSecond, 1000.0);
            expectedModel.addLiteral(experimentResource, PlatformBenchmarkConstants.runtime, 9L);
            expectedModel.addLiteral(experimentResource, PlatformBenchmarkConstants.msPerQuery, 5.0);
            // calculate std dev
            double avg = 5.0;
            double stdDev = 0;
            double values[] = new double[] { 5, 4, 5, 6 };
            for (int i = 0; i < values.length; ++i) {
                stdDev += Math.pow((avg - values[i]), 2);
            }
            stdDev = Math.sqrt(stdDev / values.length);
            expectedModel.addLiteral(experimentResource, PlatformBenchmarkConstants.queryRuntimeStdDev, stdDev);

            Set<Statement> stmts = ModelComparisonHelper.getMissingStatements(model, expectedModel);
            Assert.assertEquals(stmts.toString(), 0, stmts.size());
            stmts = ModelComparisonHelper.getMissingStatements(expectedModel, model);
            Assert.assertEquals(stmts.toString(), 0, stmts.size());
        } finally {
            IOUtils.closeQuietly(os);
        }
    }
}
