package wordnet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.SynsetID;

public class WordnetAPIMIT implements WordnetAPI{
	
	private WordnetConfiguration wordnetConfig;
	private Dictionary dict;
	
	public WordnetAPIMIT(WordnetConfiguration wordnetConfiguration ) throws IOException{
		wordnetConfig = wordnetConfiguration;

		// construct the URL to the Wordnet dictionary directory
		String wnhome = wordnetConfig.getWordnetFilePath();
		String path = wnhome + File.separator + "dict" ;
		URL url = new URL ( "file" , null , path ) ;
		// construct the dictionary object and open it
		dict = new Dictionary ( url ) ;
		dict.getCache().setEnabled(false);
		dict.open();
	}
	
	
	public List<String> lookupSynset(String word, String pos){ 
		List<String> synsets = new LinkedList<String>();
		POS posLocal = null;
		if(pos.equals("noun")) posLocal = POS.NOUN;
		if(pos.equals("verb")) posLocal = POS.VERB;
		//System.out.println(" - - - Looking up "+word+" as a ("+pos+")");
		if(pos.equals("adjective")) posLocal = POS.ADJECTIVE;
		if(pos.equals("adverb")) posLocal = POS.ADVERB;
		IIndexWord idxWord = dict.getIndexWord (word, posLocal) ;
		if(idxWord == null && pos.equals("verb")){
			idxWord = dict.getIndexWord (word, POS.ADVERB) ;
		}
		if(idxWord == null){
			//System.out.println("Haven't found a result for "+word+" as "+pos);
		} else {
			List<IWordID> wordIDs = idxWord.getWordIDs () ;
			for(IWordID wID : wordIDs){
				ISynsetID synsetID = wID.getSynsetID();
				synsets.add(synsetID.toString());
				//IWord iword = dict.getWord(wID);
				//ISynset synset = iword.getSynset();
				//System.out.println(" - - - - Synset offset: "+synsetID.getOffset()+" Synset ID: "+ synsetID+" Synset: "+synset+" with gloss "+synset.getGloss());			
			}
		}
		return synsets;
		/*System . out . println ( " Gloss = " + iword . getSynset () . getGloss () ) ;	
		System . out . println ( " idxWord = " + idxWord ) ;
		System . out . println ( " idxWord id = " + idxWord . getWordIDs () ) ;
		System . out . println ( " Id = " + wordID ) ;
		System . out . println ( " Lemma = " + iword . getLemma () ) ;
		System . out . println ( " Gloss = " + iword . getSynset () . getGloss () ) ;	*/
	}
	
	public String getSynsetDescription(String synsetID){
		ISynsetID synset = SynsetID.parseSynsetID(synsetID);
		return dict.getSynset(synset).getGloss();
	}
}
