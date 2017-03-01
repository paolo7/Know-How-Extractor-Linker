package nlp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ParsedSentenceSynsets {

	public String sentence = "";
	public String verb = "";
	public String object = "";
	public String others = "";
	
	private Set<String> verbSynsets;
	private Set<String> objectSynsets;
	private Set<String> otherSynsets;
	
	public ParsedSentenceSynsets() {
		verbSynsets = new HashSet<String>();
		objectSynsets = new HashSet<String>();
		otherSynsets = new HashSet<String>();
	}

	public Set<String> getVerbSynsets() {
		return verbSynsets;
	}

	public void addToVerbSynsets(Collection<String> verbSynsets) {
		this.verbSynsets.addAll(verbSynsets);
	}

	public Set<String> getObjectSynsets() {
		return objectSynsets;
	}

	public void addToObjectSynsets(Collection<String> objectSynsets) {
		this.objectSynsets.addAll(objectSynsets);
	}

	public Set<String> getOtherSynsets() {
		return otherSynsets;
	}

	public void addToOtherSynsets(Collection<String> otherSynsets) {
		this.otherSynsets.addAll(otherSynsets);
	}

}
