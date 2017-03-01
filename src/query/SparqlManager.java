package query;

import integrationcore.IntegrationController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.UpdateAction;

import dbpediaLinks.Logger;

public class SparqlManager {

	private Model rdfModel = null;
	private String service = null;
	private Logger controller;
	
	private class StandardLogger implements Logger{
		@Override
		public void log(String s) {
			System.out.println(s);
		}
		@Override
		public void log(int i, String s) {
			System.out.println(s);
		}
	}
	
	public SparqlManager(){
		this.controller = new StandardLogger();
	}
	
	public SparqlManager(Logger logger){
		this.controller = logger;
	}
	
	public QueryExecution query(String query){
		if(rdfModel != null) {
			return QueryExecutionFactory.create(query, rdfModel);
		} else if(service != null) {
			return QueryExecutionFactory.sparqlService(service, query);
		} else {
			controller.log(1,"ERROR - Cannot execute the query. No triplestore/endpoint has been configured.");
			return null;
		} 
	}
	
	public void setLocalDirectory(String directoryPath){
		Model rdfModel = loadModelFromDirectory(directoryPath);
		if(rdfModel != null){
			this.rdfModel = rdfModel;
			service = null;
			controller.log(1000,"CONNECTED TO RDF - Using local rdf files at "+directoryPath);
		} else {
			controller.log(1,"ERROR - Failed to load rdf model from local directory");
		}
	}
	
	public void loadModelFromFile(File file){
		this.rdfModel = ModelFactory.createDefaultModel();
		String extension = FilenameUtils.getExtension(file.getAbsolutePath());
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			controller.log(5000,"ERROR - FileNotFoundException for file: "+file.getAbsolutePath());
			return;
		}
		String extensionName = null;
		if(extension.equals("ttl")) {
			extensionName = "TURTLE";
		} else {
			controller.log(5000,"ERROR - unrecognised extension for file "+file.getAbsolutePath());
			return;
		}
		this.rdfModel.read(inputStream, null, extensionName);
		controller.log(5000,"Finished loading data. Model has size: "+this.rdfModel.size());
		
	}
	
	public void setRemoteEndpoint(String endpointURL){
		//TODO check that endpointURL is good before switching
		if(endpointURL != null){
			service = endpointURL;
			rdfModel = null;
			controller.log(1000,"CONNECTED TO RDF - Using SPARQL endpoint "+endpointURL);
		} else {
			controller.log(1,"ERROR - Unable to connect to endpoint. Endpoint URL is null");
		}
	}
	
	public void utilityBreakIntoSmallerChunks(String pathToDataDirectory, String pathToDestinationDirectory,int mbMaxSize) throws IOException{
		File dir = new File(pathToDataDirectory);
		if (!dir.isDirectory()) throw new RuntimeException("The specified path to the data directory ("+dir+") is not a directory.");
		String newLine = System.getProperty("line.separator");
		for (File child : dir.listFiles()) {
			controller.log(100,"Breaking file "+child.getAbsolutePath()+" into chunks");
			int chunkFileNumber = 1;
			File outputFile = new File(pathToDestinationDirectory+chunkFileNumber+child.getName());
			FileWriter writer = new FileWriter(outputFile);
			
			BufferedReader br = new BufferedReader(new FileReader(child));
			String line;
			while ((line = br.readLine()) != null) {
				if(outputFile.exists() && outputFile.length() > (mbMaxSize*1000000)){
					controller.log(100," - new file chunk for: "+child.getAbsolutePath());
					writer.close();
					chunkFileNumber++;
					outputFile = new File(pathToDestinationDirectory+chunkFileNumber+child.getName());
					writer = new FileWriter(outputFile);
				}
				writer.write(line+newLine);
			}
			writer.close();
			br.close();
			
			
			
		  }
		controller.log(10,"Finished prefixing data");
	}
	public void utilityAddPrefixes(String pathToDataDirectory, String pathToDestinationDirectory, String pathToTurtlePrefixes) throws IOException{
		File dir = new File(pathToDataDirectory);
		if (!dir.isDirectory()) throw new RuntimeException("The specified path to the data directory ("+dir+") is not a directory.");
		for (File child : dir.listFiles()) {
			FileInputStream inputStream;
			try {
				controller.log(100,"Appending prefixes to file "+child.getAbsolutePath());
				String extension = FilenameUtils.getExtension(child.getAbsolutePath());
				inputStream = new FileInputStream(child);
				String extensionName = null;
				if(extension.equals("ttl")) {
					extensionName = "TURTLE";
					// Prepend a prefix file if necessary
					if(pathToTurtlePrefixes != null){
						OutputStream writer = new FileOutputStream(pathToDestinationDirectory+child.getName());
						//OutputStreamWriter stream = new OutputStreamWriter(writer);
						FileReader prefixReader = new FileReader(new File(pathToTurtlePrefixes));
						FileReader dataReader = new FileReader(child);
						IOUtils.copy(prefixReader, writer);
						IOUtils.copy(dataReader, writer);
					}
				}
				if(extensionName == null){
					controller.log(5, " - ERROR, file not prefixed. Extension: '"+extension+"' not recognised");
				} else {					
					//rdfData.read(inputStream, null, extensionName);
					controller.log(100," - "+extensionName+" file ("+extension+") prefixed");
				}
			} catch (FileNotFoundException e) {
				controller.log("Error reading file: "+child.getAbsolutePath());
			}
		  }
		controller.log(10,"Finished prefixing data");
	}
	
	public void utilityToRDFXML(String pathToXMLfile) throws IOException{
		controller.log(1,"UTILITY: Converting model to RDF/XML in file "+pathToXMLfile+" START");
		FileOutputStream out = new FileOutputStream(pathToXMLfile); 
		try{
			
			RDFDataMgr.write(out, rdfModel, RDFFormat.RDFXML) ;
		} catch (com.hp.hpl.jena.shared.CannotEncodeCharacterException e){
			e.printStackTrace();
		}
		controller.log(1,"UTILITY: Converting model to RDF/XML in file "+pathToXMLfile+" FINISHED");
	}
	
	public void forgetAllModels(){
		rdfModel = null;
		service = null;
	}

	private Model loadModelFromDirectory(String pathToDataDirectory){
		Model rdfData = ModelFactory.createDefaultModel();
		File dir = new File(pathToDataDirectory);
		if (!dir.isDirectory()) {
			controller.log(1,"ERROR - The specified path to the data directory ("+dir+") is not a directory.");
			throw new RuntimeException("The specified path to the data directory ("+dir+") is not a directory.");
		}
		for (File child : dir.listFiles()) {
			InputStream inputStream;
			try {
				controller.log(5000,"Loading file "+child.getAbsolutePath());
				String extension = FilenameUtils.getExtension(child.getAbsolutePath());
				inputStream = new FileInputStream(child);
				String extensionName = null;
				if(extension.equals("ttl")) {
					extensionName = "TURTLE";
					// Prepend a prefix file if necessary
					//if(intConfig.getPathToTurtlePrefixes() != null){
					//	inputStream = new SequenceInputStream(new FileInputStream(new File(intConfig.getPathToTurtlePrefixes())), inputStream);
					//}
				}
				if(extensionName == null){
					controller.log(5, " - ERROR, file not loaded. Extension: '"+extension+"' not recognised");
				} else {		
					rdfData.read(inputStream, null, extensionName);
					controller.log(6000," - "+extensionName+" file ("+extension+") loaded successfully, now model has size: "+rdfData.size());
				}
			} catch (FileNotFoundException e) {
				controller.log("Error reading file: "+child.getAbsolutePath());
			}
		  }
		controller.log(5000,"Finished loading data. Model has size: "+rdfData.size());
		return rdfData;
	}
	
	public long utilityGetModelSize(){
		return rdfModel.size();
	}
	
	public void update(String updatequery){
		UpdateAction.parseExecute(updatequery, rdfModel);
	}
	
	public void writeModel(String path) throws FileNotFoundException{
		FileOutputStream file = new FileOutputStream(new File(path));
		RDFDataMgr.write(file, rdfModel, Lang.TURTLE) ;
	}


}
