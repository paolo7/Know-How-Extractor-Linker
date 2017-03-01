package nlp;

import java.util.ArrayList;
import java.util.Set;

public class ParsedPhrase {

	private String subject;
	private String verb;
	private String object;
	private ArrayList<String> context;
	private String full_text;
	
	// Important fields
	private Set<String> adjectives;
	private Set<String> adverbs;
	
	// this boolean is set to false if the sentence is negative, like : "do not open the door" -> <open,door,false>
	private boolean negative;
	private boolean objectAsOutput;
	private boolean objectAsInput;
	
	public ParsedPhrase() {
		
	}
	
	public ParsedPhrase(String subject, String verb, String object,ArrayList<String> context, boolean objectAsOutput) {
		this.subject = subject;
		this.verb = verb;
		this.object = object;
		this.setContext(context);
		if(objectAsOutput){
			this.objectAsOutput = true;
			this.objectAsInput = false;
		} else {
			this.objectAsOutput = false;
			this.objectAsInput = true;
		}
	}
	
	public String toString() {
		return "- SUBJ:"+subject+"\n- VERB:"+verb+"\n- OBJ:"+object+"\n- CONTEXT:"+context+"\n\n";
	}
	
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getVerb() {
		return verb;
	}

	public void setVerb(String verb) {
		this.verb = verb;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
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

	public ArrayList<String> getContext() {
		return context;
	}

	public void setContext(ArrayList<String> context) {
		this.context = context;
	}

	public boolean isNegative() {
		return negative;
	}

	public void setNegative(boolean positive) {
		this.negative = positive;
	}

	public Set<String> getAdjectives() {
		return adjectives;
	}

	public void setAdjectives(Set<String> adjectives) {
		this.adjectives = adjectives;
	}

	public Set<String> getAdverbs() {
		return adverbs;
	}

	public void setAdverbs(Set<String> adverbs) {
		this.adverbs = adverbs;
	}
	
	public String getFull_text() {
		return full_text;
	}

	public void setFull_text(String full_text) {
		this.full_text = full_text;
	}
}