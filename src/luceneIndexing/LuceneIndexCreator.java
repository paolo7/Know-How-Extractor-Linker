package luceneIndexing;

import integrationcore.IntegrationConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nlp.ParsedPhraseExtended;
import nlp.ParsedSentenceSynsets;
import nlp.TextProcessingController;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import query.SparqlManager;
import test_nlp.NLP_BOX;

public class LuceneIndexCreator {

	protected SparqlManager sparql;
	private String indexPath;
	private String[] namedGraphs;
	private IndexWriter writer;
	private TextProcessingController textPreProcessing;
	protected int maxSentencesToConsider;
	private int maxWordsToConsider;
	protected IntegrationConfiguration intConfig;
	private Map<String,String> ontologyMap = new HashMap<String,String>();
	protected IndexSearcher targetSearcher;
	protected IndexSearcher sourceSearcher;
	
	public LuceneIndexCreator(SparqlManager sparql, String indexPath, String[] namedGraphs, TextProcessingController textPreProcessing, IntegrationConfiguration intConfig) {
		this.sparql = sparql;
		this.indexPath = indexPath;
		this.namedGraphs = namedGraphs;
		this.textPreProcessing = textPreProcessing;
		this.intConfig = intConfig;
		this.maxSentencesToConsider = intConfig.getMaxSentencesToConsider();
		this.maxWordsToConsider = intConfig.getMaxWordsToConsider();
		
		String indexTargetPath = intConfig.getPathToLuceneTargetIndex();
		String indexSourcePath = intConfig.getPathToLuceneSourceIndex();
		IndexReader reader;
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(indexTargetPath)));
			targetSearcher = new IndexSearcher(reader);
			if(indexSourcePath != null && !indexSourcePath.equals(indexTargetPath)){
				IndexReader sourceReader = DirectoryReader.open(FSDirectory.open(new File(indexSourcePath)));
				sourceSearcher = new IndexSearcher(sourceReader);
			} else {
				sourceSearcher = targetSearcher;
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	public void createIndexApplication(String graphString) throws IOException{
		System.out.println("\nAPPLICATION INDEX - GRAPH "+graphString+" STARTED");
		int preprocessedEntities = 0;
		Map<String,String> thingsToAnalyse = new HashMap<String,String>();
		QueryExecution execution = sparql.query(
				" PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
				+ " PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#>  "
				+ " PREFIX madph: <http://vocab.inf.ed.ac.uk/madprohow#> "
				+ " PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ " PREFIX oa: <http://www.w3.org/ns/oa#> "
				+ " PREFIX dctypes: <http://purl.org/dc/dcmitype/> "
				+ " PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
				+ " PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
				+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>  "
				+ " SELECT DISTINCT ?entity ?label"
				+ " FROM <"+graphString+"> "
				//+ " FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
				//+ " FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
				//+ " FROM <http://vocab.inf.ed.ac.uk/graph#knowhowdecompositionlinks> "
				+ " WHERE {"
				+ " ?annotation oa:hasBody | prohow:has_step | prohow:has_method | ^prohow:has_method ?entity . "
				+ "  OPTIONAL { ?entity rdfs:label ?label . }  "
				+ " }");
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			QuerySolution result = results.next();
			String label=null;
			if(result.getLiteral("?label") != null) label = result.getLiteral("?label").getLexicalForm();
			thingsToAnalyse.put(result.getResource("?entity").getURI(), label);
		}
		execution.close();
		System.out.println("\nAPPLICATION INDEX - GRAPH "+graphString+" COLLECTED ALL ENTITIES");
		
		ArrayBlockingQueue<Runnable> blockingqueue = new ArrayBlockingQueue<Runnable>(intConfig.getMaxThreads());
		ThreadPoolExecutor executor = new ThreadPoolExecutor(intConfig.getMaxThreads()/4,intConfig.getMaxThreads(),1,TimeUnit.SECONDS,blockingqueue);
		textPreProcessing.initializePOSpipeline();
		for(String uri : thingsToAnalyse.keySet()){
			String label = thingsToAnalyse.get(uri);
			
			while(blockingqueue.remainingCapacity() < 1){
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			preprocessedEntities++;
			
				executor.execute(new LuceneIndexCreatorThread( textPreProcessing, writer,  maxSentencesToConsider,  maxWordsToConsider, uri, label, this,graphString));
			if(preprocessedEntities % 5000 == 0){
				System.out.println("\n - - - # processed: "+preprocessedEntities);
			}
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor.shutdown();
		System.out.println("In graph: "+graphString+" "+preprocessedEntities+" entities were indexed.");
		
	}
	
	public void createIndex() throws IOException{
		Date start = new Date();
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48, analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		Directory dir = FSDirectory.open(new File(indexPath));
		writer = new IndexWriter(dir, iwc);
		if(intConfig.isDoLuceneIndexing()){
			System.out.println("\nAPPLICATION INDEX - START LOOP OF ALL GRAPHS");
			for(String s : intConfig.getNamedGraphs()){
				System.out.println("APPLICATION INDEX - START ON GRAPH "+s);
				createIndexApplication(s);
			}
			createIndexApplication("http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks");
			createIndexApplication("http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks");
			createIndexApplication("http://vocab.inf.ed.ac.uk/graph#knowhowdecompositionlinks");
			System.out.println("\nAPPLICATION INDEX - CLOSE WRITER");
			writer.close();
			System.out.println("\nAPPLICATION INDEX - WRITER CLOSED");
			return;
		}
		System.out.println("Indexing to directory '" + indexPath + "'...");
		int allPreprocessedEntities = 0;
		// sanity check over snapguide
		// do not index processes which have less than 3 steps
		int viewsLimit = 500;
		int likesLimit = 25;
		Set<String> sourcesToBlock = new HashSet<String>();
		 {
			 if (!intConfig.isRestrictLuceneIndexCreationToPrimitiveEntities()){
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
							+ "  ?main madph:has_views ?views ."
							+ "  ?main madph:has_likes ?likes ."
							+ "  FILTER (xsd:integer(?views ) > "+viewsLimit+")"
							+ "  FILTER (xsd:integer(?likes ) > "+likesLimit+")"
							+ "  } "
							+ " }");
					ResultSet results = execution.execSelect();
					while(results.hasNext()){
						QuerySolution result = results.next();
						sourcesToBlock.add(extractSourceFromURI(result.getResource("?main").getURI()));
					}
					execution.close();
			 }
			
			System.out.println("Created list of sources to block: there are "+sourcesToBlock.size());
			
			// create map of classes
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
					+ " SELECT DISTINCT ?a ?b "
					+ intConfig.getOntologyGraphQueryString()
					+ " WHERE {"
					+ " ?a rdfs:subClassOf ?b . "
					+ " } ");
			ResultSet results = execution.execSelect();
			while(results.hasNext()){
				QuerySolution result = results.next();
				String supercategory = result.getResource("?b").getURI();
				String subcategory = result.getResource("?a").getURI();
				ontologyMap.put(subcategory, supercategory);
			}
			execution.close();
		}
		//
		for(String s : namedGraphs){
			System.out.println("Indexing graph: "+s+" indexing started...");
			Date startDate = new Date();
			int preprocessedEntities = 0;
			String queryString = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> SELECT DISTINCT ?uri ?label FROM <"+s+"> WHERE { "
					+ "?annotation oa:hasBody ?uri . ";
			if(intConfig.isRestrictLuceneIndexCreationToPrimitiveEntities()){
				queryString = queryString + " FILTER NOT EXISTS { ?uri prohow:has_step ?suburi . }"
						+ " FILTER NOT EXISTS { ?uri prohow:has_method ?suburi . } " ;
			}
			if(intConfig.isRestrictLuceneIndexCreationToComplexEntities()){
				queryString = queryString + "  ?uri prohow:has_step | prohow:has_method ?subentity . ";
			}
			queryString = queryString + " ?annotation oa:hasTarget ?target . "
					+ "?target oa:hasSelector ?selector . "
					+ "?selector oa:exact ?label . }";
			if(intConfig.isDoLuceneIndexingContext()){
				queryString = "PREFIX oa: <http://www.w3.org/ns/oa#> SELECT DISTINCT ?source FROM <"+s+"> WHERE { "
						+ " ?target oa:hasSource ?source . "
						+ " ?target oa:hasSelector ?selector . "
						+ " ?selector oa:exact ?label . }";
			}
			QueryExecution execution = sparql.query(queryString);
			ResultSet results = execution.execSelect();
			Map<String,String> uriLabelMap = new HashMap<String,String>();
			while(results.hasNext()){
				QuerySolution result = results.next();
				if(intConfig.isDoLuceneIndexingContext()){
					String source = result.getResource("?source").getURI();
					uriLabelMap.put(source, null);
				} else {
					String uri = result.getResource("?uri").getURI();
					String label = result.getLiteral("?label").getLexicalForm();
					uriLabelMap.put(uri, label);
				}
			}
			execution.close();
			ArrayBlockingQueue<Runnable> blockingqueue = new ArrayBlockingQueue<Runnable>(intConfig.getMaxThreads());
			ThreadPoolExecutor executor = new ThreadPoolExecutor(intConfig.getMaxThreads()/4,intConfig.getMaxThreads(),1,TimeUnit.SECONDS,blockingqueue);
			textPreProcessing.initializePOSpipeline();
			for(String uri : uriLabelMap.keySet()){
				preprocessedEntities++;
				allPreprocessedEntities++;
				String label = null;
				if( ! intConfig.isDoLuceneIndexingContext()) label = textCleaning(uriLabelMap.get(uri));
				
				while(blockingqueue.remainingCapacity() < 1){
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				if(!sourcesToBlock.contains(extractSourceFromURI(uri))){
					//System.out.print(".");
					executor.execute(new LuceneIndexCreatorThread( textPreProcessing, writer,  maxSentencesToConsider,  maxWordsToConsider, uri, label, this,s));
				}
				
				//LuceneIndexCreatorThread indexCreatorThread = new LuceneIndexCreatorThread( textPreProcessing, writer,  maxSentencesToConsider,  maxWordsToConsider, uri, label);
				
				//indexEntityPhrases();
				if(preprocessedEntities % 200 == 0){System.out.print(".");}
				if(preprocessedEntities % 10000 == 0){
					System.out.println("\n - - - in "+((new Date().getTime()-startDate.getTime())/1000)+"sec, # processed: "+preprocessedEntities);
				}
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			executor.shutdown();
			System.out.println("In graph: "+s+" "+preprocessedEntities+" entities were indexed.");
		}
		System.out.println("TOTAL entities indexed:"+allPreprocessedEntities+".");
		/*indexEntity(writer,"XXXX","xapple, xmelon and xy");
		indexEntity(writer,"YYYY","yapple, ymelon and xy");
		indexEntity(writer,"ZZZZ","xapple yapple, ymelon and xy");*/
		writer.close();
		 
		Date end = new Date();
		System.out.println(end.getTime() - start.getTime() + " total milliseconds");
	}
	
	
	// Deal with typos, or things like: "So. .. you should do x and y" at the moment So is the first sentence, and "You should..." is the fourth
	// while actually it is all one sentence
	public String textCleaning(String text){
		String newText = text;
		newText = newText.replaceAll("^Edit([A-Z0-9])", " $1");
		newText = newText.replaceAll("^Method [0-9]+ of [0-9]+", "");
		if(text.matches("")){
			
		}
			
		if(text.toLowerCase().indexOf("how to") == 0){
			text = text.substring(6).trim();
		}
		newText = newText.replaceAll("\\.\\.\\.", ",");
		newText = newText.replaceAll("\\.{2,}+", ".");
		newText = newText.replaceAll("\\?{2,}+", "?");
		newText = newText.replaceAll("!{2,}+", "!");
		newText = newText.replaceAll("!1", "!");
		/*newText = newText.replaceAll("\\.!", "!");
		newText = newText.replaceAll("!\\.", "!");
		newText = newText.replaceAll("\\.\\?", "?");
		newText = newText.replaceAll("\\?\\.", "?");*/

		newText = newText.replaceAll("! !", "!");
		newText = newText.replaceAll("\\? \\?", "?");
		
		newText = newText.replaceAll("\\s\\?([a-zA-Z0-9])", " $1");
		
		
		int index = newText.indexOf(".");
		// remove all trailing dots
		while(index >= 0 && index <= 2){
			StringBuilder sb = new StringBuilder(newText);
			newText = sb.deleteCharAt(index).toString();
			index = newText.indexOf(".");
		}
		// remove all dots which are less then 3 characters apart
		while(index != -1){
			int newIndex = newText.indexOf(".", index + 1);
			if(newIndex != -1 && newIndex-index < 5){
				if(newText.substring(index, newIndex).indexOf(" ") != -1) {
					StringBuilder sb = new StringBuilder(newText);
					sb.setCharAt(index, ';');
					newText = sb.toString();
				}
			}
			index = newIndex;
		}		
		/*if(!text.equals(newText)){
			System.out.println("string corrected");
		}*/
		return newText;
	}
	public String extractSourceFromURI(String URI){
		if(URI.indexOf("#?url=") != -1)
			return URI.substring(URI.indexOf("#?url=")+6, URI.lastIndexOf("&t="));
		else return URI;
	}
	
	protected String returnExactCategory(String uri){
		QueryExecution execution = null;
		try{
			execution = sparql.query(
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
					+ " SELECT DISTINCT ?category "
					+ intConfig.getNamedGraphsQueryString()
					+ " WHERE {"
					+ " { <"+uri+"> rdf:type ?category . }"
					+ " UNION { <"+uri+"> ^prohow:has_step ?super . ?super rdf:type ?category . }"
					+ " UNION { <"+uri+"> ^prohow:requires ?super . ?super rdf:type ?category . }"
					+ " UNION { <"+uri+"> ^prohow:has_step ?super . ?requiringmain prohow:requires ?super . ?requiringmain rdf:type ?category . }"
					+ " UNION { <"+uri+"> ^prohow:has_method ?super . ?requiringmain prohow:requires ?super . ?requiringmain rdf:type ?category . }"
					+ " UNION { <"+uri+"> ^prohow:has_step ?super . ?supersuper prohow:has_step ?super . ?supersuper rdf:type ?category . }"
					+ " UNION { <"+uri+"> ^prohow:has_step ?super . ?supersuper prohow:has_method ?super . ?supersuper rdf:type ?category . }"
					+ " } LIMIT 1");
			ResultSet results = execution.execSelect();
			if(results.hasNext()){
				QuerySolution result = results.next();
				String category = result.getResource("?category").getURI();
				execution.close();
				execution.abort();
				return category;
			}
			execution.close();
			execution.abort();
			return null;
		} catch(Exception e){
			execution.close();
			execution.abort();
			try {
				Thread.sleep(3);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.print("+");
			return returnExactCategory(uri);
		}
		
	}
	
/*	protected String returnRelatedURIsTrasv(String uri){
		QueryExecution execution = null;
		try{
			String relatedURIs = "";
			{
				execution = sparql.query(
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
								+ " SELECT DISTINCT ?uri "
								+ intConfig.getNamedGraphsQueryString()
								+ " WHERE {"
								+ " { <"+uri+"> prohow:requires+ ?uri . }"
								+ " UNION  { <"+uri+"> (^prohow:requires)+ ?uri . }"
								+ " }");
				ResultSet results = execution.execSelect();
				while(results.hasNext()){
					String result = results.next().getResource("?uri").getURI();
					if(!result.equals(uri)){
						relatedURIs = relatedURIs+" "+ result;
					}
				}
				execution.close();
				execution.abort();
			}
			return relatedURIs.trim();
		} catch(Exception e){
			execution.close();
			execution.abort();
			try {
				Thread.sleep(3);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.print("=");
			return returnRelatedURIs(uri);
		}
	}*/
	
	protected String returnRelatedURIs(String uri){
		QueryExecution execution = null;
		try{
			String relatedURIs = "";
			{
				execution = sparql.query(
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
								+ " SELECT DISTINCT ?uri "
								+ intConfig.getNamedGraphsQueryString()
								+ " WHERE {"
								+ " { <"+uri+"> prohow:has_step ?uri . }"
								+ " UNION  { <"+uri+"> prohow:has_method ?uri . }"
								+ " UNION  { ?uri prohow:has_step <"+uri+"> . }"
								+ " UNION  { ?uri prohow:has_step / prohow:has_step <"+uri+"> . }"
								+ " UNION  { ?uri prohow:has_method / prohow:has_step <"+uri+"> . }"
								+ " }");
				ResultSet results = execution.execSelect();
				while(results.hasNext()){
					QuerySolution result = results.next();
					relatedURIs = relatedURIs+" "+ result.getResource("?uri").getURI();
				}
				execution.close();
				execution.abort();
			}
			
			{
				execution = sparql.query(
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
								+ " SELECT DISTINCT ?uri "
								+ intConfig.getNamedGraphsQueryString()
								+ " WHERE {"
								+ " { <"+uri+"> prohow:requires+ ?uri . }"
								+ " UNION  { <"+uri+"> (^prohow:requires)+ ?uri . }"
								+ " }");
				ResultSet results = execution.execSelect();
				while(results.hasNext()){
					String result = results.next().getResource("?uri").getURI();
					if(!result.equals(uri)){
						relatedURIs = relatedURIs+" "+ result;
					}
				}
				execution.close();
				execution.abort();
			}
			return relatedURIs.trim();
		} catch(Exception e){
			execution.close();
			execution.abort();
			try {
				Thread.sleep(3);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.print("=");
			return returnRelatedURIs(uri);
		}
	}
	protected String[] returnAllCategories(String uri){
		List<String> categoriesChain = new LinkedList<String>();
		String superCategory = uri;
		while(superCategory != null){
			categoriesChain.add(0, superCategory);
			superCategory = ontologyMap.get(superCategory);
		}
		return categoriesChain.toArray(new String[0]);
	}
	
	protected String[] getAllWordsFromURL(String URL, String graph){
		try{
			Set<String> URIs = new HashSet<String>();
			// Get all URI in URL
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
							+ " SELECT DISTINCT ?entityuri "
							+ " FROM <"+graph+"> "
							+ " WHERE { "
							+ " ?target oa:hasSource <"+URL+"> . "
							+ " ?annotation oa:hasTarget ?target . "
							+ " ?annotation oa:hasBody ?entityuri . "
							+ " }");
			ResultSet results = execution.execSelect();
			while(results.hasNext()){
				QuerySolution result = results.next();
				URIs.add(result.getResource("?entityuri").getURI());
			}
			execution.close();
			
			String[] allWords = new String[2];
			allWords[0] = "";
			allWords[1] = "";
			if(URIs == null) return allWords;
			try {
				BooleanQuery qURI = new BooleanQuery();
				for(String uri : URIs){
					qURI.add(new TermQuery(new Term("uri",uri)), Occur.SHOULD);
				}
				TopScoreDocCollector collector = TopScoreDocCollector.create(200, true);
				sourceSearcher.search(qURI, collector);
				ScoreDoc[] hits = collector.topDocs().scoreDocs;
				for(ScoreDoc doc : hits){
					Document hitDoc = sourceSearcher.doc(doc.doc);
					for(int i = 0; i < maxSentencesToConsider; i++){
						if(hitDoc.get("sentence"+i)!=null){
							allWords[0] = allWords[0]+" "+hitDoc.get("sentence"+i);
						}
						if(hitDoc.get("core"+i)!=null){
							allWords[1] = allWords[1]+" "+hitDoc.get("core"+i);
						}
					}
				}
				collector = TopScoreDocCollector.create(200, true);
				targetSearcher.search(qURI, collector);
				hits = collector.topDocs().scoreDocs;
				for(ScoreDoc doc : hits){
					Document hitDoc = targetSearcher.doc(doc.doc);
					for(int i = 0; i < maxSentencesToConsider; i++){
						if(hitDoc.get("sentence"+i)!=null){
							allWords[0] = allWords[0]+" "+hitDoc.get("sentence"+i);
						}
						if(hitDoc.get("core"+i)!=null){
							allWords[1] = allWords[1]+" "+hitDoc.get("core"+i);
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			allWords[0] = allWords[0].trim();
			allWords[1] = allWords[1].trim();
			return allWords;
		} catch(Exception e){
			System.out.print("?");
			try {
				Thread.sleep(3);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return getAllWordsFromURL( URL,  graph);
		}
	}
	
	
	protected String returnRequirements(String uri){
		String requirements = "";
		QueryExecution execution = null;
		try{
			{
				String query = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
						+ " PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#>  "
						+ " PREFIX madph: <http://vocab.inf.ed.ac.uk/madprohow#> "
						+ " PREFIX owl: <http://www.w3.org/2002/07/owl#> "
						+ " PREFIX oa: <http://www.w3.org/ns/oa#> "
						+ " PREFIX dctypes: <http://purl.org/dc/dcmitype/> "
						+ " PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
						+ " PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
						+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>  "
						+ " SELECT DISTINCT ?req "
						+ intConfig.getNamedGraphsQueryString()
						+ " FROM <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
						+ " FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
						+ " FROM <http://vocab.inf.ed.ac.uk/graph#knowhowdecompositionlinks> "
						+ " WHERE {"
						+ " <"+uri+"> prohow:requires ?req . "
						+ " }";
				execution = sparql.query(query
						          );
				ResultSet results = execution.execSelect();
				while(results.hasNext()){
					QuerySolution result = results.next();
					requirements = requirements+ " " + result.getResource("?req").getURI();
				}
				execution.close();
				execution.abort();
			}
			return requirements.trim();
		} catch(Exception e){
			if(execution != null){
				execution.close();
				execution.abort();
			}
			try {
				Thread.sleep(3);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.print("=");
			return returnRequirements(uri);
		}
	}
	
	protected boolean isHighlight(String uri){
		if(uri.indexOf("mainentity") != -1) return false;
		QueryExecution execution = null;
		boolean isHighlight = false;
		if(uri.indexOf("mainentity") >= 0) return false;
		try{
			{
				String query = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
						+ " PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#>  "
						+ " PREFIX madph: <http://vocab.inf.ed.ac.uk/madprohow#> "
						+ " PREFIX owl: <http://www.w3.org/2002/07/owl#> "
						+ " PREFIX oa: <http://www.w3.org/ns/oa#> "
						+ " PREFIX dctypes: <http://purl.org/dc/dcmitype/> "
						+ " PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
						+ " PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
						+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>  "
						+ " SELECT ?sub "
						+ intConfig.getNamedGraphsQueryString()
						+ " FROM NAMED <http://vocab.inf.ed.ac.uk/graph#knowhowdecompositionlinks> "
						+ " WHERE {"
						+ " GRAPH <http://vocab.inf.ed.ac.uk/graph#knowhowdecompositionlinks> { ?interesting prohow:has_step ?sub . }"
						+ " <"+uri+"> (prohow:has_step | prohow:has_method)? / (prohow:has_step | prohow:has_method)? ?interesting . "
						+ " }";
				execution = sparql.query(query
						          );
				ResultSet results = execution.execSelect();
				if(results.hasNext()){
					isHighlight = true;
				}
				execution.close();
				execution.abort();
			}
			if(!isHighlight){
				String query = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
						+ " PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#>  "
						+ " PREFIX madph: <http://vocab.inf.ed.ac.uk/madprohow#> "
						+ " PREFIX owl: <http://www.w3.org/2002/07/owl#> "
						+ " PREFIX oa: <http://www.w3.org/ns/oa#> "
						+ " PREFIX dctypes: <http://purl.org/dc/dcmitype/> "
						+ " PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
						+ " PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
						+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>  "
						+ " SELECT ?dbpediaType "
						+ intConfig.getNamedGraphsQueryString()
						+ " FROM NAMED <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> "
						//+ " FROM <http://vocab.inf.ed.ac.uk/graph#dbpediaoutputlinks> "
						//+ " FROM NAMED <http://vocab.inf.ed.ac.uk/graph#knowhowdecompositionlinks> "
						+ " WHERE {"
						+ " GRAPH <http://vocab.inf.ed.ac.uk/graph#dbpediarequirementlinks> { ?interesting rdf:type ?dbpediaType . }"
						+ " <"+uri+"> (prohow:has_step | prohow:has_method)? / (prohow:has_step | prohow:has_method)? ?interesting . "
						+ " }";
				execution = sparql.query(query
						          );
				ResultSet results = execution.execSelect();
				if(results.hasNext()){
					isHighlight = true;
				}
				execution.close();
				execution.abort();
			}
			return isHighlight;
		} catch(Exception e){
			if(execution != null){
				execution.close();
				execution.abort();
			}
			try {
				Thread.sleep(3);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.print("=");
			return isHighlight(uri);
		}
	}
	
}
