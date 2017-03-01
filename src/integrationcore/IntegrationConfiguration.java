package integrationcore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class IntegrationConfiguration {
	private int logVerbosity = Integer.MAX_VALUE;
	private int maxThreads;
	private boolean timestamp = true;
	private String pathToDataDirectory = null;
	private String endpointURL = null;
	private String pathToTurtlePrefixes = null;
	private Set<String> blockSynset = new HashSet<String>();
	private Set<String> newBlockedSynsets = new HashSet<String>();
	private double verbWeight = 0.1;
	private double matchThreshold = 0.8;
	private double matchThresholdGlobal = 0.25;
	private double evidenceThreshold = 1.2;
	private String[] namedGraphs = new String[]{};
	private boolean doPreprocessing = false;
	private boolean doLuceneIndexing = false;
	private boolean doLuceneIndexingContext = false;
	private boolean doLuceneIndexingApplication = false;
	private boolean doLuceneQuerying = false;
	private boolean doLuceneActivityRecognition = false;
	private boolean doLinking = false;
	private boolean doSynsetsStatistics = false;
	private boolean doSemLinkingComputeSimilarity = false;
	private boolean doSemLinkingPreorderEntities = false;
	private boolean doSemLinkingSourceBased = false;
	private boolean doLinksPostProcessing = false;
	private boolean doDBpediaLinksPostProcessing = false;
	private boolean doEvaluation = false;
	private boolean doManualEvaluation = false;
	private boolean doEvaluationDBpedia = false;
	private boolean doManualEvaluationDBpedia = false;
	private boolean doMachineLearning = false;
	private String evaluationFolderUserGenerated = null;
	private String evaluationFolderPositive = null;
	private String evaluationFolderNegative = null;
	private String evaluationFolderDBpediaPositive = null;
	private String evaluationFolderDBpediaNegative = null;
	private String namedGraphsQueryString = "";
	private String ontologyGraphQueryString = "";
	private String extractedPropertiesGraph = "";
	private String pathToFileWithEntitiesInAlphabeticalOrder = null;
	private String pathToLuceneTargetIndex = null;
	private String pathToLuceneSourceIndex = null;
	private String pathToLuceneContextIndex = null;
	private String pathToLuceneIndexToCreate = null;
	private boolean restrictIntegrationToComplexEntities = false;
	private boolean restrictLuceneIndexCreationToPrimitiveEntities = false;
	private boolean restrictLuceneIndexCreationToComplexEntities = false;
	private int maxSentencesToConsider = 2;
	private int maxWordsToConsider = 10;
	private String plotDirectory = "";
	private String knowHowLinksResultsDirectory = "";
	private boolean luceneQueryDecompositionLinks = false;
	private int newURLsToEvaluate = 0;
	private String DBpediaLinksPostProcessingFolder;
	private int postProcCatUp = Integer.MAX_VALUE;
	private int postProcCatDown = Integer.MIN_VALUE;
	private int postProcCountUp = Integer.MAX_VALUE;
	private int postProcCountDown = Integer.MIN_VALUE;
	private boolean restrictEvaluationToOneNewLinkPerURL = false;
	private String classfierSettings = null;
	
	public void setBlockedSynsets(String path){
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String line = br.readLine();
			while (line != null) {
				blockSynset.add(line.split(" ")[0]);
				line = br.readLine();
			}
			br.close();
			System.out.println("CONFIG - Read "+blockSynset.size()+" synsets to block.");
		} catch (IOException e) {
			System.out.println("CONFIG - ERROR failed to load list of blocked synsets");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("CONFIG - Read "+blockSynset.size()+" synsets to block.");
	};
	
	public boolean isBlockedSynset(String synset){
		return blockSynset.contains(synset) || newBlockedSynsets.contains(synset);
	}
	
	public void addSynsetToBlock(String synset){
		newBlockedSynsets.add(synset);
	}
	
	public int getLogVerbosity() {
		return logVerbosity;
	}

	public void setLogVerbosity(int logVerbosity) {
		this.logVerbosity = logVerbosity;
	}

	public String getPathToDataDirectory() {
		return pathToDataDirectory;
	}

	public void setPathToDataDirectory(String pathToDataDirectory) {
		this.pathToDataDirectory = pathToDataDirectory;
	}

	public boolean isTimestamp() {
		return timestamp;
	}

	public void setTimestamp(boolean timestamp) {
		this.timestamp = timestamp;
	}

	public String getPathToTurtlePrefixes() {
		return pathToTurtlePrefixes;
	}

	public void setPathToTurtlePrefixes(String pathToTurtlePrefixes) {
		this.pathToTurtlePrefixes = pathToTurtlePrefixes;
	}

	public String getEndpointURL() {
		return endpointURL;
	}

	public void setEndpointURL(String endpointURL) {
		this.endpointURL = endpointURL;
	}

	public double getVerbWeight() {
		return verbWeight;
	}

	public void setVerbWeight(double verbWeigth) {
		this.verbWeight = verbWeigth;
	}

	public double getMatchThreshold() {
		return matchThreshold;
	}

	public void setMatchThreshold(double matchThreshold) {
		this.matchThreshold = matchThreshold;
	}

	public double getEvidenceThreshold() {
		return evidenceThreshold;
	}

	public void setEvidenceThreshold(double evidenceThreshold) {
		this.evidenceThreshold = evidenceThreshold;
	}

	public double getMatchThresholdGlobal() {
		return matchThresholdGlobal;
	}

	public void setMatchThresholdGlobal(double matchThresholdGlobal) {
		this.matchThresholdGlobal = matchThresholdGlobal;
	}

	public String[] getNamedGraphs() {
		return namedGraphs;
	}

	public void setNamedGraphs(String[] namedGraphs) {
		this.namedGraphs = namedGraphs;
		this.namedGraphsQueryString = "";
		for(int i = 0; i < namedGraphs.length; i++){
			this.namedGraphsQueryString += " FROM <"+namedGraphs[i]+"> ";
		}
		
	}

	public boolean isDoPreprocessing() {
		return doPreprocessing;
	}

	public void setDoPreprocessing(boolean doPreprocessing) {
		this.doPreprocessing = doPreprocessing;
	}

	public boolean isDoLinking() {
		return doLinking;
	}

	public void setDoLinking(boolean doLinking) {
		this.doLinking = doLinking;
	}

	public String getNamedGraphsQueryString() {
		return namedGraphsQueryString;
	}

	public boolean isDoSynsetsStatistics() {
		return doSynsetsStatistics;
	}

	public void setDoSynsetsStatistics(boolean doSynsetsStatistics) {
		this.doSynsetsStatistics = doSynsetsStatistics;
	}

	public boolean isDoSemLinkingComputeSimilarity() {
		return doSemLinkingComputeSimilarity;
	}

	public void setDoSemLinkingComputeSimilarity(
			boolean doSemLinkingComputeSimilarity) {
		this.doSemLinkingComputeSimilarity = doSemLinkingComputeSimilarity;
	}

	public boolean isDoSemLinkingPreorderEntities() {
		return doSemLinkingPreorderEntities;
	}

	public void setDoSemLinkingPreorderEntities(boolean doSemLinkingPreorderEntities) {
		this.doSemLinkingPreorderEntities = doSemLinkingPreorderEntities;
	}

	public String getPathToFileWithEntitiesInAlphabeticalOrder() {
		return pathToFileWithEntitiesInAlphabeticalOrder;
	}

	public void setPathToFileWithEntitiesInAlphabeticalOrder(
			String pathToFileWithEntitiesInAlphabeticalOrder) {
		this.pathToFileWithEntitiesInAlphabeticalOrder = pathToFileWithEntitiesInAlphabeticalOrder;
	}

	public boolean isDoSemLinkingSourceBased() {
		return doSemLinkingSourceBased;
	}

	public void setDoSemLinkingSourceBased(boolean doSemLinkingSourceBased) {
		this.doSemLinkingSourceBased = doSemLinkingSourceBased;
	}

	public String getExtractedPropertiesGraph() {
		return extractedPropertiesGraph;
	}

	public void setExtractedPropertiesGraph(String extractedPropertiesGraph) {
		this.extractedPropertiesGraph = extractedPropertiesGraph;
	}

	public boolean isDoLuceneIndexing() {
		return doLuceneIndexing;
	}

	public void setDoLuceneIndexing(boolean doLuceneIndexing) {
		this.doLuceneIndexing = doLuceneIndexing;
	}

	public String getPathToLuceneTargetIndex() {
		return pathToLuceneTargetIndex;
	}

	public void setPathToLuceneTargetIndex(String pathToLuceneIndex) {
		this.pathToLuceneTargetIndex = pathToLuceneIndex;
	}

	public boolean isDoLuceneQuerying() {
		return doLuceneQuerying;
	}

	public void setDoLuceneQuerying(boolean doLuceneQuerying) {
		this.doLuceneQuerying = doLuceneQuerying;
	}

	public int getMaxSentencesToConsider() {
		return maxSentencesToConsider;
	}

	public void setMaxSentencesToConsider(int maxSentencesToConsider) {
		this.maxSentencesToConsider = maxSentencesToConsider;
	}

	public String getPlotDirectory() {
		return plotDirectory;
	}

	public void setPlotDirectory(String plotDirectory) {
		this.plotDirectory = plotDirectory;
	}

	public String getKnowHowLinksResultsDirectory() {
		return knowHowLinksResultsDirectory;
	}

	public void setKnowHowLinksResultsDirectory(
			String knowHowLinksResultsDirectory) {
		this.knowHowLinksResultsDirectory = knowHowLinksResultsDirectory;
	}

	public int getMaxWordsToConsider() {
		return maxWordsToConsider;
	}

	public void setMaxWordsToConsider(int maxWordsToConsider) {
		this.maxWordsToConsider = maxWordsToConsider;
	}

	public boolean isDoLuceneActivityRecognition() {
		return doLuceneActivityRecognition;
	}

	public void setDoLuceneActivityRecognition(boolean doLuceneActivityRecognition) {
		this.doLuceneActivityRecognition = doLuceneActivityRecognition;
	}

	public boolean isRestrictIntegrationToComplexEntities() {
		return restrictIntegrationToComplexEntities;
	}

	public void setRestrictIntegrationToComplexEntities(
			boolean restrictIntegrationToComplexEntities) {
		this.restrictIntegrationToComplexEntities = restrictIntegrationToComplexEntities;
	}

	public boolean isRestrictLuceneIndexCreationToPrimitiveEntities() {
		return restrictLuceneIndexCreationToPrimitiveEntities;
	}

	public void setRestrictLuceneIndexCreationToPrimitiveEntities(
			boolean restrictLuceneIndexCreationToPrimitiveEntities) {
		this.restrictLuceneIndexCreationToPrimitiveEntities = restrictLuceneIndexCreationToPrimitiveEntities;
	}

	public boolean isRestrictLuceneIndexCreationToComplexEntities() {
		return restrictLuceneIndexCreationToComplexEntities;
	}

	public void setRestrictLuceneIndexCreationToComplexEntities(
			boolean restrictLuceneIndexCreationToComplexEntities) {
		this.restrictLuceneIndexCreationToComplexEntities = restrictLuceneIndexCreationToComplexEntities;
	}

	public String getPathToLuceneSourceIndex() {
		return pathToLuceneSourceIndex;
	}

	public void setPathToLuceneSourceIndex(String pathToLuceneSourceIndex) {
		this.pathToLuceneSourceIndex = pathToLuceneSourceIndex;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public String getPathToLuceneIndexToCreate() {
		return pathToLuceneIndexToCreate;
	}

	public void setPathToLuceneIndexToCreate(String pathToLuceneIndexToCreate) {
		this.pathToLuceneIndexToCreate = pathToLuceneIndexToCreate;
	}

	public boolean isLuceneQueryDecompositionLinks() {
		return luceneQueryDecompositionLinks;
	}

	public void setLuceneQueryDecompositionLinks(
			boolean luceneQueryDecompositionLinks) {
		this.luceneQueryDecompositionLinks = luceneQueryDecompositionLinks;
	}

	public boolean isDoLinksPostProcessing() {
		return doLinksPostProcessing;
	}

	public void setDoLinksPostProcessing(boolean doLinksPostProcessing) {
		this.doLinksPostProcessing = doLinksPostProcessing;
	}

	public boolean isDoEvaluation() {
		return doEvaluation;
	}

	public void setDoEvaluation(boolean doEvaluation) {
		this.doEvaluation = doEvaluation;
	}

	public boolean isDoManualEvaluation() {
		return doManualEvaluation;
	}

	public void setDoManualEvaluation(boolean doManualEvaluation) {
		this.doManualEvaluation = doManualEvaluation;
	}

	public String getEvaluationFolderPositive() {
		return evaluationFolderPositive;
	}

	public void setEvaluationFolderPositive(String evaluationFolderPositive) {
		this.evaluationFolderPositive = evaluationFolderPositive;
	}

	public String getEvaluationFolderNegative() {
		return evaluationFolderNegative;
	}

	public void setEvaluationFolderNegative(String evaluationFolderNegative) {
		this.evaluationFolderNegative = evaluationFolderNegative;
	}

	public String getOntologyGraphQueryString() {
		return ontologyGraphQueryString;
	}

	public void setOntologyGraphQueryString(String ontologyGraphQueryString) {
		this.ontologyGraphQueryString = ontologyGraphQueryString;
	}

	public int getNewURLsToEvaluate() {
		return newURLsToEvaluate;
	}

	public void setNewURLsToEvaluate(int newURLsToEvaluate) {
		this.newURLsToEvaluate = newURLsToEvaluate;
	}

	public boolean isDoEvaluationDBpedia() {
		return doEvaluationDBpedia;
	}

	public void setDoEvaluationDBpedia(boolean doEvaluationDBpedia) {
		this.doEvaluationDBpedia = doEvaluationDBpedia;
	}

	public boolean isDoManualEvaluationDBpedia() {
		return doManualEvaluationDBpedia;
	}

	public void setDoManualEvaluationDBpedia(boolean doManualEvaluationDBpedia) {
		this.doManualEvaluationDBpedia = doManualEvaluationDBpedia;
	}

	public String getEvaluationFolderDBpediaPositive() {
		return evaluationFolderDBpediaPositive;
	}

	public void setEvaluationFolderDBpediaPositive(
			String evaluationFolderDBpediaPositive) {
		this.evaluationFolderDBpediaPositive = evaluationFolderDBpediaPositive;
	}

	public String getEvaluationFolderDBpediaNegative() {
		return evaluationFolderDBpediaNegative;
	}

	public void setEvaluationFolderDBpediaNegative(
			String evaluationFolderDBpediaNegative) {
		this.evaluationFolderDBpediaNegative = evaluationFolderDBpediaNegative;
	}

	public boolean isDoDBpediaLinksPostProcessing() {
		return doDBpediaLinksPostProcessing;
	}

	public void setDoDBpediaLinksPostProcessing(boolean doDBpediaLinksPostProcessing) {
		this.doDBpediaLinksPostProcessing = doDBpediaLinksPostProcessing;
	}

	public String getDBpediaLinksPostProcessingFolder() {
		return DBpediaLinksPostProcessingFolder;
	}

	public void setDBpediaLinksPostProcessingFolder(
			String dBpediaLinksPostProcessingFolder) {
		DBpediaLinksPostProcessingFolder = dBpediaLinksPostProcessingFolder;
	}

	public int getPostProcCatUp() {
		return postProcCatUp;
	}

	public void setPostProcCatUp(int postProcCatUp) {
		this.postProcCatUp = postProcCatUp;
	}

	public int getPostProcCatDown() {
		return postProcCatDown;
	}

	public void setPostProcCatDown(int postProcCatDown) {
		this.postProcCatDown = postProcCatDown;
	}

	public int getPostProcCountUp() {
		return postProcCountUp;
	}

	public void setPostProcCountUp(int postProcCountUp) {
		this.postProcCountUp = postProcCountUp;
	}

	public int getPostProcCountDown() {
		return postProcCountDown;
	}

	public void setPostProcCountDown(int postProcCountDown) {
		this.postProcCountDown = postProcCountDown;
	}

	public boolean isDoMachineLearning() {
		return doMachineLearning;
	}

	public void setDoMachineLearning(boolean doMachineLearning) {
		this.doMachineLearning = doMachineLearning;
	}

	public boolean isRestrictEvaluationToOneNewLinkPerURL() {
		return restrictEvaluationToOneNewLinkPerURL;
	}

	public void setRestrictEvaluationToOneNewLinkPerURL(
			boolean restrictEvaluationToOneNewLinkPerURL) {
		this.restrictEvaluationToOneNewLinkPerURL = restrictEvaluationToOneNewLinkPerURL;
	}

	public boolean isDoLuceneIndexingContext() {
		return doLuceneIndexingContext;
	}

	public void setDoLuceneIndexingContext(boolean doLuceneIndexingContext) {
		this.doLuceneIndexingContext = doLuceneIndexingContext;
	}

	public String getPathToLuceneContextIndex() {
		return pathToLuceneContextIndex;
	}

	public void setPathToLuceneContextIndex(String pathToLuceneContextIndex) {
		this.pathToLuceneContextIndex = pathToLuceneContextIndex;
	}

	public String getEvaluationFolderUserGenerated() {
		return evaluationFolderUserGenerated;
	}

	public void setEvaluationFolderUserGenerated(
			String evaluationFolderUserGenerated) {
		this.evaluationFolderUserGenerated = evaluationFolderUserGenerated;
	}

	public String getClassfierSettings() {
		return classfierSettings;
	}

	public void setClassfierSettings(String classfierSettings) {
		this.classfierSettings = classfierSettings;
	}

	public boolean isDoLuceneIndexingApplication() {
		return doLuceneIndexingApplication;
	}

	public void setDoLuceneIndexingApplication(boolean doLuceneIndexingApplication) {
		this.doLuceneIndexingApplication = doLuceneIndexingApplication;
	}
	
}
