package nlp;

public class ParsedSentence {

	private String subject;
	private String verb;
	private String object;
	
	private boolean objectAsOutput;
	private boolean objectAsInput;
	
	public ParsedSentence() {
		
	}
	
	public ParsedSentence(String subject, String verb, String object, boolean objectAsOutput) {
		this.subject = subject;
		this.verb = verb;
		this.object = object;
		if(objectAsOutput){
			this.objectAsOutput = true;
			this.objectAsInput = false;
		} else {
			this.objectAsOutput = false;
			this.objectAsInput = true;
		}
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

}
