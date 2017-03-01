package wordnet;

import java.util.List;

public interface WordnetAPI {

	public List<String> lookupSynset(String word, String pos);
	public String getSynsetDescription(String synsetID);
}
