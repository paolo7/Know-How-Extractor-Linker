package nlp;

import java.util.Date;

public class TextProcessingConfig {

	private int logVerbosity = Integer.MAX_VALUE;
	private NLPtool nlpTool = NLPtool.STANFORD;
	private boolean logging = false;
	
	public boolean isBlockedWord(String word){
		if(word.equals("thing") || word.equals("things") || word.equals("stuff") || word.equals("this") || word.equals("that") || word.equals("it")
				//|| word.equals("do") 
				//|| word.equals("have") 
				|| word.equals("has") || word.equals("had") 
				|| word.equals("was") || word.equals("were") 
				//|| word.equals("be") 
				|| word.equals("am") || word.equals("are") || word.equals("is")
				/*
				|| word.equals("object") || word.equals("subject")
				*/
				) return true;
		else return false;
	}

	public int getLogVerbosity() {
		return logVerbosity;
	}

	public void setLogVerbosity(int logVerbosity) {
		this.logVerbosity = logVerbosity;
	}

	public NLPtool getNlpTool() {
		return nlpTool;
	}

	public void setNlpTool(NLPtool nlpTool) {
		this.nlpTool = nlpTool;
	}

	public boolean isLogging() {
		return logging;
	}

	public void setLogging(boolean logging) {
		this.logging = logging;
	}

}
