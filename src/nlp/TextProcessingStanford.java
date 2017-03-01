package nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.jena.atlas.lib.Pair;

import wordnet.WordnetAPI;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class TextProcessingStanford {
	
	private  Morphology morpha = new Morphology();
	
	public ParsedSentenceSynsets[] parseTextIntoSentenceSynsets(String text, int maxSentences){
		ParsedSentenceSynsets[] resultSentences = new ParsedSentenceSynsets[maxSentences];
		// Annotate the text
		Annotation textAnnotated = new Annotation(text);
		pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    sentences = sentences.subList(0, Math.min(sentences.size(), maxSentences));
		// Break down text into sentences
	    int sentenceIndex = 0;
	    for(CoreMap sentence: sentences) {
 	    	  resultSentences[sentenceIndex] = parseSentenceSynsetsTokenBased(sentence);
		      //Tree tree = sentence.get(TreeAnnotation.class);
		      //resultSentences[sentenceIndex] = parseSentenceSynset(tree);
		      sentenceIndex++;
		    }
		//
		return resultSentences;
	}
	
	public List<String> getWordsSynsets(String text, int maxWords){
		List<String> words = new LinkedList<String>();

		Annotation textAnnotated = new Annotation(text);
		pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    for(CoreMap sentence: sentences) {
	    	  List<String> wordsInSentence = parseWordSynsetsTokenBased(sentence);
	    	  for(String s : wordsInSentence){
	    		  words.add(s);
	    		  if(words.size() >= maxWords) return words;
	    	  }
		    }
		return words;
	}
	
	public List<List<String>> getAllWordsSynsetsBySentence(String text){
		List<List<String>> sentencesSynsets = new LinkedList<List<String>>();
		List<String> words = new LinkedList<String>();

		Annotation textAnnotated = new Annotation(text);
		pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    for(CoreMap sentence: sentences) {
	    	  List<String> wordsInSentence = parseWordSynsetsTokenBased(sentence);
	    	  if(wordsInSentence.size()>0){
	    		  sentencesSynsets.add(wordsInSentence);
	    	  }
		    }
		return sentencesSynsets;
	}
	
	public List<ParsedPhraseExtended> getPhrasesExtended(String text, int maxPhrases){
		List<ParsedPhraseExtended> phrases = new LinkedList<ParsedPhraseExtended>();
		Annotation textAnnotated = new Annotation(text);
		pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    for(CoreMap sentence: sentences) {
	    		List<ParsedPhraseExtended> phrasesInSentence = parsePhrasesTokenBased(sentence);
	    		for(ParsedPhraseExtended p : phrasesInSentence){
	    			phrases.addAll(phrasesInSentence);
	    			if(phrases.size() >= maxPhrases) return phrases;
	    		}
		    }
		return phrases;
	}
	
	private ParsedSentenceSynsets parseSentenceSynset(Tree tree){
		ParsedSentenceSynsets parsedSentence = new ParsedSentenceSynsets();
		parsedSentence.sentence = tree.toString();
		// TODO here I am just re-using the old system
		List<String> subjects = null;
		List<String> predicates = null;
		List<String> objects = null;
		Tree firstNP = treeFindNP(tree);
		if(firstNP != null) subjects = treeFindNouns(firstNP);
		Tree firstVP = treeFindVP(tree);
		if(firstVP!= null){
			Tree firstNPobject = treeFindNP(firstVP);
			if(firstNPobject != null) objects = treeFindNouns(firstNPobject);
			predicates = treeFindVerbs(firstVP);
		}
		/*if(predicates != null && predicates.size() > 1){
			return parsedSentence;
		}*/
		if (subjects != null) for(String s : subjects){
			parsedSentence.addToOtherSynsets(wordnetAPI.lookupSynset(s, "noun"));
			parsedSentence.others = parsedSentence.others+" "+s;
		}
		if (objects != null) for(String s : objects){
			parsedSentence.addToObjectSynsets(wordnetAPI.lookupSynset(s, "noun")); 
			parsedSentence.object = parsedSentence.object+" "+s;
		}
		if (predicates != null) for(String s : predicates){
			parsedSentence.addToVerbSynsets(wordnetAPI.lookupSynset(s, "verb")); 
			parsedSentence.verb = parsedSentence.verb+" "+s;
		}
		//
		return parsedSentence;
	}
	
	private String cleanWord(String word){
		word = word.replaceAll("[^A-Za-z0-9]", "");
		word = word.toLowerCase().trim();
		return word;
	}
	private String getStringSynsetsRepresentation(List<String> synsets){
		String synsetsRepresentation = "";
		Iterator<String> synsetIterator = synsets.iterator();
		while(synsetIterator.hasNext()){
			synsetsRepresentation = synsetsRepresentation += " "+synsetIterator.next();
			}
		return synsetsRepresentation.trim();
	}
	private List<String> parseWordSynsetsTokenBased(CoreMap sentence){
		List<String> others = new LinkedList<String>();
		List<String> predicates = new LinkedList<String>();
		List<String> objects = new LinkedList<String>();
		List<String> words = new LinkedList<String>();
	    for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        // this is the text of the token
	        String word = token.get(TextAnnotation.class);
	        word = cleanWord(word);
	        if(!controller.config.isBlockedWord(word) && word.length() > 1){
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        //String ne = token.get(NamedEntityTagAnnotation.class);  
		        String simplifiedPOS = null;
		        // adding !contains() to avoid sentences like: "Talk! Talk! Talk!" to get super relevant every time the word
		        // talk is mentioned. The verb talk should only be considered once.
		        if(pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ")){
		        	List<String> synsets = wordnetAPI.lookupSynset(word, "verb");
					if(synsets.size() == 0) {if(!predicates.contains(word)) predicates.add(word);}
					else {if(!predicates.contains(synsets)) predicates.add(getStringSynsetsRepresentation(synsets));}
		        }
		        else if(pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") || pos.equals("NNPS") || pos.equals("FW") || pos.equals("SYM")){
		        	List<String> synsets = wordnetAPI.lookupSynset(word, "noun");
		        	if(synsets.size() == 0) {if(!objects.contains(word)) objects.add(word);}
		        	else {if(!objects.contains(synsets)) objects.add(getStringSynsetsRepresentation(synsets));}
		        }
		        else if(pos.equals("JJ") || pos.equals("JJR") || pos.equals("JJS")){
		        	List<String> synsets = wordnetAPI.lookupSynset(word, "adjective");
		        	if(synsets.size() == 0) {if(!others.contains(word)) others.add(word);}
		        	else {if(!others.contains(synsets)) others.add(getStringSynsetsRepresentation(synsets));}
		        }
		        else if( pos.equals("RB") || pos.equals("RBR") || pos.equals("RBS") ){
		        	List<String> synsets = wordnetAPI.lookupSynset(word, "adverb");
		        	if(synsets.size() == 0) {if(!others.contains(word)) others.add(word);}
		        	else {if(!others.contains(synsets)) others.add(getStringSynsetsRepresentation(synsets));}
		        } else if( pos.equals("PRP")){
		        	//if(word.equals("I") || word.equals("me") || word.equals("myself")) others.add("I(pronoun)");
		        	if(word.equals("we") || word.equals("us") || word.equals("ourselves")) 
		        		{if(!others.contains("we(pronoun)")) others.add("we(pronoun)");}
		        	//else if(word.equals("you") || word.equals("yourself") || word.equals("yourselves")) others.add("you(pronoun)");
		        	else if(word.equals("he") || word.equals("him") || word.equals("himself")) 
		        		{if(!objects.contains("he(pronoun)")) objects.add("he(pronoun)");}
		        	else if(word.equals("she")|| word.equals("her") || word.equals("herself")) 
		        		{if(!objects.contains("she(pronoun)")) objects.add("she(pronoun)");}
		        	//else if(word.equals("it")) objects.add("it(pronoun)");
		        	else if(word.equals("they") || word.equals("them")|| word.equals("themself")|| word.equals("themselves")) 
		        		{if(!objects.contains("they(pronoun)")) objects.add("they(pronoun)");}
		        } else if( pos.equals("PRP$")){
		        	//if(word.equals("my") || word.equals("mine")) others.add("my(possessive)");
		        	//else if(word.equals("your") || word.equals("yours")) others.add("you(possessive)");
		        	if(word.equals("his")) 
		        		{if(!others.contains("his(possessive)")) others.add("his(possessive)");}
		        	else if(word.equals("her")|| word.equals("hers")) 
	        			{if(!others.contains("hers(possessive)")) others.add("hers(possessive)");}
		        	else if(word.equals("their") || word.equals("theirs")) 
		        		{if(!others.contains("their(possessive)")) others.add("their(possessive)");}
		        	else if(word.equals("our") || word.equals("ours")) 
		        		{if(!others.contains("our(possessive)")) others.add("our(possessive)");}
		        }
	        }
	      }
	    for(int i = 0; i< Math.max(predicates.size(), objects.size()); i++){
	    	if( i < objects.size())
	    		words.add(objects.get(i));
	    	if( i < predicates.size())
	    		words.add(predicates.get(i));
	    }
	    for(int i = 0; i< others.size(); i++){
	    	words.add(others.get(i));
	    }
	    return words;
	}
	
	private List<ParsedPhraseExtended> parsePhrasesTokenBased(CoreMap sentence){
		List<ParsedPhraseExtended> phrases = new LinkedList<ParsedPhraseExtended>();
		ParsedPhraseExtended newPhrase = new ParsedPhraseExtended();
	    for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        // this is the text of the token
	        String word = token.get(TextAnnotation.class);
	        word = cleanWord(word);
	        if(!controller.config.isBlockedWord(word) && word.length() > 1){
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        //String ne = token.get(NamedEntityTagAnnotation.class);  
		        // adding !contains() to avoid sentences like: "Talk! Talk! Talk!" to get super relevant every time the word
		        // talk is mentioned. The verb talk should only be considered once.
		        if(pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ")){
		        	if(newPhrase.getVerbs().size() > 0) {
		        		phrases.add(newPhrase);
		        		newPhrase = new ParsedPhraseExtended();
		        	}
		        	newPhrase.getVerbs().add(word);
		        	newPhrase.getVerbsSynsets().addAll(wordnetAPI.lookupSynset(word, "verb"));
		        }
		        else if(pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") || pos.equals("NNPS") || pos.equals("FW") || pos.equals("SYM")){
		        	newPhrase.getObjects().add(word);
		        	newPhrase.getObjectsSynsets().addAll(wordnetAPI.lookupSynset(word, "noun"));
		        }
		        else if(pos.equals("JJ") || pos.equals("JJR") || pos.equals("JJS")){
		        	newPhrase.getAdjectives().add(word);
		        	newPhrase.getAdjectivesSynsets().addAll(wordnetAPI.lookupSynset(word, "adjective"));
		        }
		        else if( pos.equals("RB") || pos.equals("RBR") || pos.equals("RBS") ){
		        	// TODO this is just for debug, actually they should be put in the field adverbs, not adjectives
		        	newPhrase.getAdjectives().add(word);
		        	newPhrase.getAdjectivesSynsets().addAll(wordnetAPI.lookupSynset(word, "adjective"));
		        } else if( pos.equals("PRP")){
		        	if(word.equals("he") || word.equals("him") || word.equals("himself")) 
		        		{newPhrase.getObjects().add("he(pronoun)");}
		        	else if(word.equals("she")|| word.equals("her") || word.equals("herself")) 
		        		{newPhrase.getObjects().add("she(pronoun)");}
		        	//else if(word.equals("it")) objects.add("it(pronoun)");
		        	else if(word.equals("they") || word.equals("them")|| word.equals("themself")|| word.equals("themselves")) 
		        	{newPhrase.getObjects().add("they(pronoun)");}
		        }
	        }
	      }
	    if(newPhrase.getVerbs().size() > 0 || newPhrase.getObjects().size() > 0) {
    		phrases.add(newPhrase);
    		}
	    return phrases;
	}
	
	private ParsedSentenceSynsets parseSentenceSynsetsTokenBased(CoreMap sentence){
		ParsedSentenceSynsets parsedSentence = new ParsedSentenceSynsets();
		parsedSentence.sentence = sentence.toString();
		List<String> othersAdverb = new LinkedList<String>();
		List<String> othersAdjective = new LinkedList<String>();
		List<String> predicates = new LinkedList<String>();
		List<String> objects = new LinkedList<String>();
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        // this is the text of the token
	        String word = token.get(TextAnnotation.class);
	        if(!controller.config.isBlockedWord(word)){
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        //String ne = token.get(NamedEntityTagAnnotation.class);  
		        String simplifiedPOS = null;
		        if(pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ")){
		        	predicates.add(word);
		        }
		        if(pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") || pos.equals("NNPS") || pos.equals("FW") || pos.equals("SYM")){
		        	objects.add(word);
		        }
		        if(pos.equals("JJ") || pos.equals("JJR") || pos.equals("JJS")){
		        	othersAdjective.add(word);
		        }
		        if( pos.equals("RB") || pos.equals("RBR") || pos.equals("RBS") ){
		        	othersAdverb.add(word);
		        }
	        }
	      }
	      if (objects != null) for(String s : objects){
	    	    List<String> synsets = wordnetAPI.lookupSynset(s, "noun");
	    	    if(synsets.size() == 0) synsets.add(s);
	    	    parsedSentence.addToObjectSynsets(synsets); 
				//parsedSentence.object = parsedSentence.object+" "+s;
			}
		if (predicates != null) for(String s : predicates){
			List<String> synsets = wordnetAPI.lookupSynset(s, "verb");
			if(synsets.size() == 0) synsets.add(s);
			parsedSentence.addToVerbSynsets(synsets); 
				//parsedSentence.verb = parsedSentence.verb+" "+s;
			}
		if (othersAdverb != null) for(String s : othersAdverb){
			List<String> synsets = wordnetAPI.lookupSynset(s, "adverb");
			if(synsets.size() == 0) synsets.add(s);
			parsedSentence.addToOtherSynsets(synsets); 
			//parsedSentence.others = parsedSentence.others+" "+s;
		}
		if (othersAdjective != null) for(String s : othersAdjective){
			List<String> synsets = wordnetAPI.lookupSynset(s, "adjective");
			if(synsets.size() == 0) synsets.add(s);
			parsedSentence.addToOtherSynsets(synsets); 
			//parsedSentence.others = parsedSentence.others+" "+s;
		}
			return parsedSentence;
	}
	
	TextProcessingController controller;
	StanfordCoreNLP pipeline;
	StanfordCoreNLP pipeline2;
	WordnetAPI wordnetAPI;
	
	public TextProcessingStanford(TextProcessingController controller, WordnetAPI wordnetAPI){
		this.controller = controller;
	    this.wordnetAPI = wordnetAPI;
	}
	
	public void initialiseAllPipelines(){
		initialisePOSpipeline();
		initialiseSyntacticTreePipeline();
	}
	
	public void initialisePOSpipeline(){
		 Properties props = new Properties();
		 props.put("annotators", "tokenize, ssplit, pos");//, lemma, ner, parse, dcoref");
		 pipeline = new StanfordCoreNLP(props);
		 initialiseSyntacticTreePipeline();//todo:remove
	}
	
	public void initialiseSyntacticTreePipeline(){
		Properties props2 = new Properties();
	    props2.put("annotators", "tokenize, ssplit, pos, lemma, parse");//, lemma, ner, parse, dcoref");
	    pipeline2 = new StanfordCoreNLP(props2);
	}
	
	public String describeSynset(String synsetID){
		return wordnetAPI.getSynsetDescription(synsetID);
	}
	
	public String treeParseLabel(String text, int depth) {
		Annotation textAnnotated = new Annotation(text);
		pipeline2.annotate(textAnnotated);
		List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
		for(int i = 0; i <= depth && i <sentences.size(); i++) {
			CoreMap sentence = sentences.get(i);
			sentence.toString();
			sentence.get(BasicDependenciesAnnotation.class)		;
			}
		return null;
	}
	
	public Set<String> preprocessText(String text){
		//controller.log(1000, "NLP starting with Stanford Core NLP");
	    // create an empty Annotation just with the given text
	    Annotation textAnnotated = new Annotation(text);
	    
	    // run all Annotators on this text
	    pipeline.annotate(textAnnotated);
	    
	    // these are all the sentences in this document
	    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    
	    Map<String,Set<String>> textParts = new HashMap<String,Set<String>>();
	    textParts.put("noun", new HashSet<String>());
	    textParts.put("verb", new HashSet<String>());
	    
	    Set<String> wordnetSynsets = new HashSet<String>();
	    
	    for(CoreMap sentence: sentences) {
	      // traversing the words in the current sentence
	      // a CoreLabel is a CoreMap with additional token-specific methods
	    	//controller.log(1000, " - Printing sentence");
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        // this is the text of the token
	        String word = token.get(TextAnnotation.class);
	        if(!controller.config.isBlockedWord(word)){
		        //controller.log(1000, " - WORD TOKEN: "+word);
		        // this is the POS tag of the token
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        //controller.log(1000, " - POS: "+pos);
		        // this is the NER label of the token
		        String ne = token.get(NamedEntityTagAnnotation.class);  
		        //controller.log(1000, " - NE: "+ne);
		     
		        String simplifiedPOS = null;
		        if(pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ")){
		        	simplifiedPOS = "verb";
		        }
		        // Not sure about FW, is it a name?
		        if(pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") || pos.equals("NNPS") || pos.equals("FW")){
		        	simplifiedPOS = "noun";
		        }
		        if(simplifiedPOS != null){	        	
		        	textParts.get(simplifiedPOS).add(normalise(word));
		        	wordnetSynsets.addAll(wordnetAPI.lookupSynset(word, simplifiedPOS));
		        	//wordnetSynsets.add();
		        }
	        }
	      }
	      // this is the parse tree of the current sentence
	      //Tree tree = sentence.get(TreeAnnotation.class);
	      // this is the Stanford dependency graph of the current sentence
	      //SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    }
	    
	  //debug print TODO remove
	      //controller.log(5000, " - Nouns"+textParts.get("noun"));
	      //controller.log(5000, " - Verbs"+textParts.get("verb"));
	      //controller.log(4000, " - Synsets"+wordnetSynsets);
	    // This is the coreference link graph
	    // Each chain stores a set of mentions that link to each other,
	    // along with a method for getting the most representative mention
	    // Both sentence and token offsets start at 1!
	    //Map<Integer, CorefChain> graph = 
	      //textAnnotated.get(CorefChainAnnotation.class);
	      return wordnetSynsets;
	}
	
	public List<String> extractAllWords(String text){
	    text = text.toLowerCase();
	    if(text.indexOf("how to") == 0) text = text.substring(6).trim();
		Annotation textAnnotated = new Annotation(text);
	    
	    pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    List<String> words = new LinkedList<String>();
	    
	    for(CoreMap sentence: sentences) {
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        if(pos.equals("FW") || 
		        		(pos.indexOf("RB") == 0) || (pos.indexOf("SYM") == 0) || (pos.indexOf("WRB") == 0) || 
		        		(pos.indexOf("VB") == 0)  || (pos.indexOf("NN") == 0) || (pos.indexOf("JJ") == 0) ){
		        	words.add(word);
		        }
	        }
	    }
	      return words;
	}
	
	public List<LinkedHashSet<String>> extractAllSentenceLemmas(String text){
	    text = text.toLowerCase().replaceAll("\\(.*?\\) ?", "");
	    if(text.indexOf("how to") == 0) text = text.substring(6).trim();
		Annotation textAnnotated = new Annotation(text);
	    pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    List<LinkedHashSet<String>> sentencesLemmas = new LinkedList<LinkedHashSet<String>>();
	    Set<String> stopWords = new HashSet<String>();
	    stopWords.add("the"); stopWords.add("a"); stopWords.add("an"); stopWords.add("his");
	    stopWords.add("hers"); stopWords.add("her"); stopWords.add("your"); stopWords.add("their");
	    stopWords.add("then"); stopWords.add("or"); stopWords.add("and"); stopWords.add("you");
	    stopWords.add("me"); stopWords.add("I");
	    stopWords.add("they"); stopWords.add("we"); stopWords.add("us");
	    for(CoreMap sentence: sentences) {
	    	LinkedHashSet<String> sentenceLemmas = new LinkedHashSet<String>();
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
	        String pos = token.get(PartOfSpeechAnnotation.class);
	        word = word.toLowerCase().trim();
	        String lemma = getLemma(word,pos);
	        if(lemma != null && lemma.length() > 0){word = lemma;}
	        word = word.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("`", "").replaceAll("'", "").replaceAll("\\.", "").toLowerCase().trim();
	        if(word.length() > 0 && !stopWords.contains(word)){
	        	sentenceLemmas.add(word);
	        }
	        }
	      if(sentenceLemmas.size() > 0){
	    	  sentencesLemmas.add(sentenceLemmas);
	      }
	    }
	    return sentencesLemmas;
	}
	
	public LinkedList<String> extractAllSentenceLemmasListContainingTokens(String text, String tokensToFind){
	    text = text.toLowerCase().replaceAll("\\(.*?\\) ?", "");
	    if(text.indexOf("how to") == 0) text = text.substring(6).trim();
		Annotation textAnnotated = new Annotation(text);
	    pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    Set<String> stopWords = new HashSet<String>();
	    stopWords.add("the"); stopWords.add("a"); stopWords.add("an"); stopWords.add("his");
	    stopWords.add("hers"); stopWords.add("her"); stopWords.add("your"); stopWords.add("their");
	    stopWords.add("then"); stopWords.add("or"); stopWords.add("and"); stopWords.add("you");
	    stopWords.add("me"); stopWords.add("I");
	    stopWords.add("they"); stopWords.add("we"); stopWords.add("us");
	    for(CoreMap sentence: sentences) {
	    	LinkedList<String> sentenceLemmas = new LinkedList<String>();
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
	        String pos = token.get(PartOfSpeechAnnotation.class);
	        word = word.toLowerCase().trim();
	        String lemma = getLemma(word,pos);
	        if(lemma != null && lemma.length() > 0){word = lemma;}
	        word = word.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("`", "").replaceAll("'", "").replaceAll("\\.", "").toLowerCase().trim();
	        if(word.length() > 0 && !stopWords.contains(word)){
	        	sentenceLemmas.add(word);
	        }
	        }
	      if(sentenceLemmas.size() > 0){
	    	  boolean allWords = true;
				for(String s : tokensToFind.replaceAll("[^0-9A-Za-z-]+", " ").split("\\s+")){
					if(!sentenceLemmas.contains(s)){
						allWords = false;
						break;
					}
				}
				if(allWords) return sentenceLemmas;
	      }
	    }
	    return null;
	}
	
	private synchronized String getLemma(String text, String pos){
		//morpha.stem(text);
		return morpha.lemma(text, pos);
	}
	
	public List<String> extractNouns(String text){
	    Annotation textAnnotated = new Annotation(text);
	    
	    pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    List<String> nouns = new LinkedList<String>();
	    
	    for(CoreMap sentence: sentences) {
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        if(pos.equals("FW") || pos.equals("VBD") || (pos.indexOf("VB") == 0)  || (pos.indexOf("NN") == 0) || (pos.indexOf("JJ") == 0) ){
		        	nouns.add(word);
		        }
	        }
	    }
	      return nouns;
	}
	
	public List<ParsedSentence> parseStepSentences(String text, int maxdepth){
		List<ParsedSentence> parsedSentences = new LinkedList<ParsedSentence>();
		
		Annotation textAnnotated = new Annotation(text);
	    pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    if(sentences.size() > maxdepth && maxdepth > 0) sentences = sentences.subList(0, maxdepth);
	    Set<String> wordnetSynsets = new HashSet<String>();
	    for(CoreMap sentence: sentences) {
	    	parsedSentences.add(parseSentenceTokenBased(sentence));
	      }
		return parsedSentences;
	}
	
	private ParsedSentence parseSentenceTokenBased(CoreMap sentence){
		ParsedSentence parsedSentence = new ParsedSentence();
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        // this is the text of the token
	        String word = token.get(TextAnnotation.class);
	        if(!controller.config.isBlockedWord(word)){
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        String ne = token.get(NamedEntityTagAnnotation.class);  
		        String simplifiedPOS = null;
		        if(pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ")){
		        	if(parsedSentence.getVerb() == null) parsedSentence.setVerb(word);
		        }
		        if(pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") || pos.equals("NNPS") || pos.equals("FW")){
		        	if(parsedSentence.getVerb() == null && parsedSentence.getSubject() == null) parsedSentence.setSubject(word);
		        	if(parsedSentence.getVerb() != null && parsedSentence.getObject() == null) parsedSentence.setObject(word);
		        }
	        }
	      }
	      if(parsedSentence.getVerb() != null) return parsedSentence;
	      else return null;
	}
	
	private ParsedSentence parseSentenceBruteForce(CoreMap sentence){
		ParsedSentence parsedSentence = new ParsedSentence();
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        // this is the text of the token
	        String word = token.get(TextAnnotation.class);
	        if(!controller.config.isBlockedWord(word)){
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        String ne = token.get(NamedEntityTagAnnotation.class);  
		        String simplifiedPOS = null;
		        if(pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ")){
		        	if(parsedSentence.getVerb() == null) parsedSentence.setVerb(word);
		        }
		        if(pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") || pos.equals("NNPS") || pos.equals("FW")){
		        	if(parsedSentence.getVerb() == null && parsedSentence.getSubject() == null) {
		        		if (wordnetAPI.lookupSynset(word, "verb").size() > 0 ) {
		        			// force the first word to be a verb
		        			parsedSentence.setVerb(word);
		        		}
		        	} else if(parsedSentence.getVerb() != null && parsedSentence.getObject() == null) parsedSentence.setObject(word);
		        }
	        }
	      }
	      return parsedSentence;
	}
	
	public List<ParsedSentence> parseStepSentencesTree(String text, int maxdepth){
		List<ParsedSentence> parsedSentences = new LinkedList<ParsedSentence>();
		Annotation textAnnotated = new Annotation(text);
	    pipeline2.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    if(sentences.size() > maxdepth && maxdepth > 0) sentences = sentences.subList(0, maxdepth);
	    
	    for(CoreMap sentence: sentences) {
	    	ParsedSentence extractedSentence = visitTree2(sentence.get(TreeAnnotation.class));
	    	if(extractedSentence == null || extractedSentence.getVerb() == null) extractedSentence = parseSentenceTokenBased(sentence);
	    	if(extractedSentence == null || extractedSentence.getVerb() == null) extractedSentence = parseSentenceBruteForce(sentence);
	    	if(extractedSentence != null) parsedSentences.add(extractedSentence);
	    }
	    return parsedSentences;
	}
	public Map<String,Set<String>> processText(String text){
		Map<String,Set<String>> decomposition = new HashMap<String,Set<String>>();
		Annotation textAnnotated = new Annotation(text);
	    
	    // run all Annotators on this text
		pipeline2.annotate(textAnnotated);
	    
	    // these are all the sentences in this document
	    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    
	    Map<String,Set<String>> textParts = new HashMap<String,Set<String>>();
	    
	    System.out.println("\n"+text);
	    
	    Set<List<Set<String>>> allSentences = new HashSet<List<Set<String>>>();
	    
	    for(CoreMap sentence: sentences) {
	      // traversing the words in the current sentence
	      // a CoreLabel is a CoreMap with additional token-specific methods
	    	//controller.log(1000, " - Printing sentence");
	    	
	    	Set<String> subj = new HashSet<String>();
	    	Set<String> pred = new HashSet<String>();
	    	Set<String> obj = new HashSet<String>();
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        // this is the text of the token
	        String word = token.get(TextAnnotation.class);
	        if(!controller.config.isBlockedWord(word)){
		        //controller.log(1000, " - WORD TOKEN: "+word);
		        // this is the POS tag of the token
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        //controller.log(1000, " - POS: "+pos);
		        // this is the NER label of the token
		        String ne = token.get(NamedEntityTagAnnotation.class);  
		        //controller.log(1000, " - NE: "+ne);
		     
		        String simplifiedPOS = null;
		        if(pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ")){
		        	simplifiedPOS = "verb";
		        	if(obj.size() != 0){
		        		List<Set<String>> parsedSentence = new ArrayList<Set<String>>(3); 
		        		parsedSentence.add(subj);
		        		parsedSentence.add(pred);
		        		parsedSentence.add(obj);
		        		allSentences.add(parsedSentence);
		        		subj = new HashSet<String>();
		    	    	pred = new HashSet<String>();
		    	    	obj = new HashSet<String>();
		        	}
		        	pred.add(normalise(word));
		        }
		        // Not sure about FW, is it a name?
		        if(pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") || pos.equals("NNPS") || pos.equals("FW")){
		        	simplifiedPOS = "noun";
		        	if(pred.size() == 0){subj.add(normalise(word));}
		        	else {obj.add(normalise(word));}
		        }
		        if(simplifiedPOS != null){	        
		        	for(String synset : wordnetAPI.lookupSynset(word, simplifiedPOS)){
		        		if(!decomposition.keySet().contains(synset)){
		        			decomposition.put(synset, new HashSet<String>());
		        		}
		        		decomposition.get(synset).add(normalise(word));
		        	}
		        }
	        }
	      }
	      if(subj.size() != 0 || pred.size() != 0 || obj.size() != 0){
	    	  List<Set<String>> parsedSentence = new ArrayList<Set<String>>(3); 
	    	  parsedSentence.add(subj);
	    	  parsedSentence.add(pred);
	    	  parsedSentence.add(obj);
	    	  allSentences.add(parsedSentence);
	      }
	      
	      Tree tree = sentence.get(TreeAnnotation.class);
	      tree.printLocalTree();
	      
	      	
	      	visitTree(tree);
	      	visitTree2(tree);
	      	
	    }
	    for(List<Set<String>> sentence : allSentences){
	    	System.out.println("TOKENS: "+sentence.get(0)+" >> "+sentence.get(1)+" >> "+sentence.get(2));
	    }
	      return decomposition;
	}
	
	public ParsedSentence visitTree2(Tree tree){
		List<String> subjects = null;
		List<String> predicates = null;
		List<String> objects = null;
		Tree firstNP = treeFindNP(tree);
		if(firstNP != null) subjects = treeFindNouns(firstNP);
		Tree firstVP = treeFindVP(tree);
		if(firstVP!= null){
			Tree firstNPobject = treeFindNP(firstVP);
			if(firstNPobject != null) objects = treeFindNouns(firstNPobject);
			predicates = treeFindVerbs(firstVP);
		}
		String subject = "";
		String predicate = "";
		String object = "";
		
		ParsedSentence parsedSentence = new ParsedSentence();
		if(predicates != null && predicates.size() > 1){
			return parsedSentence;
		}
		if (subjects != null) for(String s : subjects){
			subject = subject + " " + s;
		}
		if (objects != null) for(String s : objects){
			object = object + " " + s;
		}
		if (predicates != null) for(String s : predicates){
			predicate = predicate + " " + s;
		}
		if(subject.length() > 0) parsedSentence.setSubject(subject);
		if(predicate.length() > 0) parsedSentence.setVerb(predicate);
		if(object.length() > 0) parsedSentence.setObject(object);
		if(subject.length() > 0 || predicate.length() > 0 || object.length() > 0) return parsedSentence;
		else return null;
		//System.out.println("THIRD WAY: "+subjects+" <> "+predicates+" <> "+objects);
		
	}
	
	public Tree treeFindNP(Tree tree){
		return treeFind(tree, new String[]{"NP", "ADJP"},0);
	}
	
	public Tree treeFindVP(Tree tree){
		return treeFind(tree, new String[]{"VP"},0);
	}
		
	public Tree treeFind(Tree tree, String[] labelsToMatch, int depth){
		if(tree.isLeaf()) return null;
		for(int i = 0; i < labelsToMatch.length; i++) {
			if(tree.label().value().equals(labelsToMatch[i])) return tree;
		}
		Tree[] children = tree.children();
		if(tree.label().value().equals("S") || tree.label().value().equals("ROOT") || depth == 0) for(int i = 0; i<children.length; i++) {
			Tree resultFound = treeFind(children[i], labelsToMatch, depth+1);
			if(resultFound != null) return resultFound;
		}
		return null;
	}
	
	public List<String> treeFindNouns(Tree tree){
		if(tree.isLeaf()) return null;
		String label = tree.label().value();
		List<String> nouns = new LinkedList<String>();
		Tree[] children = tree.children();
		if(label.equals("NN") || label.equals("NNS") || label.equals("NNP") || label.equals("NNPS") || label.equals("FW")) {
			for(int i = 0; i<children.length; i++){
				nouns.add(children[i].label().value());
			}
			return nouns;
		}
		for(int i = 0; i<children.length; i++){
			List<String> nounsOfOneChild = treeFindNouns(children[i]);
			if(nounsOfOneChild!= null) nouns.addAll(nounsOfOneChild);
		}
		return nouns;
	}
	
	public List<String> treeFindVerbs(Tree tree){
		if(tree.isLeaf()) return null;
		String label = tree.label().value();
		List<String> verbs = new LinkedList<String>();
		Tree[] children = tree.children();
		if(label.equals("VB") || label.equals("VBD") || label.equals("VBG") || label.equals("VBN") || label.equals("VBP") || label.equals("VBZ")){
			for(int i = 0; i<children.length; i++){
				verbs.add(children[i].label().value());
			}
			return verbs;
		}
		for(int i = 0; i<children.length; i++){
			List<String> verbsOfOneChild = treeFindVerbs(children[i]);
			if(verbsOfOneChild!= null) verbs.addAll(verbsOfOneChild);
		}
		return verbs;
	}
	
	
	/*if(pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ")){
    	simplifiedPOS = "verb";
    }
    // Not sure about FW, is it a name?
    if(pos.equals("NN") || pos.equals("NNS") || pos.equals("NNP") || pos.equals("NNPS") || pos.equals("FW")){
    	simplifiedPOS = "noun";
    }*/
	
	public void visitTree(Tree tree){
		visitTree(tree, 0);
	}
	public Set<Pair<String, Object>> visitTree(Tree tree, int depth){
		String indentation = "";
		for(int i = 0; i<depth; i++){
			indentation += " - ";
		}
		Tree[] children = tree.children();
		Set<Pair<String, Object>> resultToReturn = new HashSet<Pair<String, Object>>();
		System.out.println(indentation+tree.label());
		if(tree.isLeaf()) {
			resultToReturn.add(new Pair<String, Object>("word",tree.label().value()));
			return resultToReturn;
		} 
		depth++;
		Set<Pair<String, Object>> allResults = new HashSet<Pair<String, Object>>();
		for(int i = 0; i<children.length; i++){
			allResults.addAll(visitTree(children[i], depth));
		}
		Set<String> subjects = new HashSet<String>();
		Set<String> predicates = new HashSet<String>();
		Set<String> objects = new HashSet<String>();
		for(Pair<String, Object> result : allResults){
			resultToReturn.add(result);
			if(tree.label().value().equals("JJ") || tree.label().value().indexOf("NN") == 0){ //|| tree.label().value().equals("NNS") || tree.label().value().equals("NNP") || tree.label().value().equals("PRP")){
				if(result.getLeft().equals("word")){
					resultToReturn.remove(result);
					resultToReturn.add(new Pair<String, Object>("noun",result.getRight()));
				}
			} else if(tree.label().value().indexOf("VB") == 0){ //|| tree.label().value().equals("VBP") || tree.label().value().equals("VBN") || tree.label().value().equals("VBZ")){
				if(result.getLeft().equals("word")){
					resultToReturn.remove(result);
					resultToReturn.add(new Pair<String, Object>("verb",result.getRight()));
				}
			} else if(tree.label().value().equals("NP") || tree.label().value().equals("ADJP")){
				if(result.getLeft().equals("noun")){
					resultToReturn.remove(result);
					resultToReturn.add(new Pair<String, Object>("np",result.getRight()));
				}
			} else if(tree.label().value().equals("VP")){
				if(result.getLeft().equals("np")){
					resultToReturn.remove(result);
					objects.add((String) result.getRight());
				}
				if(result.getLeft().equals("verb")){
					resultToReturn.remove(result);
					predicates.add((String) result.getRight());
				}
				if(result.getLeft().equals("vp")){
					resultToReturn.remove(result);
					Pair<Set<String>, Set<String>> verbPhrase = (Pair<Set<String>, Set<String>>) result.getRight();
					predicates.addAll(verbPhrase.getLeft());
					objects.addAll(verbPhrase.getRight());
				}
			} else if(tree.label().value().equals("S") || tree.label().value().equals("ROOT")){
				if(result.getLeft().equals("np")){
					resultToReturn.remove(result);
					subjects.add((String) result.getRight());
				}
				if(result.getLeft().equals("vp")){
					resultToReturn.remove(result);
					Pair<Set<String>, Set<String>> verbPhrase = (Pair<Set<String>, Set<String>>) result.getRight();
					predicates.addAll(verbPhrase.getLeft());
					objects.addAll(verbPhrase.getRight());
				}
			}
			
			
		}
		
		if(tree.label().value().equals("VP")){
			Pair<Set<String>, Set<String>> verbPhrase = new Pair<Set<String>, Set<String>>(predicates, objects);
			Pair<String, Object> vp = new Pair<String, Object>("vp",verbPhrase);
			resultToReturn.add(vp);
		} 
		if(tree.label().value().equals("S") || tree.label().value().equals("ROOT")){
			System.out.println(subjects+" -> "+predicates+" -> "+objects);
		}
		return resultToReturn;
	}
	
	private String normalise(String word){
		return word.toLowerCase();
	}
	
	public String extractFirstVerb(String text){
		Annotation textAnnotated = new Annotation(text);
		pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    if(sentences.size()>0){
	    	CoreMap sentence =  sentences.get(0);
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        String ne = token.get(NamedEntityTagAnnotation.class);  
		        if(pos.equals("VB") || pos.equals("VBP") || pos.equals("VBZ")){
		        	return word;
		        } else {
		        	return null;
		        }
		  }
	    }
	    return null;
  }
	
	public boolean containsVerb(String text){
		for(String s : text.split("\\s+")){
			try{
				if(wordnetAPI.lookupSynset(s, "verb").size() > 0) return true;
				
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	    
		Annotation textAnnotated = new Annotation(text);
		pipeline.annotate(textAnnotated);
	    List<CoreMap> sentences = textAnnotated.get(SentencesAnnotation.class);
	    if(sentences.size()>0){
	    	CoreMap sentence =  sentences.get(0);
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        String word = token.get(TextAnnotation.class);
	            String pos = token.get(PartOfSpeechAnnotation.class);
		        String ne = token.get(NamedEntityTagAnnotation.class);  
		        if(pos.equals("VB") || pos.equals("VBD") || pos.equals("VBG") || pos.equals("VBN") || pos.equals("VBP") || pos.equals("VBZ")){
		        	return true;
		        } 
		        if(word.indexOf("ing") > 0 && word.indexOf("ing") == word.length()-3){
		        	return true;
		        }
		        if(word.indexOf("ed") > 0 && word.indexOf("ed") == word.length()-2){
		        	return true;
		        }
		  }
	    }
	    return false;
  }
}
