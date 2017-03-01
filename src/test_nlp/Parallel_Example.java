package test_nlp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import Data.WebLabel;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import query.SparqlManager;
import dbpediaLinks.SimpleLogger;

public class Parallel_Example {
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
	public static void main(String[] args) {
		long time=System.currentTimeMillis();
		String query = prefixes+"SELECT DISTINCT ?entityuri ?exact  "
				+ "  WHERE { "
				+ " ?main prohow:has_step ?entityuri ."
				+ " FILTER "
				+ " NOT EXISTS { ?supermain prohow:has_step ?main } "
				+ " ?annotation oa:hasBody ?entityuri . "
				+ "  ?annotation oa:hasTarget ?target . "
				+ "	?target oa:hasSource ?source . "
				+ "	?target oa:hasSelector ?selector . "
				+ "	?selector oa:exact ?exact . }";
		SimpleLogger logger = new SimpleLogger();
		SparqlManager sparqlSource = new SparqlManager(logger);
		sparqlSource.setLocalDirectory("dataset");
		QueryExecution exec_ingredients = sparqlSource.query(query);
		ResultSet results = exec_ingredients.execSelect();
		NLP_BOX parser;
		int count=0;
		try {
			parser = new NLP_BOX();

			FileWriter fw = new FileWriter("D:/JenaExtraction/nlp_extraction", false);
			BufferedWriter output = new BufferedWriter(fw);
			// paralelization begins
			int max_thread=20;
			ArrayBlockingQueue<Runnable> blockingqueue = new ArrayBlockingQueue<Runnable>(max_thread);
			ThreadPoolExecutor executor = new ThreadPoolExecutor(max_thread/4,max_thread,1,TimeUnit.SECONDS,blockingqueue);
			while(results.hasNext()) {
				QuerySolution result = results.next();
				String entityuri = result.getResource("?entityuri").getURI();
				String label = result.getLiteral("?exact").getLexicalForm();
				count++;
				WebLabel lbl=new WebLabel(label,entityuri);
				while(blockingqueue.remainingCapacity() < 1){
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				executor.execute(new ExampleThread(parser, lbl,output));
			}
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.SECONDS);
			output.flush();
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		exec_ingredients.close();
	    System.out.println("executed in "+(System.currentTimeMillis()-time)/count+" ms per label");
	}
}