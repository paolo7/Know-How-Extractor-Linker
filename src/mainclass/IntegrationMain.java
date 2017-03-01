package mainclass;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import db.DatabaseConnectorPostgreSQL;
import db.DatabaseConnector;
import db.DatabaseConnectorDerby;
import db.DatabaseConnectorFile;
import db.DatabaseConnectorMySQL;
import db.DatabaseConnectorMySQL2;
import db.DatabaseConnectorMySQL3;
import wordnet.WordnetAPI;
import wordnet.WordnetAPIMIT;
import wordnet.WordnetConfiguration;
import nlp.TextProcessingController;
import nlp.TextProcessingConfig;
import integrationcore.IntegrationConfiguration;
import integrationcore.IntegrationController;

/**
 * This class is used to run all the main functionalities, except for the DBpedia integration.
 * For this last one, see StartDBpediaIntegration.java in the dbpediaLinks package
 * This class only loads the parameters from the configuration file and then calls the IntegrationController class
 * which will then start different processes depending on the supplied configuration
 * @author paolo
 *
 */
public class IntegrationMain {

	public static void main(String[] args) {
		
		Properties prop = new Properties();
		InputStream input = null;
	 
		//DatabaseConnector db = new DatabaseConnectorDerby();
		DatabaseConnector db = new DatabaseConnectorPostgreSQL();
		db.open();
		//db.close();
		
		
		try {
			
			input = new FileInputStream("config.properties");
			prop.load(input);
	 
			String localDirectory = "";
			if(prop.getProperty("LocalDirectory") != null) localDirectory = prop.getProperty("LocalDirectory");
			WordnetConfiguration wordnetConfiguration = new WordnetConfiguration();
			if(prop.getProperty("WordnetFilePath") != null) wordnetConfiguration.setWordnetFilePath(prop.getProperty("WordnetFilePath"));///
			WordnetAPI wordnetAPI;
			
			wordnetAPI = new WordnetAPIMIT(wordnetConfiguration);
			//
			TextProcessingConfig textPreProcessingConfiguration = new TextProcessingConfig();
			textPreProcessingConfiguration.setLogVerbosity(100000);
			TextProcessingController textPreProcessing = new TextProcessingController(textPreProcessingConfiguration, wordnetAPI);
			IntegrationConfiguration intConfig = new IntegrationConfiguration();
			if(prop.getProperty("MaxThreads") != null) intConfig.setMaxThreads(Integer.valueOf(prop.getProperty("MaxThreads")));
			if(prop.getProperty("PathToTurtlePrefixes") != null) intConfig.setPathToTurtlePrefixes(prop.getProperty("PathToTurtlePrefixes"));
			intConfig.setBlockedSynsets("blockedsynsets");
			intConfig.setVerbWeight(0.1);
			if(prop.getProperty("MaxSentencesToConsider") != null) intConfig.setMaxSentencesToConsider(Integer.valueOf(prop.getProperty("MaxSentencesToConsider")));
			if(prop.getProperty("MaxWordsToConsider") != null) intConfig.setMaxWordsToConsider(Integer.valueOf(prop.getProperty("MaxWordsToConsider")));
			if(prop.getProperty("RestrictIntegrationToComplexEntities") != null) intConfig.setRestrictIntegrationToComplexEntities(Boolean.valueOf(prop.getProperty("RestrictIntegrationToComplexEntities")));
			if(prop.getProperty("RestrictLuceneIndexCreationToPrimitiveEntities") != null) intConfig.setRestrictLuceneIndexCreationToPrimitiveEntities(Boolean.valueOf(prop.getProperty("RestrictLuceneIndexCreationToPrimitiveEntities")));
			if(prop.getProperty("RestrictLuceneIndexCreationToComplexEntities") != null) intConfig.setRestrictLuceneIndexCreationToComplexEntities(Boolean.valueOf(prop.getProperty("RestrictLuceneIndexCreationToComplexEntities")));
			if(prop.getProperty("PathToLuceneTargetIndex") != null) intConfig.setPathToLuceneTargetIndex(localDirectory+prop.getProperty("PathToLuceneTargetIndex"));
			if(prop.getProperty("PathToLuceneSourceIndex") != null) intConfig.setPathToLuceneSourceIndex(localDirectory+prop.getProperty("PathToLuceneSourceIndex"));
			if(prop.getProperty("PathToLuceneContextIndex") != null) intConfig.setPathToLuceneContextIndex(localDirectory+prop.getProperty("PathToLuceneContextIndex"));
			if(prop.getProperty("PathToLuceneIndexToCreate") != null) intConfig.setPathToLuceneIndexToCreate(localDirectory+prop.getProperty("PathToLuceneIndexToCreate"));
			if(prop.getProperty("PlotDirectory") != null) intConfig.setPlotDirectory(prop.getProperty("PlotDirectory"));
			if(prop.getProperty("DoLuceneIndexing") != null) intConfig.setDoLuceneIndexing(Boolean.valueOf(prop.getProperty("DoLuceneIndexing")));
			if(prop.getProperty("DoLuceneIndexingContext") != null) intConfig.setDoLuceneIndexingContext(Boolean.valueOf(prop.getProperty("DoLuceneIndexingContext")));
			if(prop.getProperty("DoLuceneIndexingApplication") != null) intConfig.setDoLuceneIndexingApplication(Boolean.valueOf(prop.getProperty("DoLuceneIndexingApplication")));
			if(prop.getProperty("DoLuceneQuerying") != null) intConfig.setDoLuceneQuerying(Boolean.valueOf(prop.getProperty("DoLuceneQuerying")));
			if(prop.getProperty("DoEvaluation") != null) intConfig.setDoEvaluation(Boolean.valueOf(prop.getProperty("DoEvaluation")));
			if(prop.getProperty("DoManualEvaluation") != null) intConfig.setDoManualEvaluation(Boolean.valueOf(prop.getProperty("DoManualEvaluation")));
			if(prop.getProperty("DoEvaluationDBpedia") != null) intConfig.setDoEvaluationDBpedia(Boolean.valueOf(prop.getProperty("DoEvaluationDBpedia")));
			if(prop.getProperty("DoManualEvaluationDBpedia") != null) intConfig.setDoManualEvaluationDBpedia(Boolean.valueOf(prop.getProperty("DoManualEvaluationDBpedia")));
			if(prop.getProperty("EvaluationFolderUserGenerated") != null) intConfig.setEvaluationFolderUserGenerated(prop.getProperty("EvaluationFolderUserGenerated"));
			if(prop.getProperty("EvaluationFolderPositive") != null) intConfig.setEvaluationFolderPositive(prop.getProperty("EvaluationFolderPositive"));
			if(prop.getProperty("EvaluationFolderNegative") != null) intConfig.setEvaluationFolderNegative(prop.getProperty("EvaluationFolderNegative"));
			if(prop.getProperty("EvaluationFolderDBpediaPositive") != null) intConfig.setEvaluationFolderDBpediaPositive(prop.getProperty("EvaluationFolderDBpediaPositive"));
			if(prop.getProperty("EvaluationFolderDBpediaNegative") != null) intConfig.setEvaluationFolderDBpediaNegative(prop.getProperty("EvaluationFolderDBpediaNegative"));
			if(prop.getProperty("DoLuceneActivityRecognition") != null) intConfig.setDoLuceneActivityRecognition(Boolean.valueOf(prop.getProperty("DoLuceneActivityRecognition")));
			if(prop.getProperty("DoLinksPostProcessing") != null) intConfig.setDoLinksPostProcessing(Boolean.valueOf(prop.getProperty("DoLinksPostProcessing")));
			if(prop.getProperty("DoDBpediaLinksPostProcessing") != null) intConfig.setDoDBpediaLinksPostProcessing(Boolean.valueOf(prop.getProperty("DoDBpediaLinksPostProcessing")));
			if(prop.getProperty("DoMachineLearning") != null) intConfig.setDoMachineLearning(Boolean.valueOf(prop.getProperty("DoMachineLearning")));
			if(prop.getProperty("LuceneQueryDecompositionLinks") != null) intConfig.setLuceneQueryDecompositionLinks(Boolean.valueOf(prop.getProperty("LuceneQueryDecompositionLinks")));
			if(prop.getProperty("OntologyGraph") != null) intConfig.setOntologyGraphQueryString(" FROM <"+prop.getProperty("OntologyGraph")+"> ");
			if(prop.getProperty("NewURLsToEvaluate") != null) intConfig.setNewURLsToEvaluate(Math.max(0,Integer.valueOf(prop.getProperty("NewURLsToEvaluate"))));
			if(prop.getProperty("DBpediaLinksPostProcessingFolder") != null) intConfig.setDBpediaLinksPostProcessingFolder(localDirectory+prop.getProperty("DBpediaLinksPostProcessingFolder"));
			if(prop.getProperty("RestrictEvaluationToOneNewLinkPerURL") != null) intConfig.setRestrictEvaluationToOneNewLinkPerURL(Boolean.valueOf(prop.getProperty("RestrictEvaluationToOneNewLinkPerURL")));
			if(prop.getProperty("ClassfierSettings") != null) intConfig.setClassfierSettings(prop.getProperty("ClassfierSettings"));
			
			if(prop.getProperty("PostProcCatDown") != null) intConfig.setPostProcCatDown(Integer.valueOf(prop.getProperty("PostProcCatDown")));
			if(prop.getProperty("PostProcCatUp") != null) intConfig.setPostProcCatUp(Integer.valueOf(prop.getProperty("PostProcCatUp")));
			if(prop.getProperty("PostProcCountDown") != null) intConfig.setPostProcCountDown(Integer.valueOf(prop.getProperty("PostProcCountDown")));
			if(prop.getProperty("PostProcCountUp") != null) intConfig.setPostProcCountUp(Integer.valueOf(prop.getProperty("PostProcCountUp")));
			
			if(prop.getProperty("PathToFileWithEntitiesInAlphabeticalOrder") != null) intConfig.setPathToFileWithEntitiesInAlphabeticalOrder(prop.getProperty("PathToFileWithEntitiesInAlphabeticalOrder"));
			if(prop.getProperty("KnowHowLinksResultsDirectory") != null) intConfig.setKnowHowLinksResultsDirectory(prop.getProperty("KnowHowLinksResultsDirectory"));
			//intConfig.setPathToDataDirectory("D:\\DataDirectory\\Integration WikiHow_22_April_2014\\folder");
			if(prop.getProperty("NamedGraphs") != null) intConfig.setNamedGraphs(prop.getProperty("NamedGraphs").split("\\s+")
					/*"http://vocab.inf.ed.ac.uk/graph#wikihow7",
					"http://vocab.inf.ed.ac.uk/graph#wikihow6",
					"http://vocab.inf.ed.ac.uk/graph#wikihow5",
					"http://vocab.inf.ed.ac.uk/graph#wikihow4",
					"http://vocab.inf.ed.ac.uk/graph#wikihow3",
					"http://vocab.inf.ed.ac.uk/graph#wikihow2",
					"http://vocab.inf.ed.ac.uk/graph#wikihow1",
					"http://vocab.inf.ed.ac.uk/graph#snapguide",*/
					//"http://vocab.inf.ed.ac.uk/graph#wikihowontology",
					//"http://vocab.inf.ed.ac.uk/graph#wikihowpostprocessed"}
					);
			//intConfig.setExtractedPropertiesGraph("http://vocab.inf.ed.ac.uk/graph#wikihowpostprocessed");
			
			
			// CHOOSE CONNECTION METHOD
			
			//intConfig.setEndpointURL("http://localhost:3030/wikihowfull/query"); // This was for jena fuseki
			if(prop.getProperty("EndpointURL") != null) intConfig.setEndpointURL(prop.getProperty("EndpointURL")); // this is for Virtuoso
			//unnecessary settings as these are already default values
			intConfig.setLogVerbosity(10000);
			intConfig.setTimestamp(true);
			IntegrationController controller = new IntegrationController(intConfig, textPreProcessing, db);

			controller.log("***INTEGRATION STARTED***");
			boolean success = controller.startIntegration();
			if(success){
				controller.log("***INTEGRATION FINISHED***");
			} else {
				controller.log("***INTEGRATION ABORTED - ERROR***");			
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			}
		db.close();
	}

}
