package nlp;

import java.util.ArrayList;

import Data.Lexeme;
import Data.LexemeArray;
import test_nlp.ContextMap;

public class ParsedPhrase2 {
	private String full_text;
	private ArrayList<LexemeArray> subjects=new ArrayList<LexemeArray>();
	private ArrayList<LexemeArray> verbs=new ArrayList<LexemeArray>();
	private ArrayList<LexemeArray> objects=new ArrayList<LexemeArray>();
	private ContextMap context=new ContextMap();
	private ArrayList<Lexeme> trash=new ArrayList<Lexeme>();
	private ArrayList<Lexeme> adjectives=new ArrayList<Lexeme>();
	private ArrayList<Lexeme> adverbs=new ArrayList<Lexeme>();
	
	private boolean objectAsOutput;
	private boolean objectAsInput;
	private boolean negative;
	
	public ParsedPhrase2() {

	}
	
	public String toString() {
		return "# "+full_text+"\n- SUBJ:"+subjects+"\n- VERB:"+verbs+"\n- OBJ:"+objects+"\n- ADVERB:"+adverbs+"\n- ADJECTIVE:"+adjectives+"\n- CONTEXT:"+context+"\n- NEG:"+negative+"\n- TRASH:"+trash+"\n\n";
	}
	
	public ArrayList<LexemeArray> getSubject() {
		return subjects;
	}

	public void addSubject(LexemeArray subject) {
		this.subjects.add(subject);
	}

	public ArrayList<LexemeArray> getVerb() {
		return verbs;
	}

	public void addVerb(LexemeArray verb) {
		this.verbs.add(verb);
	}

	public ArrayList<LexemeArray> getObject() {
		return objects;
	}

	public void addObject(LexemeArray object) {
		this.objects.add(object);
	}

	public ArrayList<Lexeme> getTrash() {
		return trash;
	}

	public boolean isObjectAsOutput() {
		return objectAsOutput;
	}

	public void setObjectAsOutput(boolean objectAsOutput) {
		this.objectAsOutput = objectAsOutput;
	}

	public boolean isObjectAsInput() {
		return objectAsInput;
	}

	public void setObjectAsInput(boolean objectAsInput) {
		this.objectAsInput = objectAsInput;
	}

	public boolean iscontext(LexemeArray array) {
		return context.iscontext(array);
	}
	public boolean iscontext(Lexeme lex) {
		return context.iscontext(lex);
	}
	
	public void addSpaceContext(LexemeArray context) {
		this.context.addSpaceContext(context);
	}
	public void addTimeContext(LexemeArray context) {
		this.context.addTimeContext(context);
	}
	public void addConditionnalContext(LexemeArray context) {
		this.context.addConditionContext(context);
	}
	public void setContext(ContextMap context) {
		this.context=context;
	}


	
	public ArrayList<LexemeArray> getTimeContext() {
		return context.getTimeContext();
	}
	public ArrayList<LexemeArray> getConditionnalContext() {
		return context.getConditionnalContext();
	}
	public ArrayList<LexemeArray> getSpaceContext() {
		return context.getSpaceContext();
	}
	public ArrayList<LexemeArray> getInformationContext() {
		return context.getInformationContext();
	}

	public String getFull_text() {
		return full_text;
	}

	public void setFull_text(String full_text) {
		this.full_text = full_text;
	}

	public boolean isNegative() {
		return negative;
	}

	public void setNegative(boolean negative) {
		this.negative = negative;
	}

	public boolean isSubject(Lexeme lex) {
		//-- look for intersection
		for(int j=0;j<subjects.size();j++) {
			LexemeArray subject=subjects.get(j);
			for(int k=0;k<subject.size();k++) {
				Lexeme word_from_parsed_phrase=subject.get(k);
				if(word_from_parsed_phrase==lex) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isAdjective(Lexeme lex) {
		//-- look for intersection
		for(int j=0;j<adjectives.size();j++) {
			Lexeme adjective=adjectives.get(j);
			if(adjective==lex) {
				return true;
			}
		}
		return false;
	}

	public boolean isAdverb(Lexeme lex) {
		//-- look for intersection
		for(int j=0;j<adverbs.size();j++) {
			Lexeme adverb=adverbs.get(j);
			if(adverb==lex) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isVerb(Lexeme lex) {
		//-- look for intersection
		for(int j=0;j<verbs.size();j++) {
			LexemeArray verb=verbs.get(j);
			for(int k=0;k<verb.size();k++) {
				Lexeme word_from_parsed_phrase=verb.get(k);
				if(word_from_parsed_phrase==lex) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isObject(Lexeme lex) {
		//-- look for intersection
		for(int j=0;j<objects.size();j++) {
			LexemeArray object=objects.get(j);
			for(int k=0;k<object.size();k++) {
				Lexeme word_from_parsed_phrase=object.get(k);
				if(word_from_parsed_phrase==lex) {
					return true;
				}
			}
		}
		return false;
	}

	public void setAdverb(Lexeme mot) {
		adverbs.add(mot);
	}
	public void setAdjective(Lexeme mot) {
		adjectives.add(mot);
	}

	public ArrayList<Lexeme> getAdjectives() {
		return adjectives;
	}

	public ArrayList<Lexeme> getAdverbs() {
		return adverbs;
	}
}
