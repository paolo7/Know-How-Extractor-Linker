package luceneIndexing;

import integrationcore.IntegrationConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nlp.TextProcessingController;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import edu.stanford.nlp.util.StringUtils;
import activityRecognition.ActionScore;
import plot.Jfreechartplotter;
import query.SparqlManager;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.ThresholdSelector;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class LuceneIndexQuery {
	
	private class StandardizedDefaultSimilarity extends DefaultSimilarity{
		
		public float idf(long docFreq, long numDocs){
			return 1f;
		}
		
		public float tf(float freq){
			return 1f;
		}
	}

	private SparqlManager sparql;
	private SparqlManager evaluationsparql;
	private String indexTargetPath;
	private String indexSourcePath;
	protected IndexSearcher targetSearcher;
	protected IndexSearcher sourceSearcher;
	protected IndexSearcher contextSearcher;
	private Analyzer analyzer;
	private int maxResults;
	private int maxSentencesToConsider;
	private Date startDate = null;
	protected TextProcessingController textPreProcessing;
	private int scoreStatisticsScale = 100;
	private int[] scoreStatistics = new int[scoreStatisticsScale];
	
	protected int marginOfScoreNotToIncludeInSet = 0;
	protected int fractionOfStatisticsSets = 5;
	protected Set<String> connectedEntities;
	protected Set<String> stepEntities;
	protected Set<String> decomposedStepEntities;
	protected int stepEntitiesLinks = 0;
	//private Set<String>[] scoreStatisticsInterconnectedSets = new Set[scoreStatisticsScale/fractionOfStatisticsSets];
	protected int[] scoreStatisticsTotal = new int[scoreStatisticsScale];
	protected float thresholdOfAcceptance = 0.0f;
	private Integer entitiesConsidered = Integer.valueOf(0);

	protected int similarEntitiesInSameSource = 0;
	protected String[] fileNames = new String[]{"linksWRONG","Links1","Links2","Links3","Links4","Links5","Links6","Links7","Links8","Links9","Links10","Links11"};
	// SIMILARITY BOOSTS
	// Sentence order
	protected float[] sentenceImportanceScaling = new float[]{1f,0.6f,0.4f,0.3f,0.2f,0.1f};
	private float[] wordsImportanceScaling = new float[]{1f,0.3f,0.2f,0.1f,0.02f};
	private float[] distanceBoost = new float[]{1f,0.9f,0.8f,0.7f,0.6f,0.5f,0.4f,0.3f,0.2f,0.1f};
	// Synsets vs actual words
	private float synsetBoost = 1f;
	//private float wordBoost = 0.5f;
	// Nouns vs verbs vs others (adjectives and adverbs)
	private float nounBoost = 2f;
	private float verbBoost = 1f;
	private float otherBoost = 0.8f;
	// Full label
	// Full label should be the least discriminant factor, so its importance is scaled according to the least important feature
	private float fullLabelBoost = 1f;//getSenteceOrderBoost(2)*otherBoost*0.2f;
	private Jfreechartplotter plotter;
	protected IntegrationConfiguration intConfig;
	//private IndexSearcher utilityLabelSearcher;
	protected String pathToResultsDirectory;
	protected int[] maxResultsToConsider = new int[]{50,500,5000,50000};
	protected Set<String> consideredURIs = new HashSet<String>();
	// sets the maximum distance in position allowed to a similar word in order to be considered.
	// for example, if we are looking for the word "eat" in position 3, we will only consider matches with other words "eat"
	// found in positions 0 to 3+wordWindowBuffer
	private int wordWindowBuffer = 5;
	protected boolean sourceEqualsTarget = false;
	protected TFIDFSimilarity similarity = new DefaultSimilarity();
	protected Instances data;
	protected FastVector attVals;
	protected FastVector attVals2;
	protected FastVector atts;
	protected Map<String,Boolean> evaluationMap = new HashMap<String,Boolean>();
	protected Map<String,Set<String>> evaluationMapPositive = new HashMap<String,Set<String>>();
	protected Map<String,Set<String>> evaluationMapNegative = new HashMap<String,Set<String>>();
	private Map<String,Set<String>> userGeneratedConnections = new HashMap<String,Set<String>>();
	private Classifier threshold = (Classifier) new ThresholdSelector();
	private Classifier threshold1 = (Classifier) new ThresholdSelector();
	private Classifier threshold2 = (Classifier) new ThresholdSelector();
	private Classifier threshold3 = (Classifier) new ThresholdSelector();
	private Classifier threshold4 = (Classifier) new ThresholdSelector();
	private Classifier threshold5 = (Classifier) new ThresholdSelector();
	private Classifier threshold6 = (Classifier) new ThresholdSelector();
	private Classifier threshold7 = (Classifier) new ThresholdSelector();
	private Classifier threshold8 = (Classifier) new ThresholdSelector();
	private Classifier threshold9 = (Classifier) new ThresholdSelector();
	private Integer allMLmatches = 0;
	private int trueIndex = 0;
	
	public LuceneIndexQuery(TextProcessingController textPreProcessing, SparqlManager sparql, int maxResults, int maxSentencesToConsider, IntegrationConfiguration intConfig) throws IOException {
		/*for(int i = 0; i < scoreStatisticsInterconnectedSets.length; i++){
			scoreStatisticsInterconnectedSets[i] = new HashSet<String>();
		}*/
		connectedEntities = new HashSet<String>();
		stepEntities = new HashSet<String>();
		decomposedStepEntities = new HashSet<String>();
		this.textPreProcessing = textPreProcessing;
		this.sparql = sparql;
		evaluationsparql = new SparqlManager();
		evaluationsparql.setLocalDirectory(intConfig.getEvaluationFolderUserGenerated());
		loadUserGeneratedLinks();
		this.indexTargetPath = intConfig.getPathToLuceneTargetIndex();
		this.indexSourcePath = intConfig.getPathToLuceneSourceIndex();
		if(indexTargetPath.equals(indexSourcePath)) sourceEqualsTarget = true;
		this.maxResults = maxResults;
		this.maxSentencesToConsider = maxSentencesToConsider;
		this.intConfig = intConfig;
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexTargetPath)));
		targetSearcher = new IndexSearcher(reader);
		if(indexSourcePath != null && !indexSourcePath.equals(indexTargetPath)){
			IndexReader sourceReader = DirectoryReader.open(FSDirectory.open(new File(indexSourcePath)));
			sourceSearcher = new IndexSearcher(sourceReader);
		} else {
			sourceSearcher = targetSearcher;
		}
		IndexReader contextReader = DirectoryReader.open(FSDirectory.open(new File(intConfig.getPathToLuceneContextIndex())));
		contextSearcher = new IndexSearcher(contextReader);
		//sourceSearcher.setSimilarity(new StandardizedDefaultSimilarity());
		//targetSearcher.setSimilarity(new StandardizedDefaultSimilarity());
		plotter = new Jfreechartplotter(intConfig.getPlotDirectory());
		pathToResultsDirectory = intConfig.getKnowHowLinksResultsDirectory();
		//analyzer = new StandardAnalyzer(Version.LUCENE_48);
		/*IndexReader utilityLabelReader = DirectoryReader.open(FSDirectory.open(new File("D:\\DataDirectory\\LuceneIndexes\\IndexOnLabels")));
		utilityLabelSearcher = new IndexSearcher(utilityLabelReader);*/

		if(intConfig.isDoMachineLearning()) {
			 
		     FastVector      attsRel;
		     
		     FastVector      attValsRel;
		     Instances       dataRel;
		     
		     double[]        valsRel;
		     int             i;
		 
		     // 1. set up attributes
		     atts = new FastVector();
		     // - numeric
		     atts.addElement(new Attribute("orderCorrect"));
		     //atts.addElement(new Attribute("orderMissing"));
		     //atts.addElement(new Attribute("orderForward"));
		     //atts.addElement(new Attribute("orderBackwards"));
		     atts.addElement(new Attribute("orderForwardAll"));
		     //atts.addElement(new Attribute("orderBackwardsAll"));
		     //atts.addElement(new Attribute("orderCorrectNorm"));
		     atts.addElement(new Attribute("orderMissingNorm"));
		     atts.addElement(new Attribute("orderForwardNorm"));
		     atts.addElement(new Attribute("orderBackwardsNorm"));
		     atts.addElement(new Attribute("orderForwardAllNorm"));
		     atts.addElement(new Attribute("orderBackwardsAllNorm"));
		     atts.addElement(new Attribute("idf"));
		     atts.addElement(new Attribute("idfnormalized"));
		     atts.addElement(new Attribute("maxCoreToCoreOverlap"));
		     atts.addElement(new Attribute("maxContextToCoreOverlap"));
		     atts.addElement(new Attribute("bestidf"));
		     //atts.addElement(new Attribute("lucenescore"));
		     //atts.addElement(new Attribute("lucenescorenormalized"));
		     atts.addElement(new Attribute("exactlabelmatch"));
		     atts.addElement(new Attribute("occurrences"));
		     atts.addElement(new Attribute("categories0"));
		     atts.addElement(new Attribute("categories1"));
		     atts.addElement(new Attribute("categories2"));
		     atts.addElement(new Attribute("wordlengthsentences"));
		     atts.addElement(new Attribute("wordlength"));
		     atts.addElement(new Attribute("offsetOfFirstWordOccurrence"));
		     atts.addElement(new Attribute("offsetFirstMatchInSentence"));
		     atts.addElement(new Attribute("offsetFirstMatchAndPhrase"));
		     //atts.addElement(new Attribute("offsetOfFirstWordOccurrencePhrases"));
		     atts.addElement(new Attribute("numberOfSharedCategories"));
		     /*atts.addElement(new Attribute("contextCoreToCore"));
		     atts.addElement(new Attribute("contextAllToAll"));
		     atts.addElement(new Attribute("contextCoreToCoreNormalized"));
		     atts.addElement(new Attribute("contextAllToAllNormalized"));*/
		     atts.addElement(new Attribute("commonWordslabelTOotherContext"));
		     atts.addElement(new Attribute("commonWordsotherlabelTOcontext"));
		     // - nominal
		     attVals = new FastVector();
		       attVals.addElement("true");
		       attVals.addElement("false");
		     attVals2 = new FastVector();
		       attVals2.addElement("true");
		       attVals2.addElement("unknown");
		       attVals2.addElement("false");
		     atts.addElement(new Attribute("hasverb", attVals));
		     atts.addElement(new Attribute("hasUserGeneratedLink", attVals2));
		     atts.addElement(new Attribute("isUserGeneratedDecomposition", attVals2));
		     atts.addElement(new Attribute("sameNegationCore", attVals));
		     //atts.addElement(new Attribute("hasSubjectCore", attVals));
		     //atts.addElement(new Attribute("hasSubjectCoreOther", attVals));
		     //atts.addElement(new Attribute("hassamenegationvalues", attVals));
		     atts.addElement(new Attribute("hashowto", attVals));
		     atts.addElement(new Attribute("sameExactCategory", attVals));
		     atts.addElement(new Attribute("sameTopCategory", attVals));
		     atts.addElement(new Attribute("iswikihow", attVals));
		     atts.addElement(new Attribute("iswikihowother", attVals));
		     atts.addElement(new Attribute("target", attVals));
		     trueIndex = attVals.indexOf("true");
		     // 2. create Instances object
		     data = new Instances("Dataset", atts, 0);
		     data.setClassIndex(data.attribute("target").index());

		}
	}
	
	private void loadUserGeneratedLinks(){
		QueryExecution execution = evaluationsparql.query(
				" PREFIX prohow: <http://vocab.inf.ed.ac.uk/prohow#>  "
				+ " SELECT DISTINCT ?a ?b "
				+ " WHERE {"
				+ " ?a prohow:has_step ?b . "
				+ " } ");
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			QuerySolution result = results.next();
			String superstep = extractSourceFromURI(result.getResource("?a").getURI());
			String substep = extractSourceFromURI(result.getResource("?b").getURI());
			if(userGeneratedConnections.containsKey(substep)){
				userGeneratedConnections.get(substep).add(superstep);
			} else {
				Set<String> decompositions = new HashSet<String>();
				decompositions.add(superstep);
				userGeneratedConnections.put(substep, decompositions);
			}
		}
		execution.close();
	}
	
	protected boolean areURLsUserConnected(String URLsub, String URLsuper){
		return userGeneratedConnections.containsKey(URLsub) && userGeneratedConnections.get(URLsub).contains(URLsuper);
	}
	
	protected boolean isUserGeneratedDecomposition(String URL){
		return userGeneratedConnections.containsKey(URL);
	}

	public void testTrainClassifiers(){
		try {
			System.out.println("\nSTART CLASSIFIER TRAINING\nDATAPOINTS: "+data.numInstances());
			
			
			threshold = (Classifier) new ThresholdSelector();
			threshold.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.95 "+intConfig.getClassfierSettings()));
			threshold.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval = new Evaluation(data);
			eval.crossValidateModel(threshold, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 10");
			System.out.println(eval.toSummaryString());
			System.out.println(eval.toClassDetailsString());
			
			threshold1 = (Classifier) new ThresholdSelector();
			threshold1.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.9 "+intConfig.getClassfierSettings()));
			threshold1.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval1 = new Evaluation(data);
			eval1.crossValidateModel(threshold1, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 9");
			System.out.println(eval1.toClassDetailsString());
			
			threshold2 = (Classifier) new ThresholdSelector();
			threshold2.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.85 "+intConfig.getClassfierSettings()));
			threshold2.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval2 = new Evaluation(data);
			eval2.crossValidateModel(threshold2, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 8");
			System.out.println(eval2.toClassDetailsString());
			
			threshold3 = (Classifier) new ThresholdSelector();
			threshold3.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.8 "+intConfig.getClassfierSettings()));
			threshold3.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval3 = new Evaluation(data);
			eval3.crossValidateModel(threshold3, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 7");
			System.out.println(eval3.toClassDetailsString());
			
			threshold4 = (Classifier) new ThresholdSelector();
			threshold4.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.75 "+intConfig.getClassfierSettings()));
			threshold4.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval4 = new Evaluation(data);
			eval4.crossValidateModel(threshold4, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 6");
			
			System.out.println(eval4.toClassDetailsString());
			
			threshold5 = (Classifier) new ThresholdSelector();
			threshold5.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.7 "+intConfig.getClassfierSettings()));
			threshold5.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval5 = new Evaluation(data);
			eval5.crossValidateModel(threshold5, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 5");
			System.out.println(eval5.toClassDetailsString());
			
			threshold6 = (Classifier) new ThresholdSelector();
			threshold6.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.65 "+intConfig.getClassfierSettings()));
			threshold6.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval6 = new Evaluation(data);
			eval6.crossValidateModel(threshold6, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 4");
			System.out.println(eval6.toClassDetailsString());
			
			threshold7 = (Classifier) new ThresholdSelector();
			threshold7.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.6 "+intConfig.getClassfierSettings()));
			threshold7.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval7 = new Evaluation(data);
			eval7.crossValidateModel(threshold7, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 3");
			System.out.println(eval7.toClassDetailsString());
			
			threshold8 = (Classifier) new ThresholdSelector();
			threshold8.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.55 "+intConfig.getClassfierSettings()));
			threshold8.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval8 = new Evaluation(data);
			eval8.crossValidateModel(threshold8, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 2");
			System.out.println(eval8.toClassDetailsString());
			
			threshold9 = (Classifier) new ThresholdSelector();
			threshold9.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.5 "+intConfig.getClassfierSettings()));
			threshold9.buildClassifier(data);
			// 10 fold cross validation
			Evaluation eval9 = new Evaluation(data);
			eval9.crossValidateModel(threshold9, data, 10, new Random(1));
			System.out.println("\nCLASSIFIER 1");
			System.out.println(eval9.toSummaryString());
			System.out.println(eval9.toClassDetailsString());
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	public void trainClassifiers(){
		try {
			threshold = (Classifier) new ThresholdSelector();
			threshold.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.95 "+intConfig.getClassfierSettings()));
			threshold.buildClassifier(data);
			
			threshold1 = (Classifier) new ThresholdSelector();
			threshold1.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.9 "+intConfig.getClassfierSettings()));
			threshold1.buildClassifier(data);
			
			threshold2 = (Classifier) new ThresholdSelector();
			threshold2.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.85 "+intConfig.getClassfierSettings()));
			threshold2.buildClassifier(data);
			
			threshold3 = (Classifier) new ThresholdSelector();
			threshold3.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.8 "+intConfig.getClassfierSettings()));
			threshold3.buildClassifier(data);
			
			threshold4 = (Classifier) new ThresholdSelector();
			threshold4.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.75 "+intConfig.getClassfierSettings()));
			threshold4.buildClassifier(data);
			
			threshold5 = (Classifier) new ThresholdSelector();
			threshold5.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.7 "+intConfig.getClassfierSettings()));
			threshold5.buildClassifier(data);
			
			threshold6 = (Classifier) new ThresholdSelector();
			threshold6.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.65 "+intConfig.getClassfierSettings()));
			threshold6.buildClassifier(data);
			
			threshold7 = (Classifier) new ThresholdSelector();
			threshold7.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.6 "+intConfig.getClassfierSettings()));
			threshold7.buildClassifier(data);
			
			threshold8 = (Classifier) new ThresholdSelector();
			threshold8.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.55 "+intConfig.getClassfierSettings()));
			threshold8.buildClassifier(data);
			
			threshold9 = (Classifier) new ThresholdSelector();
			threshold9.setOptions(weka.core.Utils.splitOptions("weka.classifiers.meta.ThresholdSelector -manual 0.5 "+intConfig.getClassfierSettings()));
			threshold9.buildClassifier(data);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	protected boolean classifyInstance(Instance i){
		i.setDataset(data);
		try {
			return threshold.classifyInstance(i) == trueIndex;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	// returns 0 for a no-match, any other integer is a match, with higher integer for higher confidence
	protected int rankInstance(Instance i){
		i.setDataset(data);
		try {
			if(threshold.classifyInstance(i) == trueIndex) return 10;
			if(threshold1.classifyInstance(i) == trueIndex) return 9;
			if(threshold2.classifyInstance(i) == trueIndex) return 8;
			if(threshold3.classifyInstance(i) == trueIndex) return 7;
			if(threshold4.classifyInstance(i) == trueIndex) return 6;
			if(threshold5.classifyInstance(i) == trueIndex) return 5;
			if(threshold6.classifyInstance(i) == trueIndex) return 4;
			if(threshold7.classifyInstance(i) == trueIndex) return 3;
			if(threshold8.classifyInstance(i) == trueIndex) return 2;
			if(threshold9.classifyInstance(i) == trueIndex) return 1;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	public void printData(){
		System.out.println("Total number of datapoints: "+allMLmatches);
		System.out.println();
		//System.out.println(data);
		try {
			//Files.delete("WekaTrainingSet.arff");
			new File("WekaTrainingSet.arff").delete();
			PrintWriter out = new PrintWriter("WekaTrainingSet.arff");
			out.println(data.toString());
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void setEvaluationMap(Map<String,Boolean> evaluationMap){
		this.evaluationMap = evaluationMap;
	}
	public void setEvaluationMapPositive(Map<String, Set<String>> evaluationMap){
		this.evaluationMapPositive = evaluationMap;
	}
	public void setEvaluationMapNegative(Map<String, Set<String>> evaluationMap){
		this.evaluationMapNegative = evaluationMap;
	}
	protected Integer getEntitiesConsidered() {
		return entitiesConsidered;
	}
	
	protected synchronized void setEntitiesConsidered(Integer entitiesConsidered) {
		this.entitiesConsidered = entitiesConsidered;
	}
	
	protected synchronized void writeLine(String filename, String line){
		File writeFile = new File(pathToResultsDirectory+filename);
		
		if(!writeFile.exists()){
			line = " @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . @prefix prohow: <http://vocab.inf.ed.ac.uk/prohow#> . @prefix madph: <http://vocab.inf.ed.ac.uk/madprohow#> . @prefix owl: <http://www.w3.org/2002/07/owl#> . @prefix oa: <http://www.w3.org/ns/oa#> . @prefix dctypes: <http://purl.org/dc/dcmitype/> . @prefix foaf: <http://xmlns.com/foaf/0.1/> . @prefix skos: <http://www.w3.org/2004/02/skos/core#> . @prefix xsd: <http://www.w3.org/2001/XMLSchema#> . "+ line;
		}
		
		PrintWriter out;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(pathToResultsDirectory+filename, true)));
			out.println(line);
			out.close();
		} catch (IOException e) {
			System.out.println("WARNING saving a link to file failed. Trying again in 2 milliseconds.");
			writeLine( filename, line);
		}
	}
	
	protected float getSenteceOrderBoost(int order){
		if(order >= 0 && order < sentenceImportanceScaling.length){
			return sentenceImportanceScaling[order];
		}
		return sentenceImportanceScaling[sentenceImportanceScaling.length-1];
	}
	
	protected float getDistanceBoost(int distance){
		if(distance >= 0 && distance < distanceBoost.length){
			return distanceBoost[distance];
		} else if (distance < 0) {
			return distanceBoost[0];
		} else return distanceBoost[distanceBoost.length-1];
	}
	
	protected float getWordOrderBoost(int order){
		if(order/2 >= 0 && order/2 < wordsImportanceScaling.length){
			return wordsImportanceScaling[order/2];
		}
		return wordsImportanceScaling[wordsImportanceScaling.length-1];
	}
	
	public void printSimilarityMatchStatistics() {
		if(intConfig.isDoMachineLearning()) return;
		for(int i = 0 ; i < scoreStatistics.length; i++  ){
			scoreStatistics[i] = /*scoreStatisticsInterconnectedSets[i/fractionOfStatisticsSets]*/connectedEntities.size();
		}
		Date nowDate = new Date();
		System.out.println();
		System.out.println("STATISTICS #################################################################");
		System.out.println(nowDate+" Entities considered: "+entitiesConsidered+". Milliseconds per entity: "+(((double) nowDate.getTime()-startDate.getTime())/(entitiesConsidered))+". Total duration in seconds: "+((nowDate.getTime()-startDate.getTime())/1000)+". Total duration in minutes: "+((nowDate.getTime()-startDate.getTime())/60000)+". Links discarded because entities are in the same source: "+similarEntitiesInSameSource);
		System.out.println(decomposedStepEntities.size()+" entities decomposed into "+stepEntities.size()+" entities (links: "+stepEntitiesLinks+")");
		/*for (int i = 0 ; i < scoreStatistics.length; i++){
			System.out.println(i+") "+((((double)scoreStatistics[i])/entitiesConsidered)*100)+"% [entities linked: "+scoreStatistics[i]+" [total links: "+scoreStatisticsTotal[i]+"]");
			
		}
		float[] xvalues = new float[scoreStatistics.length-marginOfScoreNotToIncludeInSet];
		float[] yvalues = new float[scoreStatistics.length-marginOfScoreNotToIncludeInSet];
		// Create the plot
		for (int i = marginOfScoreNotToIncludeInSet ; i < scoreStatistics.length; i++){
			xvalues[i] = i;
			yvalues[i] = ((((float)scoreStatistics[i])/2521097f)*100);
		}
		plotter.plotXYLine("LinksDistribution-"+entitiesConsidered+"entities", "% of similarity to original", "% of entities with links", xvalues, yvalues);*/
		System.out.println("STATISTICS #################################################################");
		System.out.println();
	}
	
/*	public void query(String queryString) throws IOException, ParseException{
		QueryParser parser = new QueryParser(Version.LUCENE_48, "label", analyzer);
		Query query = parser.parse(queryString);
		TopScoreDocCollector collector = TopScoreDocCollector.create(maxResults, true);
		searcher.search(query, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		//System.out.println();
		//System.out.println("Query String: " + queryString);
		for (int i = 0; i < hits.length; i++) {
		    Document hitDoc = searcher.doc(hits[i].doc);  // getting actual document
		    hitDoc.get("uri");
		    //System.out.println("URI: " + hitDoc.get("uri"));
		}
	}*/
	
	public String extractSourceFromURI(String URI){
		return URI.substring(URI.indexOf("#?url=")+6, URI.lastIndexOf("&t="));//
	}
	
	protected String[] splitBySpace(String s){
		if(s == null) return new String[0];
		return s.replaceAll("-", "").replaceAll("[^0-9A-Za-z-]+", " ").split("\\s+");
	}
	
	protected synchronized void addStepEntity(String uri){
		stepEntities.add(uri);
	}
	protected synchronized void addDecomposedStepEntities(String uri){
		decomposedStepEntities.add(uri);
	}
	
	/**
	 * This method contains the logic about deciding to block some labels which are not likely to give us good links
	 * For example labels which are too short or do not contain verbs
	 * @param label
	 * @return
	 */
	protected boolean isBlockedLabel(String label){
		label = label.toLowerCase();
		if(label.indexOf("how to") != -1) return false;
		if(label.trim().length() < 3) return true;
		if(!textPreProcessing.containsVerb(label)) return true;
		String lowerCaseLabel = label.toLowerCase();
		if(containsMainly(lowerCaseLabel,"alternate method")) return true;
		if(containsMainly(lowerCaseLabel,"enjoy")) return true;
		if(containsMainly(lowerCaseLabel,"do")) return true;
		if(containsMainly(lowerCaseLabel,"done")) return true;
		if(containsMainly(lowerCaseLabel,"before you start")) return true;
		if(containsMainly(lowerCaseLabel,"before you begin")) return true;
		if(containsMainly(lowerCaseLabel,"begin")) return true;
		if(containsMainly(lowerCaseLabel,"start")) return true;
		if(containsMainly(lowerCaseLabel,"getting ready")) return true;
		if(containsMainly(lowerCaseLabel,"get ready")) return true;
		return false;
	}
	
	private boolean containsMainly(String container, String content){
		int indexOfOccurence = container.indexOf(content);
		if((indexOfOccurence == -1) || (indexOfOccurence > 3) || (indexOfOccurence < container.length()-content.length()-4)) return false;
		return true;
	}

	public boolean isMainEntityURI(String uri){
		return uri.indexOf("mainentity") > 0;
	}
	
	protected boolean isTrustedSimilarity(String firstLabel, String secondLabel){
		
		if(firstLabel.toLowerCase().indexOf("how to") == 0){
			return true;
		} else return false;
		
		/*firstLabel = firstLabel.replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase().trim();
		secondLabel = secondLabel.replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase().trim();
		if(firstLabel.toLowerCase().indexOf("how to") == 0){
			firstLabel = firstLabel.substring(6).trim();
		}
		if(firstLabel.length() < 5) return false;
		// test that there is at least one verb
		if(!textPreProcessing.containsVerb(firstLabel)) return false;*/
		
		// Impose some constraints on the label, it should have at least 5 characters and at least be 2 words
		//if(secondLabel.indexOf(firstLabel) >= 0) return true;
		
		/*if(firstLabel.length() > 4)
		if(splitBySpace(firstLabel).length > 1){
			// check that the string appears in the second label, even if not exactly in the same order
			String[] tokens = splitBySpace(firstLabel);
			int lastIndex = -1;
			for(String s : tokens){
				int newIndex = secondLabel.indexOf(s, lastIndex+1);
				if(newIndex <= lastIndex ) return false;
				lastIndex = newIndex+s.length();
			}
			return true;
		}*/
		//return true;
	}

	public List<ActionScore> returnBestSimilarity(String[] sentenceWords) throws ParseException, IOException {
			List<ActionScore> scores = new LinkedList<ActionScore>();
			BooleanQuery similarityQuery = new BooleanQuery();
			//Object[] wordsOfThisURI = (sentenceWords.toArray());
			for(int wi = 0; wi < sentenceWords.length && sentenceWords[wi] != null; wi++){
				TermQuery q = new TermQuery(new Term("label",(String)sentenceWords[wi]));
				similarityQuery.add(q, Occur.SHOULD);
			}
			// Collect the top n results
		    int queryIteration = 0;
		    boolean goodResultsExceedingCurrentLimit = true;
		    ScoreDoc[] similarityHits = new ScoreDoc[0];
		    float goldStandardForSimilarity = Float.MAX_VALUE;
		    while(goodResultsExceedingCurrentLimit){
		    	TopScoreDocCollector similarityCollector = TopScoreDocCollector.create(maxResultsToConsider[queryIteration], true);		    
		    	targetSearcher.search(similarityQuery, similarityCollector);
		    	similarityHits = similarityCollector.topDocs().scoreDocs;
		    	// If there are no hits return
		    	if(similarityHits.length == 0) return scores;
		    	// The maximum score, is the score between an item and itself, it is the gold standard
		    	if(queryIteration == 0) {
		    		goldStandardForSimilarity = similarityHits[0].score;
		    	}
		    	if(similarityHits.length < maxResultsToConsider[queryIteration]){
		    		// The query returned less results than the maximum: no need to expand it
		    		goodResultsExceedingCurrentLimit = false;
		    	} else {
		    		float worseScore = similarityHits[similarityHits.length-1].score / goldStandardForSimilarity;
		    		if(worseScore < thresholdOfAcceptance) {
		    			// There might be more results than the maximum, but they won't be acceptable according to the give threshold
		    			// there fore there is no need to expand the query further.
		    			goodResultsExceedingCurrentLimit = false;
		    		} else {
		    			// There could be more results, and they could be above the threshold, it is necessary to expand the query and get more results
		    			queryIteration++;
		    			if(queryIteration >= maxResultsToConsider.length-2) {
		    				// The maximum number of results has been hit, no more should be taken into account
		    				goodResultsExceedingCurrentLimit = false;
		    			}
		    		}
		    	}
		    	
		    }
			// Iterate through all the results
			for(int i = 1; i < similarityHits.length; i++){
				Document similarDoc = targetSearcher.doc(similarityHits[i].doc);
				String similarDocURI = similarDoc.get("uri");
						float score = similarityHits[i].score / goldStandardForSimilarity;
						if(score > 0.6){
							ActionScore action = new ActionScore(similarDocURI,(double) score);
							action.setLabel(similarDoc.get("label"));
							scores.add(action);
						}
			}
			return scores;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	/*private String utilityQueryLabel(String uri) throws IOException, ParseException{
		Set<String> fieldsToLoad = new HashSet<String>();
		fieldsToLoad.add("uri");
		fieldsToLoad.add("label");
		BooleanQuery qURI = new BooleanQuery();
		qURI.add(new TermQuery(new Term("label","dog")), Occur.MUST);
		//qURI.add(new TermQuery(new Term("label")), Occur.SHOULD);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1, true);
		searcher.search(qURI, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		//System.out.println();
		//System.out.println("Query String: " + queryString);
		if(hits.length > 0) {
			Document hitDoc = searcher.doc(hits[0].doc,fieldsToLoad);  // getting actual document
			return hitDoc.get("label");
		}
		return null;
	}*/
	
	/*private String utilityQueryLabel2(String uri) throws IOException, ParseException{
		QueryExecution execution = sparql.query("PREFIX oa: <http://www.w3.org/ns/oa#> SELECT ?label "+intConfig.getNamedGraphsQueryString()+" WHERE { "
				+ "?annotation oa:hasBody <"+uri+"> . "
				+ "?annotation oa:hasTarget ?target . "
				+ "?target oa:hasSelector ?selector . "
				+ "?selector oa:exact ?label . } LIMIT 1");
		ResultSet results = execution.execSelect();
		while(results.hasNext()){
			QuerySolution result = results.next();
			String label = result.getLiteral("?label").getLexicalForm();
			execution.close();
			return label;
			}
		execution.close();
		return null;
	}*/
	
	protected Float computeWordOrderScore(String labela, String labelb){
		String[] arrlabela =  splitBySpace(labela);
		String[] arrlabelb =  splitBySpace(labelb);
		int errorsplus = 0;
		int errorsminus = 0;
		int correct = 0;
		for(int i = 0; i<arrlabela.length-1; i++){
			int thisIndex = indexOf(arrlabela[i],arrlabelb);
			int nextIndex = indexOf(arrlabela[i+1],arrlabelb)-1;
			if(nextIndex == thisIndex) {
				correct++;
				continue;
			}
			if(thisIndex>nextIndex) return -2.5f;//errorsminus += 0.3;
			if(nextIndex>thisIndex) errorsplus += 3+nextIndex-thisIndex;
		}
		if(correct == 0 && errorsplus == 0 && errorsminus > 0) return new Float(-1);
		if(errorsplus == 0 && errorsminus == 0) return new Float(1.5);
		return new Float(
				Math.max(-1f, 
						( (
						     1/
						       ( Math.max(0.8, 
						    		   	 	(1+(errorsplus*0.2f)/arrlabela.length)
						    		   	 	-(correct*0.1f) 
						             ) 
						        ) 
						   )
						   -errorsminus
						) 
				)
			);
	}
	
	// indexes explanation
	// 0 - correct ones
	// 1 - number of wrong forward
	// 2 - number of wrong backwards
	// 3 - number of all wrong spaces forward
	// 4 - number of all wrong spaces backwards
	// 5 - number of missing
	protected int[] computeWordOrderScoreDetailed(String labela, String labelb){
		int[] orderScores = new int[12];
		String[] arrlabela =  splitBySpace(labela);
		String[] arrlabelb =  splitBySpace(labelb);
		int indexLast = 0;
		for(int i = 0; i<arrlabela.length-1; i++){
			int thisIndex = indexOf(arrlabela[i],arrlabelb, indexLast);
			int nextIndex = indexOf(arrlabela[i+1],arrlabelb, indexLast)-1;
			
			if(thisIndex > arrlabelb.length || nextIndex > arrlabelb.length || thisIndex < 0 || nextIndex < 0 ){
				orderScores[5] = orderScores[5]+1;
			} else {
				indexLast = thisIndex+1;
				if(nextIndex == thisIndex) {
					orderScores[0] = orderScores[0]+1;
					continue;
				}
				// occurrence is before
				if(thisIndex>nextIndex){
					orderScores[2] = orderScores[2]+1;
					orderScores[4] = orderScores[4]+thisIndex-nextIndex;
				}
				// occurrence is too forward
				if(nextIndex>thisIndex) {
					orderScores[1] = orderScores[1]+1;
					orderScores[3] = orderScores[3]+nextIndex-thisIndex;
				} 
			}
		}
		orderScores[6] = (orderScores[0]+1)/arrlabela.length;
		orderScores[7] = (orderScores[1]+1)/arrlabela.length;
		orderScores[8] = (orderScores[2]+1)/arrlabela.length;
		orderScores[9] = (orderScores[3]+1)/arrlabela.length;
		orderScores[10] = (orderScores[4]+1)/arrlabela.length;
		orderScores[11] = (orderScores[5]+1)/arrlabela.length;
		return orderScores;
	}
	
	protected int indexOf(String token,String[] array){
		return indexOf(token, array, 0);
	}
	protected int indexOf(String token,String[] array, int from){
		if(from < 0) from = 0;
		if(from >= array.length) from = array.length-1;
		for(int i = from; i < array.length; i++) if(array[i].equals(token)) return i;
		for(int i = from; i >= 0; i--) if(array[i].equals(token)) return i;
		//System.out.println("ERROR, no token found for "+token+" in:");
		//System.out.println(array);
		return Integer.MAX_VALUE;
	}
	private int lastIndexOf(String[] tokens,String[] array){
		int index = Integer.MAX_VALUE;
		Set<String> tokensToMatch = new HashSet<String>();
		for(int i = 0; i < tokens.length; i++){
			tokensToMatch.add(tokens[i]);
		}
		for(int i = 0; i < array.length; i++){
			tokensToMatch.remove(array[i]);
			if(tokensToMatch.size() == 0) {
				return i;
			}
		} 
		System.out.println("ERROR, no token found for "+tokens+" in:");
		System.out.println(array);
		return index;
	}
	
	// label a should be contained in at least one of the phrases of both A and B
	protected boolean sameNegationValue(String labela, String labelb, String allLabelA, String allLabelB){
		if(labela.indexOf("not") == -1 && labelb.indexOf("not") == -1){
			return true;
		}
		if(labela.indexOf("not") != -1 && labelb.indexOf("not") == -1){
			return false;
		}
		List<String> listLabelA = textPreProcessing.extractAllSentenceLemmasListContainingTokens(allLabelA.replaceAll(",", " PUNCTUATIONCOMMA "),labela);
		List<String> listLabelB = textPreProcessing.extractAllSentenceLemmasListContainingTokens(allLabelB.replaceAll(",", " PUNCTUATIONCOMMA "),labela);
		if(listLabelA == null || listLabelB == null){
			return true;
		}
		String[] arrlabela =  listLabelA.toArray(new String[0]);
		String[] arrlabelb =  listLabelB.toArray(new String[0]);
		int lastIndexInB = lastIndexOf(arrlabela,arrlabelb);
		String[] negationWords = new String[]{"not","avoid","prevent"};
		for(String neg : negationWords){
			for(int i = 0; i <= lastIndexInB; i++){
				if(arrlabelb[i].equals(neg)){
					boolean negationFound = false;
					boolean negationRelevant = false;
					if(i+1 < arrlabelb.length){
						String tokenAfter = arrlabelb[i+1];
						int indexInA = indexOf(tokenAfter,arrlabela);
						if(indexInA < arrlabela.length && indexInA > 0){
							negationRelevant = true;
							if(indexInA+1 < arrlabela.length && arrlabela[indexInA+1].equals(neg)) negationFound = true;
							if(indexInA-1 > 0 && arrlabela[indexInA-1].equals(neg)) negationFound = true;
						}
					}
					if(i-1 > 0){
						String tokenBefore = arrlabelb[i-1];
						int indexInA = indexOf(tokenBefore,arrlabela);
						if(indexInA < arrlabela.length && indexInA > 0){
							negationRelevant = true;
							if(indexInA+1 < arrlabela.length && arrlabela[indexInA+1].equals(neg)) negationFound = true;
							if(indexInA-1 > 0 && arrlabela[indexInA-1].equals(neg)) negationFound = true;
						}
					}
					if(negationRelevant && !negationFound) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public  synchronized void add1ToAllMLmatches() {
		this.allMLmatches = allMLmatches+1;
	}
	
	public float computeExactLabelMatchScore(String[] labela, String[] labelb){
		int labelalength = labela.length;
		int matchesFound = 0;
		for(int i = 0; i < labela.length; i++){
			if(labela[i].equals("the") || labela[i].equals("an") || labela[i].equals("a")){
				labelalength--;
				continue;
			}
			for(int j = 0; j < labelb.length; j++){
				if(labelb[j].equals(labela[i])){
					matchesFound++;
					break;
				}
			}
		}
		return ((float)matchesFound)/labelalength;
	}
	
	protected Set<String> getAllUniqueWordsFromURIs(String URIs){
		Set<String> allWords = new HashSet<String>();
		if(URIs == null) return allWords;
		try {
			BooleanQuery qURI = new BooleanQuery();
			for(String uri : URIs.split("\\s+")){
				qURI.add(new TermQuery(new Term("uri",uri)), Occur.SHOULD);
			}
			TopScoreDocCollector collector = TopScoreDocCollector.create(100, true);
			sourceSearcher.search(qURI, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			for(ScoreDoc doc : hits){
				Document hitDoc = sourceSearcher.doc(doc.doc);
				for(int i = 0; i < maxSentencesToConsider; i++){
					if(hitDoc.get("sentence"+i)!=null) for(String s : hitDoc.get("sentence"+i).split("\\s+")){
						allWords.add(s);
					}
				}
			}
			collector = TopScoreDocCollector.create(100, true);
			targetSearcher.search(qURI, collector);
			hits = collector.topDocs().scoreDocs;
			for(ScoreDoc doc : hits){
				Document hitDoc = targetSearcher.doc(doc.doc);
				for(int i = 0; i < maxSentencesToConsider; i++){
					if(hitDoc.get("sentence"+i)!=null) for(String s : hitDoc.get("sentence"+i).split("\\s+")){
						allWords.add(s);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return allWords;
	}
	
	protected void getAllWordsFromURIs(String URIs, List<String> all, List<String> core){
		if(URIs == null) return;
		String source = extractSourceFromURI(URIs);
		try {
			BooleanQuery qURI = new BooleanQuery();
			qURI.add(new TermQuery(new Term("uri",source)), Occur.MUST);
			TopScoreDocCollector collector = TopScoreDocCollector.create(1, true);
			contextSearcher.search(qURI, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			for(ScoreDoc doc : hits){
				Document hitDoc = contextSearcher.doc(doc.doc);
				if(hitDoc.get("context")!=null) for(String s : hitDoc.get("context").split("\\s+")){
					all.add(s);
				}
				if(hitDoc.get("core")!=null) for(String s : hitDoc.get("core").split("\\s+")){
					core.add(s);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected float getIDF(String term){
		int termFrequency = 0;
		for(int z = 0; z < intConfig.getMaxSentencesToConsider(); z++){
			Term t = new Term("sentence"+z,term);
			try {
				termFrequency += targetSearcher.getIndexReader().docFreq(t);
			} catch (IOException e) {
				e.printStackTrace();
				return 0f;
			}
		}
		return similarity.idf(termFrequency, targetSearcher.getIndexReader().numDocs());
	}
}
