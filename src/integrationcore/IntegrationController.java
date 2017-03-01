package integrationcore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.ws.http.HTTPException;

import luceneIndexing.LuceneIndexCreator;
import luceneIndexing.LuceneIndexCreatorThread;
import luceneIndexing.LuceneIndexQuery;
import luceneIndexing.LuceneIndexQueryThread;
import nlp.TextProcessingController;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.web.HttpException;
import org.apache.lucene.queryparser.classic.ParseException;

import query.SparqlManager;
import activityRecognition.ActivityRecognizer;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

import comparison.ComparisonController;
import db.DatabaseConnector;
import dbpediaLinks.Logger;
import edu.mit.jwi.item.Synset;
import evaluation.LinksEvaluation;

public class IntegrationController implements Logger {

	
	public IntegrationConfiguration intConfig;
	public TextProcessingController textPreProcessing;
	public SparqlManager sparql = null;
	private int preprocessedEntities = 0;
	private DatabaseConnector databaseConnection;
	private ComparisonController comparison;
	private int comparedSynsets = 0;
	private String prefixes = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>  "
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
			+ "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#>  "
			+ "PREFIX madph: <http://vocab.inf.ed.ac.uk/madprohow#>  "
			+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>  "
			+ "PREFIX oa: <http://www.w3.org/ns/oa#>  "
			+ "PREFIX dctypes: <http://purl.org/dc/dcmitype/>  "
			+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>  "
			+ "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>  "
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";
	
	private Map<String,Set<String>> dbpediaEntitiesInSource = new HashMap<String,Set<String>>();
	
	public IntegrationController(IntegrationConfiguration intConfig, TextProcessingController textPreProcessing, DatabaseConnector databaseConnection){
		this.intConfig = intConfig;
		this.textPreProcessing = textPreProcessing;
		this.databaseConnection = databaseConnection;
		comparison = new ComparisonController(this);
		sparql = new SparqlManager(this);
		if(intConfig.getPathToDataDirectory() != null) {
			sparql.setLocalDirectory(intConfig.getPathToDataDirectory());
		} else if(intConfig.getEndpointURL() != null){
			sparql.setRemoteEndpoint(intConfig.getEndpointURL());
		} else {
			log(1,"ERROR - Integration failed. It was not possible to identify a way to connect to a RDF triplestore.");
		}
	 	//log(rdfData.getProperty(ResourceFactory.createResource("http://vocab.inf.ed.ac.uk/procont#?url=http://www.wikihow.com/cross-your-eyes&t=1393507329343&n=4921222&k=annotationselector"), ResourceFactory.createProperty("http://www.w3.org/ns/oa#exact")).toString());
	}

	public boolean startIntegration(){
		
		// Utility to clean the worst articles from snapguide
		/*utilityCleanSnapGuide("D:\\DataDirectory\\SnapGuide_14_April_2014-Cleaned6June\\",100,10);
		System.out.println(""); if(true)return true;*/
		
		/*try {
			sparql.utilityToRDFXML("D:\\DataDirectory\\SnapGuide_14_April_2014\\AAA\\Newfolder\\art\\art.rdf");
			} catch (com.hp.hpl.jena.shared.CannotEncodeCharacterException e) {
				e.printStackTrace();
			}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(true) return false;*/
		
		
		/*try {
			sparql.utilityBreakIntoSmallerChunks("D:\\DataDirectory\\Integration WikiHow_29_April_2014\\ToConvert","D:\\DataDirectory\\Integration WikiHow_29_April_2014\\Converted\\",40);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			sparql.utilityAddPrefixes("D:\\DataDirectory\\Integration WikiHow_29_April_2014\\Converted","D:\\DataDirectory\\Integration WikiHow_29_April_2014\\ConvertedAndPrefixed\\",intConfig.getPathToTurtlePrefixes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		//sparql.loadModelFromDirectory("D:\\DataDirectory\\Integration WikiHow_29_April_2014\\validate");
		/*try {
			sparql.forgetAllModels();
			sparql.setLocalDirectory("D:\\DataDirectory\\Integration WikiHow_29_April_2014\\xmlify\\");
			sparql.utilityToRDFXML("D:\\DataDirectory\\Integration WikiHow_29_April_2014\\xmlify\\2ResultsVerbsEntities.rdf");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		log(400,"\nINTEGRATION - Planned operations:");
		if(intConfig.isDoLuceneIndexing()) log(100,"INTEGRATION - - Planned: LUCENE INDEX CREATION");
		if(intConfig.isDoLuceneQuerying()) log(100,"INTEGRATION - - Planned: LUCENE INDEX QUERY");
		if(intConfig.isDoMachineLearning()) log(100,"INTEGRATION - - Planned: LUCENE MACHINE LEARNING DATAPOINTS GENERATION (QUERY)");
		if(intConfig.isDoLinksPostProcessing()) log(100,"INTEGRATION - - Planned: LINKS POST PROCESSING");
		if(intConfig.isDoDBpediaLinksPostProcessing()) log(100,"INTEGRATION - - Planned: DBPEDIA LINKS POST PROCESSING");
		if(intConfig.isDoEvaluation()) log(100,"INTEGRATION - - Planned: EVALUATION of decomposition links");
		if(intConfig.isDoManualEvaluation()) log(100,"INTEGRATION - - Planned: MANUAL EVALUATION of decomposition links");
		if(intConfig.isDoEvaluationDBpedia()) log(100,"INTEGRATION - - Planned: EVALUATION of DBpedia links");
		if(intConfig.isDoManualEvaluationDBpedia()) log(100,"INTEGRATION - - Planned: MANUAL EVALUATION of DBpedia links");
		if(intConfig.isDoLuceneActivityRecognition()) log(100,"INTEGRATION - - Planned: LUCENE ACTIVITY RECOGNITION");
		if(intConfig.isDoPreprocessing()) log(100,"INTEGRATION - - Planned: PREPROCESSING");
		if(intConfig.isDoLinking()) log(100,"INTEGRATION - - Planned: INTERLINKING");
		log(100,"\n");
					
		if(intConfig.isDoLuceneIndexing()){
			doLuceneIndex();
		}
		
		if(intConfig.isDoLuceneQuerying()){
			doLuceneQuery();
		}
		
		if(intConfig.isDoLinksPostProcessing()){
			LinksPostProcessing postProcessLinks = new LinksPostProcessing(this);
			postProcessLinks.loadLinksFromDirectory(intConfig.getKnowHowLinksResultsDirectory());
			postProcessLinks.postProcess(intConfig.getPostProcCatUp(),intConfig.getPostProcCatDown(),intConfig.getPostProcCountUp(),intConfig.getPostProcCountDown());
		}
		
		if(intConfig.isDoDBpediaLinksPostProcessing()){
			DBpediaLinksPostProcessing postProcessingDBpediaLinks = new DBpediaLinksPostProcessing(this,"Resources\\Results\\DBpediaLinks\\",intConfig.getDBpediaLinksPostProcessingFolder());
			postProcessingDBpediaLinks.postProcess();
		}	
		
		LinksEvaluation evaluation = null;
		
		if(intConfig.isDoEvaluation() || intConfig.isDoManualEvaluation()) {
			evaluation = new LinksEvaluation(intConfig.getKnowHowLinksResultsDirectory(), intConfig.getEvaluationFolderPositive(), intConfig.getEvaluationFolderNegative(), this, "Decomposition Links");
			evaluation.calculatePrecisionAndRecall();
		}
		if(intConfig.isDoManualEvaluation()) {
			try {
				evaluation.manuallyAnnotateUnkownLinks();
				evaluation.calculatePrecisionAndRecall();
			} catch (IOException e) {
				System.out.println("ERROR - manual evaluation of the unknown links aborted");
				e.printStackTrace();
			}
		}
		
		LinksEvaluation evaluationDBpedia = null;
		
		if(intConfig.isDoEvaluationDBpedia() || intConfig.isDoManualEvaluationDBpedia()) {
			evaluationDBpedia = new LinksEvaluation("Resources\\Results\\DBpediaLinks\\", intConfig.getEvaluationFolderDBpediaPositive(), intConfig.getEvaluationFolderDBpediaNegative(), this, "DBpedia Links");
			//evaluationDBpedia.calculatePrecisionAndRecall();
		}
		if(intConfig.isDoManualEvaluationDBpedia()) {
			try {
				evaluationDBpedia.manuallyAnnotateUnkownLinks();
				//evaluationDBpedia.calculatePrecisionAndRecall();
			} catch (IOException e) {
				System.out.println("ERROR - manual evaluation of the unknown links aborted");
				e.printStackTrace();
			}
		}
		
		if(intConfig.isDoLuceneActivityRecognition()){
			doLuceneActivityRecognition();
		}
				
		//Perform Test query
		//log(1,"INTEGRATION - Test triplestore");
		/*String triplestoreSize = testSPARQLService();
		if(triplestoreSize == null){
			log(1,"ERROR - Triplestore is empty or unreachable");
			return false;
		} else log(100,"INTEGRATION - Test triplestore OK. Triplestore size: "+triplestoreSize+" triples");*/
		
		/*log(1,"INTEGRATION - Test database");
		boolean dbWorking = testDB();
		if(!dbWorking){
			log(1,"ERROR - Database cannot be modified");
			return false;
		} else log(100,"INTEGRATION - Test database OK");*/
		
		
		//if(intConfig.isDoPreprocessing()) preprocessAllEntities();
		
		//processAllSynsets();
		//if(intConfig.isDoSynsetsStatistics()) utilityComputeSynsetsStatistics();
		
		//if(intConfig.isDoLinking()) computeSimilarityAll();
		
		/*if(intConfig.isDoSemLinkingPreorderEntities()) {
			doSemLinkingPreorderEntities();
		}
		
		if(intConfig.isDoSemLinkingComputeSimilarity()) {
			try {
				doSemLinkingComputeSimilarity();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(intConfig.isDoSemLinkingSourceBased()) {
			try {
				doSemLinkingSourceBased();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		
		return true;
	}
	
	private void doLuceneIndex(){
		log(10,"PREPROCESSING - LUCENE INDEX CREATION - START");
		LuceneIndexCreator luceneIndex = new LuceneIndexCreator(sparql,intConfig.getPathToLuceneIndexToCreate(),intConfig.getNamedGraphs(),textPreProcessing,intConfig);
		try {
			luceneIndex.createIndex();
		} catch (IOException e) {
			log(10,"ERROR - PREPROCESSING - LUCENE INDEX CREATION -");
			e.printStackTrace();
		}
		log(10,"PREPROCESSING - LUCENE INDEX CREATION - END");
	}
	
	private void doLuceneActivityRecognition(){
		log(10,"ACTIVITY RECOGNITION - LUCENE - START");
		LuceneIndexQuery luceneQuery;
		try {
			luceneQuery = new LuceneIndexQuery(textPreProcessing,sparql,100,intConfig.getMaxSentencesToConsider(), intConfig);
			ActivityRecognizer recognizer = new ActivityRecognizer(textPreProcessing, luceneQuery, sparql, intConfig);
			
			String input = "";
			while(!input.equals("-q")){
				System.out.println("ACTIVITY RECOGNITION FROM TEXT");
				System.out.println("To exit, enter -q");
				System.out.println("To run the tests, enter -test");
				System.out.println("Enter the text of the activity to recognize:)");
				//input = System.console().readLine();
				
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
				input = bufferRead.readLine();
			    
			    if(input.equals("-test")){
					recognizer.recognizeActivity("I boiled the water. I put the teabag in the cup. I put the boiling water in the cup. I drink my tea.");
					recognizer.recognizeActivity("Jenny is wearing makeup. She goes to the party. She is talking with John. She dances.");
					/*recognizer.recognizeActivity("I was wondering whether you had it on a jack or not. Make sure the flat clears the ground before you start pulling it off. You can't be too rough or you will have the car fall off the jack. "
							+ " What I am guessing at is that the centre hub hole is not large enough for the dust cover of the wheel bearing in the center. "
							+ " Non stock rims sometimes come that way....you gotta grind the centre hole larger. "
							+ "Now to get it off. If it is in the air, take a sledge hammer and do some \"putting - golfing\" with it and sledge the rim "
							+ " (from the outside hitting the outside of the rim)on the lower portion, then rotate the rim 180 degrees and do another putt. "
							+ "If you hit the bottom, the top should flip outward toward you. Then do a 90 degree turn and give it a similiar smack. "
							+ " If the rim does not seem to move at all, then you are gonna have to hit it from the inside outward. "
							+ " BUT FIRST SHORE UP THE CAR, OVERDO THE BLOCKING,,,JUST MAKE SURE THE CAR DOES NOT COME CRASHING DOWN ONTOP OF YOU. "
							+ " Oil, grease, heat will not work here. You bolted up tight a rim that did not fit on correctly. Make sure that the centre hole is more than big enough. That won't affect the trueness of the wheel, the bolts hold the wheel in the centre. Jobber rims do not fit well. You will be using a grinder.");
*/				}
				else {
					recognizer.recognizeActivity(input);
				}
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log(10,"ACTIVITY RECOGNITION - LUCENE - END");
	}
	
	private void doLuceneQuery(){
			log(10,"INTEGRATION - LUCENE QUERY - START");
			Map<String,Boolean> evaluationMap = new HashMap<String,Boolean>();
			LuceneIndexQuery luceneQuery;
			try {
				textPreProcessing.initializePOSpipeline();
				luceneQuery = new LuceneIndexQuery(textPreProcessing,sparql,100,intConfig.getMaxSentencesToConsider(), intConfig);
				Date start = new Date();
				Set<String> uris = new HashSet<String>();
				
				if(intConfig.isDoMachineLearning()){
					log(10,"INTEGRATION - LUCENE QUERY - Query ONLY links in the evaluation set");
					SparqlManager evaluationDataPositive = new SparqlManager();
					evaluationDataPositive.setLocalDirectory(intConfig.getEvaluationFolderPositive());
					
					String queryString = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#>  "
							+ "SELECT DISTINCT ?a ?uri WHERE { ?a prohow:has_step ?uri . } ";
					
					QueryExecution execution = evaluationDataPositive.query(queryString);
					ResultSet results = execution.execSelect();
					Map<String, Set<String>> positiveMap = new HashMap<String, Set<String>>();
					while(results.hasNext()){
						QuerySolution result = results.next();
						String entityUri = result.getResource("?uri").getURI();
						String decomposedUri = result.getResource("?a").getURI();
						uris.add(entityUri);
						evaluationMap.put(decomposedUri+entityUri, Boolean.TRUE);
						if(positiveMap.containsKey(entityUri)) positiveMap.get(entityUri).add(decomposedUri);
						else {
							Set<String> decomposedURIs = new HashSet<String>();
							decomposedURIs.add(decomposedUri);
							positiveMap.put(entityUri, decomposedURIs);
						}
					}
					execution.close();
					
					SparqlManager evaluationDataNegative = new SparqlManager();
					evaluationDataNegative.setLocalDirectory(intConfig.getEvaluationFolderNegative());
					
					execution = evaluationDataNegative.query(queryString);
					results = execution.execSelect();
					Map<String, Set<String>> negativeMap = new HashMap<String, Set<String>>();
					while(results.hasNext()){
						QuerySolution result = results.next();
						String entityUri = result.getResource("?uri").getURI();
						String decomposedUri = result.getResource("?a").getURI();
						uris.add(entityUri);
						evaluationMap.put(decomposedUri+entityUri, Boolean.FALSE);
						if(negativeMap.containsKey(entityUri)) negativeMap.get(entityUri).add(decomposedUri);
						else {
							Set<String> decomposedURIs = new HashSet<String>();
							decomposedURIs.add(decomposedUri);
							negativeMap.put(entityUri, decomposedURIs);
						}
					}
					execution.close();
					luceneQuery.setEvaluationMap(evaluationMap);
					luceneQuery.setEvaluationMapPositive(positiveMap);
					luceneQuery.setEvaluationMapNegative(negativeMap);
				} 
				
				ArrayBlockingQueue<Runnable> blockingqueue = new ArrayBlockingQueue<Runnable>(intConfig.getMaxThreads());
				ThreadPoolExecutor executor = new ThreadPoolExecutor(intConfig.getMaxThreads()/4,intConfig.getMaxThreads(),1,TimeUnit.SECONDS,blockingqueue);
				for(String uri : uris){
					preprocessedEntities++; //System.out.print(".");
					//luceneQuery.querySimilarity(uri);
					int iteration = 0;
					while(blockingqueue.remainingCapacity() < 1){
						if(iteration > 0 && iteration % 10000 == 0) {
							System.out.println("WARNING, process has been queued for "+iteration/1000+" seconds");
						}
						iteration++;
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					executor.execute(new LuceneIndexQueryThread(luceneQuery, uri));
					
				}
				executor.shutdown();
				if(intConfig.isDoMachineLearning()){
					log(10,"INTEGRATION - LUCENE QUERY - Print out of the machine learning dataset:");
					luceneQuery.printData();
					luceneQuery.testTrainClassifiers();
					luceneQuery.trainClassifiers();
				}
				intConfig.setDoMachineLearning(false);
				
				{
					// TODO this works only because there are less than 1,000,000 requirement entities. If there were more, the results would be capped to 1 million
					// in this case the query should be broken down into smaller chunks (sub-graphs) like it is done below to get all the process entities
					// which are more than 1 million.
					File tempIndexOfEntities = new File("temEntitiesUriAll.txt");
					if(intConfig.isRestrictIntegrationToComplexEntities()){
						tempIndexOfEntities = new File("temEntitiesUriComplexOnly.txt");
					}
					if(!tempIndexOfEntities.exists()){
						log(10,"INTEGRATION - LUCENE QUERY - Cache file with entities URIs to consider was not found. It will be then created extracting URIs from the triplestore.");
						Set<String> mainUriRequirements = new HashSet<String>();
						{
							log(10,"INTEGRATION - LUCENE QUERY - Query for main entities requirements to link - start");
							QueryExecution execution = sparql.query("PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX oa: <http://www.w3.org/ns/oa#> SELECT DISTINCT ?uri "+intConfig.getNamedGraphsQueryString()+" WHERE { "
									+ " ?mainentity prohow:requires ?uri . "
									+ " FILTER regex(str(?mainentity), \"mainentity$\", \"i\" ) "
									+ "}");
							ResultSet results = execution.execSelect();
							while(results.hasNext()){
								QuerySolution result = results.next();
								mainUriRequirements.add(result.getResource("?uri").getURI());
							}
							execution.close();
						}
						{
							log(10,"INTEGRATION - LUCENE QUERY - Query for main entities requirements to link - Main entities requirements found: "+mainUriRequirements.size());
							log(10,"INTEGRATION - LUCENE QUERY - Query for main entities requirements to link - end");
							
							log(10,"INTEGRATION - LUCENE QUERY - Query for entities to link - start");
							int totEntitiesConsidered = 0;
							int totRequirementsEntitiesDiscarded = 0;
							
							
							for(String s : intConfig.getNamedGraphs()) {
								log(10,"INTEGRATION - LUCENE QUERY - Query for entities to link - retrieving entities from graph "+s);
								String queryString = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#>  SELECT DISTINCT ?uri FROM <"+s+"> WHERE { "
										+ "?annotation oa:hasBody ?uri . ";
								if(intConfig.isRestrictIntegrationToComplexEntities()){
									queryString = queryString + " ?uri prohow:has_step | prohow:has_method ?subentity . "
											+ " FILTER regex(str(?uri), \"mainentity$\", \"i\" ) "
											+ "} ";
								} else {
									queryString = queryString+" } ";
								}
								QueryExecution execution = sparql.query(queryString);
								ResultSet results = execution.execSelect();
								while(results.hasNext()){
									totEntitiesConsidered++;
									QuerySolution result = results.next();
									String entityUri = result.getResource("?uri").getURI();
									if(!mainUriRequirements.contains(entityUri) || !intConfig.isRestrictIntegrationToComplexEntities()){
										uris.add(entityUri);
									} else {
										totRequirementsEntitiesDiscarded++;
									}
								}
								execution.close();
							}
							log(10,"INTEGRATION - LUCENE QUERY - Query for entities to link - Step entities found: "+uris.size());
							log(10,"INTEGRATION - LUCENE QUERY - Query for entities to link - Tot entities considered: "+totEntitiesConsidered+" Tot requirements entities discarded: "+totRequirementsEntitiesDiscarded);
							log(10,"INTEGRATION - LUCENE QUERY - Query for entities to link - end");
						}
						log(10,"INTEGRATION - LUCENE QUERY - Cache step entities write in file - start");
						FileWriter writer = new FileWriter(tempIndexOfEntities);
						String newLine = System.getProperty("line.separator");
						for(String uri : uris)
						{
							writer.write(uri + newLine);
						}
						writer.close();
						log(10,"INTEGRATION - LUCENE QUERY - Cache step entities write in file - end");
					}
					log(10,"INTEGRATION - LUCENE QUERY - Read URIs from cache - start");
					uris = new HashSet<String>();
					BufferedReader br = new BufferedReader(new FileReader(tempIndexOfEntities));
					String line;
					while ((line = br.readLine()) != null) {
						uris.add(line);
					}
					br.close();
					log(10,"INTEGRATION - LUCENE QUERY - Read URIs from cache - "+uris.size()+" entities URIs read from file");
					log(10,"INTEGRATION - LUCENE QUERY - Read URIs from cache - end");
					luceneQuery.setStartDate(new Date());
				}
				log(10,"INTEGRATION - LUCENE QUERY - Compute similarity as Lucene search - start");
				
				executor = new ThreadPoolExecutor(intConfig.getMaxThreads()/4,intConfig.getMaxThreads(),1,TimeUnit.SECONDS,blockingqueue);
				for(String uri : uris){
					preprocessedEntities++; //System.out.print(".");
					//luceneQuery.querySimilarity(uri);
					int iteration = 0;
					while(blockingqueue.remainingCapacity() < 1){
						if(iteration > 0 && iteration % 10000 == 0) {
							System.out.println("WARNING, process has been queued for "+iteration/1000+" seconds");
						}
						iteration++;
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					executor.execute(new LuceneIndexQueryThread(luceneQuery, uri));
				}
				executor.shutdown();
				try {
					log(10,"INTEGRATION - LUCENE QUERY - Shutting down the query execution threads");
					if(executor.awaitTermination(5,TimeUnit.MINUTES)) log(10,"INTEGRATION - LUCENE QUERY - Execution threads correctly shut down");
					else log(10,"INTEGRATION - LUCENE QUERY - Execution threads shut down by timeout - WARNING");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				log(10,"INTEGRATION - LUCENE QUERY - Printing out the final log: ");
				luceneQuery.printSimilarityMatchStatistics();
				
				log(10,"INTEGRATION - LUCENE QUERY - Compute similarity as Lucene search - end");
				Date end = new Date();
				//System.out.println("1000 queries answered in "+(end.getTime()-start.getTime())+" milliseconds");
				//System.out.println("Query execution time for each single query: "+((end.getTime()-start.getTime())/1000)+" milliseconds");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			log(10,"INTEGRATION - LUCENE QUERY - END");
	}
	
	/**
	 * Loads all entities from the triplestore, sort them and store them in alphabetical order in a text file
	 */
	private void doSemLinkingPreorderEntities(){
		log(100,"INTEGRATION - Semantic interlinking START");
		int resultsFound = 0 ;
		List<String> allEntities = new LinkedList();
		for(String s : intConfig.getNamedGraphs()){
			System.out.println(s);
			QueryExecution execution = sparql.query("PREFIX oa: <http://www.w3.org/ns/oa#> SELECT ?a FROM <"+s+"> WHERE { "
					+ " ?ann oa:hasBody ?a . "
					+ " }  ");
			ResultSet results = execution.execSelect();
			while(results.hasNext()){
				resultsFound++;
				QuerySolution result = results.next();
				String uri = result.getResource("?a").getURI();
				allEntities.add(uri);
				//System.out.println(resultsFound+" > "+uri);
			}
			execution.close();
			
		}
		log(100,"INTEGRATION - Sorting the entities START");
		java.util.Collections.sort(allEntities);
		log(100,"INTEGRATION - Sorting the entities END");
		int index = 0;
		try {
			File writeFile = new File(intConfig.getPathToFileWithEntitiesInAlphabeticalOrder());
			FileWriter writer = new FileWriter(writeFile, true);
			for(String s : allEntities){
				index++;
					writer.write(s+"\n");
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log(100,"INTEGRATION - Semantic interlinking END");
	}
	
	// group similar articles before going deeper to the individual entities
	private void doSemLinkingSourceBased() throws UnsupportedEncodingException{
		
		log(100,"INTEGRATION - Semantic interlinking Source-Based START");
		// Retrieve a list of DBpedia properties
		Set<String> dbpediaProperties = new HashSet<String>();
		
		/*Map<StringPair,Integer> test = new HashMap<StringPair,Integer>();
		StringPair one = new StringPair("a", "b");
		StringPair two = new StringPair("a", "b");
		StringPair three = new StringPair("b", "a");
		StringPair four = new StringPair("a", "a");
		test.put(one,new Integer(1));
		boolean tr = test.containsKey(one);
		boolean tr1 = test.containsKey(two);
		boolean tr2 = test.containsKey(three);
		boolean fa = test.containsKey(four);*/
		
		Map<StringPair,Integer> similarityGraph = new HashMap<StringPair,Integer>();
		QueryExecution execution = sparql.query(prefixes + " SELECT distinct ?type FROM <"+intConfig.getExtractedPropertiesGraph()+"> WHERE { "
				+ " ?ent rdf:type ?type . } ");
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			QuerySolution result = results.next();
			String type = result.getResource("?type").getURI();
			dbpediaProperties.add(type);
		}
		execution.close();
		int expectedNumOfTypes = 19532;
		int typesAnalysed = 0;
		int bestScore = 0;
		StringPair bestPair = null;
		
		for(String type : dbpediaProperties){
			typesAnalysed++;
			execution = sparql.query(prefixes + " SELECT distinct ?entity "+intConfig.getNamedGraphsQueryString()+" WHERE { "
					+ " ?entity rdf:type <"+type+"> . } ");
			results = execution.execSelect();
			List<String> sourcesWithObjectInCommon = new LinkedList<String>();
			List<String> sourcesWith2ObjectsInCommon = new LinkedList<String>();
			while(results.hasNext()){
				QuerySolution result = results.next();
				String entity = result.getResource("?entity").getURI();
				String source  = URLEncoder.encode(entity.substring(39, entity.indexOf("&t=")), "UTF-8");
				if(sourcesWithObjectInCommon.contains(source)){
					if(!sourcesWith2ObjectsInCommon.contains(source)) sourcesWith2ObjectsInCommon.add(source);
				} else {
					sourcesWithObjectInCommon.add(source);
				}
			}
			execution.close();
			int listIndex = 0;
			int numOfSourcesInSet = sourcesWith2ObjectsInCommon.size();
			int newlinksFound = 0;

			if(numOfSourcesInSet <2000){
				while(listIndex < sourcesWith2ObjectsInCommon.size()) {
					Iterator<String> i = sourcesWith2ObjectsInCommon.subList(listIndex, sourcesWith2ObjectsInCommon.size()).iterator();
					String firstEntity = i.next();
					while(i.hasNext()){
						String otherEntity = i.next();
						if(!firstEntity.equals(otherEntity) &&
								// make sure they are not from the same website
								( firstEntity.indexOf(otherEntity.substring(0, 19)) != 0 ) 
								 ){
							StringPair p = new StringPair(firstEntity,otherEntity);
							if(similarityGraph.keySet().contains(p)){
								Integer previousScore = similarityGraph.get(p);
								similarityGraph.put(p, new Integer(previousScore+1));
								if(bestScore < previousScore+1) {
									bestScore = previousScore+1;
									bestPair = p;
								}
							} else {
								similarityGraph.put(p, new Integer(1));
								newlinksFound++;
								/*if(similarityGraph.size() > 2000000){
									throw new RuntimeException("PREEMPTIVE ERROR, the similarity graph has grown too big. Shutting down to avoid problems");
								}*/
							}
						}
					}
					listIndex++;
				}
			} else {
				System.out.println("ABORTED link descovery for "+type+" Too generic: excessive number of sources: "+numOfSourcesInSet);
			}
			execution.close();
			System.out.println("("+Math.round(100*(((double)typesAnalysed)/expectedNumOfTypes))+"%) ("+typesAnalysed+") Type: "+type+" #Entities: "+numOfSourcesInSet+" #NewLinks: "+newlinksFound+" Total size of Map: "+similarityGraph.size());
		}
		
		// order the sources:
		
		try {
			File writeFile = new File("OrderedPairs.txt");
			FileWriter writer = new FileWriter(writeFile, true);
			//
			for(int desc = bestScore; desc > 0; desc--){
				Integer topScore = new Integer(desc);
				if(similarityGraph.containsValue(topScore)){
					for(StringPair s : similarityGraph.keySet()){
						if(similarityGraph.get(s).equals(topScore)){
							System.out.println(desc+"}} "+s);
							writer.write(desc+"}} "+s+"\n");
							//similarityGraph.remove(s);
						}
					}
				}
			}
			//
					
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		// printe the sources:
		
		log(100,"INTEGRATION - Semantic interlinking Source-Based END");
	}
	
	
	private void doSemLinkingComputeSimilarity() throws IOException{
		// Go through the list of entities in alphabetical order
		BufferedReader br = new BufferedReader(new FileReader(intConfig.getPathToFileWithEntitiesInAlphabeticalOrder()));
		String line;
		int index = 0;
		Date startDate = new Date();
		while ((line = br.readLine()) != null) {
			index++;
			if(index >= 737769){
				Date now = new Date();
				if((index-737769) > 0) System.out.println(((((double)(now.getTime()-startDate.getTime()))/1000)/(index-737769))+"<"+(index-737769));				
				doSemLinkingAnalyseEntity(line.trim());
			}
			
			}
		br.close();
	}
	
	private void doSemLinkingAnalyseEntity(String entity) {
		String source  = entity.substring(39, entity.indexOf("&t="));
		System.out.println("### Entity: "+entity+ " ("+source+")");
		Set<String> relatedEntities = null;
		if(dbpediaEntitiesInSource.containsKey(source)){
			relatedEntities = dbpediaEntitiesInSource.get(source);
		} else {
			relatedEntities = new HashSet<String>();
			// Retrieve the list of DBpedia concepts
			QueryExecution execution = sparql.query(prefixes + " SELECT distinct ?type "+intConfig.getNamedGraphsQueryString()+" WHERE { "
					+ " <"+entity+"> ( prohow:has_step | prohow:requires | prohow:has_method | ^prohow:has_step | ^prohow:requires | ^prohow:has_method )+ ?ent . "
					+ " ?ent rdf:type ?type . } ");
			ResultSet results = execution.execSelect();
			while(results.hasNext()){
				QuerySolution result = results.next();
				String type = result.getResource("?type").getURI();
				if(type.indexOf("http://dbpedia.org") == 0) relatedEntities.add(type);
			}
			execution.close();
			dbpediaEntitiesInSource.put(source, relatedEntities);
		}
		for(String s : relatedEntities) {
			System.out.println(">"+s);
		}
	}

	
	/**
	 * This method preprocess all entities and stores them in the database dividing them by synsets
	 */
	private void preprocessAllEntities(){
		log(100,"INTEGRATION - Preprocessing of all entities START");
		QueryExecution execution = sparql.query("PREFIX oa: <http://www.w3.org/ns/oa#> SELECT ?uri ?label "+intConfig.getNamedGraphsQueryString()+" WHERE { "
				+ "?annotation oa:hasBody ?uri . "
				+ "?annotation oa:hasTarget ?target . "
				+ "?target oa:hasSelector ?selector . "
				+ "?selector oa:exact ?label . }");
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			preprocessedEntities++; System.out.print(".");
			QuerySolution result = results.next();
			String uri = result.getResource("?uri").getURI();
			String label = result.getLiteral("?label").getLexicalForm();
			for(String synset : textPreProcessing.preprocessText(label)){
				if(!intConfig.isBlockedSynset(deSanitiseSynset(synset))){
					addSynset(uri, synset);
				}
			}
			
			if(preprocessedEntities % 1000 == 0){
				log("\n["+preprocessedEntities+"]");
				databaseConnection.forceCommit();
			}
			
		}
		execution.close();
		databaseConnection.forceCommit();
		log(100,"INTEGRATION - Preprocessing of all entities FINISHED");
	}
	
	/**
	 * This method goes through all the synsets stored in the database and computes the similarity between all the entities that share at least one synset
	 */
	private boolean computeSimilarityAll(){
		log(100,"INTEGRATION - Compute similarity of all entities START");
		Date integrationStart = new Date();
		try {
				java.sql.ResultSet allTables = databaseConnection.getAllTables();
				List<String> tables = new LinkedList<String>();
				if(allTables == null) return false;
				while(allTables.next()){
					tables.add(allTables.getString("TABLE_NAME"));
				}
				for(String table : tables){
					if(table.indexOf("SID") == 0 && !intConfig.isBlockedSynset(deSanitiseSynset(table))){
						comparedSynsets++;
						List<String> uriSet = new LinkedList<String>();
						//log(1000,"INTEGRATION - - Synset "+table);
						java.sql.ResultSet allURIs = databaseConnection.getAllEntitiesInTable(table);
						while(allURIs.next()){
							uriSet.add(allURIs.getString("entity"));
							//log(10000,"INTEGRATION - - - Entity "+entity);
						}
						comparison.compareSet(uriSet);
						if(comparedSynsets <3 || comparedSynsets % 10 == 0){
							Date integrationNow = new Date();
							log(1000,"INTEGRATION - - "+comparedSynsets+" compared synsets in "+((integrationNow.getTime()-integrationStart.getTime())/1000)+" seconds.");
						}
					}
				}
				log(100,"INTEGRATION - Compute similarity of all entities FINISHED");
				return true;
		} catch (SQLException e){
			e.printStackTrace();
		}
		log(100,"INTEGRATION - Compute similarity of all entities FAILED");
		return false;	
	}
	
	private void utilityComputeSynsetsStatistics(){
		List<IntStringPair> statisticsNouns = new LinkedList<IntStringPair>();
		List<IntStringPair> statisticsVerbs = new LinkedList<IntStringPair>();
		int totVerbs = 0;
		int totVerbsCount = 0;
		int totNouns = 0;
		int totNounsCount = 0;
		log(100,"INTEGRATION - UTILITY SYNSET STATISTICS START");
		try {
				java.sql.ResultSet allTables = databaseConnection.getAllTables();
				List<String> tables = new LinkedList<String>();
				if(allTables == null) return;
				while(allTables.next()){
					tables.add(allTables.getString("TABLE_NAME"));
				}
				for(String table : tables){
					if(table.indexOf("SID") == 0){
						if(table.charAt(table.length()-1) == 'V'){
							int totalInThisTable = databaseConnection.countAllEntitiesInTable(table);
							totVerbs++;
							totVerbsCount = totVerbsCount + totalInThisTable;
							statisticsVerbs.add(new IntStringPair(totalInThisTable,deSanitiseSynset(table)));
						} else if(table.charAt(table.length()-1) == 'N'){
							int totalInThisTable = databaseConnection.countAllEntitiesInTable(table);
							totNouns++;
							totNounsCount = totNounsCount + totalInThisTable;
							statisticsNouns.add(new IntStringPair(totalInThisTable,deSanitiseSynset(table)));					
						} else {
							System.out.println("WARNING synset name malformed");
						}
						
						/*int entities = 0;
						java.sql.ResultSet allURIs = databaseConnection.getAllEntitiesInTable(table);
						while(allURIs.next()){
							entities++;
						}
						statistics.add(new IntStringPair(entities,table));*/
					}
				}
		} catch (SQLException e){
			e.printStackTrace();
		}
		Collections.sort(statisticsNouns);
		for(IntStringPair pair : statisticsNouns){
			System.out.println(pair.getString()+" -> "+pair.getInt()+" Meaning -> "+textPreProcessing.describeSynset(pair.getString()));
		}
		Collections.sort(statisticsVerbs);
		for(IntStringPair pair : statisticsVerbs){
			System.out.println(pair.getString()+" -> "+pair.getInt()+" Meaning -> "+textPreProcessing.describeSynset(pair.getString()));
		}
		log(100,"INTEGRATION - UTILITY NOUNS "+totNouns+" for a total number of hits: "+totNounsCount);
		log(100,"INTEGRATION - UTILITY VERBS "+totVerbs+" for a total number of hits: "+totVerbsCount);
		log(100,"INTEGRATION - UTILITY SYNSET STATISTICS FINISHED");
	}
	
	private void addSynset(String uri, String synset){
		databaseConnection.storeMap(synset, uri);
	}
	
	public Pair<Set<String>,Set<String>> getSuperEntities(String entitya, String entityb){
		return getSuperEntities(entitya, entityb, "prohow:has_step" , false);
	}
	
	public Pair<Set<String>,Set<String>> getRequiredEntities(String entitya, String entityb){
		return getSuperEntities(entitya, entityb, "prohow:requires" , true);
	}
	
	public Pair<Set<String>,Set<String>> getRequiredByEntities(String entitya, String entityb){
		return getSuperEntities(entitya, entityb, "prohow:requires" , false);
	}
	
	public Pair<Set<String>,Set<String>> getSubEntities(String entitya, String entityb){
		return getSuperEntities(entitya, entityb, "prohow:has_step" , true);
	}
	
	public String getLabel(String uri){
		String label = null;
		{
			QueryExecution execution = sparql.query(prefixes+" SELECT ?label "+intConfig.getNamedGraphsQueryString()+" WHERE { "
					+ "<"+uri+"> rdfs:label ?label . } LIMIT 1");
			ResultSet results = execution.execSelect();
			if(results.hasNext()){
				QuerySolution result = results.next();
				label = result.getLiteral("?label").getLexicalForm();
			}
			execution.close();
		}
		if(label != null) {
			return label;
		} else {
			{
				QueryExecution execution = sparql.query(prefixes+" SELECT ?label "+intConfig.getNamedGraphsQueryString()+" FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks>  WHERE { "
						+ "?super prohow:has_step | prohow:has_method <"+uri+"> . "
								+ " ?super rdfs:label ?label . } LIMIT 1");
				ResultSet results = execution.execSelect();
				if(results.hasNext()){
					QuerySolution result = results.next();
					label = result.getLiteral("?label").getLexicalForm();
				}
				execution.close();
			}
			
		}
		return label;
	}
	
	public String getLabelOutput(String uri){
		String label = null;
		{
			QueryExecution execution = sparql.query(prefixes+" SELECT ?label "+intConfig.getNamedGraphsQueryString()+" FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ " WHERE { "
					+ "<"+uri+"> prohow:has_method ?main . "
							+ " ?main rdfs:label ?label . } LIMIT 1");
			ResultSet results = execution.execSelect();
			if(results.hasNext()){
				QuerySolution result = results.next();
				label = result.getLiteral("?label").getLexicalForm();
			}
			execution.close();
		}
		return label;
	}
	
	private Pair<Set<String>,Set<String>> getSuperEntities(String entitya, String entityb, String relation, boolean direction){
		Set<String> supera = new HashSet<String>();
		String querybase = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> SELECT ?other "+intConfig.getNamedGraphsQueryString()+" WHERE { ";
		String querya = querybase;
		if (direction) querya += " <"+entitya+"> "+relation+" ?other }";
		else querya += " ?other "+relation+" <"+entitya+"> }";
		QueryExecution executiona = sparql.query(querya);
		ResultSet resultsa = executiona.execSelect();
		while(resultsa.hasNext()){
			QuerySolution result = resultsa.next();
			supera.add(result.getResource("?other").getURI());
			}
		executiona.close();
		if(supera.size() == 0) return null;
		Set<String> superb = new HashSet<String>();
		String queryb = querybase;
		if (direction) queryb += " <"+entityb+"> "+relation+" ?other }";
		else queryb += " ?other "+relation+" <"+entityb+"> }";
		QueryExecution executionb = sparql.query(queryb);
		ResultSet resultsb = executionb.execSelect();
		while(resultsb.hasNext()){
			QuerySolution result = resultsb.next();
			superb.add(result.getResource("?other").getURI());
			}
		executionb.close();
		if(superb.size() == 0) return null;
		return new Pair<Set<String>,Set<String>>(supera,superb);
	}
	
	private String testSPARQLService(){
		QueryExecution execution = sparql.query("SELECT (COUNT(*) AS ?no) "+intConfig.getNamedGraphsQueryString()+" { ?s ?p ?o  }");//("SELECT ?s WHERE { ?s ?p ?o . } LIMIT 1");
		String resultString = null;
		try{
			ResultSet results = execution.execSelect();
			if(results.hasNext()) {
				QuerySolution result = results.next();
				resultString = result.getLiteral("?no").getLexicalForm();
			}
			execution.close();
			return resultString;
		} catch(Exception e){
			execution.close();
			return null;
		} 
	}
	
	public boolean testDB(){
		databaseConnection.storeMap("test1", "test2");
		java.sql.ResultSet result = databaseConnection.getAllEntitiesInTable("test1");
		Set<String> resultsSet = new HashSet<String>();
			try {
				while(result.next()){
				resultsSet.add(result.getString("entity"));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//log(10000,"INTEGRATION - - - Entity "+entity);
			databaseConnection.dropTable("test1");
			databaseConnection.forceCommit();
		if(resultsSet.size() == 1 && resultsSet.contains("test2")) return true;
		else return false;
	}

	public void log(String message){
		log(9999,message);
	}
	public void log(int verbosity, String message){
		if(intConfig.isTimestamp()) {
			message = new Date()+" > "+ message;
		}
		if(verbosity <= intConfig.getLogVerbosity()) System.out.println(message);
	}
	public String deSanitiseSynset(String sysnset){
		return sysnset.replaceAll("AxA", "-");
	}
	
	// Clean Snapguide ttl files removing the lines about processes with less than 3 steps, and those which have less than x views and y likes
	private void utilityCleanSnapGuide(String snapguideDestinationFolder,int viewsLimit, int intlikesLimit){
		Set<String> sourcesToBlock = new HashSet<String>();
		QueryExecution execution = sparql.query(
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
				+ " PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#>  "
				+ " PREFIX madph: <http://vocab.inf.ed.ac.uk/madprohow#> "
				+ " PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ " PREFIX oa: <http://www.w3.org/ns/oa#> "
				+ " PREFIX dctypes: <http://purl.org/dc/dcmitype/> "
				+ " PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
				+ " PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
				+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>  "
				+ " SELECT DISTINCT ?main "
				+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide> "
				+ " WHERE {"
				+ " ?main prohow:requires ?entityuri . "
				+ " FILTER regex(str(?main), \"mainentity$\", \"i\" )   "
				+ " FILTER NOT EXISTS {"
				+ "  ?main prohow:has_step ?step1 ."
				+ "  ?main prohow:has_step ?step2 ."
				+ "  FILTER(str(?step1)!=str(?step2))"
				+ "  ?main prohow:has_step ?step3 ."
				+ "  FILTER(str(?step1)!=str(?step3))"
				+ "  FILTER(str(?step2)!=str(?step3))"
				+ "  ?main madph:has_views ?views ."
				+ "  ?main madph:has_likes ?likes ."
				+ "  FILTER (xsd:integer(?views ) > "+viewsLimit+")"
				+ "  FILTER (xsd:integer(?likes ) > "+intlikesLimit+")"
				+ "  } "
				+ " }");
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			QuerySolution result = results.next();
			sourcesToBlock.add(extractSourceFromURI(result.getResource("?main").getURI()));
		}
		execution.close();
		System.out.println(sourcesToBlock.size() + " sources to block ");
		// CHANGE THE FILES
		File dir = new File(snapguideDestinationFolder);
		if (!dir.isDirectory()) {
			throw new RuntimeException("The specified path to the data directory ("+dir+") is not a directory.");
		}
		for (File child : dir.listFiles()) {
			removeLineFromFile(child,sourcesToBlock);
		  }
	}
	
	private void removeLineFromFile(File inFile, Set<String> lineToRemove) {
		System.out.println("Removing blocked lines from "+inFile);
	    try {
	      if (!inFile.isFile()) {
	        System.out.println("Parameter is not an existing file");
	        return;
	      }

	      //Construct the new file that will later be renamed to the original filename.
	      File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

	      BufferedReader br = new BufferedReader(new FileReader(inFile));
	      PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

	      String line = null;
	      int lineRemoved = 0;
	      //Read from the original file and write to the new
	      //unless content matches data to be removed.
	      boolean previousDeleted = false;
	      while ((line = br.readLine()) != null) {
	    	  String sourceInLine = "";
	    	  if(line.indexOf("<http://vocab.inf.ed.ac.uk/procont") == 0){
	    		  sourceInLine = extractSourceFromURI(line);
	    	  } else if(previousDeleted) {
	    		  lineRemoved++;
	    		  continue;
	    	  }
	    	  if(lineToRemove.contains(sourceInLine)){
	    		  lineRemoved++;
	    		  previousDeleted = true;
	    	  } else {
	    		  previousDeleted = false;
	    		  pw.println(line);
	    		  pw.flush();
	    	  }
	      }
	      pw.close();
	      br.close();

	      //Delete the original file
	      if (!inFile.delete()) {
	        System.out.println("Could not delete file");
	        return;
	      }

	      //Rename the new file to the filename the original file had.
	      if (!tempFile.renameTo(inFile))
	        System.out.println("Could not rename file");

	      System.out.println("Lines removed: "+lineRemoved);
	    }
	    catch (FileNotFoundException ex) {
	      ex.printStackTrace();
	    }
	    catch (IOException ex) {
	      ex.printStackTrace();
	    }
	  }
	
	private String extractSourceFromURI(String URI){
		if(URI.length() < 10) return "";
		if(URI.indexOf("#?url=")+6 < 0 || URI.indexOf("#?url=")+6 > URI.length()+1) 
			return "";
		if(URI.indexOf("&t=") < 0 || URI.indexOf("&t=") > URI.length()+1) 
			return "";
		if(URI.indexOf("#?url=")+6>URI.indexOf("&t="))
			return "";
		return URI.substring(URI.indexOf("#?url=")+6, URI.indexOf("&t="));//
	}
	
}