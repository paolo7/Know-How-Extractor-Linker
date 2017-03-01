package activityRecognition;

import integrationcore.IntegrationConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.ParseException;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import query.SparqlManager;
import luceneIndexing.LuceneIndexQuery;
import nlp.ParsedSentenceSynsets;
import nlp.TextProcessingController;

public class ActivityRecognizer {

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
	private IntegrationConfiguration intConfig;
	private TextProcessingController textProcessing;
	private LuceneIndexQuery luceneQuery;
	private SparqlManager sparql;
	private double threshold = 0.3;
	
	public ActivityRecognizer(TextProcessingController textProcessing, LuceneIndexQuery luceneQuery, SparqlManager sparql, IntegrationConfiguration intConfig) {
		this.textProcessing = textProcessing;
		this.luceneQuery = luceneQuery;
		this.sparql = sparql;
		this.intConfig = intConfig;
	}
	
	public List<ActionScore> recognizeActivity(String text){
		List<List<String>> allSentences = textProcessing.getAllWordsSynsetsBySentence(text);
		Map<String,ActionScore> allScores = new HashMap<String,ActionScore>();
		for(List<String> words: allSentences){
			try {
				List<ActionScore> actionScores = luceneQuery.returnBestSimilarity(text.split("\\s+"));
				for(ActionScore action : actionScores){
					if(action.getScore() > threshold){
						// Get all the super entities
						QueryExecution execution = sparql.query(prefixes + 
								" SELECT DISTINCT ?uri ?label "+intConfig.getNamedGraphsQueryString()+" FROM <http://vocab.inf.ed.ac.uk/graph#knowhowdecompositionlinks> "
										+ " WHERE { "
										+ " <"+action.getUri()+"> ((^prohow:has_method) | (^prohow:has_step))* ?uri . "
										+ " OPTIONAL {"
										+ " ?uri rdfs:label ?label . "
										+ " } "
										+ " } ");
						ResultSet results = execution.execSelect();
						while(results.hasNext()){
							QuerySolution result = results.next();
							String uri = result.getResource("?uri").getURI();
							String label = result.getLiteral("?label").getLexicalForm();
							double generalityFactor = 1;
							if(!action.getUri().equals(uri)) generalityFactor = 1.01;
							if(!allScores.containsKey(uri)){
								ActionScore newAction = new ActionScore(uri, action.getScore()*action.getScore()*generalityFactor);
								newAction.setLabel(label);
								allScores.put(uri, newAction);
							} else {
								ActionScore oldAction = allScores.get(uri);
								oldAction.setScore(oldAction.getScore() + action.getScore()*action.getScore()*generalityFactor);
							}
						}
						execution.close();
					}
				}
				
				
			} catch (ParseException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		List<ActionScore> bestActivities = sort(allScores);
		
		//TODO remove next line
		debugPrint(text, bestActivities);
		
		return bestActivities;
	}
	
	public List<ActionScore> sort(Map<String,ActionScore> allScores){
		List<ActionScore> orderedList = new LinkedList<ActionScore>();
		while(allScores.size() > 0) {
			double best = Double.MIN_VALUE;
			String bestUri = null;
			for(String s : allScores.keySet()){
				if(allScores.get(s).getScore() > best){
					bestUri =  s;
					best = allScores.get(s).getScore();
				}
			}
			if(bestUri != null){
				orderedList.add(allScores.get(bestUri));
				allScores.remove(bestUri);
			}
			else {
				return orderedList;
			}
		}
		return orderedList;
	}
	
	public void debugPrint(String text, List<ActionScore> bestResults){
		
		if(bestResults.size() > 0){
			String bestMatch = bestResults.get(0).getLabel();
			if(bestMatch.indexOf("How to") == 0) bestMatch = bestMatch.substring(6).trim().toLowerCase();
			System.out.println("Are you trying to... "+bestMatch+"?");
			System.out.println();
		}
		
		System.out.println("ACTIVITY RECOGNITION");
		System.out.println("-> TEXT:");
		System.out.println(text);
		int counter = 0;
		System.out.println("-> RESULTS:");
		for(ActionScore a : bestResults){
			counter++;
			if(counter <= 10){
				System.out.println("--> ("+counter+") Result Score: "+a.getScore());
				System.out.println(a.getLabel());
				System.out.println("+ URI: "+a.getUri()+" <--");
			}
		}
		System.out.println();
		if(bestResults.size() > 0){
			String bestMatch = bestResults.get(0).getLabel();
			if(bestMatch.indexOf("How to") == 0) bestMatch = bestMatch.substring(6).trim();
			bestMatch = Character.toLowerCase(bestMatch.charAt(0)) + (bestMatch.length() > 1 ? bestMatch.substring(1) : "");
			System.out.println("Are you trying to... "+bestMatch+"?");
			System.out.println();
		}
		System.out.println();
	}

}
