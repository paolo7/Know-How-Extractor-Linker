package integrationcore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import plot.Jfreechartplotter;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.UpdateAction;

import query.SparqlManager;

public class DBpediaLinksPostProcessing {

	private String pathToLinks;
	private String pathToPostProcessedDirectory;
	private IntegrationController intControl;
	private Set<String> blockedTypes = new HashSet();
	private Set<String> allowedTypes = new HashSet();
	private SparqlManager dbpedia = new SparqlManager();
	
	public DBpediaLinksPostProcessing(IntegrationController intControl, String pathToLinks, String pathToPostProcessedDirectory) {
		this.intControl = intControl;
		this.pathToLinks = pathToLinks;
		this.pathToPostProcessedDirectory = pathToPostProcessedDirectory;
		dbpedia.setRemoteEndpoint("http://localhost:8890/sparql");//("http://dbpedia.org/sparql");
	}
	
	public void postProcess(){
		File dir = new File(pathToLinks);
		if (!dir.isDirectory()) {
			intControl.log(1,"ERROR - The specified path to the data directory ("+dir+") is not a directory.");
			throw new RuntimeException("The specified path to the data directory ("+dir+") is not a directory.");
		}
		for (File child : dir.listFiles()) {
			intControl.log(5000,"Loading file "+child.getAbsolutePath());
			String extension = FilenameUtils.getExtension(child.getAbsolutePath());
			if(extension.equals("ttl")) {
				postProcess(child);
			}
		  }
		
		
		
	}
		
	private void postProcess(File turtleFile){		
		SparqlManager links =  new SparqlManager(intControl);
		links.loadModelFromFile(turtleFile);
		// FIND ALL TYPES
		Set<String> types = new HashSet<String>();
		String queryString = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
				+ " SELECT DISTINCT ?type "
				+ "WHERE { "
				+ " ?a rdf:type ?type . "
				+ " }";
		QueryExecution execution = links.query(queryString);
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			QuerySolution result = results.next();
			types.add(result.getResource("?type").getURI());
		}
		execution.close();
		intControl.log(100,types.size()+" types were found");
		System.out.println();
		System.out.println("Blocking: ");
		// FIND ALL TYPES TO REMOVE
		for(String s : types){
			if(!blockedTypes.contains(s) && !allowedTypes.contains(s)){
				if(hasDBpediaAbstract(s)) allowedTypes.add(s);
				else {
					blockedTypes.add(s);
					System.out.print(s+" ");
				}
			}
		}
		System.out.println();
		intControl.log(5000,blockedTypes.size()+ " TYPES FLAGGED FOR REMOVAL");
		// REMOVE ALL URIS CONNECTED WITH THE TYPES TO REMOVE
		Set<String> toRemove = new HashSet<String>();
		queryString = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
				+ " SELECT DISTINCT ?a ?type "
				+ "WHERE { "
				+ " ?a rdf:type ?type . "
				+ " }";
		execution = links.query(queryString);
		results = execution.execSelect();
		int linksRemoved = 0;
		while(results.hasNext()){
			QuerySolution result = results.next();
			if(blockedTypes.contains(result.getResource("?type").getURI())){
				toRemove.add(result.getResource("?a").getURI());
			}
		}
		execution.close();
		for(String s : toRemove){
			removeLinks(links,s);
			linksRemoved++;
		}
		// SAVE THE RESULTING FILE
		try {
			links.writeModel(pathToPostProcessedDirectory+turtleFile.getName());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		intControl.log(5000,"Saved file to "+pathToPostProcessedDirectory+turtleFile.getName());
		intControl.log(5000,linksRemoved+ " links removed.");
	}
	
	private void removeLinks(SparqlManager linksModel, String linkURI){
		linksModel.update("PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> "
				+ " DELETE { ?a ?b <"+linkURI+"> . } "
				+ " WHERE { ?a ?b <"+linkURI+"> . } ");
		linksModel.update("PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> "
				+ " DELETE { <"+linkURI+"> ?b ?a . } "
				+ " WHERE { <"+linkURI+"> ?b ?a . } ");
	}
	
	public boolean hasDBpediaAbstract(String URI){
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

}
