package evaluation;

import integrationcore.IntegrationController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import query.SparqlManager;

public class LinksEvaluation {

	// These are, among the generated links, those which we should consider
	//private Set<Link> automaticallyGeneratedLinks;
	
	/*private Set<Link> correctLinks;
	private Set<Link> wrongLinks;*/
	private String pathToCorrectLinks;
	private String pathToWrongLinks;
	private SparqlManager positive = new SparqlManager();
	private SparqlManager negative = new SparqlManager();
	private SparqlManager found = new SparqlManager();
	private SparqlManager dbpedia = new SparqlManager();
	private Set<String> sourcesToConsider = new HashSet<String>();
	private Map<String,String> urisToConsider = new HashMap<String,String>();
	private Map<String,String> urisToConsiderOutput = new HashMap<String,String>();
	private Set<Link> linksToCheck = new HashSet();
	private IntegrationController controller;
	private String evaluationType;
	
	private Set<String> alreadyAnnotatedURIs = new HashSet<String>();
	
	private boolean isDecompositionEvaluation = false;
	private boolean isDBpediaEvaluation = false;
	
	public LinksEvaluation(String pathToAutomaticLinks, String pathToCorrectLinks, String pathToWrongLinks, IntegrationController controller, String evaluationType) {
		this.controller = controller;
		this.evaluationType = evaluationType;
		if(evaluationType.equals("DBpedia Links")) isDBpediaEvaluation = true;
		else if(evaluationType.equals("Decomposition Links")) isDecompositionEvaluation = true;
		else throw new RuntimeException("ERROR evaluation parameter: evaluation type has not been set correctly. Was expecting one of 'DBpedia Links', 'Decomposition Links'");
		//automaticallyGeneratedLinks = selectAutomaticallyGeneratedLinks(pathToAutomaticLinks);
		this.pathToCorrectLinks = pathToCorrectLinks;
		this.pathToWrongLinks = pathToWrongLinks;
		System.out.println("EVALUATION - DL - Loading automatically found links from "+pathToAutomaticLinks);
		found.setLocalDirectory(pathToAutomaticLinks);
		System.out.println("EVALUATION - DL - Loading positive links from "+pathToCorrectLinks);
		positive.setLocalDirectory(pathToCorrectLinks);
		System.out.println("EVALUATION - DL - Loading negative links from "+pathToWrongLinks);
		negative.setLocalDirectory(pathToWrongLinks);
		System.out.println("EVALUATION - DL - Computing annotated URLs - start");
		initializeURLsToConsider(positive);
		initializeURLsToConsider(negative);
		dbpedia.setRemoteEndpoint("http://localhost:8890/sparql");
		System.out.println("EVALUATION - DL - Computing annotated URLs - The links belong to "+sourcesToConsider.size()+" different URLs ");
		System.out.println("EVALUATION - DL - Computing annotated URLs - end ");
		
		
		if(isDBpediaEvaluation){
			{
				String queryString = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ " SELECT DISTINCT ?s "
						+ " WHERE {  ?s rdf:type ?o . }";
				QueryExecution execution = positive.query(queryString);
				ResultSet results = execution.execSelect();
				while(results.hasNext()){
					QuerySolution result = results.next();
					String entity = result.getResource("?s").getURI();
					alreadyAnnotatedURIs.add(entity);
				}
				execution.close();
			}
			{
				String queryString = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ " SELECT DISTINCT ?s "
						+ " WHERE {  ?s rdf:type ?o . }";
				QueryExecution execution = negative.query(queryString);
				ResultSet results = execution.execSelect();
				while(results.hasNext()){
					QuerySolution result = results.next();
					String entity = result.getResource("?s").getURI();
					alreadyAnnotatedURIs.add(entity);
				}
				execution.close();
			}
			System.out.println("ENTITIES ALREADY ANNOTATED: "+alreadyAnnotatedURIs.size());
		}
		
				if(controller.intConfig.getNewURLsToEvaluate()>0){
					addNewSources(controller.intConfig.getNewURLsToEvaluate());
				}
	}
	
	private void reloadEvaluationCorpus(){
		System.out.println("EVALUATION - refreshing the evaluation corpus - start");
		positive = new SparqlManager();
		negative = new SparqlManager();
		positive.setLocalDirectory(pathToCorrectLinks);
		negative.setLocalDirectory(pathToWrongLinks);
		//initializeURLsToConsider(positive);
		//initializeURLsToConsider(negative);
		System.out.println("EVALUATION - refreshing the evaluation corpus - finish");
	}
	
	private void initializeURLsToConsider(SparqlManager sparql){
		String queryString = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ " SELECT DISTINCT ?uri "
				+ " WHERE { ";
		if(evaluationType.equals("DBpedia Links")) queryString = queryString+ " ?uri rdf:type ?decomposedUri . }";
		else if(evaluationType.equals("Decomposition Links")) queryString = queryString+ " ?uri prohow:has_step ?decomposedUri . }";
		
		QueryExecution execution = sparql.query(queryString);
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			QuerySolution result = results.next();
			sourcesToConsider.add(extractSourceFromURI(result.getResource("?uri").getURI()));
		}
		execution.close();
	}
	
	public void calculatePrecisionAndRecall(){
		long foundNumber = found.utilityGetModelSize();
		long positiveNumber = positive.utilityGetModelSize();
		long negativeNumber = negative.utilityGetModelSize();
		
		long inPositive = 0;
		long notInPositive = 0;
		long inNegative = 0;
		long notInNegative = 0;
		
		// Iterate through all the automatically generated links
		String queryString = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ " SELECT DISTINCT ?s ?o "
				+ " WHERE { ";
		if(evaluationType.equals("DBpedia Links")) queryString = queryString+ " ?s rdf:type ?o . }";
		else if(evaluationType.equals("Decomposition Links")) queryString = queryString + " ?s prohow:has_step ?o . }";
		QueryExecution execution = found.query(queryString);
		ResultSet results = execution.execSelect();
		int linksfound = 0;
		while(results.hasNext()){
			linksfound++;
			QuerySolution result = results.next();
			String entity = result.getResource("?s").getURI();
			String target = result.getResource("?o").getURI();
			String entitySource = extractSourceFromURI(entity);
			if(sourcesToConsider.contains(entitySource)){
				boolean isInPositive = isLinkInModel(entity,target,positive);
				boolean isInNegative = isLinkInModel(entity,target,negative);
				if(isInPositive) inPositive++;
				else notInPositive++;
				if(isInNegative) inNegative++;
				else notInNegative++;
				if(isInPositive && isInNegative){
					System.out.println("ERROR, a link was found both as positive and negative set");
					System.out.println("ERROR Link: <"+entity+"> -- <"+target+">");
				}
				if(!isInPositive && !isInNegative){
					if(evaluationType.equals("DBpedia Links")) linksToCheck.add(new Link(entity, "rdf:type",  target));
					else if(evaluationType.equals("Decomposition Links")) linksToCheck.add(new Link(entity, "prohow:has_step",  target));
					//TODO remove the 0.33 chance match
					if(controller.intConfig.isRestrictEvaluationToOneNewLinkPerURL() && Math.random() > 0.33){sourcesToConsider.remove(entitySource);}
				}
			}
		}
		System.out.println("EVALUATION - All links found: "+linksfound+" Positive links: "+positiveNumber+" Negative links: "+negativeNumber);
		execution.close();
		System.out.println(" ###### RESULTS ######");
		System.out.println("Number of URLs considered: "+sourcesToConsider.size());
		System.out.println("Number of unidentified links: "+linksToCheck.size());
		System.out.println("Positive set: [IN:"+inPositive+" OUT:"+notInPositive+"]");
		System.out.println("Negative set: [IN:"+inNegative+" OUT:"+notInNegative+"]");
		System.out.println("Precision: "+ ((double) inPositive)/((double) inPositive+notInPositive)  );
		System.out.println("Recall: "+((double) inPositive)/((double) positiveNumber) );
	}
	
	public void addNewSources(int howMany){
		if(isDBpediaEvaluation){
			Map<String,String> allSources = new HashMap<String,String>();
			Map<String,String> allSourcesOutput = new HashMap<String,String>();
			
			System.out.println("EVALUATION - Adding "+howMany+" new DBpedia links");
			int sourcesAdded=0;
			{
				// Iterate through all the automatically generated links
				String queryString = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ " SELECT DISTINCT ?s ?o"
						+ " WHERE {  ?s rdf:type ?o . ?s prohow:has_method ?m . } LIMIT "+howMany*100;
				QueryExecution execution = found.query(queryString);
				ResultSet results = execution.execSelect();
				while(results.hasNext()){
					QuerySolution result = results.next();
					String entity = result.getResource("?s").getURI();
					String type = result.getResource("?o").getURI();
					allSourcesOutput.put(entity,type);
					
				}
				execution.close();
			}
			
			System.out.println("EVALUATION - Adding "+howMany+" new DBpedia links");
			{
				// Iterate through all the automatically generated links
				String queryString = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ " SELECT DISTINCT ?s ?o"
						+ " WHERE {  ?s rdf:type ?o . FILTER NOT EXISTS { ?s prohow:has:method ?m . } } LIMIT "+howMany*100;
				QueryExecution execution = found.query(queryString);
				ResultSet results = execution.execSelect();
				while(results.hasNext()){
					QuerySolution result = results.next();
					String entity = result.getResource("?s").getURI();
					String type = result.getResource("?o").getURI();
					if(entity.indexOf("=mainentity&t") == -1) allSources.put(entity,type);
					
				}
				execution.close();
			}
			// REQUIREMENTS
			for(int i = 0; i<howMany; i++){
				
					int item = new Random().nextInt(allSources.size())-1;
					int index = 0;
					for(String s : allSources.keySet()) {
					    if (index == item){
					    	if(!urisToConsider.containsKey(s) && !alreadyAnnotatedURIs.contains(s)){
					    		urisToConsider.put(s,allSources.get(s));
					    		sourcesAdded++;
					    	} else {
					    		i--;
					    	}
					    	break;
					    } 
					    index++;
					}
				
				
			}
			// OUTPUTS
			for(int i = 0; i<howMany; i++){
				int item = new Random().nextInt(allSourcesOutput.size())-1;
				int index = 0;
				for(String s : allSourcesOutput.keySet()) {
				    if (index == item){
				    	if(!urisToConsiderOutput.containsKey(s) && !alreadyAnnotatedURIs.contains(s)){
				    		urisToConsiderOutput.put(s,allSourcesOutput.get(s));
				    		sourcesAdded++;
				    	} else {
				    		i--;
				    	}
				    	break;
				    } 
				    index++;
				}
			
		}
			
			System.out.println("EVALUATION - "+sourcesAdded+" URIS to consider have been added to the manual annotation pool.");
		} else {
			System.out.println("EVALUATION - Adding "+howMany+" URLs to consider to the manual annotation pool.");
			int sourcesAdded=0;
			// Iterate through all the automatically generated links
			String queryString = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ " SELECT DISTINCT ?s "
					+ " WHERE { ";
			if(evaluationType.equals("DBpedia Links")) queryString = queryString+ " ?s rdf:type ?o . }";
			else if(evaluationType.equals("Decomposition Links")) queryString = queryString + " ?s prohow:has_step ?o . }";
			Set<String> allSources = new HashSet<String>();
			QueryExecution execution = found.query(queryString);
			ResultSet results = execution.execSelect();
			while(results.hasNext()){
				QuerySolution result = results.next();
				String entity = result.getResource("?s").getURI();
				String entitySource = extractSourceFromURI(entity);
				allSources.add(entitySource);
				
			}
			execution.close();
			for(int i = 0; i<howMany; i++){
				int item = new Random().nextInt(allSources.size());
				int index = 0;
				for(String s : allSources) {
					if (index == item) sourcesToConsider.add(s);
					index++;
					sourcesAdded++;
				}
			}
			System.out.println("EVALUATION - "+sourcesAdded+" URLs to consider have been added to the manual annotation pool.");
			
		}
	}
	
	private boolean isLinkInModel(String s, String o, SparqlManager model){
		String queryString = "PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";
		if(evaluationType.equals("DBpedia Links")) queryString = queryString + " ASK { <"+s+"> rdf:type <"+o+"> . }";
		else if(evaluationType.equals("Decomposition Links")) queryString = queryString + " ASK { <"+s+"> prohow:has_step <"+o+"> . }";
		
		QueryExecution execution = model.query(queryString);
		boolean isFound = execution.execAsk();
		execution.close();
		return isFound;
	}
	
	private String extractSourceFromURI(String URI){
		return URI.substring(URI.indexOf("#?url=")+6, URI.lastIndexOf("&t="));//
	}
	
	public void manuallyAnnotateUnkownLinks() throws IOException{
		if(isDBpediaEvaluation){
			System.out.println("###########################################");
			System.out.println("### Annotation started");
			System.out.println("### y - yes the link is positive");
			System.out.println("### n - no the link is not positive");
			System.out.println("### q - quit");
			System.out.println("### anything else - skip");
			System.out.println("###########################################");
			int iteration = 0;
			int linksAnnotatedAsCorrect = 0;
			int linksAnnotatedAsWrong = 0;
			int linksAnnotatedAsCorrectOutput = 0;
			int linksAnnotatedAsWrongOutput = 0;
			int skipped = 0;
			boolean quit = false; 
			System.out.println("\n\nANNOTATE OUTPUTS:\n\n");
			for(String s : urisToConsiderOutput.keySet()){
				if(!quit){
					iteration++;
					boolean isOutput = true;
					String subjectLabel = controller.getLabelOutput(s);
					if(subjectLabel == null){
						subjectLabel = controller.getLabel(s);
						isOutput = false;
						
					}
					Link newLinkToSave = new Link(s,"rdf:type",urisToConsiderOutput.get(s));
					String subjectURL = extractSourceFromURI(s);
					String objectLabel = urisToConsiderOutput.get(s);
					String objectURL = urisToConsiderOutput.get(s);
						objectLabel = getDBpediaAbstract(objectURL);
					System.out.println("object:"+s);
					System.out.println("type:"+urisToConsiderOutput.get(s));
					System.out.println("_________________________________________________");
					System.out.println("["+iteration+"/"+urisToConsiderOutput.keySet().size()+"]");
					System.out.println(subjectLabel);
					System.out.println("("+subjectURL+")");
					System.out.println(" | ");
					System.out.println(" | ");
					if(isOutput) System.out.println(" GENERATES THE FOLLOWING OUTPUT ");
					else System.out.println(" IS (OR CONTAINS) THIS TYPE ");
					System.out.println(" | ");
					System.out.println(" V ");
					System.out.println(objectLabel);
					System.out.println("("+objectURL+")");
					BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
					String input = bufferRead.readLine();
					if(input.equals("y")){
						System.out.println(" YES ");
						writeLink(newLinkToSave, true,true);
						linksAnnotatedAsCorrectOutput++;
					} else if(input.equals("n")){
						System.out.println(" NO ");
						writeLink(newLinkToSave, false,true);
						linksAnnotatedAsWrongOutput++;
					} else if(input.equals("q")){
						quit = true;
					}else {
						System.out.println(" SKIP ");
						skipped++;
					}
				}
			}
			iteration = 0;
			System.out.println("\n\nANNOTATE REQUIREMENTS:\n\n");
			for(String s : urisToConsider.keySet()){
				if(!quit){
					iteration++;
					boolean isOutput = true;
					String subjectLabel = controller.getLabelOutput(s);
					if(subjectLabel == null){
						subjectLabel = controller.getLabel(s);
						isOutput = false;
						
					}
					Link newLinkToSave = new Link(s,"rdf:type",urisToConsider.get(s));
					String subjectURL = extractSourceFromURI(s);
					String objectLabel = urisToConsider.get(s);
					String objectURL = urisToConsider.get(s);
						objectLabel = getDBpediaAbstract(objectURL);
					System.out.println("object:"+s);
					System.out.println("type:"+urisToConsider.get(s));
					System.out.println("_________________________________________________");
					System.out.println("["+iteration+"/"+urisToConsider.keySet().size()+"]");
					System.out.println(subjectLabel);
					System.out.println("("+subjectURL+")");
					System.out.println(" | ");
					System.out.println(" | ");
					if(isOutput) System.out.println(" GENERATES THE FOLLOWING OUTPUT ");
					else System.out.println(" IS (OR CONTAINS) THIS TYPE ");
					System.out.println(" | ");
					System.out.println(" V ");
					System.out.println(objectLabel);
					System.out.println("("+objectURL+")");
					BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
					String input = bufferRead.readLine();
					if(input.equals("y")){
						System.out.println(" YES ");
						writeLink(newLinkToSave, true);
						linksAnnotatedAsCorrect++;
					} else if(input.equals("n")){
						System.out.println(" NO ");
						writeLink(newLinkToSave, false);
						linksAnnotatedAsWrong++;
					} else if(input.equals("s")){
						System.out.println(" SKIP ");
						skipped++;
					}else quit = true;
				}
			} 
			
			System.out.println("###########################################");
			System.out.println("### Annotation finished, you have annotated "+linksAnnotatedAsCorrect+"+ "+linksAnnotatedAsWrong+"- (requirements) and "+linksAnnotatedAsCorrectOutput+"+ "+linksAnnotatedAsWrongOutput+"- (outputs) links out of "+urisToConsider.keySet().size()+". (skipped: "+skipped+")");
			System.out.println("### The precision of the annotated links (OUTPUTS) was: "+(double)linksAnnotatedAsCorrectOutput/(linksAnnotatedAsCorrectOutput+linksAnnotatedAsWrongOutput));
			System.out.println("### The precision of the annotated links (REQUIREMENTS) was: "+(double)linksAnnotatedAsCorrect/(linksAnnotatedAsCorrect+linksAnnotatedAsWrong));
			reloadEvaluationCorpus();
		} else {
			System.out.println("###########################################");
			System.out.println("### Annotation started");
			System.out.println("### y - yes the link is positive");
			System.out.println("### n - no the link is not positive");
			System.out.println("### s - skip");
			System.out.println("### anything else - quit");
			System.out.println("###########################################");
			int numberOfLinksToCheck = linksToCheck.size();
			int iteration = 0;
			int linksAnnotatedAsCorrect = 0;
			int linksAnnotatedAsWrong = 0;
			int skipped = 0;
			boolean quit = false;
			for(Link link : linksToCheck){
				if(!quit){
					iteration++;
					String subjectLabel = controller.getLabel(link.getSubject());
					if(subjectLabel==null){
						subjectLabel = controller.getLabel(getRelatedURI(link.getSubject()));
						if(subjectLabel ==  null){
							subjectLabel = "ERROR, label not found for: "+link.getSubject();
						} else {
							subjectLabel = "[decomposed from:] "+subjectLabel;
						}
					}
					String subjectURL = extractSourceFromURI(link.getSubject());
					String objectLabel = link.getObject();
					String objectURL = link.getObject();
					if(evaluationType.equals("Decomposition Links")){
						objectLabel = controller.getLabel(link.getObject());
						objectURL = extractSourceFromURI(link.getObject());
					} else if(evaluationType.equals("DBpedia Links")) {
						objectLabel = getDBpediaAbstract(link.getObject());
					}
					System.out.println("subj:"+link.getSubject());
					System.out.println("obj:"+link.getObject());
					System.out.println("_________________________________________________");
					System.out.println("["+iteration+"/"+numberOfLinksToCheck+"]");
					System.out.println(subjectLabel);
					System.out.println("("+subjectURL+")");
					System.out.println(" | ");
					System.out.println(" V ");
					System.out.println(objectLabel);
					System.out.println("("+objectURL+")");
					BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
					String input = bufferRead.readLine();
					if(input.equals("y")){
						System.out.println(" YES ");
						writeLink(link, true);
						linksAnnotatedAsCorrect++;
					} else if(input.equals("n")){
						System.out.println(" NO ");
						writeLink(link, false);
						linksAnnotatedAsWrong++;
					} else if(input.equals("s")){
						System.out.println(" SKIP ");
						skipped++;
					}else quit = true;
				}
			}
			System.out.println("###########################################");
			System.out.println("### Annotation finished, you have annotated "+(linksAnnotatedAsCorrect+linksAnnotatedAsWrong)+" links out of "+numberOfLinksToCheck+". (skipped: "+skipped+")");
			System.out.println("### The precision of the annotated links was: "+(double)linksAnnotatedAsCorrect/(linksAnnotatedAsCorrect+linksAnnotatedAsWrong));
			reloadEvaluationCorpus();
		}
	}
	
	private void writeLink(Link l, boolean positive) throws IOException{
		writeLink(l,positive,false);
	}
	
	private void writeLink(Link l, boolean positive, boolean outputs) throws IOException{
		String writer = pathToCorrectLinks+"ManualPositive.ttl";
		if(!positive) writer = pathToWrongLinks+"ManualNegative.ttl";
		if(outputs){
			writer = pathToCorrectLinks+"ManualPositiveOutput.ttl";
			if(!positive) writer = pathToWrongLinks+"ManualNegativeOutput.ttl";
		}
		PrintWriter out;
		out = new PrintWriter(new BufferedWriter(new FileWriter(writer, true)));
		out.println(" <"+l.getSubject()+"> "+l.getLinkType()+" <"+l.getObject()+"> . ");
		out.close();
	}
	
	
	/**
	 * Utility method to generate a random set of pages to consider 
	 * In pathToPagesToConsider, write #numberOfPages of randomly selected uris from allUris
	 * @param allUris the set of all entities URIs
	 * @param pathToPagesToConsider the file where to write the random URLs
	 * @param numberOfPages the number of random URLs to write
	 */
	public static void generateRandomSetOfWebPages(Set<String> allUris, String pathToPagesToConsider, int numberOfPages){
		//TODO
	}
	
	public String getRelatedURI(String URI){
		QueryExecution execution = found.query(" PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> SELECT ?relatedUri "
				+ " WHERE {"
				+ " { ?relatedUri prohow:has_step <"+URI+"> } "
				+ " UNION "
				+ " { ?relatedUri prohow:has_method <"+URI+"> } "
				+ " UNION "
				+ " { <"+URI+"> prohow:has_method ?relatedUri } "
				+ " } ");
		ResultSet results = execution.execSelect();
		if(results.hasNext()){
			QuerySolution result = results.next();
			return result.getResource("?relatedUri").getURI();
		}
		execution.close();
		return null;
	}
	
	/*
	 * Return the abstract if available, otherwise the label, otherwise just the URI
	 */
	public String getDBpediaAbstract(String URI){
		QueryExecution execution = dbpedia.query("select ?abstract FROM <http://dbpedia.org> where "
				+ "{<"+URI+"> <http://dbpedia.org/ontology/abstract> ?abstract . "
				+ "FILTER(langMatches(lang(?abstract ), \"EN\")) } LIMIT 1");
		ResultSet results = execution.execSelect();
		if(results.hasNext()){
			QuerySolution result = results.next();
			return result.getLiteral("?abstract").getLexicalForm();
		}
		execution.close();
		execution = dbpedia.query("select ?label FROM <http://dbpedia.org> where "
				+ "{<"+URI+"> <http://www.w3.org/2000/01/rdf-schema#label> ?label . "
				+ "FILTER(langMatches(lang(?label ), \"EN\")) } LIMIT 1");
		results = execution.execSelect();
		if(results.hasNext()){
			QuerySolution result = results.next();
			return result.getLiteral("?label").getLexicalForm();
		}
		execution.close();
		return URI;
	}

}
