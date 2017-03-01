package test_nlp;

import java.io.IOException;
import java.util.ArrayList;

import org.annolab.tt4j.TokenHandler;

import nlp.ParsedPhrase2;
import Data.Graph;
import Data.GraphElement;
import Data.GraphNode;
import Data.Lexeme;
import Data.LexemeArray;
import Data.TAL_generator;
import Extension.Outil;
import Extension.Regle;
import Outils.Stanford_Parser;

public class Rule_Parsing_Sentence extends Regle<ParsedPhrase2> {
	public Rule_Parsing_Sentence(TAL_generator<ParsedPhrase2> gen) throws IOException {
		super(gen);
	}

    public class TokenLemma implements TokenHandler<String> {
    	private String _lemme;
    	private String _tag;
    	private String _word;
    	
		@Override
		public void token(String arg0, String arg1, String arg2) {
			_lemme= arg2;
			_tag= arg1;
			_word= arg0;
		}

		public String get_lemme() {
			return _lemme;
		}

		public String get_tag() {
			return _tag;
		}

		public String get_word() {
			return _word;
		}
    }
    
	@SuppressWarnings("rawtypes")
	@Override
	public boolean match_criteria(Graph g) {
		if(g.isEmpty()) return false;
		try {
			Stanford_Parser parser=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
			GraphNode root=g.getFirst().getRoot(parser);
			ArrayList<Lexeme> result=new ArrayList<Lexeme>();
			root.findTagInLeaf(result, 'V',parser);
			if(result.isEmpty()) return false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	@Override
	public ArrayList<Regle<ParsedPhrase2> > GetRegleDependency() {
		return new ArrayList<Regle<ParsedPhrase2> >();
	}

	@Override
	public ArrayList<Outil> GetOutilDependency() {
		ArrayList<Outil> list=new ArrayList<Outil>();
		try {
			list.add((Stanford_Parser)_gen.get_definition("Stanford_Parser"));
			list.add((Tool_Manual_Enumeration)_gen.get_definition("Tool_Manual_Enumeration"));
			list.add((Tool_Manual_Verb_Group)_gen.get_definition("Tool_Manual_Verb_Group"));
			list.add((Tool_Stanford_Lemmatizer)_gen.get_definition("Tool_Stanford_Lemmatizer"));
			list.add((Tool_Manual_Context)_gen.get_definition("Tool_Manual_Context"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public String getType() {
		return "Rule_Parsing_Sentence";
	}

	@Override
	synchronized public void apply(Graph<ParsedPhrase2> g) throws Exception {
	    Stanford_Parser stanford=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
		// FIRST PARSE THE VERB
	    LexemeArray verb_result=match_verb(g);
	    if(verb_result.isEmpty()) {
	    	return;
	    }
	    int place_verb=verb_result.getFirst().getPlace();
	    GraphNode verb_sentence=verb_result.getFirst().getRootWithType(stanford, "S");
	    
	    // SECOND, PARSE THE SUBJECT
	    LexemeArray subject_result=match_subject(g,place_verb,verb_sentence);

	    // THIRD, GET CONTEXT
	    ContextMap context_result=match_context(g,place_verb,verb_sentence);
	    
	    // FOURTHLY, PARSE THE OBJECT
	    LexemeArray object_result=match_object(g,context_result,place_verb,verb_sentence);
	    if(!object_result.isEmpty()) {
		    // parsing "to" word
		    LexemeArray[] split_to=object_result.split("to");
		    if(split_to.length>1) {
		    	object_result=split_to[0];
		    }
		    // parsing "that" word
		    LexemeArray[] split_that=object_result.split("that");
		    if(split_that.length>1) {
		    	object_result=split_that[0];
		    }
	    }
	    
	    ParsedPhrase2 s=new ParsedPhrase2();
	    s.setFull_text(g.getTexte());
	    
	   	GraphNode enumeration_subject_node=getEnumerationNode(subject_result);
	   	if(enumeration_subject_node==null) {
   			epure(subject_result);
	   		s.addSubject(subject_result);
	   	}
	   	else {
	   		for(int i=0;i<enumeration_subject_node.get_children().size();i++) {
	   			LexemeArray array=new LexemeArray(enumeration_subject_node.get_children().get(i));
	   			epure(array);
	   			s.addSubject(array);
	   		}
	   	}
	   	//-- negation
	   	for(int i=0;i<verb_result.size();i++)
	    	if(verb_result.get(i).getWord().equals("not")||verb_result.get(i).getWord().equals("n't")) {
	    		verb_result.remove(i);
	    		s.setNegative(true);
	    		break;
	    	}
	    //-- adverb
	    for(int i=0;i<verb_result.size();i++)
	    	if(verb_result.get(i).getTag("SYNTAX").equals("RB")) {
	    		s.setAdverb(verb_result.get(i));
	    		verb_result.remove(i);
	    		break;
	    	}
	    //-- adjective
	    for(int i=0;i<verb_result.size();i++)
	    	if(verb_result.get(i).getTag("SYNTAX").equals("JJ")) {
	    		s.setAdjective(verb_result.get(i));
	    		verb_result.remove(i);
	    		break;
	    	}
	   	/*GraphNode enumeration_verb_node=getEnumerationNode(verb_result);
	   	if(enumeration_verb_node==null)
	   		s.addVerb(convert_to_lemma(verb_result));
	   	else {
	   		for(int i=0;i<enumeration_verb_node.get_children().size();i++) {
	   			s.addVerb(convert_to_lemma(new LexemeArray(enumeration_verb_node.get_children().get(i))));
	   		}
	   	}*/
	    s.addVerb(verb_result);
	   	
	   	GraphNode enumeration_object_node=getEnumerationNode(object_result);
	   	if(enumeration_object_node==null) {
	    	epure(object_result);
	   		s.addObject(object_result);
	   	}
	   	else {
	   		for(int i=0;i<enumeration_object_node.get_children().size();i++) {
	   			LexemeArray array=new LexemeArray(enumeration_object_node.get_children().get(i));
	   			epure(array);
	   			s.addObject(array);
	   		}
	   	}

	    s.setContext(context_result);
	    
	    fill_trash_box(g, s);
	    
	    g.declencher(this,g,s);
	}

	private GraphNode getEnumerationNode(LexemeArray array) throws Exception {
		Tool_Manual_Enumeration tool=(Tool_Manual_Enumeration)_gen.get_definition("Tool_Manual_Enumeration");
		for(int i=0;i<array.size();i++) {
			GraphNode n=array.get(i).getRootWithType(tool, "list");
			if(n!=null) return n;
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private void fill_trash_box(Graph g,ParsedPhrase2 s) throws Exception {
		LexemeArray tmp=new LexemeArray();
		for(int i=0;i<g.size();i++) {
			Lexeme lex=g.get(i);
			if(!s.isSubject(lex)&&!s.isVerb(lex)&&!s.isObject(lex)&&!s.isAdverb(lex)&&!s.isAdjective(lex)&&!s.iscontext(lex)&&lex.getWord().length()>1)
				tmp.add(lex);
		}
		epure(tmp);
		for(int i=0;i<tmp.size();i++) {
			s.getTrash().add(tmp.get(i));
		}
	}
	private ContextMap match_context(Graph<ParsedPhrase2> g,int place_verb,GraphNode verb_sentence) throws Exception {
		ContextMap context_result=new ContextMap();

		Tool_Manual_Context tool=(Tool_Manual_Context)_gen.get_definition("Tool_Manual_Context");
		
		{
		    ArrayList<GraphElement> context_nodes=new ArrayList<GraphElement>();
		    g.getFirst().getRoot(tool).findTagInNode(context_nodes,"CONTEXT_TIME",tool);
		    for(int i=0;i<context_nodes.size();i++){
		    	LexemeArray array=new LexemeArray(context_nodes.get(i));
		    	epure(array);
		    	context_result.addTimeContext(array);
		    }
		}

		{
		    ArrayList<GraphElement> context_nodes=new ArrayList<GraphElement>();
		    g.getFirst().getRoot(tool).findTagInNode(context_nodes,"CONTEXT_SPACE",tool);
		    for(int i=0;i<context_nodes.size();i++){
		    	LexemeArray array=new LexemeArray(context_nodes.get(i));
		    	epure(array);
		    	context_result.addSpaceContext(array);
		    }
		}

		{
		    ArrayList<GraphElement> context_nodes=new ArrayList<GraphElement>();
		    g.getFirst().getRoot(tool).findTagInNode(context_nodes,"CONTEXT_CONDITION",tool);
		    for(int i=0;i<context_nodes.size();i++){
		    	LexemeArray array=new LexemeArray(context_nodes.get(i));
		    	epure(array);
		    	context_result.addConditionContext(array);
		    }
		}

		{
		    ArrayList<GraphElement> context_nodes=new ArrayList<GraphElement>();
		    g.getFirst().getRoot(tool).findTagInNode(context_nodes,"CONTEXT_INFORMATION",tool);
		    for(int i=0;i<context_nodes.size();i++){
		    	LexemeArray array=new LexemeArray(context_nodes.get(i));
		    	epure(array);
		    	context_result.addInformationContext(array);
		    }
		}
		
		return context_result;
	}

	private void epure(LexemeArray array) {
		boolean modify=true;
		while(modify) {
			modify=false;
			for(int i=0;i<array.size();i++) {
				if(array.get(i).getTag("SYNTAX").equals("DT")
				||array.get(i).getTag("SYNTAX").equals("PRP$")) {
					array.remove(i);
					modify=true;
					break;
				}
			}
		}
	}
	private LexemeArray match_object(Graph<ParsedPhrase2> g,ContextMap context_result, int place_verb,GraphNode verb_sentence) throws Exception {
	    LexemeArray object_result=new LexemeArray();
	    Stanford_Parser stanford=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
	    
	    ArrayList<GraphElement> np_group=new ArrayList<GraphElement>();
	    g.getFirst().getRoot(stanford).findTagInNode(np_group,"NP",stanford);
	    ArrayList<GraphElement> potential_objects=new ArrayList<GraphElement>();
	    for(int i=0;i<np_group.size();i++) {
	    	GraphNode sentence=np_group.get(i).getRootWithType(stanford, "S");
		    if(np_group.get(i).getPlace()>place_verb&&sentence==verb_sentence) {
	    		LexemeArray array=new LexemeArray(np_group.get(i));
	    		if(!context_result.iscontext(array))
		    		potential_objects.add(np_group.get(i));
	    	}
	    }
	   	if(potential_objects.isEmpty()) return object_result;
	   	object_result.add(potential_objects.get(0));
	   	
	   	return object_result;
	}

	private LexemeArray match_subject(Graph<ParsedPhrase2> g, int place_verb,GraphNode verb_sentence) throws Exception {
		LexemeArray result=new LexemeArray();
		Stanford_Parser stanford=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
	    //--we are looking for nouns before verbs
	    ArrayList<GraphElement> np_group=new ArrayList<GraphElement>();
	    g.getFirst().getRoot(stanford).findTagInNode(np_group,"NP",stanford);

	    ArrayList<GraphElement> potential_subjects=new ArrayList<GraphElement>();
	    for(int i=0;i<np_group.size();i++) {
	    	GraphNode sentence=np_group.get(i).getRootWithType(stanford, "S");
	    	if(np_group.get(i).getPlace()<place_verb&&sentence==verb_sentence)
	    		potential_subjects.add(np_group.get(i));
	    }
	    
	    if(!potential_subjects.isEmpty())
	    	result.add(potential_subjects.get(potential_subjects.size()-1));
	    
	    return result;
	}

	private LexemeArray match_verb(Graph<ParsedPhrase2> g) throws Exception {
		Tool_Manual_Context context_tool=(Tool_Manual_Context)_gen.get_definition("Tool_Manual_Context");
		Tool_Manual_Verb_Group verb_tool=(Tool_Manual_Verb_Group)_gen.get_definition("Tool_Manual_Verb_Group");
		Stanford_Parser stanford=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
		
		GraphNode root=g.getFirst().getRoot(verb_tool);
		ArrayList<GraphElement> result=new ArrayList<GraphElement>();
		root.findTagInNode(result, "VERB_GROUP", verb_tool);
		if(result.isEmpty()) return new LexemeArray();
		// taking the first one not included in a condition context element
		for(int choice=0;choice<result.size();choice++) {
			Lexeme first=result.get(choice).getFirst();
			if(first==null) continue;
			GraphNode parent=first.getParent(context_tool);
			if(parent==null||parent.get_type().equals("ROOT"))
				return new LexemeArray(result.get(choice));
		}
		return new LexemeArray();
	}

	@Override
	public String get_name() {
		return "Rule_Parsing_Sentence";
	}
}
