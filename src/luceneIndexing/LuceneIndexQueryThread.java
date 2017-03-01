package luceneIndexing;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.similarities.Similarity;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;

public class LuceneIndexQueryThread implements Runnable{

	private LuceneIndexQuery queryController;
	private String uri;
	
	public LuceneIndexQueryThread(LuceneIndexQuery queryController, String uri) {
		this.queryController = queryController;
		this.uri = uri;
	}

	@Override
	public void run() {
		// just a debug test
		//if(!queryController.isMainEntityURI(uri)) return;
		
		if(queryController.getEntitiesConsidered() > 0 && (queryController.getEntitiesConsidered() == 250 || queryController.getEntitiesConsidered() == 1000 || queryController.getEntitiesConsidered() == 2000 || queryController.getEntitiesConsidered() == 3000 || queryController.getEntitiesConsidered() % 5000 == 0)) {
			queryController.printSimilarityMatchStatistics();
		}
		queryController.setEntitiesConsidered(new Integer(queryController.getEntitiesConsidered().intValue()+1));
		//queryController.consideredURIs.add(uri);
		try {
			BooleanQuery qURI = new BooleanQuery();
			qURI.add(new TermQuery(new Term("uri",uri)), Occur.MUST);
			String source = queryController.extractSourceFromURI(uri);
			
			//QueryParser parser = new QueryParser(Version.LUCENE_48, "uri", analyzer);
			//Query query = parser.parse("uri:\""+uri+"\"");
			TopScoreDocCollector collector = TopScoreDocCollector.create(1, true);
			queryController.sourceSearcher.search(qURI, collector);
			
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			//System.out.println();
			//System.out.println("Query String: " + queryString);
			if(hits.length > 0) {
				Document hitDoc = queryController.sourceSearcher.doc(hits[0].doc);  // getting actual document
				boolean introSystemPrint = false;
				
				BooleanQuery similarityQuery = new BooleanQuery();
				//similarityQuery.setMinimumNumberShouldMatch(2);
				// QUERY FOR THE LABEL
				String label = hitDoc.get("label").trim();
				String exactCategory = hitDoc.get("exactcategory");
				String topCategory = hitDoc.get("topcategory");
				String allCategories = hitDoc.get("allcategories");
				boolean isFictional = false;
				if(allCategories!= null && (allCategories.indexOf("http://www.wikihow.com/category:video-games") != -1 || allCategories.indexOf("http://snapguide.com/guides/topic/games-tricks") != -1)) isFictional = true;
				List<String> neighboursAll = new LinkedList<String>();
				List<String> neighboursCore = new LinkedList<String>();
				queryController.getAllWordsFromURIs(uri,neighboursAll,neighboursCore);
				for(int i = 0; i < queryController.intConfig.getMaxSentencesToConsider(); i++){
					if(hitDoc.get("sentence"+i) != null) for(String s : hitDoc.get("sentence"+i).split("\\s+")) 
						neighboursAll.remove(s);
					if(hitDoc.get("core"+i) != null) for(String s : hitDoc.get("core"+i).split("\\s+")) 
						neighboursCore.remove(s);
				}
				
				//Calculate the idf for the terms in the query
				float cumulativeidf = 1;
				float bestidf = 0;
				for(String s : queryController.splitBySpace(hitDoc.get("sentence0"))){
					int termFrequency = 0;
					for(int z = 0; z < queryController.intConfig.getMaxSentencesToConsider(); z++){
					Term t = new Term("sentence"+z,s);
					termFrequency += queryController.targetSearcher.getIndexReader().docFreq(t);
					}
					float idf = queryController.similarity.idf(termFrequency, queryController.targetSearcher.getIndexReader().numDocs());
					if(idf > bestidf) bestidf = idf;
					cumulativeidf = cumulativeidf+idf;
				}
				float cumulativeidfnormalized = cumulativeidf/(queryController.splitBySpace(hitDoc.get("sentence0")).length);
				float cumulativeidfnormalized2 = Math.min(200, cumulativeidfnormalized)/200;
				
				ScoreDoc[] similarityHits = new ScoreDoc[0];
				float goldStandardForSimilarity = Float.MAX_VALUE;
				
				int occurrences = 0;
				
				{
					
					if(queryController.isBlockedLabel(label) && !queryController.intConfig.isDoMachineLearning()) {
						System.out.println("BLOCKED LABEL: "+label);
						return;
					}
					int querySyle = 0;
					
					if(querySyle == 0){
						for(int i = 0; i < queryController.intConfig.getMaxSentencesToConsider(); i++){
							DisjunctionMaxQuery phraseQuery = new DisjunctionMaxQuery(0f);
							String[] fieldsToConsider = new String[]{"sentence"/*"object","verb","context"*//*,"objectSynsets","verbSynsets","adjSynsets"*/};
							for(int x = Math.max(0, i-1); x < Math.min(i+1,queryController.intConfig.getMaxSentencesToConsider()); x++){
								BooleanQuery phraseQueryAtPosition = new BooleanQuery();
								for(String fieldToConsider : fieldsToConsider){
									if(hitDoc.get(fieldToConsider+i) != null) {
										String[] queryTokens = queryController.splitBySpace(hitDoc.get(fieldToConsider+i));
										BooleanQuery fieldQuery = new BooleanQuery();
										for(String s : queryTokens){
											TermQuery q = new TermQuery(new Term(fieldToConsider+x,s));
											//q.setBoost(sentenceImportanceScaling[i]/(1f+Math.abs(x-i)));
											// At least one field token per field should match
											/*if(fieldToConsider.equalsIgnoreCase("objectSynsets") || fieldToConsider.equalsIgnoreCase("verbSynsets") || fieldToConsider.equalsIgnoreCase("adjSynsets") )
												fieldQuery.add(q, Occur.SHOULD);
											else*/
											fieldQuery.add(q, !fieldToConsider.equals("context") ? Occur.MUST : Occur.SHOULD);
											//fieldQuery.setMinimumNumberShouldMatch(Math.max(1, queryTokens.length-1));
										}
										// to be a match, all fields should be present
										/*if(fieldToConsider.equalsIgnoreCase("objectSynsets") || fieldToConsider.equalsIgnoreCase("verbSynsets") || fieldToConsider.equalsIgnoreCase("adjSynsets") )
											phraseQueryAtPosition.add(fieldQuery, Occur.MUST);
										else*/
										phraseQueryAtPosition.add(fieldQuery, !fieldToConsider.equals("context") ? Occur.MUST : Occur.SHOULD);
									}
								}
								// the phrase should match at least in one position
								if(phraseQueryAtPosition.clauses().size() > 0){
									phraseQuery.add(phraseQueryAtPosition);
								}
							}
							// each phrase should match
							if(phraseQuery.iterator().hasNext()){
								similarityQuery.add(phraseQuery, Occur.MUST);
							}
							//similarityQuery.setMinimumNumberShouldMatch(1);
							/*List<String> labelTokens = queryController.textPreProcessing.extractAllWords(hitDoc.get("label"));
						BooleanQuery labelQuery = new BooleanQuery();
						for(String s : labelTokens){
							TermQuery q = new TermQuery(new Term("label",s));
							labelQuery.add(q, Occur.MUST);
						}
						labelQuery.setBoost(0f);
						similarityQuery.add(labelQuery, Occur.MUST);*/
						}
					}
					
					// Collect the top n results
					int queryIteration = 0;
					boolean goodResultsExceedingCurrentLimit = true;
					similarityHits = new ScoreDoc[0];
					
					// compute the golden standard on the source corpus if different from the  target
					if(!queryController.sourceEqualsTarget) {
						TopScoreDocCollector similarityCollector = TopScoreDocCollector.create(queryController.maxResultsToConsider[1], true);		    
						queryController.sourceSearcher.search(similarityQuery, similarityCollector);
						similarityHits = similarityCollector.topDocs().scoreDocs;
						// If there are no hits return
						if(similarityHits.length == 0 && !queryController.intConfig.isDoMachineLearning()) return;
						// The maximum score, is the score between an item and itself, it is the gold standard
						if(!queryController.intConfig.isDoMachineLearning()) goldStandardForSimilarity = similarityHits[0].score;
					}
					
					
					
					while(goodResultsExceedingCurrentLimit){
						if(queryIteration > 1 && queryIteration == queryController.maxResultsToConsider.length-1 && !queryController.intConfig.isDoMachineLearning()){
							System.out.println("WARNING - STATISTICAL ANOMALY - Over "+queryController.maxResultsToConsider[queryIteration-1]+" results found for label "+label+" (URI: "+uri+")");
							return;
						}
						TopScoreDocCollector similarityCollector = TopScoreDocCollector.create(queryController.maxResultsToConsider[queryIteration], true);		    
						queryController.targetSearcher.search(similarityQuery, similarityCollector);
						similarityHits = similarityCollector.topDocs().scoreDocs;
						// If there are no hits return
						if(similarityHits.length == 0 && !queryController.intConfig.isDoMachineLearning()) return;
						// The maximum score, is the score between an item and itself, it is the gold standard
						if(queryIteration == 0 && queryController.sourceEqualsTarget && !queryController.intConfig.isDoMachineLearning()) {
							goldStandardForSimilarity = similarityHits[0].score;
						}
						if(similarityHits.length < queryController.maxResultsToConsider[queryIteration]){
							// The query returned less results than the maximum: no need to expand it
							goodResultsExceedingCurrentLimit = false;
						} else {
							float worseScore = similarityHits[similarityHits.length-1].score / goldStandardForSimilarity;
							if(worseScore < queryController.thresholdOfAcceptance) {
								// There might be more results than the maximum, but they won't be acceptable according to the give threshold
								// there fore there is no need to expand the query further.
								goodResultsExceedingCurrentLimit = false;
							} else {
								// There could be more results, and they could be above the threshold, it is necessary to expand the query and get more results
								queryIteration++;
								if(queryIteration >= queryController.maxResultsToConsider.length) {
									// The maximum number of results has been hit, no more should be taken into account
									goodResultsExceedingCurrentLimit = false;
								}
							}
						}
					}
					
					occurrences = similarityHits.length;
				} 
				// compute the category generality
				Map<String,Integer> matchedTypes = new HashMap<String,Integer>();
				for(int i = 0; i < similarityHits.length; i++){
					String category = queryController.targetSearcher.doc(similarityHits[i].doc).get("topcategory");
					if(matchedTypes.containsKey(category)){
						matchedTypes.put(category,new Integer(matchedTypes.get(category)+1));
					} else {
						matchedTypes.put(category,new Integer(1));
					}
				}
				int[] thresholds = new int[3];
				thresholds[0] = Math.max(Math.min(2, occurrences-1), (int) Math.round(occurrences/(Math.max(1,20*5))));
				thresholds[1] = Math.max(Math.min(2, occurrences-1), (int) Math.round(occurrences/(Math.max(1,20*18))));
				thresholds[2] = Math.max(Math.min(2, occurrences-1), (int) Math.round(occurrences/(Math.max(1,20*50))));
				int aboveThreshold[] = new int[3];
				for(Integer i : matchedTypes.values()){
					for(int x = 0; x < thresholds.length; x++){
						if(i >= thresholds[x]) aboveThreshold[x] = aboveThreshold[x]+1;
					}
				}
				
				
				if(queryController.intConfig.isDoMachineLearning()) {
					List<ScoreDoc> hitDocs2 = new LinkedList<ScoreDoc>();
					
					// retrieve the matches from the evaluation corpus
					if(queryController.evaluationMapPositive.containsKey(uri)) for(String s : queryController.evaluationMapPositive.get(uri)){
						BooleanQuery qURIdec = new BooleanQuery();
						qURIdec.add(new TermQuery(new Term("uri",s)), Occur.MUST);
						TopScoreDocCollector collector2 = TopScoreDocCollector.create(1, true);
						queryController.targetSearcher.search(qURIdec, collector2);
						ScoreDoc[] hits2 = collector2.topDocs().scoreDocs;
						if(hits2.length > 0) {
							hitDocs2.add(hits2[0]);
							System.out.print("+");
						}
					}
					if(queryController.evaluationMapNegative.containsKey(uri)) for(String s : queryController.evaluationMapNegative.get(uri)){
						BooleanQuery qURIdec = new BooleanQuery();
						qURIdec.add(new TermQuery(new Term("uri",s)), Occur.MUST);
						TopScoreDocCollector collector2 = TopScoreDocCollector.create(1, true);
						queryController.targetSearcher.search(qURIdec, collector2);
						ScoreDoc[] hits2 = collector2.topDocs().scoreDocs;
						if(hits2.length > 0) {
							System.out.print("-");
							hitDocs2.add(hits2[0]);
						}
					}
					similarityHits = hitDocs2.toArray(new ScoreDoc[0]);
				}
				
				// Iterate through all the results
				String debugPrint = "";
				
				
				for(int i = queryController.sourceEqualsTarget ? 1 : 0; i < similarityHits.length; i++){
					Document similarDoc = queryController.targetSearcher.doc(similarityHits[i].doc);
					String similarDocURI = similarDoc.get("uri");
					String similarDocLabel = similarDoc.get("label");
					
					if(queryController.intConfig.isLuceneQueryDecompositionLinks()){
						// Test that the hit document does not represent another entity on the same page:
						String similaritySource = queryController.extractSourceFromURI(similarDocURI);
						if(!similaritySource.equals(source)){
							
							float maxCoreToCoreOverlap = 0;
							float maxContextToCoreOverlap = 0;
							boolean sameNegationCore = true;
							boolean hasSubjectCore = false;
							boolean hasSubjectCoreOther = false;
							
							float score = 0;
							// factor the length of the phrase (removed as it is kinda considered in the idf)
							//score += Math.min(1f, Math.max(0f, (queryController.splitBySpace(label.replaceAll("how to", "").replaceAll("the", "").replaceAll("a", "").replaceAll("an", "")).length*0.1f)));

							// check word similarity between neighbours 
							List<String> neighboursAllOther = new LinkedList<String>();
							List<String> neighboursCoreOther = new LinkedList<String>();
							queryController.getAllWordsFromURIs(similarDocURI,neighboursAllOther,neighboursCoreOther);
							for(int k = 0; k < queryController.intConfig.getMaxSentencesToConsider(); k++){
								if(similarDoc.get("sentence"+k) != null) for(String s : similarDoc.get("sentence"+k).split("\\s+")) neighboursAllOther.remove(s);
								if(similarDoc.get("core"+k) != null) for(String s : similarDoc.get("core"+k).split("\\s+")) neighboursCoreOther.remove(s);
							}
							
							float contextCoreToCore = 0f;
							float contextAllToAll = 0f;
							int numberMatchedCore = 0;
							int numberMatchedAll = 0;
							/*for(String s : neighboursAll){
								for(String so : neighboursAllOther){
									if(s.equals(so)){
										numberMatchedAll++;
										contextAllToAll += queryController.getIDF(s);
									}
								}
							}
							for(String s : neighboursCore){
								for(String so : neighboursCoreOther){
									if(s.equals(so)){
										numberMatchedCore++;
										contextCoreToCore += queryController.getIDF(s);
									}
								}
							}*/
							float contextCoreToCoreNormalized = contextCoreToCore/numberMatchedCore;
							float contextAllToAllNormalized = contextAllToAll/numberMatchedAll;
							
							List<String> commonWordslabelTOotherContext = new LinkedList<String>();
							List<String> commonWordsotherlabelTOcontext = new LinkedList<String>();
							
							// compute the distance score
							List<Float> distanceScores = new LinkedList<Float>();
							int[] orderScores = new int[122];
							float exactLableMatch = 0;
							int offsetFirstMatch = 0;
							int offsetFirstMatchInSentence = 0;
							int offsetFirstMatchAndPhrase = 0;
							int offsetPhraseFirstMatch = 0;
							//boolean samenegationvalue = true;
							
							//Check categories
							String exactCategoryOther = similarDoc.get("exactcategory");
							String topCategoryOther = similarDoc.get("topcategory");
							String allCategoriesOther = similarDoc.get("allcategories");
							int numberOfCategoriesInCommon = 0;
							if(topCategory != null && topCategoryOther != null && topCategoryOther.equals(topCategory)){
								for(String s : allCategories.split("\\s+")){
									if(allCategoriesOther.indexOf(s) >= 0){
										numberOfCategoriesInCommon++;
									}
								}
							}
							boolean isFictionalOther = false;
							if(allCategoriesOther != null && (allCategoriesOther.indexOf("http://www.wikihow.com/category:video-games") != -1 || allCategoriesOther.indexOf("http://snapguide.com/guides/topic/games-tricks") != -1)) isFictionalOther = true;
							
							if(isFictionalOther != isFictional){
								if(!queryController.intConfig.isDoMachineLearning()) 
									return;
							}
							
							for(int x = 0; x < queryController.intConfig.getMaxSentencesToConsider(); x++){
								String sentenceString = hitDoc.get("sentence"+x);
								boolean hasSubject = hitDoc.get("hassubject"+x) == null || hitDoc.get("hassubject"+x).equals("true");
								boolean isPositive = hitDoc.get("isnegative"+x) == null || hitDoc.get("isnegative"+x).equals("false");
								String coreWords = hitDoc.get("core"+x);
								String contextWords = hitDoc.get("context"+x);
								
								if(sentenceString!= null && sentenceString.length() > 0) 
								for(int y = 0; y < queryController.intConfig.getMaxSentencesToConsider(); y++){
									String otherSentenceString = similarDoc.get("sentence"+y);
									if(otherSentenceString != null && otherSentenceString.length()>0){
										boolean allWords = true;
										String[] mainLabelSplit = queryController.splitBySpace(sentenceString);
										for(String s : mainLabelSplit){
											if(!otherSentenceString.contains(s)){
												allWords = false;
												offsetFirstMatch += queryController.splitBySpace(otherSentenceString).length;
												offsetPhraseFirstMatch++;
												break;
											}
										}
										for(String s : mainLabelSplit){
											if(neighboursAllOther.contains(s)){commonWordslabelTOotherContext.add(s);}
										}
										if(allWords && mainLabelSplit.length > 0){
											// simple nlp checks
											String[] otherLabelSentenceString = queryController.splitBySpace(otherSentenceString);
											for(String s : otherLabelSentenceString){
												if(neighboursAll.contains(s)){commonWordsotherlabelTOcontext.add(s);}
											}
											offsetFirstMatch += queryController.indexOf(mainLabelSplit[0], otherLabelSentenceString);
											offsetFirstMatchInSentence = queryController.indexOf(mainLabelSplit[0], otherLabelSentenceString);
											offsetFirstMatchAndPhrase = offsetFirstMatch + y;
											/*if(!queryController.sameNegationValue(hitDoc.get("sentence"+x),similarDoc.get("sentence"+y),hitDoc.get("label"),similarDoc.get("label"))){
												//samenegationvalue = false;
												if(!queryController.intConfig.isDoMachineLearning()) return;
											}*/
											distanceScores.add(new Float(queryController.computeWordOrderScore(hitDoc.get("sentence"+x),similarDoc.get("sentence"+y))));
											orderScores = queryController.computeWordOrderScoreDetailed(hitDoc.get("sentence"+x),similarDoc.get("sentence"+y));
											exactLableMatch = queryController.computeExactLabelMatchScore(queryController.splitBySpace(hitDoc.get("label").replaceAll("[^a-zA-Z0-9\\s]", "")),queryController.splitBySpace(similarDocLabel.replaceAll("[^a-zA-Z0-9\\s]", "")));
											
											break;
										}
									}
								}
								
								for(int y = 0; y < queryController.intConfig.getMaxSentencesToConsider(); y++) {
									// complex nlp features
									String coreWordsOther = similarDoc.get("core"+x);
									if(coreWordsOther != null && coreWords != null){
										float tempMaxCoreToCoreOverlap = 0;
										String[] coreWordsSplit = coreWords.split("\\s+");
										for(String s : coreWordsSplit){
											if(coreWordsOther.indexOf(s) >= 0) tempMaxCoreToCoreOverlap +=1;
										}
										tempMaxCoreToCoreOverlap = tempMaxCoreToCoreOverlap/coreWordsSplit.length;
										if(tempMaxCoreToCoreOverlap > maxCoreToCoreOverlap){
											maxCoreToCoreOverlap = tempMaxCoreToCoreOverlap;
											hasSubjectCore = hasSubject;
											hasSubjectCoreOther = similarDoc.get("hassubject"+x) == null || similarDoc.get("hassubject"+x).equals("true");
											boolean isPositiveOther = similarDoc.get("isnegative"+x) == null || similarDoc.get("isnegative"+x).equals("false");
											sameNegationCore = isPositiveOther == isPositive;
										}
									}
									String contextWordsOther = similarDoc.get("context"+x);
									if(contextWordsOther != null && contextWords != null){
										float tempMaxContextToCoreOverlap = 0;
										String[] contextWordsSplit = coreWords.split("\\s+");
										for(String s : contextWordsSplit){
											if(contextWordsOther.indexOf(s) >= 0) tempMaxContextToCoreOverlap +=1;
										}
										tempMaxContextToCoreOverlap = tempMaxContextToCoreOverlap/contextWordsSplit.length;
										if(tempMaxContextToCoreOverlap > maxContextToCoreOverlap){
											maxContextToCoreOverlap = tempMaxContextToCoreOverlap;
										}
									}
								}
							}
							// calculate idf of
							int commonWordslabelTOotherContextidf = 0;
							for(String s : commonWordslabelTOotherContext){
									int termFrequency = 0;
									for(int z = 0; z < queryController.intConfig.getMaxSentencesToConsider(); z++){
										Term t = new Term("sentence"+z,s);
										termFrequency += queryController.targetSearcher.getIndexReader().docFreq(t);
									}
									float idf = queryController.similarity.idf(termFrequency, queryController.targetSearcher.getIndexReader().numDocs());
									commonWordslabelTOotherContextidf += idf;
							}
							int commonWordsotherlabelTOcontextidf = 0;
							for(String s : commonWordsotherlabelTOcontext){
									int termFrequency = 0;
									for(int z = 0; z < queryController.intConfig.getMaxSentencesToConsider(); z++){
										Term t = new Term("sentence"+z,s);
										termFrequency += queryController.targetSearcher.getIndexReader().docFreq(t);
									}
									float idf = queryController.similarity.idf(termFrequency, queryController.targetSearcher.getIndexReader().numDocs());
									commonWordsotherlabelTOcontextidf += idf;
							}
							
							
							float orderScore = 0;
							if(distanceScores.size() > 0) orderScore = distanceScores.get(0);
							// if there are multiple phrases for the decomposing entity, then take the worse order score
							for(int j = 1; j < distanceScores.size(); j++){
								if(distanceScores.get(j) < orderScore) orderScore = distanceScores.get(j);
							}
							score += orderScore;
							// if it is trusted similarity give a bonus
							if(queryController.isTrustedSimilarity(hitDoc.get("label"),similarDocLabel)) score += 0.3f;
							// if there is no verb give a penalty
							if(!queryController.textPreProcessing.containsVerb(hitDoc.get("label"))) score += -0.4f;
							
							score += cumulativeidfnormalized2;
							//System.out.println(cumulativeidfnormalized2+"%  <>  "+cumulativeidfnormalized+"  <<>>  "+hitDoc.get("sentence0")+cumulativeidf);

							
							
							//check if it is wikihow or if it is snapguide
							boolean iswikihow = false;
							boolean iswikihowother = false;
							if(source.indexOf("http://www.wikihow.com/")>=0) iswikihow = true;
							if(similarDocURI.indexOf("http://www.wikihow.com/")>=0) iswikihowother = true;
							
							double[] vals = new double[queryController.data.numAttributes()-1];
							if(queryController.intConfig.isDoMachineLearning()){
								vals = new double[queryController.data.numAttributes()];
							}
							{
								queryController.add1ToAllMLmatches();
								//CREATE THE DATASET FOR THE MACHINE LEARNING WEKA ALGORITHMS
								vals[queryController.data.attribute("idf").index()] = cumulativeidf;
								vals[queryController.data.attribute("idfnormalized").index()] = cumulativeidfnormalized;
								vals[queryController.data.attribute("maxCoreToCoreOverlap").index()] = maxCoreToCoreOverlap;
								vals[queryController.data.attribute("maxContextToCoreOverlap").index()] = maxContextToCoreOverlap;
								vals[queryController.data.attribute("bestidf").index()] = bestidf;
								//vals[queryController.data.attribute("lucenescore").index()] = luceneScoreRaw;
								//vals[queryController.data.attribute("lucenescorenormalized").index()] = luceneScoreNormalised;
								vals[queryController.data.attribute("exactlabelmatch").index()] = exactLableMatch;
								vals[queryController.data.attribute("occurrences").index()] = occurrences;
								for(int x = 0; x < aboveThreshold.length; x++){
									vals[queryController.data.attribute("categories"+x).index()] = aboveThreshold[x];
								}
								vals[queryController.data.attribute("offsetOfFirstWordOccurrence").index()] = offsetFirstMatch;
								vals[queryController.data.attribute("offsetFirstMatchInSentence").index()] = offsetFirstMatchInSentence;
								vals[queryController.data.attribute("offsetFirstMatchAndPhrase").index()] = offsetFirstMatchAndPhrase;
								//vals[queryController.data.attribute("offsetOfFirstWordOccurrencePhrases").index()] = offsetPhraseFirstMatch;
								vals[queryController.data.attribute("orderCorrect").index()] = orderScores[0];
								//vals[queryController.data.attribute("orderForward").index()] = orderScores[1];
								//vals[queryController.data.attribute("orderBackwards").index()] = orderScores[2];
								vals[queryController.data.attribute("orderForwardAll").index()] = orderScores[3];
								//vals[queryController.data.attribute("orderBackwardsAll").index()] = orderScores[4];
								//vals[queryController.data.attribute("orderMissing").index()] = orderScores[5];
								//vals[queryController.data.attribute("orderCorrectNorm").index()] = orderScores[6];
								vals[queryController.data.attribute("orderForwardNorm").index()] = orderScores[7];
								vals[queryController.data.attribute("orderBackwardsNorm").index()] = orderScores[8];
								vals[queryController.data.attribute("orderForwardAllNorm").index()] = orderScores[9];
								vals[queryController.data.attribute("orderBackwardsAllNorm").index()] = orderScores[10];
								vals[queryController.data.attribute("orderMissingNorm").index()] = orderScores[11];
								
								vals[queryController.data.attribute("wordlengthsentences").index()] = queryController.splitBySpace(label.replaceAll("how to ", "").replaceAll("the ", "").replaceAll("a ", "").replaceAll("an ", "")).length;
								vals[queryController.data.attribute("wordlength").index()] = label.replaceAll("how to", "").replaceAll("the ", "").replaceAll("a ", "").replaceAll("an ", "").length();
								
								/*vals[queryController.data.attribute("contextCoreToCore").index()] = contextCoreToCore;
								vals[queryController.data.attribute("contextAllToAll").index()] = contextAllToAll;
								vals[queryController.data.attribute("contextCoreToCoreNormalized").index()] = contextCoreToCoreNormalized;
								vals[queryController.data.attribute("contextAllToAllNormalized").index()] = contextAllToAllNormalized;*/
								vals[queryController.data.attribute("commonWordslabelTOotherContext").index()] = commonWordslabelTOotherContextidf;
								vals[queryController.data.attribute("commonWordsotherlabelTOcontext").index()] = commonWordsotherlabelTOcontextidf;
								int indexTRUE = queryController.attVals.indexOf("true");
								int indexFALSE = queryController.attVals.indexOf("false");
								// 
								if(sameNegationCore){
									vals[queryController.data.attribute("sameNegationCore").index()] = indexTRUE;
								} else { vals[queryController.data.attribute("sameNegationCore").index()] = indexFALSE;	}
								//
								// check if a link between URLs already exists in the user generated dataset
								if(!iswikihow){
									vals[queryController.data.attribute("hasUserGeneratedLink").index()] = queryController.attVals2.indexOf("unknown");
									vals[queryController.data.attribute("isUserGeneratedDecomposition").index()] = queryController.attVals2.indexOf("unknown");
								} else {
									if(queryController.isUserGeneratedDecomposition(source)){
										vals[queryController.data.attribute("isUserGeneratedDecomposition").index()] = queryController.attVals2.indexOf("true");
									} else { vals[queryController.data.attribute("isUserGeneratedDecomposition").index()] = queryController.attVals2.indexOf("false");	}
									if(!iswikihowother){
										vals[queryController.data.attribute("hasUserGeneratedLink").index()] = queryController.attVals2.indexOf("unknown");
									} else if(queryController.areURLsUserConnected(source, similaritySource)){
										vals[queryController.data.attribute("hasUserGeneratedLink").index()] = queryController.attVals2.indexOf("true");
									} else { vals[queryController.data.attribute("hasUserGeneratedLink").index()] = queryController.attVals2.indexOf("false");	}
								}
								
								// 
								
								/*// 
								if(hasSubjectCore){
									vals[queryController.data.attribute("hasSubjectCore").index()] = indexTRUE;
								} else { vals[queryController.data.attribute("hasSubjectCore").index()] = indexFALSE;	}
								// 
								if(hasSubjectCoreOther){
									vals[queryController.data.attribute("hasSubjectCoreOther").index()] = indexTRUE;
								} else { vals[queryController.data.attribute("hasSubjectCoreOther").index()] = indexFALSE;	}*/
								// has how-to
								if(queryController.isTrustedSimilarity(hitDoc.get("label"),similarDocLabel)){
									vals[queryController.data.attribute("hashowto").index()] = indexTRUE;
								} else { vals[queryController.data.attribute("hashowto").index()] = indexFALSE;	}
								// contains verb
								if(queryController.textPreProcessing.containsVerb(hitDoc.get("label"))){
									vals[queryController.data.attribute("hasverb").index()] = indexTRUE;
								} else { vals[queryController.data.attribute("hasverb").index()] = indexFALSE; }
								// has same negation value
								/*if(samenegationvalue){
									vals[queryController.data.attribute("hassamenegationvalues").index()] = indexTRUE;
								} else { vals[queryController.data.attribute("hassamenegationvalues").index()] = indexFALSE; }*/
								// is decomposition wikihow
								if(iswikihow){ 
									vals[queryController.data.attribute("iswikihow").index()] = indexTRUE;
								} else { vals[queryController.data.attribute("iswikihow").index()] = indexFALSE; }
								// is decomposed wikihow
								if(iswikihowother){
									vals[queryController.data.attribute("iswikihowother").index()] = indexTRUE;
								} else { vals[queryController.data.attribute("iswikihowother").index()] = indexFALSE; }
								

								vals[queryController.data.attribute("numberOfSharedCategories").index()] = numberOfCategoriesInCommon;

								if(exactCategory != null && exactCategoryOther != null && exactCategory.equals(exactCategoryOther)){
									vals[queryController.data.attribute("sameExactCategory").index()] = indexTRUE;
								} else {
									vals[queryController.data.attribute("sameExactCategory").index()] = indexFALSE;
								}
								if(topCategory != null && topCategoryOther != null && topCategory.equals(topCategoryOther)){
									vals[queryController.data.attribute("sameTopCategory").index()] = indexTRUE;
								} else {
									vals[queryController.data.attribute("sameTopCategory").index()] = indexFALSE;
								}
								
								if(queryController.intConfig.isDoMachineLearning()){
									Boolean positiveExample = queryController.evaluationMap.get(similarDoc.get("uri")+hitDoc.get("uri"));
									if(positiveExample == null) 
										return;
									// positive or negative
									if(positiveExample){
										vals[queryController.data.attribute("target").index()] = indexTRUE;
									} else {
										vals[queryController.data.attribute("target").index()] = indexFALSE;
									}
									// add
									Instance newinstance = new Instance(1.0, vals);
									if(newinstance == null /*|| (! iswikihow || ! iswikihowother)*/){
										System.out.println("WARNING - NULL INSTANCE");
									}else{
										queryController.data.add(newinstance);
									}
									
								} else {
									Instance newinstance = new Instance(1.0, vals);
									int prediction = queryController.rankInstance(newinstance);
									// SAVE THE LINKS IN THE RESULT FILE
									{
										if(Math.random() > 0.99){
											if(prediction == 0){
												System.out.println("NO: "+label);
												System.out.println("NO: "+similarDocLabel);											
											} else if(prediction == 1) {
												System.out.println("("+prediction+")  1+ "+label);
												System.out.println("("+prediction+")  1+ "+similarDocLabel);		
											} else if(prediction == 2) {
												System.out.println("("+prediction+")  2++ "+label);
												System.out.println("("+prediction+")  2++ "+similarDocLabel);		
											} else if(prediction == 3) {
												System.out.println("("+prediction+")  3+++ "+label);
												System.out.println("("+prediction+")  3+++ "+similarDocLabel);		
											} else if(prediction == 4) {
												System.out.println("("+prediction+")  4++++ "+label);
												System.out.println("("+prediction+")  4++++ "+similarDocLabel);		
											} else if(prediction == 5) {
												System.out.println("("+prediction+")  5+++++ "+label);
												System.out.println("("+prediction+")  5+++++ "+similarDocLabel);		
											}  else if(prediction == 6) {
												System.out.println("("+prediction+")  6++++++ "+label);
												System.out.println("("+prediction+")  6++++++ "+similarDocLabel);		
											}  else if(prediction == 7) {
												System.out.println("("+prediction+")  7+++++++ "+label);
												System.out.println("("+prediction+")  7+++++++ "+similarDocLabel);		
											}  else if(prediction == 8) {
												System.out.println("("+prediction+")  8++++++++ "+label);
												System.out.println("("+prediction+")  8++++++++ "+similarDocLabel);		
											}  else if(prediction == 9) {
												System.out.println("("+prediction+")  9+++++++++ "+label);
												System.out.println("("+prediction+")  9+++++++++ "+similarDocLabel);		
											}  else if(prediction == 10) {
												System.out.println("("+prediction+")  10++++++++++ "+label);
												System.out.println("("+prediction+")  10++++++++++ "+similarDocLabel);		
											} 
										}
										
										File writeFile = new File(queryController.pathToResultsDirectory+queryController.fileNames[prediction]+".ttl");
										
										if(writeFile.exists() && writeFile.length() > 200000000){
											queryController.fileNames[prediction] = queryController.fileNames[prediction]+"i";
										}
										// record statistics
										if(prediction >= 1){
											queryController.addStepEntity(uri);
											queryController.addDecomposedStepEntities(similarDocURI);
											queryController.stepEntitiesLinks++;
										}
										queryController.writeLine(queryController.fileNames[prediction]+".ttl"," <"+similarDocURI+"> prohow:has_step <"+uri+"> . # "+score);
										/*if(queryController.getEntitiesConsidered() % 10 == 0 && i >= 0 && i < 6 && score >= queryController.thresholdOfAcceptance){
											if(!introSystemPrint){
												debugPrint = debugPrint+"> "+hitDoc.get("label")+"  <-> (### "+queryController.getEntitiesConsidered()+" ###) URI: " + uri+" Gold standard score: "+goldStandardForSimilarity+"\n";//hitDoc.get("uri"));
												introSystemPrint = true;
											}
											if(queryController.isTrustedSimilarity(label,similarDocLabel)) debugPrint = debugPrint+"*";
											debugPrint = debugPrint+"   "+i+") ("+score+" score) ->  "+similarDocLabel+"  <-> URI: "+similarDocURI+" Raw Score: "+similarityHits[i].score+"\n";
										}*/
									}
								}
							} 
							
						}
						else {
							queryController.similarEntitiesInSameSource++;
						}
					} 
					
				}
				if(debugPrint.length() > 0) System.out.print(debugPrint);
			} else {
				//System.out.println("ERROR - COULD NOT FIND ENTITY "+uri+" IN THE INDEX");
			}
		} catch (IOException e) {
			System.out.println("ERROR - Input/Output problem detected");
			e.printStackTrace();
		}
	}

}
