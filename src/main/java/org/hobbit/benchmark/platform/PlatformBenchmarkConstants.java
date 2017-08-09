package org.hobbit.benchmark.platform;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public class PlatformBenchmarkConstants {

    ////////// Docker image names
    public static final String GENERAL_IMAGE_NAME = "git.project-hobbit.eu:4567/gitadmin/platform-benchmark";
    public static final String DATA_GENERATOR_IMAGE = GENERAL_IMAGE_NAME;
    public static final String TASK_GENERATOR_IMAGE = GENERAL_IMAGE_NAME;
    public static final String EVALUATION_MODULE_IMAGE = GENERAL_IMAGE_NAME;
    public static final String SYSTEM_IMAGE = "git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system";

    ////////// RDF Constants
    public static final String VOCAB_NAMESPACE = "http://w3id.org/hobbit/platform-benchmark/vocab#";
    
    public static final Property numberOfQueries = new PropertyImpl(VOCAB_NAMESPACE, "numberOfQueries");
    public static final Property numberOfDataGenerators = new PropertyImpl(VOCAB_NAMESPACE, "numberOfDataGenerators");
    public static final Property numberOfTaskGenerators = new PropertyImpl(VOCAB_NAMESPACE, "numberOfTaskGenerators");
    public static final Property numberOfSystemAdapters = new PropertyImpl(VOCAB_NAMESPACE, "numberOfSystemAdapters");
    public static final Property seed = new PropertyImpl(VOCAB_NAMESPACE, "seed");
    
    public static final Property responsesPerSecond = new PropertyImpl(VOCAB_NAMESPACE, "responsesPerSecond");
    public static final Property msPerQuery = new PropertyImpl(VOCAB_NAMESPACE, "msPerQuery");
    public static final Property queryRuntimeStdDev = new PropertyImpl(VOCAB_NAMESPACE, "queryRuntimeStdDev");
    public static final Property runtime = new PropertyImpl(VOCAB_NAMESPACE, "runtime");
    public static final Property errorCount = new PropertyImpl(VOCAB_NAMESPACE, "errorCount");

    ////////// Processing constants

    public static final int SPARQL_QUERY_LENGTH = 545;
    public static final int RESULT_SET_LENGTH = 12200;
    public static final String DATA_RESOURCE_NAME = "exampleData.txt";

    ////////// Environmental variables
    public static final String CLASS_NAME_PARAMETER_KEY = "CLASS";
    public static final String SEED_PARAMETER_KEY = "SEED";
    public static final String NUMBER_OF_QUERIES_PARAMETER_KEY = "QUERIES";

}
