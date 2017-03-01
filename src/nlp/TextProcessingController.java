package nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import Data.Output;
import Data.WebLabel;
import test_nlp.NLP_BOX;
import wordnet.WordnetAPI;



public class TextProcessingController {

	TextProcessingConfig config;
	private TextProcessingStanford standfordNLP = null;
	private WordnetAPI wordnetAPI;
	NLP_BOX parser = null;
	public TextProcessingController(TextProcessingConfig config, WordnetAPI wordnetAPI){
		this.config = config;
		this.wordnetAPI = wordnetAPI;
		try {
			this.parser = new NLP_BOX();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Set<String> preprocessText(String text){
		//log(1000, "Start analysing entity "+entityUri+" with the following text:\n -> "+text);
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialiseAllPipelines();
			} 
			return standfordNLP.preprocessText(text);
		} else {
			log(10,"Error preprocessing the text, the chosen annotation tool is not supported (or hasn't been set)");
			return new HashSet<String>();
		}
		//log(1000, "Preprocessing completed successfully.");
	}
	
	public Map<String,Set<String>> processText(String text){
		//log(1000, "Start analysing entity "+entityUri+" with the following text:\n -> "+text);
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialiseAllPipelines();
			} 
			return standfordNLP.processText(text);
		} else {
			log(10,"Error preprocessing the text, the chosen annotation tool is not supported (or hasn't been set)");
			return new HashMap<String,Set<String>>();
		}
		//log(1000, "Preprocessing completed successfully.");
	}
	
	public String describeSynset(String synsetID){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialiseAllPipelines();
			} 
			return standfordNLP.describeSynset(synsetID);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return "";
		}
	}
	
	public List<String> extractNouns(String label){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.extractNouns(label);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	public String extractFirstVerb(String text){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.extractFirstVerb(text);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	public boolean containsVerb(String text){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.containsVerb(text);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return false;
		}
	}
	public List<ParsedSentence> parseStepSentences(String text, int maxdepth){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.parseStepSentences(text,maxdepth);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	public List<ParsedSentence> parseStepSentencesTree(String text, int maxdepth){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialiseAllPipelines();;
			} 
			return standfordNLP.parseStepSentencesTree(text,maxdepth);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	public List<String> extractAllWords(String text){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialiseAllPipelines();
			} 
			return standfordNLP.extractAllWords(text);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	public String treeParseLabel(String text, int depth){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialiseAllPipelines();
			} 
			return standfordNLP.treeParseLabel(text,depth);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	public List<String> getWordsSynsets(String text, int maxWords){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.getWordsSynsets(text, maxWords);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	public List<List<String>> getAllWordsSynsetsBySentence(String text){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.getAllWordsSynsetsBySentence(text);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	public ParsedSentenceSynsets[] parseTextIntoSentenceSynsets(String text, int maxSentences){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.parseTextIntoSentenceSynsets(text,maxSentences);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	public void log(String message){
		log(9999,message);
	}
	public void log(int verbosity, String message){
		if(verbosity <= config.getLogVerbosity()) System.out.println(message);
	}
	
	public void initializePOSpipeline(){
		if(standfordNLP == null){
			standfordNLP = new TextProcessingStanford(this, wordnetAPI);
			standfordNLP.initialisePOSpipeline();
		} 
	}
		
	public List<ParsedPhraseExtended> getPhrasesExtended(String text, int maxPhrases){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.getPhrasesExtended(text,maxPhrases);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	/**
	 * Return a list of parsed phrases from the given text snippet.
	 * For example, "Do X and then do Y" should return two parsed phrases: <do,X> and <do,Y>
	 * Instead, "Do X when Z is happening" should return just one <do,X,<happening,Z>> (in this case <happening,Z> is an auxiliary phrase)
	 * @param text the natural language label to analyze
	 * @param maxPhrases the maximum number of phrases that we want to extract, or -1 if we want to extract all of them.
	 * @param quick true if we want to perform the extraction quickly (ideally less than 10ms), false if we want to perform the extraction accurately (but still staying around 100ms, ideally)
	 * @return the extracted phrases
	 */
	public List<ParsedPhrase2> parseTextIntoPhrases(String text, String uri, int maxPhrases, boolean quick){
		
		List<ParsedPhrase2> phrases = new LinkedList<ParsedPhrase2>();
		
		try {
			WebLabel lbl = new WebLabel(text,uri);
			ArrayList<Output<ParsedPhrase2>> parsed_sentences=parser.parse(lbl,maxPhrases,quick);
			for(int i=0;i<parsed_sentences.size();i++){
				phrases.add(parsed_sentences.get(i).arg);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*parser.parse(lbl,2,false);
		ArrayList<Output<ParsedPhrase> > parsed_sentences=parser.process();
		for(int i=0;i<parsed_sentences.size();i++) {
			System.out.println(parsed_sentences.get(i).arg);
			output.write(parsed_sentences.get(i).arg.toString());
		}*/
		return phrases;
	}
	public List<ParsedPhrase2> parseTextIntoPhrases(String text, String uri, int maxPhrases){
		return parseTextIntoPhrases( text,  uri,  maxPhrases, false);
	}
	
	public List<LinkedHashSet<String>> extractAllSentenceLemmas(String text){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.extractAllSentenceLemmas(text);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
	public LinkedList<String> extractAllSentenceLemmasListContainingTokens(String text, String tokens){
		if(config.getNlpTool().equals(NLPtool.STANFORD)){
			// initialise stanford NLP object if null
			if(standfordNLP == null){
				standfordNLP = new TextProcessingStanford(this, wordnetAPI);
				standfordNLP.initialisePOSpipeline();
			} 
			return standfordNLP.extractAllSentenceLemmasListContainingTokens(text, tokens);
		} else {
			log(10,"Error retrieving the synset description: the chosen annotation tool is not supported (or hasn't been set)");
			return null;
		}
	}
	
}
