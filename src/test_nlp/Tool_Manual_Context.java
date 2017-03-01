package test_nlp;

import java.util.ArrayList;

import Data.Graph;
import Data.GraphElement;
import Data.GraphNode;
import Data.Lexeme;
import Data.LexemeArray;
import Data.TAL_generator;
import Extension.Outil;
import Extension.Outil_Traitement;
import Outils.Stanford_Parser;

public class Tool_Manual_Context extends Outil_Traitement {

	@SuppressWarnings("rawtypes")
	public Tool_Manual_Context(TAL_generator gen) {
		super(gen);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void apply(Graph g) {
		GraphNode root=new GraphNode("ROOT",g);
	    Stanford_Parser stanford;
		Tool_Manual_Phrasal_Verb phrasal_verb_tool;
		
		try {
			stanford = (Stanford_Parser)_gen.get_definition("Stanford_Parser");
			phrasal_verb_tool = (Tool_Manual_Phrasal_Verb)_gen.get_definition("Tool_Manual_Phrasal_Verb");
			
			{
			    ArrayList<GraphElement> condition_group=new ArrayList<GraphElement>();
			    g.getFirst().getRoot(stanford).findTagInNode(condition_group,"SBAR",stanford);
			    for(int i=0;i<condition_group.size();i++) {
			    	String first_lemma=condition_group.get(i).getFirst().getWord().toLowerCase();
			    	if(!first_lemma.equals("if")&&!first_lemma.equals("since")&&!first_lemma.equals("while")) continue;
			    	GraphNode parent=new GraphNode("CONTEXT_CONDITION", g);
			    	
					root.addChildren(parent);
					parent.addParent(root, this);
					
			    	LexemeArray list=new LexemeArray(condition_group.get(i));
				    for(int j=0;j<list.size();j++) {
				    	parent.addChildren(list.get(j));
				    	list.get(j).addParent(parent, this);
				    }
			    }
			}
			
			{
			    ArrayList<GraphElement> pp_group=new ArrayList<GraphElement>();
			    g.getFirst().getRoot(stanford).findTagInNode(pp_group,"PP",stanford);
			    for(int i=0;i<pp_group.size();i++) {
			    	Lexeme first=pp_group.get(i).getFirst();
			    	if(first.getParent(phrasal_verb_tool)!=null
			    	&&!first.getParent(phrasal_verb_tool).get_type().equals("ROOT")) continue;
			    	String word=first.getWord().toLowerCase();
			    	
			    	if(word.equals("since")||word.equals("for")||word.equals("during")||word.equals("after")||word.equals("before")||word.equals("ago")) {
				    	GraphNode parent=new GraphNode("CONTEXT_TIME", g);
				    	
						root.addChildren(parent);
						parent.addParent(root, this);
						
				    	LexemeArray list=new LexemeArray(pp_group.get(i));
					    for(int j=0;j<list.size();j++) {
					    	parent.addChildren(list.get(j));
					    	list.get(j).addParent(parent, this);
					    }
			    	}
	
			    	if(word.equals("near")||word.equals("nearby")||word.equals("between")||word.equals("below")
			    			||word.equals("under")||word.equals("up")||word.equals("over")||word.equals("down")||word.equals("around")
			    			||word.equals("through")||word.equals("inside")||word.equals("outside")||word.equals("between")||word.equals("beside")
			    			||word.equals("beyond")||word.equals("behind")||word.equals("next")||word.equals("above")||word.equals("around")
			    			||word.equals("beneath")||word.equals("underneath")||word.equals("among")
			    			||word.equals("along")||word.equals("among")
			    			
			    	||word.equals("over")||word.equals("in")||word.equals("on")||word.equals("inside")||word.equals("from")) {
				    	GraphNode parent=new GraphNode("CONTEXT_SPACE", g);
				    	
						root.addChildren(parent);
						parent.addParent(root, this);
						
				    	LexemeArray list=new LexemeArray(pp_group.get(i));
					    for(int j=0;j<list.size();j++) {
					    	parent.addChildren(list.get(j));
					    	list.get(j).addParent(parent, this);
					    }
			    	}
	
			    	if(word.equals("about")||word.equals("against")) {
				    	GraphNode parent=new GraphNode("CONTEXT_INFORMATION", g);
				    	
						root.addChildren(parent);
						parent.addParent(root, this);
						
				    	LexemeArray list=new LexemeArray(pp_group.get(i));
					    for(int j=0;j<list.size();j++) {
					    	parent.addChildren(list.get(j));
					    	list.get(j).addParent(parent, this);
					    }
			    	}
			    }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// noding all lexeme without parent
		try {
			for(int i=0;i<g.size();i++) {
				Lexeme lex=g.get(i);
				GraphNode parent=lex.getParent(this);
				if(parent==null) {
					lex.addParent(root,this);
					root.addChildren(lex);
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
			list.add((Tool_Manual_Phrasal_Verb)_gen.get_definition("Tool_Manual_Phrasal_Verb"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public String get_name() {
		return "Tool_Manual_Context";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean match_criteria(Graph g) {
		if(g.isEmpty()) return false;
		return true;
	}
}
