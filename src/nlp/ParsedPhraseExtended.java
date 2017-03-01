package nlp;

import java.util.HashSet;
import java.util.Set;

public class ParsedPhraseExtended extends ParsedPhrase{

	private Set<String> objects;
	private Set<String> objectsSynsets;
	
	private Set<String> verbs;
	private Set<String> verbsSynsets;
	
	private boolean positive;
	
	private Set<String> adjectives;
	private Set<String> adjectivesSynsets;
	
	private Set<String> adverbs;
	private Set<ParsedPhrase> auxiliaryPhrases;
	private Set<String> subjects;
	
	public ParsedPhraseExtended() {
		objects = new HashSet<String>();
		objectsSynsets = new HashSet<String>();
		verbs = new HashSet<String>();
		verbsSynsets = new HashSet<String>();
		adjectives = new HashSet<String>();
		adjectivesSynsets = new HashSet<String>();
		adverbs = new HashSet<String>();
		subjects = new HashSet<String>();
		auxiliaryPhrases = new HashSet<ParsedPhrase>();
	}

	public Set<ParsedPhrase> getAuxiliaryPhrases() {
		return auxiliaryPhrases;
	}

	public void setAuxiliaryPhrases(Set<ParsedPhrase> auxiliaryPhrases) {
		this.auxiliaryPhrases = auxiliaryPhrases;
	}

	public Set<String> getAdjectivesSynsets() {
		return adjectivesSynsets;
	}

	public void setAdjectivesSynsets(Set<String> adjectivesSynsets) {
		this.adjectivesSynsets = adjectivesSynsets;
	}

	public Set<String> getAdjectives() {
		return adjectives;
	}

	public void setAdjectives(Set<String> adjectives) {
		this.adjectives = adjectives;
	}

	public boolean isPositive() {
		return positive;
	}

	public void setPositive(boolean positive) {
		this.positive = positive;
	}

	public Set<String> getVerbsSynsets() {
		return verbsSynsets;
	}

	public void setVerbsSynsets(Set<String> verbsSynsets) {
		this.verbsSynsets = verbsSynsets;
	}

	public Set<String> getObjectsSynsets() {
		return objectsSynsets;
	}

	public void setObjectsSynsets(Set<String> objectsSynsets) {
		this.objectsSynsets = objectsSynsets;
	}

	public Set<String> getObjects() {
		return objects;
	}

	public void setObjects(Set<String> objects) {
		this.objects = objects;
	}

	public Set<String> getVerbs() {
		return verbs;
	}

	public void setVerbs(Set<String> verbs) {
		this.verbs = verbs;
	}

	public Set<String> getAdverbs() {
		return adverbs;
	}

	public void setAdverbs(Set<String> adverbs) {
		this.adverbs = adverbs;
	}

	public Set<String> getSubjects() {
		return subjects;
	}

	public void setSubjects(Set<String> subjects) {
		this.subjects = subjects;
	}

}
