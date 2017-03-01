package comparison;

import integrationcore.IntegrationController;

import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.atlas.web.HttpException;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class ComparisonController {
	
	private List<LinkResult> linkResults;
	private IntegrationController controller;
	private Map<String,String> uriToSource;
	private Map<String,String> uriToLabel;
	private Map<String,Map<String,Set<String>>> uriToSynsets;
	private LinkedList<String> insertionOrder;
	private int maxCache = 100000;
	//private Map<String,String> uriToTokens;
	
	private int comparedLevel1 = 0;
	private int comparedAll = 0;
	private int aborted = 0;
	private int allmatched = 0;
	private double verbWeight;
	private double matchThreshold;
	private double matchThresholdGlobal;
	private double evidenceThreshold;
	
	public ComparisonController(IntegrationController controller){
		this.controller = controller;
		this.verbWeight = controller.intConfig.getVerbWeight();
		this.matchThreshold = controller.intConfig.getMatchThreshold();
		this.evidenceThreshold = controller.intConfig.getEvidenceThreshold();
		this.matchThresholdGlobal = controller.intConfig.getMatchThresholdGlobal();
		linkResults = new LinkedList<LinkResult>();
	}
	
	public void compareSet(List<String> entities){
		uriToSource = new HashMap<String,String>();
		uriToLabel = new HashMap<String,String>();
		uriToSynsets =  new HashMap<String,Map<String,Set<String>>>();
		insertionOrder = new LinkedList<String>();
		compareSetRecursive(entities, 0);
	}
	public void compareSetRecursive(List<String> entities, int from){
		if(from + 2 > entities.size()) return;
		String firstEntity = entities.get(from);
		int i = from+1;
		while(i<entities.size()){
			comparePair(firstEntity, entities.get(i));
			i++;
		}
		compareSetRecursive(entities, from+1);
	}
	
	public void comparePair(String entitya, String entityb) {
		if(!uriToSource.containsKey(entitya)){
			QueryExecution execution = controller.sparql.query("PREFIX oa: <http://www.w3.org/ns/oa#> SELECT ?source ?label WHERE { "
					+ "?annotation oa:hasBody <"+entitya+"> . "
					+ "?annotation oa:hasTarget ?target . "
					+ "?target oa:hasSelector ?selector . "
					+ "?target oa:hasSource ?source . "
					+ "?selector oa:exact ?label . } LIMIT 1");
			//System.out.println(execution.getQuery().serialize());
			//execution.setTimeout(5000);
			ResultSet results = execution.execSelect();
			if(results.hasNext()){
				QuerySolution result = results.next();
				uriToSource.put(entitya, result.getResource("?source").getURI());
				uriToLabel.put(entitya, result.getLiteral("?label").getLexicalForm());
				
			}
			execution.close();
		}
		if(!uriToSource.containsKey(entityb)){
			QueryExecution execution = controller.sparql.query("PREFIX oa: <http://www.w3.org/ns/oa#> SELECT ?source ?label WHERE { "
					+ "?annotation oa:hasBody <"+entityb+"> . "
					+ "?annotation oa:hasTarget ?target . "
					+ "?target oa:hasSelector ?selector . "
					+ "?target oa:hasSource ?source . "
					+ "?selector oa:exact ?label . } LIMIT 1");
			//System.out.println(execution.getQuery().serialize());
			
			//execution.setTimeout(5000);
			ResultSet results = execution.execSelect();
			if(results.hasNext()){
				QuerySolution result = results.next();
				uriToSource.put(entityb, result.getResource("?source").getURI());
				uriToLabel.put(entityb, result.getLiteral("?label").getLexicalForm());
			}
			execution.close();
		}

		comparedAll++;
		if(uriToSource.get(entitya).equals(uriToSource.get(entityb))){
			//System.out.print(" ABORT(same source) ");
			aborted++;
			return;
		}
		comparedLevel1++;
		
		Map<String,Set<String>> synsetsa = null;
		Map<String,Set<String>> synsetsb = null;
		// Compute synset similarity
		if(!uriToSynsets.containsKey(entitya)){
			synsetsa = controller.textPreProcessing.processText(uriToLabel.get(entitya));
			uriToSynsets.put(entitya, synsetsa);
			insertionOrder.add(entitya);
		} else {
			synsetsa = uriToSynsets.get(entitya);
		}
		if(!uriToSynsets.containsKey(entityb)){
			synsetsb = controller.textPreProcessing.processText(uriToLabel.get(entityb));
			uriToSynsets.put(entityb, synsetsb);
			insertionOrder.add(entityb);
		} else {
			synsetsb = uriToSynsets.get(entityb);
		}
		if(synsetsa.size() == 0 || synsetsb.size() == 0){
			//System.out.print(" ABORT(same no valid synset) ");
			aborted++;
			return;
		}
		if(uriToSynsets.size()>maxCache){
			uriToSynsets.remove(insertionOrder.removeFirst());
		}
		Set<String> commonWordsa = new HashSet<String>();
		Set<String> commonWordsb = new HashSet<String>();
		Set<String> commonVerbsa = new HashSet<String>();
		Set<String> commonVerbsb = new HashSet<String>();
		int nounsinboth = 0;
		int verbsinboth = 0;
		Set<String> wordsa = new HashSet<String>();
		Set<String> wordsb = new HashSet<String>();
		Set<String> verbsa = new HashSet<String>();
		Set<String> verbsb = new HashSet<String>();
		
		for(String ina : synsetsa.keySet()){
			if(synsetsb.keySet().contains(ina)) {
				if(isVerb(ina)){
					commonVerbsa.addAll(synsetsa.get(ina));
					commonVerbsb.addAll(synsetsb.get(ina));
					verbsinboth++;
				} else {
					commonWordsa.addAll(synsetsa.get(ina));
					commonWordsb.addAll(synsetsb.get(ina));
					nounsinboth++;
				}
			}	
			if(isVerb(ina)){
				verbsa.addAll(synsetsa.get(ina));
			} else {
				wordsa.addAll(synsetsa.get(ina));				
			}
		}
		for(String inb : synsetsb.keySet()){
			if(isVerb(inb)){
				verbsb.addAll(synsetsb.get(inb));
			} else {
				wordsb.addAll(synsetsb.get(inb));				
			}
		}
		
		int numwordsa = wordsa.size();
		int numwordsb = wordsb.size();
		int numverbsa = verbsa.size();
		int numverbsb = verbsb.size();
		
		int numWordsInCommon = (commonWordsa.size()+commonWordsb.size())/2;
		int numVerbsInCommon = (commonVerbsa.size()+commonVerbsb.size())/2;
		
		double overlapa = ((double)commonWordsa.size()+((double)commonVerbsa.size()*verbWeight))/(numwordsa+(verbWeight*numverbsa));
		double overlapb = ((double)commonWordsb.size()+((double)commonVerbsb.size()*verbWeight))/(numwordsb+(verbWeight*numverbsb));			
		double globaloverlap = (2*((double)numWordsInCommon+((double)numVerbsInCommon*verbWeight)))/
				(numwordsa+numwordsb+(verbWeight*(numverbsa+numverbsb)));	
		
		boolean matchFound = false;
		double totalScore = 0;
		double maxSuper = -1;
		double maxSub = -1;
		double maxRequired = -1;
		double maxRequiredBy = -1;
		double averageSub = 0;
		double[] allScores = new double[6];
		allScores[0] = globaloverlap - (1/(1+numWordsInCommon+(numVerbsInCommon*verbWeight)));
		totalScore = Math.max(globaloverlap, Math.max(overlapa, overlapb)*0.66) - (1/(1+numWordsInCommon+(numVerbsInCommon*verbWeight)));
		allScores[1] = totalScore;
		if(totalScore > 0.3 && (numWordsInCommon+(numVerbsInCommon*verbWeight)) > 2.2){//globaloverlap > 0.2 && (overlapa > 0.4 || overlapb > 0.4) && (numWordsInCommon+(numVerbsInCommon*verbWeight)) > 1.1){
			if(globaloverlap > 0.5 && (overlapa > 0.9 || overlapb > 0.9) && (numWordsInCommon+(numVerbsInCommon*verbWeight)) > 2.5){
				//System.out.println(" - + MATCH FOUND");
				matchFound = true;
			} else {
				// CONTEXT - Supersteps
				Pair<Set<String>,Set<String>> superEntities = controller.getSuperEntities(entitya,entityb);
				if(superEntities != null){
					List<Double> superEntitiesScore = compareContextEntitiesAll(superEntities.getLeft(),superEntities.getRight());
					for(Double d : superEntitiesScore){
						if(d > maxSuper) maxSuper = d;
					}
					if(maxSuper > 0.3) totalScore = Math.min(1, totalScore+(maxSuper/5));
					allScores[2] = totalScore;
					if(totalScore > 0.55) matchFound = true;
				}
				// CONTEXT - Substeps
				if(!matchFound){
					Pair<Set<String>,Set<String>> subEntities = controller.getSubEntities(entitya,entityb);
					if(subEntities != null){
						List<Double> subEntitiesScore = compareContextEntitiesAll(subEntities.getLeft(),subEntities.getRight());
						for(Double d : subEntitiesScore){
							averageSub += d;
							if(d > maxSub) maxSub = d;
						}
						averageSub = averageSub/subEntitiesScore.size();
						if(maxSub > 0.3) totalScore = Math.min(1, totalScore+(maxSub/6)+averageSub);
						allScores[3] = totalScore;
						if(totalScore > 0.60) matchFound = true;
					}
				}
				if(!matchFound){
					Pair<Set<String>,Set<String>> reqEntities = controller.getRequiredEntities(entitya,entityb);
					if(reqEntities != null){
						List<Double> reqEntitiesScore = compareContextEntitiesAll(reqEntities.getLeft(),reqEntities.getRight());
						for(Double d : reqEntitiesScore){
							if(d > maxRequired) maxRequired = d;
						}
						if(maxRequired > 0.3) totalScore = Math.min(1, totalScore+(maxRequired/6));
						allScores[4] = totalScore;
						if(totalScore > 0.60) matchFound = true;
					}
				}
				if(!matchFound){
					Pair<Set<String>,Set<String>> reqByEntities = controller.getRequiredByEntities(entitya,entityb);
					if(reqByEntities != null){
						List<Double> reqByEntitiesScore = compareContextEntitiesAll(reqByEntities.getLeft(),reqByEntities.getRight());
						for(Double d : reqByEntitiesScore){
							if(d > maxRequiredBy) maxRequiredBy = d;
						}
						if(maxRequiredBy > 0.3) totalScore = Math.min(1, totalScore+(maxRequiredBy/6));
						allScores[5] = totalScore;
						if(totalScore > 0.60) matchFound = true;
					}
				}
				
				
			}
		} 
		if(matchFound){
			String description = "\n"+totalScore+" SCORE MATCH";
			for(int i = 0; i < allScores.length ; i++){
				description += ((Math.round(allScores[i]*100))+"%, ");
			}
			description+="\nFirst  from "+uriToSource.get(entitya)+" \n - Label: "+uriToLabel.get(entitya);
			description+="\nSecond from "+uriToSource.get(entityb)+" \n - Label: "+uriToLabel.get(entityb);
			description+="\n - - Total overlap: "+globaloverlap+" (first: "+overlapa+") (second: "+overlapb+")";
			description+="\n - - Evidence: overlap: "+(numWordsInCommon+(numVerbsInCommon*verbWeight))+" Actual words in common: "+numWordsInCommon+" verbs in common: "+numVerbsInCommon;
			//System.out.println(description);
			linkResults.add(new LinkResult(totalScore, entitya, entityb, 1, description));
			allmatched++;
			if(allmatched < 5 || allmatched == 20 || allmatched == 100 || allmatched % 1000 == 0){
				try {
					saveAllLinks();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//System.out.println(" - + MATCHED CONTEXT: MaxSuper: "+maxSuper+" MaxSub: "+averageSub+" AvgSub: "+maxSub);
		}
	}
	
	private void saveAllLinks() throws IOException{
		Collections.sort(linkResults);
		FileWriter wr = new FileWriter("LinkResults.txt");
		for(LinkResult result : linkResults){
			wr.write(result.toString());
		}
		wr.close();
	}
	
	public List<Double> compareContextEntitiesAll(Set<String> contexta, Set<String> contextb){
		List<Double> scoreIndexes = new LinkedList<Double>();
		for(String entitya : contexta){
			for(String entityb : contextb){
				Double score = compareContextEntities(entitya,entityb);
				if (score != null) scoreIndexes.add(score);
			}
		}
		return scoreIndexes;
	}
	public Double compareContextEntities(String entitya, String entityb){
		if(!uriToSource.containsKey(entitya)){
			QueryExecution execution = controller.sparql.query("PREFIX oa: <http://www.w3.org/ns/oa#> SELECT ?source ?label WHERE { "
					+ "?annotation oa:hasBody <"+entitya+"> . "
					+ "?annotation oa:hasTarget ?target . "
					+ "?target oa:hasSelector ?selector . "
					+ "?target oa:hasSource ?source . "
					+ "?selector oa:exact ?label . } LIMIT 1");
			ResultSet results = execution.execSelect();
			if(results.hasNext()){
				QuerySolution result = results.next();
				uriToSource.put(entitya, result.getResource("?source").getURI());
				uriToLabel.put(entitya, result.getLiteral("?label").getLexicalForm());
			}
			execution.close();
		}
		if(!uriToSource.containsKey(entityb)){
			QueryExecution execution = controller.sparql.query("PREFIX oa: <http://www.w3.org/ns/oa#> SELECT ?source ?label WHERE { "
					+ "?annotation oa:hasBody <"+entityb+"> . "
					+ "?annotation oa:hasTarget ?target . "
					+ "?target oa:hasSelector ?selector . "
					+ "?target oa:hasSource ?source . "
					+ "?selector oa:exact ?label . } LIMIT 1");
			ResultSet results = execution.execSelect();
			if(results.hasNext()){
				QuerySolution result = results.next();
				uriToSource.put(entityb, result.getResource("?source").getURI());
				uriToLabel.put(entityb, result.getLiteral("?label").getLexicalForm());
			}
			execution.close();
		}
		if(uriToSource.get(entitya).equals(uriToSource.get(entityb))){
			//System.out.print(" ABORT(same source) ");
			return null;
		}
		Map<String,Set<String>> synsetsa = null;
		Map<String,Set<String>> synsetsb = null;
		// Compute synset similarity
		if(!uriToSynsets.containsKey(entitya)){
			synsetsa = controller.textPreProcessing.processText(uriToLabel.get(entitya));
			uriToSynsets.put(entitya, synsetsa);
			insertionOrder.add(entitya);
		} else {
			synsetsa = uriToSynsets.get(entitya);
		}
		if(!uriToSynsets.containsKey(entityb)){
			synsetsb = controller.textPreProcessing.processText(uriToLabel.get(entityb));
			uriToSynsets.put(entityb, synsetsb);
			insertionOrder.add(entityb);
		} else {
			synsetsb = uriToSynsets.get(entityb);
		}
		if(synsetsa.size() == 0 || synsetsb.size() == 0){
			//System.out.print(" ABORT(same no valid synset) ");
			aborted++;
			return null;
		}
		if(uriToSynsets.size()>maxCache){
			uriToSynsets.remove(insertionOrder.removeFirst());
		}
		Set<String> commonWordsa = new HashSet<String>();
		Set<String> commonWordsb = new HashSet<String>();
		Set<String> commonVerbsa = new HashSet<String>();
		Set<String> commonVerbsb = new HashSet<String>();
		int nounsinboth = 0;
		int verbsinboth = 0;
		Set<String> wordsa = new HashSet<String>();
		Set<String> wordsb = new HashSet<String>();
		Set<String> verbsa = new HashSet<String>();
		Set<String> verbsb = new HashSet<String>();
		
		for(String ina : synsetsa.keySet()){
			if(synsetsb.keySet().contains(ina)) {
				if(isVerb(ina)){
					commonVerbsa.addAll(synsetsa.get(ina));
					commonVerbsb.addAll(synsetsb.get(ina));
					verbsinboth++;
				} else {
					commonWordsa.addAll(synsetsa.get(ina));
					commonWordsb.addAll(synsetsb.get(ina));
					nounsinboth++;
				}
			}	
			if(isVerb(ina)){
				verbsa.addAll(synsetsa.get(ina));
			} else {
				wordsa.addAll(synsetsa.get(ina));				
			}
		}
		for(String inb : synsetsb.keySet()){
			if(isVerb(inb)){
				verbsb.addAll(synsetsb.get(inb));
			} else {
				wordsb.addAll(synsetsb.get(inb));				
			}
		}
		
		int numwordsa = wordsa.size();
		int numwordsb = wordsb.size();
		int numverbsa = verbsa.size();
		int numverbsb = verbsb.size();
		
		int numWordsInCommon = (commonWordsa.size()+commonWordsb.size())/2;
		int numVerbsInCommon = (commonVerbsa.size()+commonVerbsb.size())/2;
		
		double overlapa = ((double)commonWordsa.size()+((double)commonVerbsa.size()*verbWeight))/(numwordsa+(verbWeight*numverbsa));
		double overlapb = ((double)commonWordsb.size()+((double)commonVerbsb.size()*verbWeight))/(numwordsb+(verbWeight*numverbsb));			
		double globaloverlap = (2*((double)numWordsInCommon+((double)numVerbsInCommon*verbWeight)))/
				(numwordsa+numwordsb+(verbWeight*(numverbsa+numverbsb)));	
		
		return new Double(Math.max(globaloverlap, Math.max(overlapa*0.66, overlapb*0.66)));
		
		//if(globaloverlap > 0.2 && (overlapa > 0.5 || overlapb > 0.5) && (numWordsInCommon+(numVerbsInCommon*verbWeight)) > 1.3){
	}
	
	private boolean isVerb(String synset){
		if(synset.charAt(synset.length()-1) == 'V') return true;
		else return false;
	}
	
	
}
