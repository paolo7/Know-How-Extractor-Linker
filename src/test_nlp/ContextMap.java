package test_nlp;

import java.util.ArrayList;
import java.util.HashMap;
import Data.Lexeme;
import Data.LexemeArray;

public class ContextMap {
	private HashMap<String,ArrayList<LexemeArray>> map=new HashMap<String,ArrayList<LexemeArray>>();
	
	public ContextMap() {
		map.put("time", new ArrayList<LexemeArray>());
		map.put("space", new ArrayList<LexemeArray>());
		map.put("condition", new ArrayList<LexemeArray>());
		map.put("information", new ArrayList<LexemeArray>());
	}
	public boolean iscontext(LexemeArray array) {
		if(iscontext("time", array))
			return true;
		if(iscontext("space", array))
			return true;
		if(iscontext("condition", array))
			return true;
		if(iscontext("information", array))
			return true;
		return false;
	}
	private boolean iscontext(String type, LexemeArray array) {
		ArrayList<LexemeArray> contexts=map.get(type);
		//-- look for intersection
		for(int i=0;i<array.size();i++) {
			for(int j=0;j<contexts.size();j++) {
				LexemeArray context=contexts.get(j);
				for(int k=0;k<context.size();k++) {
					Lexeme word_from_parsed_phrase=context.get(k);
					Lexeme word_from_lexeme_array=array.get(i);
					if(word_from_parsed_phrase==word_from_lexeme_array) {
						return true;
					}
				}
			}
		}
		return false;
	}
	public boolean iscontext(Lexeme lex) {
		if(iscontext("time", lex))
			return true;
		if(iscontext("space", lex))
			return true;
		if(iscontext("condition", lex))
			return true;
		if(iscontext("information", lex))
			return true;
		return false;
	}
	public boolean iscontext(String type,Lexeme lex) {
		ArrayList<LexemeArray> contexts=map.get(type);
		for(int j=0;j<contexts.size();j++) {
			LexemeArray context=contexts.get(j);
			for(int k=0;k<context.size();k++) {
				Lexeme word_from_parsed_phrase=context.get(k);
				if(word_from_parsed_phrase==lex) {
					return true;
				}
			}
		}
		return false;
	}
	public void addTimeContext(LexemeArray string) {
		map.get("time").add(string);
	}
	public void addSpaceContext(LexemeArray string) {
		map.get("space").add(string);
	}
	public void addConditionContext(LexemeArray string) {
		map.get("condition").add(string);
	}
	public void addInformationContext(LexemeArray string) {
		map.get("information").add(string);
	}
	public ArrayList<LexemeArray> getTimeContext() {
		return map.get("time");
	}
	public ArrayList<LexemeArray> getConditionnalContext() {
		return map.get("condition");
	}
	public ArrayList<LexemeArray> getSpaceContext() {
		return map.get("space");
	}
	public ArrayList<LexemeArray> getInformationContext() {
		return map.get("information");
	}
	public String toString() {
		return map.toString();
	}
}
