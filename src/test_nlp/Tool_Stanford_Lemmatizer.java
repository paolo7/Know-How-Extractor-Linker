package test_nlp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import edu.stanford.nlp.process.Morphology;
import Data.Graph;
import Data.TAL_generator;
import Extension.Outil;
import Extension.Outil_Traitement;

public class Tool_Stanford_Lemmatizer extends Outil_Traitement {
    private Morphology morpha = new Morphology();
    private ConcurrentHashMap<String,String> lemma_map = new ConcurrentHashMap<String,String>();
	@SuppressWarnings("rawtypes")
	public Tool_Stanford_Lemmatizer(TAL_generator gen) {
		super(gen);
	}

	@SuppressWarnings("rawtypes")
	@Override
	synchronized public void apply(Graph g) {
		for(int i=0;i<g.size();i++) {
			String word=g.get(i).getWord().toLowerCase();
			if(word.length()<2) {
				g.get(i).setLemme(word);
			}
			else if(lemma_map.containsKey(word)) {
				g.get(i).setLemme(lemma_map.get(word));
			}
			else {
				try {
					String lemma=morpha.stem(word);
					if(lemma==null) lemma=word;
					g.get(i).setLemme(lemma);
					lemma_map.put(word, lemma);
				}
				catch(Exception e) {
					g.get(i).setLemme(word);
				}
			}
		}
	}

	@Override
	public ArrayList<Outil> GetOutilDependency() {
		return new ArrayList<Outil>();
	}

	@Override
	public boolean isPOS() {
		return false;
	}
	
	@Override
	public String get_name() {
		return "Tool_Stanford_Lemmatizer";
	}

	@Override
	public boolean match_criteria(Graph arg0) {
		return true;
	}

}
