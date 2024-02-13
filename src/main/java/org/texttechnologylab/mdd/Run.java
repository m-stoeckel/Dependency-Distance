// package org.texttechnologylab.mdd;

// import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

// import java.util.Arrays;
// import java.util.Iterator;

// import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
// import org.texttechnologylab.DockerUnifiedUIMAInterface.connection.mongodb.MongoDBConfig;
// import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIPipelineComponent;
// import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
// import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
// import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
// import org.texttechnologylab.mdd.engine.DependencyDistanceEngine;
// import org.texttechnologylab.parliament.duui.DUUIGerParCorReader;

// import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;

// public class Run {

//     public static void main(String[] args) {
//         String pMongoDbConfigPath = System.getProperty("config");
//         if (pMongoDbConfigPath == null || pMongoDbConfigPath.isEmpty()) {
//             throw new IllegalArgumentException("No MongoDB config file provided");
//         }

//         String pFilter = System.getProperty("filter", "{}");
//         int pScale = Integer.parseInt(System.getProperty("scale", "8"));

//         String pOutput = System.getProperty("output", "/storage/projects/stoeckel/syntactic-language-change/mdd/");
//         boolean pOverwrite = Boolean.parseBoolean(System.getProperty("overwrite", "false"));
//         CompressionMethod pCompression = CompressionMethod.valueOf(System.getProperty("compression", "NONE"));

//         boolean pFailOnError = Boolean.parseBoolean(System.getProperty("failOnError", "false"));

//         Iterator<String> iterator = Arrays.stream(args).iterator();
//         while (iterator.hasNext()) {
//             String argName = iterator.next();
//             switch (argName) {
//                 case "--config":
//                     pMongoDbConfigPath = iterator.next();
//                     break;
//                 case "--filter":
//                     pFilter = iterator.next();
//                     break;
//                 case "--scale":
//                     pScale = Integer.parseInt(iterator.next());
//                     break;
//                 case "--output":
//                     pOutput = iterator.next();
//                     break;
//                 case "--overwrite":
//                     pOverwrite = Boolean.parseBoolean(iterator.next());
//                     break;
//                 case "--compression":
//                     pCompression = CompressionMethod.valueOf(iterator.next());
//                     break;
//                 case "--failOnError":
//                     pFailOnError = Boolean.parseBoolean(iterator.next());
//                     break;
//                 default:
//                     throw new IllegalArgumentException(
//                         String.format("Encountered illegal argument '%s' in CLI arguments: %s", argName, Arrays.toString(args))
//                     );
//             }
//         }

//         try {
//             MongoDBConfig mongoDbConfig = new MongoDBConfig(pMongoDbConfigPath);
//             DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(new DUUIGerParCorReader(mongoDbConfig, pFilter));

//             DUUIComposer composer = new DUUIComposer()
//                 .withSkipVerification(true)
//                 .withWorkers(pScale)
//                 .withCasPoolsize(4 * pScale)
//                 .withLuaContext(new DUUILuaContext().withJsonLibrary());

//             DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
//             composer.addDriver(uimaDriver);

//             DUUIPipelineComponent dependency = new DUUIUIMADriver.Component(
//                 createEngineDescription(
//                     DependencyDistanceEngine.class,
//                     DependencyDistanceEngine.PARAM_TARGET_LOCATION,
//                     pOutput,
//                     DependencyDistanceEngine.PARAM_OVERWRITE,
//                     pOverwrite,
//                     DependencyDistanceEngine.PARAM_COMPRESSION,
//                     pCompression,
//                     DependencyDistanceEngine.PARAM_FAIL_ON_ERROR,
//                     pFailOnError
//                 )
//             )
//                 .withScale(pScale)
//                 .build();
//             composer.add(dependency);

//             composer.run(processor, "mDD");
//             composer.shutdown();

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
// }
