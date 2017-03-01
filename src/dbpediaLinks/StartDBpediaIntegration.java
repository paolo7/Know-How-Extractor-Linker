package dbpediaLinks;

import integrationcore.IntegrationConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlp.ParsedSentence;
import nlp.TextProcessingConfig;
import nlp.TextProcessingController;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import edu.stanford.nlp.process.Morphology;
import query.SparqlManager;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import wordnet.WordnetAPI;
import wordnet.WordnetAPIMIT;
import wordnet.WordnetConfiguration;

public class StartDBpediaIntegration {

	private static SparqlManager sparqlSource;
	//private static SparqlManager sparqlTarget;
	private static double inspectThreashold = 0.8;
	private static double acceptThreasholdRequirement = 0.85;
	private static double acceptThreasholdMainEntityCreation = 0.8;
	private static double acceptThreasholdMiddleEntityCreation = 0.8;
	private static double acceptThreasholdStepEntityCreation = 0.8;
	private static int analysedLabels = 0;
	private static int analysedRequirementLabels = 0;
	private static int analysedMainLabels = 0;
	private static int analysedSteps = 0;
	private static int stepOutputsFound = 0;
	private static int stepInputsFound = 0;
	private static int stepOutputsFoundWithEntity = 0;
	private static int stepInputsFoundWithEntity = 0;
	private static int matchesFoundRequirements = 0;
	private static int matchesFoundMain = 0;
	private static int matchesFound = 0;
	private static int matchesFound100 = 0;
	private static int matchesFound90 = 0;
	private static int matchesFound80 = 0;
	private static int matchesFound70 = 0;
	private static int matchesFound60 = 0;
	private static int newUriCount = 0;
	private static int totNumberOfCreationVerbs = 0;
	private static String resultRdfTypeLemmaExact = "Resources\\Results\\DBpediaLinks\\ResultsRequirementTypeLemmaExact.ttl";
	private static String resultMainOutputLemmaExact = "Resources\\Results\\DBpediaLinks\\ResultsOutputTypeLemmaExact.ttl";
	private static String resultMiddleOutputLemmaExact = "Resources\\Results\\DBpediaLinks\\ResultsMiddleOutputTypeLemmaExact.ttl";
	private static String resultRdfType = "Resources\\Results\\DBpediaLinks\\ResultsRequirementType.ttl";
	private static String resultMainOutput = "Resources\\Results\\DBpediaLinks\\ResultsOutputType.ttl";
	private static String resultMiddleOutput = "Resources\\Results\\DBpediaLinks\\ResultsMiddleOutputType.ttl";
	private static String resultRdfTypeWarning = "Resources\\Results\\DBpediaLinks\\ResultsRequirementTypeWarning.ttl";
	private static String resultMainOutputWarning = "Resources\\Results\\DBpediaLinks\\ResultsOutputTypeWarning.ttl";
	private static String resultMiddleOutputWarning = "Resources\\Results\\DBpediaLinks\\ResultsMiddleOutputTypeWarning.ttl";
	private static String resultStepWarning = "Resources\\Results\\DBpediaLinks\\ResultsStepsWarning.ttl";
	private static String resultRdfTypeWarningBad = "Resources\\Results\\DBpediaLinks\\ResultsRequirementTypeWarningBad.ttl";
	private static String resultMainOutputWarningBad = "Resources\\Results\\DBpediaLinks\\ResultsOutputTypeWarningBad.ttl";
	private static String resultMiddleOutputWarningBad = "Resources\\Results\\DBpediaLinks\\ResultsMiddleOutputTypeWarningBad.ttl";
	private static String resultStepWarningBad = "Resources\\Results\\DBpediaLinks\\ResultsStepsWarningBad.ttl";
	private static String resultRdfTypeTrust = "Resources\\Results\\DBpediaLinks\\ResultsRequirementTypeTrust.ttl";
	private static String resultMainOutputTrust = "Resources\\Results\\DBpediaLinks\\ResultsOutputTypeTrust.ttl";
	private static String resultMiddleOutputTrust = "Resources\\Results\\DBpediaLinks\\ResultsMiddleOutputTypeTrust.ttl";
	private static String resultStepTrust = "Resources\\Results\\DBpediaLinks\\ResultsStepsTrust.ttl";
	private static String resultStep = "Resources\\Results\\DBpediaLinks\\ResultsSteps.ttl";
	private static String verbLabels = "Resources\\Results\\DBpediaLinks\\ResultsVerbsLabels.ttl";
	private static String verbEntities = "Resources\\Results\\DBpediaLinks\\ResultsVerbsEntities.ttl";
	private static String lastWriterUsed;
	private static SimpleLogger logger;
	private static Date startTime;
	private static TextProcessingController textPreProcessing;
	private static String prefixes = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
			+ "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> "
			+ "PREFIX madph: <http://vocab.inf.ed.ac.uk/madprohow#> "
			+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
			+ "PREFIX oa: <http://www.w3.org/ns/oa#> "
			+ "PREFIX dctypes: <http://purl.org/dc/dcmitype/> "
			+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
			+ "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";
	private static Set<String> outputVerbSynsets;
	private static Set<String> outputVerbWords;
	private static Set<String> verbsAlreadySaved = new HashSet<String>();
	private static Set<String> unitsOfMeasure = new HashSet<String>();
	private static WordnetAPI wordnetAPI;
	
	private static boolean doProcessRequirements = true;
	private static boolean doProcessMainEntities = true;
	private static boolean doProcessSteps = true;
	private static IntegrationConfiguration intConfig = null;
	private static SparqlManager dbpedia = new SparqlManager();
	private static Set<String> blockedTypes = new HashSet<String>();
	private static Set<String> allowedTypes = new HashSet<String>();
	private static Morphology morpha = new Morphology();
	private static Set<String> sourcesToBlock = new HashSet<String>();
	private static Set<String> units = new HashSet<String>();
	public static void main(String[] args) throws IOException, InterruptedException {
		logger = new SimpleLogger();
		String[] unitsArray = new String[]{"tablespoon","group","splash","teaspoon","amount","cup","punnet","jar","bucket","m","bar","bag","variety","gallon","copy","yard","pair",
				"ft","grade","bunch","sheet","box","feet","foot","source","slice","pinch","lot","copies","number","can","glass", "ml","bottle","kind","piece",
				"tsp","g","capful","tbsp","ball","packet","inch","inches","oz","tbsp","quart","lbs","ounce","drop"};
		for(String s : unitsArray){
			units.add(s);
		}
		
		intConfig = new IntegrationConfiguration();
		dbpedia.setRemoteEndpoint("http://localhost:8890/sparql");
		
		
		sparqlSource = new SparqlManager(logger);
		sparqlSource.setRemoteEndpoint("http://localhost:8890/sparql");
		
		//sparqlSource.setLocalDirectory("D:\\DataDirectory\\Subset\\wikihow");
		//sparqlTarget = new SparqlManager(logger);
		//sparqlTarget.setRemoteEndpoint("http://dbpedia.org/sparql");
		
		WordnetConfiguration wordnetConfiguration = new WordnetConfiguration();
		wordnetConfiguration.setWordnetFilePath("Resources/WordNet-3.0/");
		wordnetAPI = new WordnetAPIMIT(wordnetConfiguration);
		
		TextProcessingConfig textPreProcessingConfiguration = new TextProcessingConfig();
		textPreProcessingConfiguration.setLogVerbosity(100000);
		textPreProcessing = new TextProcessingController(textPreProcessingConfiguration, wordnetAPI);

		
		//boolean doWikiHowMultiLevelDecompositionCheck = true;
		
		startTime = new Date();
		//utilityPrintHowManyDifferentURLareConnectedToDBpedia();
		//if(true) return;
		//utilityPrintHowManyDifferentURLareInterconnected();
		//utilityPrintHowManyDifferentURLareInterconnectedKNOWHOW();
		//utilityPrintHowManyDifferentURLareInterconnectedALL();
		//utilityPrintHowManyDifferentURLareInterconnectedUserGenerated();
		
		SparqlManager sparql = new SparqlManager();
		sparql.setRemoteEndpoint("http://localhost:8890/sparql");
		
		 {
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
							+ "  ?main prohow:has_step ?step4 ."
							+ "  FILTER(str(?step1)!=str(?step4))"
							+ "  FILTER(str(?step2)!=str(?step4))"
							+ "  FILTER(str(?step3)!=str(?step4))"
							//+ "  ?main madph:has_views ?views ."
							//+ "  ?main madph:has_likes ?likes ."
							//+ "  FILTER (xsd:integer(?views ) > "+viewsLimit+")"
							//+ "  FILTER (xsd:integer(?likes ) > "+likesLimit+")"
							+ "  } "
							+ " }");
					ResultSet results = execution.execSelect();
					while(results.hasNext()){
						QuerySolution result = results.next();
						sourcesToBlock.add(extractSourceFromURI(result.getResource("?main").getURI()));
					}
					execution.close();
			 }
		
		/*intConfig.setNamedGraphs(new String[]{
				"http://vocab.inf.ed.ac.uk/graph#wikihow6",
				//"http://vocab.inf.ed.ac.uk/graph#wikihow5",
				//"http://vocab.inf.ed.ac.uk/graph#wikihow4",
				//"http://vocab.inf.ed.ac.uk/graph#wikihow3",
				//"http://vocab.inf.ed.ac.uk/graph#wikihow2",
				//"http://vocab.inf.ed.ac.uk/graph#wikihow1",
				//"http://vocab.inf.ed.ac.uk/graph#snapguide",
				//"http://vocab.inf.ed.ac.uk/graph#wikihowontology"
				});*/
		/*processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow7"},false,false,true);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow6"},false,false,true);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow5"},false,false,true);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow4"},false,false,true);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow3"},false,false,true);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow2"},false,false,true);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow1"},false,false,true);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#snapguide"},false,false,true);*/
		
		 processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow7"},false,true,false);
		 processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow6"},false,true,false);
		 processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow5"},false,true,false);
		 processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow4"},false,true,false);
		 processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow3"},false,true,false);
		 processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow2"},false,true,false);
		 processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow1"},false,true,false);
		 processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#snapguide"},false,true,false);
		
		/*processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow7"},true,false,false);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow6"},true,false,false);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow5"},true,false,false);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow4"},true,false,false);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow3"},true,false,false);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow2"},true,false,false);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#wikihow1"},true,false,false);
		processNamedGraph(new String[]{"http://vocab.inf.ed.ac.uk/graph#snapguide"},true,false,false);*/
		
		
		//analysedLabels
		logger.log("Number of labels analised: "+analysedLabels+" (of which requirements: "+analysedRequirementLabels+" main entities: "+analysedMainLabels+")");
		logger.log("Number of matches found: "+matchesFound+" From requirements: "+matchesFoundRequirements+" As output of main entities: "+matchesFoundMain);
		logger.log("Number of steps analysed: "+analysedSteps+" Outputs found: "+stepOutputsFound+" Inputs found "+stepInputsFound);
		logger.log("100% => "+matchesFound100);
		logger.log("90% => "+matchesFound90);
		logger.log("80% => "+matchesFound80);
		logger.log("70% => "+matchesFound70);
		logger.log("60% => "+matchesFound60);
		logger.log("50% => "+matchesFound);
		Date endTime = new Date();
		int durationInSeconds = Math.round((endTime.getTime()-startTime.getTime())/1000);
		//int durationRequirements = Math.round((afterRequirementsTime.getTime()-startTime.getTime())/1000);
		//int durationMain = Math.round((endTime.getTime()-afterRequirementsTime.getTime())/1000);
		logger.log("Duration "+durationInSeconds);//+" seconds (Duration for requirements: "+durationRequirements+" s Duration for output of main entities: "+durationMain+" s)");
		logger.log("Seconds per entity: "+((double)durationInSeconds)/analysedLabels);
		logger.log("Seconds per link: "+((double)durationInSeconds)/matchesFound);
		logger.log("Finding Links Finished");
	}
	
	public static String extractSourceFromURI(String URI){
		return URI.substring(URI.indexOf("#?url=")+6, URI.lastIndexOf("&t="));//
	}
	
	public static void processNamedGraph(String[] namedGraphs) throws IOException{
		processNamedGraph(namedGraphs, doProcessRequirements, doProcessMainEntities, doProcessSteps);
	}
	
	public static void processNamedGraph(String[] namedGraphs, boolean doProcessRequirements, boolean doProcessMainEntities, boolean doProcessSteps) throws IOException{
		logger.log("INSPECTING THE FOLLOWING NAMED GRAPHS "+namedGraphs);
		for(String s : namedGraphs)logger.log(">> "+s);
		intConfig.setNamedGraphs(namedGraphs);
		if(doProcessRequirements){
			logger.log("LOOKING FOR LINKS ABOUT REQUIREMENTS - START");
			Map<String,String> sparqlresults = new HashMap<String,String>();
			{
				String queryRequirements = prefixes+"SELECT DISTINCT ?entityuri ?exact  "+intConfig.getNamedGraphsQueryString()
						+ "  WHERE { "
						+ " ?main prohow:requires ?entityuri ."
						+ " FILTER regex(str(?main), \"mainentity$\", \"i\" ) "
						+ " ?annotation oa:hasBody ?entityuri . "
						+ "  ?annotation oa:hasTarget ?target . "
						+ "	?target oa:hasSelector ?selector . "
						+ "	?selector oa:exact ?exact . "
						+ " }";
				//logger.log(query);
				QueryExecution exec = sparqlSource.query(queryRequirements);
				ResultSet results = exec.execSelect();
				while(results.hasNext()){
					analysedLabels++;
					analysedRequirementLabels++;
					QuerySolution result = results.next();
					sparqlresults.put(result.getResource("?entityuri").getURI(), toLowerCaseNoAcronyms(result.getLiteral("?exact").getLexicalForm()).trim());
				}
				exec.close();
			}
			{
				String queryRequirements = prefixes+"SELECT DISTINCT ?entityuri ?exact  "+intConfig.getNamedGraphsQueryString()
						+ "  WHERE { "
						+ " ?main prohow:requires ?superentity ."
						+ " ?superentity prohow:has_step ?entityuri ."
						+ " FILTER regex(str(?main), \"mainentity$\", \"i\" ) "
						+ " ?annotation oa:hasBody ?entityuri . "
						+ "  ?annotation oa:hasTarget ?target . "
						+ "	?target oa:hasSelector ?selector . "
						+ "	?selector oa:exact ?exact . "
						+ " }";
				//logger.log(query);
				QueryExecution exec = sparqlSource.query(queryRequirements);
				ResultSet results = exec.execSelect();
				while(results.hasNext()){
					analysedLabels++;
					analysedRequirementLabels++;
					QuerySolution result = results.next();
					sparqlresults.put(result.getResource("?entityuri").getURI(), toLowerCaseNoAcronyms(result.getLiteral("?exact").getLexicalForm()).trim());
				}
				exec.close();
			}
			{
				String queryRequirements = prefixes+"SELECT DISTINCT ?entityuri ?exact  "+intConfig.getNamedGraphsQueryString()
						+ "  WHERE { "
						+ " ?main prohow:requires ?superentity ."
						+ " ?superentity prohow:has_method ?entityuri ."
						+ " FILTER regex(str(?main), \"mainentity$\", \"i\" ) "
						+ " ?annotation oa:hasBody ?entityuri . "
						+ "  ?annotation oa:hasTarget ?target . "
						+ "	?target oa:hasSelector ?selector . "
						+ "	?selector oa:exact ?exact . "
						+ " }";
				//logger.log(query);
				QueryExecution exec = sparqlSource.query(queryRequirements);
				ResultSet results = exec.execSelect();
				while(results.hasNext()){
					analysedLabels++;
					analysedRequirementLabels++;
					QuerySolution result = results.next();
					sparqlresults.put(result.getResource("?entityuri").getURI(), toLowerCaseNoAcronyms(result.getLiteral("?exact").getLexicalForm()).trim());
				}
				exec.close();
			}
			Iterator<String> it = sparqlresults.keySet().iterator();
			while(it.hasNext()){
				String entityuri = it.next();
				if(sourcesToBlock.contains(extractSourceFromURI(entityuri))) continue;
				String label = sparqlresults.get(entityuri);
				if(!findMatch(label, entityuri, "requirement")){
					// remove everything in brackets, assuming it is not necessary
					label = label.replaceAll("\\{.*\\}", "");
					label = label.replaceAll("\\[.*\\]", "");
					label = label.replaceAll("\\(.*\\)", "");
					if(!findMatch(label, entityuri, "requirement")){
						String[] sublabels = label.split(" and |&|, ");
						if(sublabels.length > 1) {
							for(int i = 0; i<sublabels.length; i++){
								if(findMatch(sublabels[i].trim(), entityuri+"and"+i, "requirement")){
									saveTriple(encapsulateURI(entityuri),"prohow:has_step",(encapsulateURI(entityuri+"and"+i)),lastWriterUsed);
									//System.out.println(label+" AND "+sublabels[i]);
								} //else System.out.println(" . . . . "+label+" AND "+sublabels[i]);
							}
						}
						String[] subsublabels = label.replaceAll("[0-9]\\/[0-9]", "1").replaceAll("\\/", " or ").split(" or ");
						if(subsublabels.length > 1) {
							for(int ii = 0; ii<subsublabels.length; ii++){
								if(findMatch(subsublabels[ii].trim(), entityuri+"or"+ii, "requirement")){
									saveTriple(encapsulateURI(entityuri),"prohow:has_method",(encapsulateURI(entityuri+"or"+ii)),lastWriterUsed);
									//System.out.println(label+" OR "+subsublabels[ii]);
								} //else System.out.println(" . . . . "+label+" OR "+subsublabels[ii]);
							}
						}
					}
				}
				
				
			}
			logger.log("LOOKING FOR LINKS ABOUT REQUIREMENTS - END");
		}
		Date afterRequirementsTime = new Date();
		String[] createWords = new String[]{"make","build","create","assemble","cook","prepare","do","bake","brew","install","find","calculate","compute","setup","construct","generate","produce","become","get","organize","perform","catch","capture","buy","acquire","purchase","obtain","grow","harvest"};
		outputVerbSynsets = new HashSet<String>();
		outputVerbWords = new HashSet<String>();
		for(String s : createWords){
			outputVerbWords.add(s);
			//outputVerbSynsets.addAll(wordnetAPI.lookupSynset(s, "verb"));
		}
		if(doProcessMainEntities){
			int numberOfCreationVerbs = 0;
			logger.log("LOOKING FOR LINKS ABOUT MAIN ENTITY - START");
			String queryMainEntity = prefixes+"SELECT DISTINCT ?main ?exact  "+intConfig.getNamedGraphsQueryString()
					+ "  WHERE { "
					+ " ?annotation oa:hasBody ?main . "
					+ " FILTER regex(str(?main), \"mainentity$\", \"i\" ) "
					+ "  ?annotation oa:hasTarget ?target . "
					+ "	?target oa:hasSelector ?selector . "
					+ "	?selector oa:exact ?exact . "
					+ "}";
			QueryExecution execMain = sparqlSource.query(queryMainEntity);
			ResultSet resultsMain = execMain.execSelect();
			Map<String,String> sparqlresults = new HashMap<String,String>();
			while(resultsMain.hasNext()){
				analysedLabels++;
				analysedMainLabels++;
				QuerySolution result = resultsMain.next();
				sparqlresults.put(result.getResource("?main").getURI(), toLowerCaseNoAcronyms(result.getLiteral("?exact").getLexicalForm()).trim());
				}
			execMain.close();
			Iterator<String> i = sparqlresults.keySet().iterator();
			while(i.hasNext()){
				String entityuri = i.next();
				if(sourcesToBlock.contains(extractSourceFromURI(entityuri))) continue;
				String label = sparqlresults.get(entityuri);
				String normalisedLabel = label.trim();
				
				/*String normalisedLabelLowercase = normalisedLabel.toLowerCase();
				if(normalisedLabelLowercase.indexOf("ways to ") < 5) {
					normalisedLabel = normalisedLabel.substring(normalisedLabelLowercase.indexOf("ways to ")+7).trim();
				}*/
				
				normalisedLabel = toLowerCaseNoAcronyms(normalisedLabel);
				if(normalisedLabel.indexOf("how to ") == 0) {
					normalisedLabel = normalisedLabel.substring(6).trim();
				}
				String firstVerb = textPreProcessing.extractFirstVerb(normalisedLabel);
				if(firstVerb != null){
					if(isCreationVerb(firstVerb, normalisedLabel)){
						totNumberOfCreationVerbs++;
						numberOfCreationVerbs++;
						//System.out.println(firstVerb+" <> "+label);
						normalisedLabel = normalisedLabel.substring(normalisedLabel.indexOf(firstVerb)+firstVerb.length()).trim();
						findMatch(normalisedLabel, entityuri, "mainEntity", firstVerb);
					} else {
						if(normalisedLabel.indexOf("set up ") == 0){
							numberOfCreationVerbs++;
							totNumberOfCreationVerbs++;
							firstVerb = "set up";
							normalisedLabel = normalisedLabel.substring(7);
							findMatch(normalisedLabel, entityuri, "mainEntity", firstVerb);
						}
					}
				}
			}
			logger.log("LOOKING FOR LINKS ABOUT MAIN ENTITY - Number of creation entities found in this graph: "+numberOfCreationVerbs);
			logger.log("LOOKING FOR LINKS ABOUT MAIN ENTITY - Total number of creation entities: "+totNumberOfCreationVerbs);
			logger.log("LOOKING FOR LINKS ABOUT MAIN ENTITY - END");
			
		}
		
		if(doProcessSteps){
			logger.log("LOOKING FOR LINKS ABOUT MIDDLE STEPS ENTITIES - START");
			String queryMiddleEntity = prefixes+"SELECT DISTINCT ?uri ?exact  "+intConfig.getNamedGraphsQueryString()
					+ "  WHERE { "
					+ " ?main prohow:has_step | prohow:has_method ?uri . "
					+ " FILTER regex(str(?main), \"mainentity$\", \"i\" ) "
					+ " FILTER EXISTS {?uri prohow:has_step ?uri2 . } "
					+ " ?annotation oa:hasBody ?uri . "
					+ " ?annotation oa:hasTarget ?target . "
					+ " ?target oa:hasSelector ?selector . "
					+ " ?selector oa:exact ?exact . "
					+ "}";
			QueryExecution execMiddle = sparqlSource.query(queryMiddleEntity);
			ResultSet resultsMiddle = execMiddle.execSelect();
			Map<String,String> sparqlresults = new HashMap<String,String>();
			while(resultsMiddle.hasNext()){
				analysedLabels++;
				analysedMainLabels++;
				QuerySolution result = resultsMiddle.next();
				sparqlresults.put(result.getResource("?uri").getURI(), toLowerCaseNoAcronyms(result.getLiteral("?exact").getLexicalForm()).trim());
				}
			execMiddle.close();
			Iterator<String> i = sparqlresults.keySet().iterator();
			while(i.hasNext()){
				String entityuri = i.next();
				if(sourcesToBlock.contains(extractSourceFromURI(entityuri))) continue;
				String label = sparqlresults.get(entityuri);
				String normalisedLabel = label.trim();
				
				/*String normalisedLabelLowercase = normalisedLabel.toLowerCase();
				if(normalisedLabelLowercase.indexOf("ways to ") < 5) {
					normalisedLabel = normalisedLabel.substring(normalisedLabelLowercase.indexOf("ways to ")+7).trim();
				}*/
				
				normalisedLabel = toLowerCaseNoAcronyms(normalisedLabel);
				if(normalisedLabel.indexOf("how to ") == 0) {
					normalisedLabel = normalisedLabel.substring(6).trim();
				}
				String firstVerb = textPreProcessing.extractFirstVerb(normalisedLabel);
				if(firstVerb != null){
					if(isCreationVerb(firstVerb, normalisedLabel)){
						//System.out.println(firstVerb+" <> "+label);
						normalisedLabel = normalisedLabel.substring(normalisedLabel.indexOf(firstVerb)+firstVerb.length()).trim();
						findMatch(normalisedLabel, entityuri, "middleEntity", firstVerb);
					}
				}
			}
			logger.log("LOOKING FOR LINKS ABOUT MAIN ENTITY - END");
		}
		
		if(false){
			logger.log("LOOKING FOR LINKS ABOUT STEP ENTITIES - START");
			String querySteps = prefixes+"SELECT DISTINCT ?entity ?exact  "+intConfig.getNamedGraphsQueryString()
					+ "  WHERE { "
					+ " { "
					+ " ?super prohow:has_step ?entity ."
					+ " ?annotation oa:hasBody ?entity . "
					+ " ?annotation oa:hasTarget ?target . "
					+ "	?target oa:hasSelector ?selector . "
					+ "	?selector oa:exact ?exact . "
					+ " } UNION { "
					+ " ?super prohow:has_method ?entity ."
					+ " ?annotation oa:hasBody ?entity . "
					+ " ?annotation oa:hasTarget ?target . "
					+ "	?target oa:hasSelector ?selector . "
					+ "	?selector oa:exact ?exact . "
					+ " } "
					+ " } ";
			QueryExecution execSteps = sparqlSource.query(querySteps);
			ResultSet resultsSteps = execSteps.execSelect();
			
			Map<String,String> sparqlresults = new HashMap<String,String>();
			while(resultsSteps.hasNext()){
				analysedLabels++;
				analysedSteps++;
				QuerySolution result = resultsSteps.next();
				sparqlresults.put(result.getResource("?entity").getURI(), toLowerCaseNoAcronyms(result.getLiteral("?exact").getLexicalForm()).trim());
				}
			execSteps.close();
			Iterator<String> i = sparqlresults.keySet().iterator();
			while(i.hasNext()){
				String entityuri = i.next();
				String label = sparqlresults.get(entityuri);
				String normalisedLabel = toLowerCaseNoAcronyms(label).replaceAll("make sure you", "").trim();
				normalisedLabel.replaceAll("make sure", "");
				/*if(label.indexOf("make sure you") > 0 && label.indexOf("make sure you") < 5) {
					label = label.substring(label.indexOf("make sure you")+13).trim();
				}
				if(label.indexOf("make sure") > 0 && label.indexOf("make sure you") < 5) {
					label = label.substring(label.indexOf("make sure you")+9).trim();
				}*/
				boolean oneVerbPhraseFound = false;
				List<ParsedSentence> parsedSentences = textPreProcessing.parseStepSentencesTree(normalisedLabel, 100);
				String prevEntity = null;
				for(ParsedSentence sentence : parsedSentences){
					if(sentence.getVerb() != null){
						String verb = toLowerCaseNoAcronyms(sentence.getVerb().trim());
						String object = null;
						if(sentence.getObject() != null) object = toLowerCaseNoAcronyms(sentence.getObject().trim());
						String newSubStep = entityuri;
						
						if(parsedSentences.size() != 1){
							newSubStep = generateNewUri(entityuri,"substep");
							saveTriple(encapsulateURI(entityuri),"prohow:has_step",encapsulateURI(newSubStep),resultStep);
							if(prevEntity != null) saveTriple(encapsulateURI(newSubStep),"prohow:requires",encapsulateURI(prevEntity),resultStep);
							prevEntity = newSubStep;
						}
						if( isCreationVerb(verb, label)){
							if(findMatch(object, newSubStep, "stepOutput",verb)){
								//System.out.println("Tree OUTPUT : ("+verb+") -> ("+object+") "+label);
								stepOutputsFound++;
								oneVerbPhraseFound = true;
							}
						} else {
							if(findMatch(object, newSubStep, "stepInput",verb)){
								//System.out.println("Tree req. : ("+verb+") -> ("+object+") "+label);
								stepInputsFound++;
								oneVerbPhraseFound = true;
							}
						}
							
					}
				}
				
				
				/*if(!oneVerbPhraseFound){
					parsedSentences = textPreProcessing.parseStepSentences(label, 1);
					for(ParsedSentence sentence : parsedSentences){
						if(sentence.getSubject() == null && sentence.getVerb() != null && sentence.getObject() != null){
							String verb = sentence.getVerb().trim().toLowerCase();
							String object = sentence.getObject().trim().toLowerCase();
							if( isCreationVerb(verb)){
								if(findMatch(object, entityuri, "stepOutput",verb)){
									//System.out.println("TK OUTPUT : ("+verb+") -> ("+object+") "+label);
									stepOutputsFound++;
									oneVerbPhraseFound = true;
								}
							} else {
								if(findMatch(object, entityuri, "stepInput",verb)){
									//System.out.println("TK req. : ("+verb+") -> ("+object+") "+label);
									stepInputsFound++;
									oneVerbPhraseFound = true;
								}
							}
								
						}	
					}
				}*/
			}
			logger.log("LOOKING FOR LINKS ABOUT STEP ENTITIES - END");
			}
		logger.log("TEMP-STATISTICS");
		logger.log("Number of labels analised: "+analysedLabels+" (of which requirements: "+analysedRequirementLabels+" main entities: "+analysedMainLabels+")");
		logger.log("Number of matches found: "+matchesFound+" From requirements: "+matchesFoundRequirements+" As output of main entities: "+matchesFoundMain);
		logger.log("Number of steps analysed: "+analysedSteps+" Outputs found: "+stepOutputsFound+" Inputs found "+stepInputsFound);
		logger.log("100% => "+matchesFound100);
		logger.log("90% => "+matchesFound90);
		logger.log("80% => "+matchesFound80);
		Date endTime = new Date();
		int durationInSeconds = Math.round((endTime.getTime()-startTime.getTime())/1000);
		//int durationRequirements = Math.round((afterRequirementsTime.getTime()-startTime.getTime())/1000);
		//int durationMain = Math.round((endTime.getTime()-afterRequirementsTime.getTime())/1000);
		logger.log("Duration so far"+durationInSeconds);//+" seconds (Duration for requirements: "+durationRequirements+" s Duration for output of main entities: "+durationMain+" s)");
		logger.log("Seconds per entity: "+((double)durationInSeconds)/analysedLabels);
		logger.log("Seconds per link: "+((double)durationInSeconds)/matchesFound);
	}
	
	public static boolean isCreationVerb(String verb, String label){
		return ( (outputVerbWords.contains(verb) && (label.indexOf("get in") == -1 
				&& label.indexOf("get out") == -1  && label.indexOf("get off") == -1 
				&& label.indexOf("get along") == -1  && label.indexOf("get off") == -1 
				&& label.indexOf("get at") == -1  && label.indexOf("get down") == -1 
				&& label.indexOf("get up") == -1  && label.indexOf("find out") == -1
				&& label.indexOf("catch up") == -1
				)
				/*|| wordnetAPI.lookupSynset(verb, "verb").removeAll(outputVerbSynsets) ) && (! verb.equals("fix")) 
				&& (! verb.equals("repair")) && (! verb.equals("refurbish")) && (! verb.equals("let")) && (! verb.equals("have"))  
						&& (! verb.equals("turn")) && (! verb.equals("hold")) && (! verb.equals("bring")) && (! verb.equals("arrange")*/
						));

	}
	
	public static boolean findMatch(String label, String entity, String linkProperty) throws IOException{
		return findMatch(label, entity, linkProperty, null);
	}
	public static boolean findMatch(String label, String entity, String linkProperty, String verbLabel) throws IOException{
		if(label.indexOf("a ") == 0) label = label.substring(2);
		if(label.indexOf("an ") == 0) label = label.substring(3);
		if(label.indexOf("the ") == 0) label = label.substring(4);
		label = label.replaceAll("\\(.*\\)", "").replaceAll("-", "").trim();
		
		
		if( (linkProperty.equals("mainEntity") || linkProperty.equals("middleEntity") || linkProperty.equals("stepOutput") || linkProperty.equals("stepInput") ) && verbLabel != null
				&& verbLabel.length() > 1 && !verbLabel.equals("\\")) {
			String verbURI = encapsulateURI("http://vocab.inf.ed.ac.uk/procont#?verbextraction=1&verblabel="+URLEncoder.encode(verbLabel, "UTF-8"));
			saveTriple(encapsulateURI(entity),"madph:verb_type",verbURI,verbEntities);
			if(!verbsAlreadySaved.contains(verbLabel)){
				verbsAlreadySaved.add(verbLabel);
				saveTriple(verbURI,"rdfs:label",encapsulateLongLiteral(verbLabel.replaceAll("[^a-zA-Z0-9\\s]", "")),verbLabels);
			}
		}
		if(label == null) return false;
		String unitOfMeasure = "";
		boolean optional = false;
		if(linkProperty.equals("requirement")){
			
			if(label.indexOf("optional") > 0){
				label = label.replaceFirst("optional", "");
				optional = true;
			}
			
			if(label.indexOf(" of ") > 0 && (label.indexOf("(") < 0 || label.indexOf("(") > label.indexOf(" of ") || label.indexOf(")") < label.indexOf(" of "))) {
				unitOfMeasure = label.substring(0, label.indexOf(" of "));
				unitsOfMeasure.add(unitOfMeasure);
				label = label.substring(label.indexOf(" of ")+4);
				//if( optional ) System.out.println(unitOfMeasure+" <><> "+label+ "<><>"+optional);
			} else if ((label.length() > 5 && label.substring(0, 4).matches(".*[0-9].*")) || (label.length() <= 5 && label.matches(".*[0-9].*")) ){
				// if it starts with a number, remove the numerical value
				// then remove the first unit found
				label = label.replaceFirst("[0-9]+[%-\\.,/\\\\][0-9]*|[0-9]*[%-\\.,/\\\\]?[0-9]+", "").trim();
				label = label.replaceFirst("[0-9]+[%-\\.,/\\\\][0-9]*|[0-9]*[%-\\.,/\\\\]?[0-9]+", "").trim();
				for(String u : units){
					if(label.matches(".*[^\\w]"+u+"[s]?.*|^"+u+"[s]?.*")){
						label = label.replaceFirst(u+"[s]? ", "").trim();
						break;
					}
				}
			}
		}
		{
			boolean changed = true;
			int maxIterations = 5;
			while(changed && maxIterations > 0){
				maxIterations--;
				if(label.indexOf("ground ") >= 0 && label.indexOf("ground ") < 3) {
					label = label.replaceFirst("ground\\s", "");
					continue;
				}
				if(label.indexOf("melted ") >= 0 && label.indexOf("melted ") < 3) {
					label = label.replaceFirst("melted\\s", "");
					continue;
				}
				if(label.indexOf("fresh ") >= 0 && label.indexOf("fresh ") < 3) {
					label = label.replaceFirst("fresh\\s", "");
					continue;
				} 
				if(label.indexOf("sliced ") >= 0 && label.indexOf("sliced ") < 3) {
					label = label.replaceFirst("sliced\\s", "");
					continue;
				} 
				if(label.indexOf("grated ") >= 0 && label.indexOf("grated ") < 3) {
					label = label.replaceFirst("grated\\s", "");
					continue;
				} 
				if(label.indexOf("diced ") >= 0 && label.indexOf("diced ") < 3) {
					label = label.replaceFirst("diced\\s", "");
					continue;
				} 
				if(label.indexOf("Ground ") >= 0 && label.indexOf("Ground ") < 3) {
					label = label.replaceFirst("Ground\\s", "");
					continue;
				}
				if(label.indexOf("Melted ") >= 0 && label.indexOf("Melted ") < 3) {
					label = label.replaceFirst("Melted\\s", "");
					continue;
				}
				if(label.indexOf("Fresh ") >= 0 && label.indexOf("Fresh ") < 3) {
					label = label.replaceFirst("Fresh\\s", "");
					continue;
				} 
				if(label.indexOf("Sliced ") >= 0 && label.indexOf("Sliced ") < 3) {
					label = label.replaceFirst("Sliced\\s", "");
					continue;
				} 
				if(label.indexOf("Grated ") >= 0 && label.indexOf("Grated ") < 3) {
					label = label.replaceFirst("Grated\\s", "");
					continue;
				} 
				if(label.indexOf("Diced ") >= 0 && label.indexOf("Diced ") < 3) {
					label = label.replaceFirst("Diced\\s", "");
					continue;
				} 
				changed = false;
			}
		}
		
		//textPreProcessing.treeParseLabel(label,2);
		
		List<String> nouns = textPreProcessing.extractNouns(label);
		String onlyNouns = "";
		for(String s : nouns){
			onlyNouns = onlyNouns+" "+s;
		}
		onlyNouns = removeFirstUppercase(onlyNouns.trim());
		String onlyNounsToSingular = "";
		for(String s : nouns){
			if(s.lastIndexOf('s') == s.length()-1) onlyNounsToSingular = onlyNounsToSingular+" "+s.substring(0, s.length() -1);
			else onlyNounsToSingular = onlyNounsToSingular+" "+s;
		}
		String onlyNounsLemma = "";
		for(String s : nouns){
			onlyNounsLemma = onlyNounsLemma + " " +morpha.lemma(s, "NN");
		}
		
		
		onlyNounsToSingular = toLowerCaseNoAcronyms(onlyNounsToSingular).trim();
		String apicall = "http://localhost:1111/api/search/KeywordSearch?MaxHits=60&QueryString="+URLEncoder.encode(onlyNouns.trim(), "ISO-8859-1")+"";//Accept: application/json
		//System.out.println("\n"+apicall);
		JSONObject json = null;
		try {
			json = readJsonFromUrl(apicall);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		Set<String> possibleMatches = new HashSet<String>();
		if(json != null && json.containsKey("results")  ) {
			JSONArray results = ((JSONArray) json.get("results"));
			if(results.size()>0){
				//logger.log(" > "+label+" ("+entity+")");
				Levenshtein similarityLevenshtein = new Levenshtein();
				Iterator<JSONObject> resultsIterator = ((JSONArray) json.get("results")).iterator();
				
				float bestSimilarity = 0;
				float bestExactSimilarity = 0;
				float bestLinksRank = 0;
				float bestlemmatizedMatch = 0;
				String bestLabel = null;
				String bestDescription = null;
				String bestdbpediaEntity = null;
				
				entitiesSearch: while(resultsIterator.hasNext()){
					JSONObject result = resultsIterator.next();
					
					JSONArray classes = ((JSONArray) result.get("classes"));
					if(classes != null && classes.size() > 0){
						Iterator<JSONObject> classesIterator = classes.iterator();
						while(classesIterator.hasNext()){
							JSONObject classObj = classesIterator.next();
							String classURI = (String) classObj.get("uri");
							if(classURI.equals("http://xmlns.com/foaf/0.1/Person") || classURI.equals("http://dbpedia.org/ontology/MusicalArtist") ||
								classURI.equals("http://schema.org/Person") || classURI.equals("http://dbpedia.org/ontology/Artist") ||
								classURI.equals("http://schema.org/MusicGroup") || classURI.equals("http://dbpedia.org/ontology/Film") ||
								classURI.equals("http://schema.org/Movie") || classURI.equals("http://dbpedia.org/ontology/TelevisionShow") ||
								classURI.equals("http://schema.org/CreativeWork") || classURI.equals("http://dbpedia.org/ontology/WrittenWork") ||
								classURI.equals("http://dbpedia.org/ontology/Writer") || classURI.equals("http://purl.org/ontology/bibo/Book") ||
								classURI.equals("http://schema.org/Organization") || classURI.equals("http://dbpedia.org/ontology/Band") ||

								classURI.equals("http://schema.org/MusicAlbum") || classURI.equals("http://dbpedia.org/ontology/MusicalWork") ||
								classURI.equals("http://dbpedia.org/ontology/Album") || classURI.equals("http://dbpedia.org/ontology/Single") ||
								classURI.equals("http://dbpedia.org/ontology/Musical") ||
								
								classURI.equals("http://schema.org/Book") || classURI.equals("http://dbpedia.org/ontology/Organisation")
									){
								continue entitiesSearch;
							}
						}
					}
					
					//String resultLabel = (String) result.get("label");
					String resultLabelNormalised = toLowerCaseNoAcronyms(((String) result.get("label"))).replaceAll("\\(.*\\)", "").replaceAll("-", "").trim();
					String resultdescription = (String) result.get("description");
					String dbpediaEntity = (String) result.get("uri");
					String dbpediaEntityLowerCase = toLowerCaseNoAcronyms(dbpediaEntity);
					
					if(dbpediaEntityLowerCase.indexOf("(") != -1 && dbpediaEntityLowerCase.indexOf(")") != -1
							&& dbpediaEntityLowerCase.indexOf("exercise)") == -1 && dbpediaEntityLowerCase.indexOf("cake)") == -1
							&& dbpediaEntityLowerCase.indexOf("seat)") == -1 && dbpediaEntityLowerCase.indexOf("cake)") == -1
							&& dbpediaEntityLowerCase.indexOf("food)") == -1
							/*dbpediaEntityLowerCase.indexOf("film)") != -1 || dbpediaEntityLowerCase.indexOf("navigation)") != -1 ||
							dbpediaEntityLowerCase.indexOf("bus)") != -1 || dbpediaEntityLowerCase.indexOf("song)") != -1 ||
							dbpediaEntityLowerCase.indexOf("person)") != -1 || dbpediaEntityLowerCase.indexOf("cartoon)") != -1 ||
							dbpediaEntityLowerCase.indexOf("music)") != -1 || dbpediaEntityLowerCase.indexOf("magazine)") != -1 ||
							dbpediaEntityLowerCase.indexOf("company)") != -1 || dbpediaEntityLowerCase.indexOf("album)") != -1 ||
							dbpediaEntityLowerCase.indexOf("doctor_who)") != -1 || dbpediaEntityLowerCase.indexOf("novel)") != -1 ||
							dbpediaEntityLowerCase.indexOf("decorative_arts)") != -1 || dbpediaEntityLowerCase.indexOf("wrestling)") != -1 ||
							dbpediaEntityLowerCase.indexOf("rock)") != -1 || dbpediaEntityLowerCase.indexOf("bundle)") != -1 ||
							dbpediaEntityLowerCase.indexOf("train)") != -1 || dbpediaEntityLowerCase.indexOf("plane)") != -1 ||
							dbpediaEntityLowerCase.indexOf("glee)") != -1 || dbpediaEntityLowerCase.indexOf("band)") != -1 ||
							dbpediaEntityLowerCase.indexOf("game)") != -1 || dbpediaEntityLowerCase.indexOf("automobile)") != -1 ||
							dbpediaEntityLowerCase.indexOf("physics)") != -1 || dbpediaEntityLowerCase.indexOf("crime)") != -1 ||
							dbpediaEntityLowerCase.indexOf("figure)") != -1 || dbpediaEntityLowerCase.indexOf("government)") != -1 ||
							dbpediaEntityLowerCase.indexOf("roman)") != -1 || dbpediaEntityLowerCase.indexOf("government)") != -1 ||
							dbpediaEntityLowerCase.indexOf("group)") != -1 || 
							dbpediaEntityLowerCase.indexOf("series)") != -1 || dbpediaEntityLowerCase.indexOf("developer)") != -1*/
							){
						continue;}
					if(blockedTypes.contains(dbpediaEntity)) continue;
					if(!allowedTypes.contains(dbpediaEntity)){
						if(!hasDBpediaAbstract(dbpediaEntity)){
							blockedTypes.add(dbpediaEntity);
							continue;
						} else {
							allowedTypes.add(dbpediaEntity);
						}
					}
					
					/*if(!resultLabelNormalised.equals(((String) result.get("label")).replaceAll("\\(.*\\)", "").replaceAll("-", "").trim().toLowerCase()
							)){
						System.out.print("a");
					}*/
					
					int linksRank = ((Number) result.get("refCount")).intValue();
					float similarity = Math.max(similarityLevenshtein.getSimilarity(onlyNouns.trim(), resultLabelNormalised.trim())+0.01f,similarityLevenshtein.getSimilarity(onlyNounsToSingular.trim(), resultLabelNormalised.trim())+0.01f);
					similarity = Math.max(similarity, similarityLevenshtein.getSimilarity(toLowerCaseNoAcronyms(onlyNouns).trim(), toLowerCaseNoAcronyms(resultLabelNormalised).trim()));
					float exactsimilarity = similarityLevenshtein.getSimilarity(toLowerCaseNoAcronyms(label).trim(), toLowerCaseNoAcronyms(resultLabelNormalised).trim());
					
					String[] outputLabelTokens =  toLowerCaseNoAcronyms(resultLabelNormalised).split("\\s+");
					String outputlabelLemmatized = "";
					for(String s : outputLabelTokens){
						outputlabelLemmatized = outputlabelLemmatized + " " + morpha.lemma(s, "NN");
					}
					float lemmatizedMatch = similarityLevenshtein.getSimilarity(onlyNounsLemma.trim() , outputlabelLemmatized.trim());
					
					if(similarity > bestSimilarity || (similarity == bestSimilarity && linksRank > bestLinksRank) ){
						bestSimilarity = similarity;
						bestExactSimilarity = exactsimilarity;
						bestLinksRank = linksRank;
						bestLabel = resultLabelNormalised;
						bestDescription = resultdescription;
						bestdbpediaEntity = dbpediaEntity;
						bestlemmatizedMatch = lemmatizedMatch;
					}
				}
				if(bestSimilarity > inspectThreashold){
					if(bestSimilarity >= 1) matchesFound100++;
					if(bestSimilarity >= 0.9) matchesFound90++;
					if(bestSimilarity >= 0.8) matchesFound80++;
					if(bestSimilarity >= 0.7) matchesFound70++;
					if(bestSimilarity >= 0.6) matchesFound60++;
					matchesFound++;
					//logger.log("MATCH FOUND BETWEEN: "+label+" ["+onlyNouns+"] ("+entity+")\nAND ("+bestSimilarity+") "+bestLabel+" ("+bestdbpediaEntity+") "+bestDescription);
					
					/*// detect redirects to find a common entity
					URLConnection con = new URL( bestdbpediaEntity ).openConnection();
					con.connect();
					InputStream is = con.getInputStream();
					String redirectUrl = con.getURL().toString();
					if(!bestdbpediaEntity.equals(redirectUrl)){
						System.out.println( bestdbpediaEntity+" redirected url: " +redirectUrl  );
					}
					is.close();*/
					
					
					if(linkProperty.equals("requirement")) {
						if((bestSimilarity >= 1 && bestExactSimilarity >= 0.75) || (bestSimilarity > 0.85 && bestlemmatizedMatch == 1)) {
							matchesFoundRequirements++;
							String writer = resultRdfType;
							if(bestExactSimilarity > 0.75) writer = resultRdfTypeTrust;
							//if(bestSimilarity < 1) writer = resultRdfTypeWarning;
							//if(bestSimilarity < 0.9) writer = resultRdfTypeWarningBad;
							lastWriterUsed = writer;
							saveTriple(encapsulateURI(entity),"rdf:type",encapsulateURI(bestdbpediaEntity),writer);
							if(bestSimilarity < 1 && bestSimilarity > 0.85 && bestlemmatizedMatch == 1){
								saveTriple(encapsulateURI(entity),"rdf:type",encapsulateURI(bestdbpediaEntity),resultRdfTypeLemmaExact);
							}
							return true;
						} else {
							boolean changed = false;
							boolean tempchanged = true;
							int maxIterations = 5;
							while(tempchanged && maxIterations > 0){
								maxIterations--;
								label = label.toLowerCase();
								if(label.indexOf("cold ") >= 0 && label.indexOf("cold ") < 3) {
									label = label.replaceFirst("cold\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("warm ") >= 0 && label.indexOf("warm ") < 3) {
									label = label.replaceFirst("warm\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("hot ") >= 0 && label.indexOf("hot ") < 3) {
									label = label.replaceFirst("hot\\s", "");
									changed = true;
									continue;
								} 
								if(label.indexOf("boiled ") >= 0 && label.indexOf("boiled") < 3) {
									label = label.replaceFirst("boiled\\s", "");
									changed = true;
									continue;
								} 
								if(label.indexOf("cooked ") >= 0 && label.indexOf("cooked ") < 3) {
									label = label.replaceFirst("cooked\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("fried ") >= 0 && label.indexOf("fried ") < 3) {
									label = label.replaceFirst("fried\\s", "");
									changed = true;
									continue;
								} 
								if(label.indexOf("mashed ") >= 0 && label.indexOf("mashed ") < 3) {
									label = label.replaceFirst("mashed\\s", "");
									changed = true;
									continue;
								} 
								if(label.indexOf("squashed ") >= 0 && label.indexOf("squashed ") < 3) {
									label = label.replaceFirst("squashed\\s", "");
									changed = true;
									continue;
								} 
								if(label.indexOf("sharp ") >= 0 && label.indexOf("sharp ") < 3) {
									label = label.replaceFirst("sharp\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("small ") >= 0 && label.indexOf("small ") < 3) {
									label = label.replaceFirst("small\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("big ") >= 0 && label.indexOf("big ") < 3) {
									label = label.replaceFirst("big\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("medium ") >= 0 && label.indexOf("medium ") < 3) {
									label = label.replaceFirst("medium\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("large ") >= 0 && label.indexOf("large ") < 3) {
									label = label.replaceFirst("large\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("unsalted ") >= 0 && label.indexOf("unsalted ") < 3) {
									label = label.replaceFirst("unsalted\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("unsweetened ") >= 0 && label.indexOf("unsalted ") < 3) {
									label = label.replaceFirst("unsalted\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("granulated ") >= 0 && label.indexOf("granulated ") < 3) {
									label = label.replaceFirst("granulated\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("chopped ") >= 0 && label.indexOf("chopped ") < 3) {
									label = label.replaceFirst("chopped\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("all-purpose ") >= 0 && label.indexOf("all-purpose ") < 3) {
									label = label.replaceFirst("all-purpose\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("packed ") >= 0 && label.indexOf("packed ") < 3) {
									label = label.replaceFirst("packed\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("light ") >= 0 && label.indexOf("light ") < 3) {
									label = label.replaceFirst("light\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("heavy ") >= 0 && label.indexOf("heavy ") < 3) {
									label = label.replaceFirst("heavy\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("strong ") >= 0 && label.indexOf("strong ") < 3) {
									label = label.replaceFirst("strong\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("soft ") >= 0 && label.indexOf("soft ") < 3) {
									label = label.replaceFirst("soft\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("one ") >= 0 && label.indexOf("one ") < 3) {
									label = label.replaceFirst("one\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("two ") >= 0 && label.indexOf("two ") < 3) {
									label = label.replaceFirst("two\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("three ") >= 0 && label.indexOf("three ") < 3) {
									label = label.replaceFirst("three\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("four ") >= 0 && label.indexOf("four ") < 3) {
									label = label.replaceFirst("four\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("five ") >= 0 && label.indexOf("five ") < 3) {
									label = label.replaceFirst("five\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("six ") >= 0 && label.indexOf("six ") < 3) {
									label = label.replaceFirst("six\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("seven ") >= 0 && label.indexOf("seven ") < 3) {
									label = label.replaceFirst("seven\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("eight ") >= 0 && label.indexOf("eight ") < 3) {
									label = label.replaceFirst("eight\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("nine ") >= 0 && label.indexOf("nine ") < 3) {
									label = label.replaceFirst("nine\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("ten ") >= 0 && label.indexOf("ten ") < 3) {
									label = label.replaceFirst("ten\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("dozen ") >= 0 && label.indexOf("dozen ") < 3) {
									label = label.replaceFirst("dozen\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("dozens ") >= 0 && label.indexOf("dozens ") < 3) {
									label = label.replaceFirst("dozens\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("few ") >= 0 && label.indexOf("few ") < 3) {
									label = label.replaceFirst("few\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("many ") >= 0 && label.indexOf("many ") < 3) {
									label = label.replaceFirst("many\\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("several ") >= 0 && label.indexOf("several  ") < 3) {
									label = label.replaceFirst("several \\s", "");
									changed = true;
									continue;
								}
								if(label.indexOf("to taste") >= 0 && label.indexOf("to taste") >= label.length()-10) {
									label = label.replaceFirst("to taste", "").trim();
									changed = true;
									continue;
								}
								tempchanged = false;
							}
							if(changed) return findMatch( label,  entity,  linkProperty,  verbLabel);
						}
						
					}
					if(linkProperty.equals("mainEntity")) if(bestSimilarity > acceptThreasholdMainEntityCreation)
						{
							matchesFoundMain++;
							String resultingURI = generateNewUri(entity,"output");
							String writer = resultMainOutput;
							if(bestExactSimilarity > 0.8)  writer = resultMainOutputTrust;
							if(bestSimilarity < 1) writer = resultMainOutputWarning;
							if(bestSimilarity < 0.9) writer = resultMainOutputWarningBad;
							lastWriterUsed = writer;
							saveTriple(encapsulateURI(resultingURI),"prohow:has_method",encapsulateURI(entity),writer);
							saveTriple(encapsulateURI(resultingURI),"rdf:type",encapsulateURI(bestdbpediaEntity),writer);
							if(bestSimilarity < 1 && bestSimilarity > 0.85 && bestlemmatizedMatch == 1){
								saveTriple(encapsulateURI(resultingURI),"prohow:has_method",encapsulateURI(entity),resultMainOutputLemmaExact);
								saveTriple(encapsulateURI(resultingURI),"rdf:type",encapsulateURI(bestdbpediaEntity),resultMainOutputLemmaExact);
							}
							return true;
						}
					if(linkProperty.equals("middleEntity")) if(bestSimilarity > acceptThreasholdMiddleEntityCreation)
					{
						matchesFoundMain++;
						String resultingURI = generateNewUri(entity,"output");
						String writer = resultMiddleOutput;
						if(bestExactSimilarity > 0.8)  writer = resultMiddleOutputTrust;
						if(bestSimilarity < 1) writer = resultMiddleOutputWarning;
						if(bestSimilarity < 0.9) writer = resultMiddleOutputWarningBad;
						lastWriterUsed = writer;
						saveTriple(encapsulateURI(resultingURI),"prohow:has_method",encapsulateURI(entity),writer);
						saveTriple(encapsulateURI(resultingURI),"rdf:type",encapsulateURI(bestdbpediaEntity),writer);
						if(bestSimilarity < 1 && bestSimilarity > 0.85 && bestlemmatizedMatch == 1){
							saveTriple(encapsulateURI(resultingURI),"prohow:has_method",encapsulateURI(entity),resultMiddleOutputLemmaExact);
							saveTriple(encapsulateURI(resultingURI),"rdf:type",encapsulateURI(bestdbpediaEntity),resultMiddleOutputLemmaExact);
						}
						return true;
					}
					
					if(linkProperty.equals("stepOutput") || linkProperty.equals("stepInput")) if(bestSimilarity > acceptThreasholdStepEntityCreation){
						String resultingURI = null;
						
						String writer = resultStep;
						if(bestExactSimilarity > 0.8)  writer = resultStepTrust;
						if(bestSimilarity < 1) writer = resultStepWarning;
						if(bestSimilarity < 0.9) writer = resultStepWarningBad;
						lastWriterUsed = writer;
						if(linkProperty.equals("stepOutput")){
							stepOutputsFoundWithEntity++;
							resultingURI = generateNewUri(entity,"stepoutput");
							saveTriple(encapsulateURI(resultingURI),"prohow:has_method",encapsulateURI(entity),writer);
						} else {
							stepInputsFoundWithEntity++;
							resultingURI = generateNewUri(entity,"stepinput");
							saveTriple(encapsulateURI(entity),"prohow:requires",encapsulateURI(resultingURI),writer);
						}
						saveTriple(encapsulateURI(resultingURI),"rdf:type",encapsulateURI(bestdbpediaEntity),writer);
						saveTriple(encapsulateURI(resultingURI),"rdfs:label",encapsulateLongLiteral(label),writer);
						return true;
					}
						
					
				}
			}
		}
		return false;
	}
	
	public static boolean hasDBpediaAbstract(String URI){
		QueryExecution execution = dbpedia.query("select ?abstract FROM <http://dbpedia.org> where "
				+ "{<"+URI+"> <http://dbpedia.org/ontology/abstract> ?abstract .  } LIMIT 1");
		ResultSet results = execution.execSelect();
		boolean found = false;
		if(results.hasNext()){
			//QuerySolution result = results.next();
			found = true;
		}
		execution.close();
		return found;
	}

	public static String generateNewUri(String href, String message) {
		String coumputedUri = URI.create(href).toString() + "&t="
				+ System.currentTimeMillis() + "&n=" + newUriCount;
		if (message != null)
			coumputedUri = coumputedUri + "&k=" + message;
		newUriCount++;
		return coumputedUri;
	}
	
	public static String encapsulateURI(String stringToEncapsulate) {
		return "<" + stringToEncapsulate + ">";
	}
	
	public static String encapsulateLongLiteral(String longLiteral) {
		// remove three " in a row
		if (longLiteral.indexOf("\"\"\"") > 0) {
			longLiteral = longLiteral.replaceAll("\"", "''");
		}
		// duplicate all backslashes
		longLiteral = (longLiteral.replace("\\", "a\"\"\"a")).replace("a\"\"\"a", "\\\\");
		if (longLiteral.lastIndexOf("\"") == (longLiteral.length() - 1)) {
			return "\"\"\"" + longLiteral + " \"\"\"@en";
		} else {
			return "\"\"\"" + longLiteral + "\"\"\"@en";
		}
	}
	
	/*public static String encapsulateLongLiteral(String stringToEncapsulate) {
		stringToEncapsulate.replaceAll("\\", "");
		stringToEncapsulate.replaceAll("/", "");
		if(stringToEncapsulate.contains("\\") && stringToEncapsulate.contains("/"))
			return "\"\"\"" + stringToEncapsulate + "\"\"\"@en";
		else 
			return "\"\"\" " + stringToEncapsulate + " \"\"\"@en";
	}*/
	
	public static void saveTriple(String s, String p, String o, String file) throws IOException {
		File writeFile = new File(file);
		String prefix = "";
		if(!writeFile.exists()) {
			prefix = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . "
					+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . "
					+ "@prefix prohow: <http://vocab.inf.ed.ac.uk/prohow#> . "
					+ "@prefix madph: <http://vocab.inf.ed.ac.uk/madprohow#> . "
					+ "@prefix owl: <http://www.w3.org/2002/07/owl#> . "
					+ "@prefix oa: <http://www.w3.org/ns/oa#> . "
					+ "@prefix dctypes: <http://purl.org/dc/dcmitype/> . "
					+ "@prefix foaf: <http://xmlns.com/foaf/0.1/> . "
					+ "@prefix skos: <http://www.w3.org/2004/02/skos/core#> . "
					+ "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . ";
		}
		
		FileWriter writer = new FileWriter(writeFile, true);
		writer.write(prefix + s + " " + p + " " + o + " .\n");
		writer.flush();
		writer.close();
	}

	public static String constructQueryString(String match){
		Set<String> matchSet = new HashSet<String>();
		matchSet.add(match);
		return constructQueryString(matchSet);
	}
	public static String constructQueryString(Set<String> matches){
		String queryString = prefixes+" SELECT DISTINCT ?label ?entity "
				+ " WHERE { "
				+ " ?entity foaf:isPrimaryTopicOf ?z .  "
				+ " ?entity rdfs:label ?label . "
				+ " FILTER ( ";
		for(String s : matches){
			queryString = queryString + "  regex(str(?label), \""+s+"\", \"i\" )   || ";
		}
		queryString = queryString.substring(0, queryString.length()-4);
		return queryString+ " ) } ";
		
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, ParseException {
		HttpGet getRequest = new HttpGet(url);
		getRequest.setHeader(new BasicHeader("Accept", "application/json"));
		HttpClient client = new DefaultHttpClient();
		HttpResponse response = client.execute(getRequest);
		
	    InputStream is = response.getEntity().getContent();
	    try {
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      String jsonText = readAll(rd);
	      //System.out.println(jsonText);
	      JSONObject json = ((JSONObject)new JSONParser().parse(jsonText));
	      return json;
	    } finally {
	      is.close();
	    }
	}
	
	private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	  }
	
	/**
	 * Replace the first occurence of a capital letter if it is not followed by another one
	 * @param str
	 * @return
	 */
	public static String removeFirstUppercase(String str) {        
		int firstOccurrence = -1;
		int lastOccurence = -2;
	    for(int i=0; i< str.length(); i++) {
	        if(Character.isUpperCase(str.charAt(i))) {
	        	lastOccurence = i;
	        	if(firstOccurrence == -1) firstOccurrence = i;
	        }
	    }
	    if(firstOccurrence == lastOccurence) {
	    	StringBuilder strBuilder = new StringBuilder(str);
	    	strBuilder.setCharAt(firstOccurrence, Character.toLowerCase(str.charAt(firstOccurrence)));
	    	return strBuilder.toString();
	    }
	    return str;
	}
	
	public static String toLowerCaseNoAcronyms(String s){
		return WordUtils.uncapitalize(s,new char[]{' ','-','/','.','(',')','_'});
	}
	
	private static void utilityPrintHowManyDifferentURLareInterconnected(){
		Set<String> interconnectedURLs = new HashSet<String>();
		Set<String> interconnectedURLsOnlyWikiHow = new HashSet<String>();
		Set<String> interconnectedURLsInterRepositories = new HashSet<String>();
		int numberOfLinksInterRepositories = 0;
		int numberOfLinksTotal = 0;
		int numberOfLinksOnlyWikiHow = 0;
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires / prohow:has_step ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires / prohow:has_method ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires / prohow:has_step / prohow:has_step ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires / prohow:has_step / prohow:has_method ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		System.out.println("\n DBPEDIA \n Number of links: "+numberOfLinksTotal+" Number of wikiHow only links: "+numberOfLinksOnlyWikiHow);
		System.out.println("INTER REPOSITORIES NODES DBPEDIA (wikihow <--> Snapguide): "+interconnectedURLsInterRepositories.size()+" thanks to: "+numberOfLinksInterRepositories+" links");
		System.out.println("INTERCONNECTED NODES DBPEDIA (only wikihow): "+interconnectedURLsOnlyWikiHow.size());
		System.out.println("INTERCONNECTED NODES DBPEDIA: "+interconnectedURLs.size());
	}
	
	private static void utilityPrintHowManyDifferentURLareInterconnectedKNOWHOW(){
		Set<String> interconnectedURLs = new HashSet<String>();
		Set<String> interconnectedURLsOnlyWikiHow = new HashSet<String>();
		Set<String> interconnectedURLsInterRepositories = new HashSet<String>();
		int numberOfLinksInterRepositories = 0;
		int numberOfLinksTotal = 0;
		int numberOfLinksOnlyWikiHow = 0;
		
		String queryLinks = prefixes
				+ "SELECT ?entity ?decomp "
				+ " FROM <http://vocab.inf.ed.ac.uk/graph#knowhowdecompositionlinks> "
				+ " WHERE { ?entity prohow:has_step ?decomp . }";
		//logger.log(query);
		QueryExecution exec = sparqlSource.query(queryLinks);
		ResultSet results = exec.execSelect();
		while(results.hasNext()){
			numberOfLinksTotal++;
			analysedLabels++;
			analysedRequirementLabels++;
			QuerySolution result = results.next();
			interconnectedURLs.add(extractSourceFromURI(result.getResource("?entity").getURI()));
			interconnectedURLs.add(extractSourceFromURI(result.getResource("?decomp").getURI()));
			if(isWikiHow(result.getResource("?entity").getURI()) && isWikiHow(result.getResource("?decomp").getURI())){
				numberOfLinksOnlyWikiHow++;
				interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?entity").getURI()));
				interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?decomp").getURI()));
			}
			if(isWikiHow(result.getResource("?entity").getURI()) != isWikiHow(result.getResource("?decomp").getURI())){
				numberOfLinksInterRepositories++;
				interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?entity").getURI()));
				interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?decomp").getURI()));
			}
		}
		exec.close();
		System.out.println("\n KNOW HOW \n Number of links: "+numberOfLinksTotal+" Number of wikiHow only links: "+numberOfLinksOnlyWikiHow);
		System.out.println("INTER REPOSITORIES NODES KNOW HOW (wikihow <--> Snapguide): "+interconnectedURLsInterRepositories.size()+" thanks to: "+numberOfLinksInterRepositories+" links");
		System.out.println("INTERCONNECTED NODES IN KNOW HOW (only wikihow): "+interconnectedURLsOnlyWikiHow.size());
		System.out.println("INTERCONNECTED NODES IN KNOW HOW: "+interconnectedURLs.size());
		
	}
	
	private static void utilityPrintHowManyDifferentURLareConnectedToDBpedia(){
		Set<String> interconnectedURLs = new HashSet<String>();
		Set<String> interconnectedURLsOnlyWikiHow = new HashSet<String>();
		Set<String> interconnectedURLsOnlySnapguide = new HashSet<String>();
		int numberOfLinksOnlySnapguide = 0;
		int numberOfLinksTotal = 0;
		int numberOfLinksOnlyWikiHow = 0;
		
		String queryLinks = prefixes
				+ " SELECT ?entity "
				+ " FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
				+ " FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
				+ " WHERE { ?entity rdf:type ?type . }";
		//logger.log(query);
		QueryExecution exec = sparqlSource.query(queryLinks);
		ResultSet results = exec.execSelect();
		while(results.hasNext()){
			numberOfLinksTotal++;
			QuerySolution result = results.next();
			interconnectedURLs.add(extractSourceFromURI(result.getResource("?entity").getURI()));
			if(isWikiHow(result.getResource("?entity").getURI())){
				numberOfLinksOnlyWikiHow++;
				interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?entity").getURI()));
			} else {
				numberOfLinksOnlySnapguide++;
				interconnectedURLsOnlySnapguide.add(extractSourceFromURI(result.getResource("?entity").getURI()));
			}
		}
		exec.close();
		System.out.println("\n KNOW HOW \n Number of links: "+numberOfLinksTotal+" Number of wikiHow only links: "+numberOfLinksOnlyWikiHow+" Number of Snapguide only links: "+numberOfLinksOnlySnapguide);
		System.out.println("PAGES CONNECTED TO DBPEDIA (only wikihow): "+interconnectedURLsOnlyWikiHow.size());
		System.out.println("PAGES CONNECTED TO DBPEDIA (only snapguide): "+interconnectedURLsOnlySnapguide.size());
		System.out.println("PAGES CONNECTED TO DBPEDIA: "+interconnectedURLs.size());
		
	}
	
	private static void utilityPrintHowManyDifferentURLareInterconnectedUserGenerated(){
		{
			Set<String> interconnectedURLs = new HashSet<String>();
			String queryLinks = prefixes
					+ "SELECT ?entity ?decomp "
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#wikihowmanuallinksobjects> "
					+ " WHERE { ?entity prohow:has_step ?decomp . }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?entity").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?decomp").getURI()));
			}
			exec.close();
			System.out.println("INTERCONNECTED NODES - USER GENERATED - REQUIREMENTS: "+interconnectedURLs.size());
		}
		{
			Set<String> interconnectedURLs = new HashSet<String>();
			String queryLinks = prefixes
					+ "SELECT ?entity ?decomp "
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#wikihowmanuallinksstep> "
					+ " WHERE { ?entity prohow:has_step ?decomp . }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?entity").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?decomp").getURI()));
			}
			exec.close();
			System.out.println("INTERCONNECTED NODES - USER GENERATED - STEPS: "+interconnectedURLs.size());
		}
		{
			Set<String> interconnectedURLs = new HashSet<String>();
			String queryLinks = prefixes
					+ "SELECT ?entity ?decomp "
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#wikihowmanuallinksstep> "
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#wikihowmanuallinksobjects> "
					+ " WHERE { ?entity prohow:has_step ?decomp . }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?entity").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?decomp").getURI()));
			}
			exec.close();
			System.out.println("INTERCONNECTED NODES - USER GENERATED - REQUIREMENTS + OBJECTS: "+interconnectedURLs.size());
		}
	}
	
	private static void utilityPrintHowManyDifferentURLareInterconnectedALL(){
		Set<String> interconnectedURLs = new HashSet<String>();
		Set<String> interconnectedURLsOnlyWikiHow = new HashSet<String>();
		Set<String> interconnectedURLsInterRepositories = new HashSet<String>();
		int numberOfLinksInterRepositories = 0;
		int numberOfLinksTotal = 0;
		int numberOfLinksOnlyWikiHow = 0;
		{
			String queryLinks = prefixes
					+ "SELECT ?entity ?decomp "
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#knowhowdecompositionlinks> "
					+ " WHERE { ?entity prohow:has_step ?decomp . }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?entity").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?decomp").getURI()));
				if(isWikiHow(result.getResource("?entity").getURI()) && isWikiHow(result.getResource("?decomp").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?entity").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?decomp").getURI()));
				}
				if(isWikiHow(result.getResource("?entity").getURI()) != isWikiHow(result.getResource("?decomp").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?entity").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?decomp").getURI()));
				}
			}
			exec.close();
		}
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires / prohow:has_step ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires / prohow:has_method ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires / prohow:has_step / prohow:has_step ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		{
			String queryLinks = prefixes
					+ "SELECT DISTINCT ?outputter ?requirer "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow7> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow6> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow5> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow4> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow3> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow2> "
					+ "FROM <http://vocab.inf.ed.ac.uk/graph#wikihow1>"
					+ " FROM <http://vocab.inf.ed.ac.uk/graph#snapguide>"
					+ " WHERE {  "
					+ " ?entity rdf:type ?type . "
					+ " ?entity prohow:has_method ?outputter ."
					+ "  ?entity2 rdf:type ?type . "
					+ " ?requirer prohow:requires / prohow:has_step / prohow:has_method ?entity2 }";
			//logger.log(query);
			QueryExecution exec = sparqlSource.query(queryLinks);
			ResultSet results = exec.execSelect();
			while(results.hasNext()){
				numberOfLinksTotal++;
				analysedLabels++;
				analysedRequirementLabels++;
				QuerySolution result = results.next();
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
				interconnectedURLs.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				if(isWikiHow(result.getResource("?outputter").getURI()) && isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksOnlyWikiHow++;
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsOnlyWikiHow.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
				if(isWikiHow(result.getResource("?outputter").getURI()) != isWikiHow(result.getResource("?requirer").getURI())){
					numberOfLinksInterRepositories++;
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?outputter").getURI()));
					interconnectedURLsInterRepositories.add(extractSourceFromURI(result.getResource("?requirer").getURI()));
				}
			}
			exec.close();
		}
		System.out.println("\n KNOW HOW + DBPEDIA \n Number of links: "+numberOfLinksTotal+" Number of wikiHow only links: "+numberOfLinksOnlyWikiHow);
		System.out.println("INTER REPOSITORIES NODES KNOW HOW + DBPEDIA (wikihow <--> Snapguide): "+interconnectedURLsInterRepositories.size()+" thanks to: "+numberOfLinksInterRepositories+" links");
		System.out.println("INTERCONNECTED NODES IN KNOW HOW + DBPEDIA (only wikiHow): "+interconnectedURLsOnlyWikiHow.size());
		System.out.println("INTERCONNECTED NODES IN KNOW HOW + DBPEDIA: "+interconnectedURLs.size());
	}
	
	private static boolean isWikiHow(String uri){
		if(uri.indexOf("http://vocab.inf.ed.ac.uk/procont#?url=http://www.wikihow.com") >= 0) return true;
		if(uri.indexOf("http://vocab.inf.ed.ac.uk/procont#?url=http://snapguide.com/") >= 0) return false;
		System.out.println("ERROR, unrecognized dataset for URI: "+uri);
		return false;
	}
	
	
	  
}
