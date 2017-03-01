package integrationcore;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import plot.Jfreechartplotter;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.update.UpdateAction;

import query.SparqlManager;

public class LinksPostProcessing {

	private IntegrationController intControl;
	private SparqlManager links;
	private Map<String,String> topCategories;
	private int maxCategories;
	private Jfreechartplotter plotter;
	
	public LinksPostProcessing(IntegrationController intControl) {
		this.intControl = intControl;
		this.links = new SparqlManager(intControl);
		topCategories =  new HashMap<String,String>();
		loadTopCategories();
		plotter = new Jfreechartplotter(intControl.intConfig.getPlotDirectory());
	}
	
	/*private void loadTopCategories(){
		File topCategoriesMap = new File("TopCategoriesMap.txt");
		if(!topCategoriesMap.exists()) createTopCategoriesFile();
	}*/
	private void loadTopCategories(){
		String queryString = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>  "
				+ " SELECT DISTINCT ?sub ?top "+intControl.intConfig.getOntologyGraphQueryString()
				+ "WHERE { "
				+ " ?secondtop rdfs:subClassOf ?top . "
				+ " FILTER NOT EXISTS {?top rdfs:subClassOf ?supertop . } "
				+ " ?sub (rdfs:subClassOf)* ?top . "
				+ " }";
		QueryExecution execution = intControl.sparql.query(queryString);
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			QuerySolution result = results.next();
			String subClass = result.getResource("?sub").getURI();
			String secondTopClass = result.getResource("?top").getURI();
			topCategories.put(subClass, secondTopClass);
		}
		execution.close();
		Set<String> valueSet = new HashSet<String>();
		valueSet.addAll(topCategories.values());
		maxCategories = valueSet.size()+17;
		System.out.println(topCategories.size()+" categories assigned to "+valueSet.size()+" top categories.");
	}
	
	public void loadLinksFromDirectory(String pathToDirectory){
		links.setLocalDirectory(pathToDirectory);
	}
	
	protected void postProcess(double thresholdCategoryUp, double thresholdCategoryLower, double countUp, double countDown){
		System.out.println("LINKS POST PROCESSING - start");
		System.out.println("LINKS POST PROCESSING - Settings:");
		if(thresholdCategoryUp != Integer.MAX_VALUE) System.out.println("LINKS POST PROCESSING - Categories (upper bound)"+thresholdCategoryUp);
		if(thresholdCategoryLower != Integer.MIN_VALUE) System.out.println("LINKS POST PROCESSING - Categories (lower bound)"+thresholdCategoryLower);
		if(countUp != Integer.MAX_VALUE) System.out.println("LINKS POST PROCESSING - Occurrences (upper bound)"+countUp);
		if(countDown != Integer.MIN_VALUE) System.out.println("LINKS POST PROCESSING - Occurrences (lower bound)"+countDown);
		Set<String> linksToRemove = new HashSet<String>();
		String queryString = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> "
				+ " SELECT DISTINCT ?uri ( COUNT (distinct ?super) AS ?no ) "
				+ "WHERE { "
				+ "?super prohow:has_step ?uri ."
				+ " } GROUP BY ?uri ORDER BY DESC(?no)";
		
		QueryExecution execution = links.query(queryString);
		ResultSet results = execution.execSelect();
		int totLinksConsidered = 0;
		ArrayList<Float> occurrences = new ArrayList<Float>();
		ArrayList<Float> percentage = new ArrayList<Float>();
		ArrayList<Float> percentageSecondary = new ArrayList<Float>();
		ArrayList<Float> percentageAll = new ArrayList<Float>();
		while(results.hasNext()){
			totLinksConsidered++;
			QuerySolution result = results.next();
			//if(totLinksConsidered  > 5) continue;
			String entityUri = result.getResource("?uri").getURI();
			int count = result.getLiteral("?no").getInt();
			if(
					count > countUp
					|| count < countDown
					){
				linksToRemove.add(entityUri);
			} else {
				int categoriesMain = getNumberOfDifferentCategoriesWithFrequencyThreshold(entityUri,count)[2];
				int categoriesSecondary = getNumberOfDifferentCategoriesWithFrequencyThreshold(entityUri,count)[1];
				int categoriesAll = getNumberOfDifferentCategoriesWithFrequencyThreshold(entityUri,count)[0];
				System.out.println(totLinksConsidered+") main:"+Math.round(((double) categoriesMain*100)/(maxCategories))+"% "
						+ "sec.:"+Math.round(((double) categoriesSecondary*100)/(maxCategories))+"% "
						+ "all:"+Math.round(((double) categoriesAll*100)/(maxCategories))+"% <##> "
						+intControl.getLabel(entityUri)+" <##> ("+count+") TopCategories: "+getNumberOfDifferentCategories(entityUri)+" URI:"+entityUri);
				printRelatedSteps(entityUri, 5);
				
				if(
						Math.round(((double) categoriesSecondary*100)/(maxCategories)) > thresholdCategoryUp
						|| Math.round(((double) categoriesSecondary*100)/(maxCategories)) < thresholdCategoryLower
						){
					linksToRemove.add(entityUri);
				} else {
					occurrences.add((float) count);
					percentage.add((float) Math.round(((double) categoriesMain*100)/(maxCategories)));
					percentageSecondary.add((float) Math.round(((double) categoriesSecondary*100)/(maxCategories)));
					percentageAll.add((float) Math.round(((double) categoriesAll*100)/(maxCategories)));
				}
				
				if(totLinksConsidered == 10 || totLinksConsidered == 100 || totLinksConsidered % 1000 == 0) 
					plotter.plotScatterPlot("CategoryFrequencyDistribution", "total occurences", "% of main categories", occurrences.toArray(), percentage.toArray(),percentageSecondary.toArray(),percentageAll.toArray(), new String[]{"Main Categories","Secondary Categories","All Categories"});
			}
			
		}
		plotter.plotScatterPlot("CategoryFrequencyDistribution", "total occurences", "% of main categories", occurrences.toArray(), percentage.toArray(),percentageSecondary.toArray(),percentageAll.toArray(), new String[]{"Main Categories","Secondary Categories", "All Categories"});
		execution.close();
		
		for(String s : linksToRemove){
			links.update("PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> "
				+ " DELETE { ?super prohow:has_step <"+s+"> . } "
				+ " WHERE { ?super prohow:has_step <"+s+"> . } ");
		}
		
		try {
			links.writeModel("ProcessedLinks.ttl");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("LINKS POST PROCESSING - end");
	}
	
	
	private int getNumberOfDifferentCategories(String URI){
		String queryString = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ " SELECT distinct ?type "+intControl.intConfig.getNamedGraphsQueryString()
				+ " FROM <http://vocab.inf.ed.ac.uk/graph#wikihowlinkstemp> "
				+ " WHERE { "
				+ " { <"+URI+"> ^prohow:has_step  ?super . "
				+ " ?supersuper prohow:has_step ?super . "
				+ " ?supersuper rdf:type ?type . } UNION "
				+ " { <"+URI+"> ^prohow:has_step  ?super . "
				+ " ?supersuper prohow:has_step / prohow:has_step  ?super . "
				+ " ?supersuper rdf:type ?type . } UNION "
				+ " { <"+URI+"> ^prohow:has_step  ?super . "
				+ " ?supersuper prohow:has_method / prohow:has_step ?super . "
				+ " ?supersuper rdf:type ?type . } "
				+ " } ";
		QueryExecution execution = intControl.sparql.query(queryString);
		ResultSet results = execution.execSelect();
		Set<String> matchedTypes = new HashSet<String>();
		while(results.hasNext()){
			QuerySolution result = results.next();
			String type = result.getResource("?type").getURI();
			if(topCategories.containsKey(type)) matchedTypes.add(topCategories.get(type));
			else matchedTypes.add(type);
		}
		execution.close();
		return matchedTypes.size();
	}
	
	/**
	 * returns an array of three integers
	 * [0] all categories
	 * [1] secondary categories (at least 1/10th of average distribution)
	 * [2] main categories (at least 1/4th of average distribution)
	 * @param URI
	 * @return
	 */
	private int[] getNumberOfDifferentCategoriesWithFrequencyThreshold(String URI, int frequency){
		String queryString = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ " SELECT distinct ?type "+intControl.intConfig.getNamedGraphsQueryString()
				+ " FROM <http://vocab.inf.ed.ac.uk/graph#wikihowlinkstemp> "
				+ " WHERE { "
				+ " { <"+URI+"> ^prohow:has_step  ?super . "
				+ " ?supersuper prohow:has_step ?super . "
				+ " ?supersuper rdf:type ?type . } UNION "
				+ " { <"+URI+"> ^prohow:has_step  ?super . "
				+ " ?supersuper prohow:has_step / prohow:has_step  ?super . "
				+ " ?supersuper rdf:type ?type . } UNION "
				+ " { <"+URI+"> ^prohow:has_step  ?super . "
				+ " ?supersuper prohow:has_method / prohow:has_step ?super . "
				+ " ?supersuper rdf:type ?type . } "
				+ " } ";
		QueryExecution execution = intControl.sparql.query(queryString);
		ResultSet results = execution.execSelect();
		Map<String,Integer> matchedTypes = new HashMap<String,Integer>();
		while(results.hasNext()){
			QuerySolution result = results.next();
			String type = result.getResource("?type").getURI();
			if(topCategories.containsKey(type)) {
				if(matchedTypes.containsKey(topCategories.get(type))){
					matchedTypes.put(topCategories.get(type),new Integer(matchedTypes.get(topCategories.get(type))+1));
				} else {
					matchedTypes.put(topCategories.get(type),new Integer(1));
				}
			} else {
				if(matchedTypes.containsKey(type)){
					matchedTypes.put(type,new Integer(matchedTypes.get(type)+1));
				} else {
					matchedTypes.put(type,new Integer(1));
				}
			}
		}
		execution.close();
		int frequencyThresholdSecondary = Math.max(Math.min(2, frequency-1), (int) Math.round(frequency/(Math.max(1,maxCategories*5))));
		int frequencyThresholdMain = Math.max(Math.min(2, frequency-1), (int) Math.round(frequency/(Math.max(1,maxCategories*2))));
		int aboveThreshold[] = new int[3];
		for(Integer i : matchedTypes.values()){
			aboveThreshold[0] = aboveThreshold[0] +1;
			if(i >= frequencyThresholdSecondary) aboveThreshold[1] = aboveThreshold[1]+1;
			if(i >= frequencyThresholdMain) aboveThreshold[2] = aboveThreshold[2]+1;
		}
		
		return aboveThreshold;
	}
	
	private void printRelatedSteps(String uri, int limit){
		String queryString = "PREFIX oa: <http://www.w3.org/ns/oa#> PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#> "
				+ " SELECT DISTINCT ?uri "
				+ "WHERE { "
				+ "?uri prohow:has_step <"+uri+"> ."
				+ " } LIMIT "+limit;
		
		QueryExecution execution = links.query(queryString);
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			QuerySolution result = results.next();
			String entityUri = result.getResource("?uri").getURI();
			System.out.println(" - - "+intControl.getLabel(entityUri)+" ["+entityUri+"]");
		}
		execution.close();
	}

}
