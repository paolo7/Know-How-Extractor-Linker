package test_nlp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.WordSense;
import edu.smu.tspell.wordnet.impl.file.SynsetFactory;
import edu.smu.tspell.wordnet.impl.file.SynsetPointer;
import wordnet.WordnetAPI;
import wordnet.WordnetAPIMIT;
import wordnet.WordnetConfiguration;
import Data.Graph;
import Data.GraphNode;
import Data.Lexeme;
import Data.TAL_generator;
import Extension.Outil;
import Extension.Outil_Traitement;
import Outils.Stanford_Parser;

public class Tool_Manual_Phrasal_Verb extends Outil_Traitement {
	private WordNetDatabase database;
	private SynsetFactory synsetFactory;
	@SuppressWarnings("rawtypes")
	public Tool_Manual_Phrasal_Verb(TAL_generator gen) throws IOException {
		super(gen);
    	System.setProperty("wordnet.database.dir", "WordNet-3.0/dict"); 
		database = WordNetDatabase.getFileInstance(); 
		synsetFactory = SynsetFactory.getInstance();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void apply(Graph g) {
		try {
			Stanford_Parser stanford=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
			GraphNode root=g.getFirst().getRoot(stanford);
			ArrayList<Lexeme> verbs=new ArrayList<Lexeme>();
			root.findTagInLeaf(verbs, 'V',stanford);
			for(int k=0;k<verbs.size();k++) {
				Lexeme v=verbs.get(k);
				Lexeme potential_particle=v.getNextLexeme();
				
		    	if(potential_particle==null) continue;
		    	
		    	while(true) {
			    	if(potential_particle.getWord().equals("no")||potential_particle.getWord().equals("not")||potential_particle.getWord().equals("n't")) {
			    		potential_particle=potential_particle.getNextLexeme();
			    	}
			    	else break;
		    	}
		    	
		    	if(potential_particle==null) continue;
		    	
		    	Synset[] synsets =null;
		    	try {
		    		synsets = database.getSynsets(v.getLemme(),SynsetType.VERB);
		    	}
		    	catch(Exception e) {
					//System.out.println("lemme error phrasal = "+v.getLemme()+" "+v.getWord());
					continue;
		    	}
		    	
				for(int i=0;i<synsets.length;i++) {
					VerbSynset s=(VerbSynset)synsets[i];
					WordSense[] w=s.getPhrases(v.getLemme());
					for(int j=0;j<w.length;j++) {
						
						/*System.out.println(w[j].getWordForm()+" = "+w[j].getSynset().getDefinition());
						for(int m=0;m<w[j].getSynset().getUsageExamples().length;m++) {
							System.out.println("   "+w[j].getSynset().getUsageExamples()[m]);
						}*/
						if(w[j].getWordForm().contains(v.getLemme())) {
							
							String particle=null;
							if(w[j].getWordForm().equals(v.getLemme())) particle=v.getLemme();
							else particle=w[j].getWordForm().substring(v.getLemme().length()+1);
							
							if(particle.equals(potential_particle.toString())) {
								GraphNode parent=new GraphNode("PHRASAL_VERB", g);
								parent.addChildren(v);
								parent.addChildren(potential_particle);
								v.addParent(parent, this);
								potential_particle.addParent(parent, this);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	@Override
	public boolean isPOS() {
		return false;
	}

	@Override
	public ArrayList<Outil> GetOutilDependency() {
		ArrayList<Outil> list=new ArrayList<Outil>();
		try {
			list.add((Tool_Stanford_Lemmatizer)_gen.get_definition("Tool_Stanford_Lemmatizer"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public String get_name() {
		return "Tool_Manual_Phrasal_Verb";
	}

	@Override
	public boolean match_criteria(Graph g) {
		if(g.isEmpty()) return false;
		return true;
	}

	 /**
	  * 
	  * @param synsetOffset Synset number to look up frames for
	  * @return Array of frames for the synset; only returns frames
	  * which apply to every word in the synset
	  * frames 
	  */
	  private List<String> getGeneralSynsetFrames(int synsetOffset){
		  SynsetPointer sp = new SynsetPointer(SynsetType.VERB, synsetOffset);
		  VerbSynset vSynset = (VerbSynset) synsetFactory.getSynset(sp);
		  List<String> frames = new ArrayList<String>(); 
		  for(String s : vSynset.getSentenceFrames())
			  frames.add(s);
		  return frames;
	 }
	 
	 /**
	  * 
	  * @param lemma Base form of the word you want to look up
	  * @return Array of frames for the lemma; only returns those
	  * that are specific to a particular word form within each synset.
	  * frames 
	  */
	 private List<String> getWordFramesSpecific(String lemma){
		 List<String> frames = new ArrayList<String>();
		 Synset[] synsets = database.getSynsets(lemma,SynsetType.VERB);
		 for(Synset synset : synsets){
			   for(String s : ((VerbSynset) synset).getSentenceFrames(lemma))
				   frames.add(s);
		 }
		 return frames;
	 }
	  
	 /**
	  * This one is more difficult to understand...
	  * @param lemma Base form of the word you want to look up
	  * @return Array of frames for the lemma; only returns those
	  * that match every word in each of the synsets that contain this word.
	  */
	  private List<String> getWordFramesGeneral(String lemma){
		  List<String> frames = new ArrayList<String>(); 
		  Synset[] synsets = database.getSynsets(lemma,SynsetType.VERB);
		  for(Synset synset : synsets){
		   for(String s : ((VerbSynset) synset).getSentenceFrames())
		    frames.add(s);
		  }
		  return frames;
	 }
	 
	 /**
	  * This method is the best. It returns all possible frames
	  * given a synset number and the accompanying word.
	  * @param lemma Base form of the word you want to look up
	  * @param synsetOffset Synset number to look up frames for
	  * @return Array of frames for the synset; returns all frames
	  * for this word within this synset.
	  * frames 
	  */ 
	  private List<String> getWordFramesComplete(String lemma, int synsetOffset){
		  SynsetPointer sp = new SynsetPointer(SynsetType.VERB, synsetOffset);
		  VerbSynset vSynset = (VerbSynset) synsetFactory.getSynset(sp);
		  List<String> frames = new ArrayList<String>();
		  for(String s : vSynset.getSentenceFrames(lemma))
		   frames.add(s);
		  for(String s : vSynset.getSentenceFrames())
		   frames.add(s);
		  return frames;
		 }
}
