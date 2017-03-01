package luceneIndexing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import nlp.ParsedPhrase;
import nlp.ParsedPhrase2;
import nlp.ParsedPhraseExtended;
import nlp.TextProcessingController;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

import query.SparqlManager;
import Data.Lexeme;
import Data.LexemeArray;
import Data.Output;
import Data.WebLabel;

public class LuceneIndexCreatorThread implements Runnable{

	private TextProcessingController textPreProcessing;
	private int maxSentencesToConsider;
	private int maxWordsToConsider;
	private IndexWriter writer;
	private String uri; 
	private String label;
	private LuceneIndexCreator creatorController;
	private String graph;
	
	public LuceneIndexCreatorThread(TextProcessingController textPreProcessing, IndexWriter writer, int maxSentencesToConsider, int maxWordsToConsider, String uri, String label, LuceneIndexCreator creatorController, String graph) {
		this.textPreProcessing = textPreProcessing;
		this.writer = writer;
		this.maxSentencesToConsider = maxSentencesToConsider;
		this.maxWordsToConsider = maxWordsToConsider;
		this.uri = uri;
		this.label = label;
		this.creatorController = creatorController;
		this.graph = graph;
	}
	
	private String constructStringFromLexemeArray(List<LexemeArray> lexemes){
		String allTogether = "";
		for(LexemeArray lexeme : lexemes){
			for(Lexeme x : lexeme){
				allTogether = allTogether+" "+x.getLemme();
			}
		}
		return allTogether.trim().toLowerCase();
	}
	private String constructStringFromLexemeList(List<Lexeme> lexemes){
		String allTogether = "";
			for(Lexeme x : lexemes){
				allTogether = allTogether+" "+x.getLemme();
			}
		return allTogether.trim().toLowerCase();
	}
	
	public void run() {
		if(Math.random() > 0.99) System.out.print(".");
		if(creatorController.intConfig.isDoLuceneIndexingApplication()){
			Document doc = new Document();
			String requirements = creatorController.returnRequirements(uri);
			if(requirements != null){
				Field requirementsField = new TextField("requirements", requirements, Field.Store.YES);
				doc.add(requirementsField);
			}
			boolean isHighlight = creatorController.isHighlight(uri);
			Field highlightField = new TextField("highlight", "false", Field.Store.YES);
			if(isHighlight) 
				highlightField = new TextField("highlight", "true", Field.Store.YES);
			doc.add(highlightField);
		
			
			
			if(label != null){
				Field labelField = new TextField("label", label, Field.Store.YES);
				doc.add(labelField);
			} 
			Field uriField = new StringField("uri", uri, Field.Store.YES);
			doc.add(uriField);
			try {
				writer.addDocument(doc);
			} catch (IOException e) {
				System.out.println("ERROR - it was not possible to add document to the Lucene Index");
				e.printStackTrace();
			}
			return;
		}
		Document doc = new Document();
		if(creatorController.intConfig.isDoLuceneIndexingContext()){
			String[] contextWords = creatorController.getAllWordsFromURL(uri, graph);
			Field contextField = new TextField("context", contextWords[0], Field.Store.YES);
			doc.add(contextField);
			Field coreField = new TextField("core", contextWords[1], Field.Store.YES);
			doc.add(coreField);
		} else {
			String labelOriginal = label.replaceAll("don\\?t", "don't").replaceAll("Don\\?t", "Don't");
			label = labelOriginal.toLowerCase();
			if(label.indexOf("how to") == 0) label = label.substring(6).trim();
			label = label.replaceAll("you have to ", "").replaceAll("you should ", "").replaceAll("make sure ", "").replaceAll("try to ", "").replaceAll("attempt to ", "").replaceAll("start to ", "").replaceAll("begin to ", "");
			
			//List<ParsedPhraseExtended> phrases = textPreProcessing.getPhrasesExtended(label, maxSentencesToConsider);
			//List<ParsedPhrase2> phrases = null;
			
			// LABEL COMPLEX NLP
			List<ParsedPhrase2> parsedSentences = textPreProcessing.parseTextIntoPhrases(label.substring(0, 1).toUpperCase() + label.substring(1) + ".",uri,creatorController.maxSentencesToConsider,false);
			int phrasecount = 0;
			/*if(parsedSentences.size() == 0){
			System.out.print("'");			
		}*/
			for(ParsedPhrase2 phrase : parsedSentences){
				String hasSubject = "false";
				for(LexemeArray la : phrase.getSubject()) for(Lexeme l : la) if(!l.getLemme().equals("you")) hasSubject="true";
				Field hasSubjectField = new StringField("hassubject"+phrasecount, hasSubject, Field.Store.YES);
				doc.add(hasSubjectField);
				
				String core = "";
				Set<String> mainwords = new HashSet<String>();
				for(LexemeArray la : phrase.getVerb()) for(Lexeme l : la) mainwords.add(l.getLemme());
				for(Lexeme l : phrase.getAdverbs()) mainwords.add(l.getLemme());
				for(Lexeme l : phrase.getAdjectives()) mainwords.add(l.getLemme());
				for(LexemeArray la : phrase.getObject()) for(Lexeme l : la) mainwords.add(l.getLemme());
				for(String s : mainwords) core = core+" "+s;
				Field coreField = new TextField("core"+phrasecount, core.trim(), Field.Store.YES);
				doc.add(coreField);
				
				String context = "";
				Set<String>contextwords = new HashSet<String>();
				for(LexemeArray la : phrase.getConditionnalContext()) for(Lexeme l : la) contextwords.add(l.getLemme());
				for(LexemeArray la : phrase.getSpaceContext()) for(Lexeme l : la) contextwords.add(l.getLemme());
				for(LexemeArray la : phrase.getTimeContext()) for(Lexeme l : la) contextwords.add(l.getLemme());
				for(LexemeArray la : phrase.getInformationContext()) for(Lexeme l : la) contextwords.add(l.getLemme());
				for(String s : contextwords) context = context+" "+s;
				Field contextField = new TextField("context"+phrasecount, context.trim(), Field.Store.YES);
				doc.add(contextField);
				
				String isnegative = "false";
				if(phrase.isNegative()) isnegative = "true";
				if(!labelOriginal.toLowerCase().equals(label))
					System.out.print("");
				Field isnegativeField = new StringField("isnegative"+phrasecount, isnegative, Field.Store.YES);
				doc.add(isnegativeField);
				
				phrasecount++;
			}
			
			// LABEL SIMPLE
			List<LinkedHashSet<String>> sentences = textPreProcessing.extractAllSentenceLemmas(label);
			int phraseCounter = 0;
			for(LinkedHashSet<String> sentence : sentences){
				String sentenceReConstructed = "";
				for(String s : sentence){
					sentenceReConstructed = sentenceReConstructed+" "+s;
				}
				sentenceReConstructed = sentenceReConstructed.trim();
				Field field = new TextField("sentence"+phraseCounter, sentenceReConstructed, Field.Store.YES);
				doc.add(field);
				phraseCounter++;
			}
			
			
			// CATEGORIES
			// exact category
			String exactCategoryURI =creatorController.returnExactCategory(uri);
			
			if(exactCategoryURI != null){
				Field exactCategory = new StringField("exactcategory", exactCategoryURI, Field.Store.YES);
				doc.add(exactCategory);
				String[] allCategories = creatorController.returnAllCategories(exactCategoryURI);
				if(allCategories.length>0){
					// standardized category
					Field topCategory = new StringField("topcategory", allCategories[0], Field.Store.YES);
					doc.add(topCategory);
					String allcategoriesString = "";
					for(String s : allCategories) allcategoriesString = allcategoriesString+" "+s;
					// all all categories
					Field allCategoriesField = new TextField("allcategories", allcategoriesString.trim(), Field.Store.YES);
					doc.add(allCategoriesField);
				}
			}
			
			/*// RELATED URIS
		String relatedURIs = creatorController.returnRelatedURIs(uri);
		if(relatedURIs.length() > 0){
			Field relatedURIsField = new TextField("relateduris", relatedURIs, Field.Store.YES);
			doc.add(relatedURIsField);
		}
		String[] relatedURIsWords = creatorController.getAllWordsFromURIs(relatedURIs);
		if(relatedURIsWords[0] != null && relatedURIsWords[0].length() > 0){
			Field relatedURIsWordsField = new TextField("relateduriswords", relatedURIsWords[0], Field.Store.YES);
			doc.add(relatedURIsWordsField);
		}		
		if(relatedURIsWords[1] != null && relatedURIsWords[1].length() > 0){
			Field relatedURIsCoreWordsField = new TextField("relateduriscore", relatedURIsWords[1], Field.Store.YES);
			doc.add(relatedURIsCoreWordsField);
		}			*/
			
			// MISC
			Field labelField = new TextField("label", labelOriginal, Field.Store.YES);
			doc.add(labelField);
		}
		Field uriField = new StringField("uri", uri, Field.Store.YES);
		doc.add(uriField);
		
		try {
			writer.addDocument(doc);
		} catch (IOException e) {
			System.out.println("ERROR - it was not possible to add document to the Lucene Index");
			e.printStackTrace();
		}
	}
}
