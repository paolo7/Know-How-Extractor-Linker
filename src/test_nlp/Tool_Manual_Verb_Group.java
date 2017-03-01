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

public class Tool_Manual_Verb_Group extends Outil_Traitement {
	@SuppressWarnings("rawtypes")
	public Tool_Manual_Verb_Group(TAL_generator gen) {
		super(gen);
	}
	@SuppressWarnings("rawtypes")
	@Override
	public boolean match_criteria(Graph g) {
		if(g.isEmpty()) return false;
		return true;
	}
	@SuppressWarnings("rawtypes")
	@Override
	public void apply(Graph g) {
		GraphNode root=new GraphNode("ROOT",g);
		Stanford_Parser stanford=null;
		Tool_Manual_Phrasal_Verb phrasal_verb_tool=null;

		try {
			stanford=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
			phrasal_verb_tool=(Tool_Manual_Phrasal_Verb)_gen.get_definition("Tool_Manual_Phrasal_Verb");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		for(int i=0;i<g.size();i++) {
		    try {
				LexemeArray result=new LexemeArray();
				Lexeme begin=g.get(i);
				String tag_begin=begin.getParent(stanford).get_type();
				if(!tag_begin.equals("MD")&&tag_begin.charAt(0)!='V') continue;
				if(begin.getParent(this)!=null) continue;
				//-- finding negation , cunjunction and verbs
				while(begin!=null) {
					String tag=begin.getParent(stanford).get_type();
					String word=begin.getWord().toLowerCase();
					
					if(!tag.equals("MD")
						&&tag.charAt(0)!='V'
						&&!tag.equals("RB")
						&&!tag.equals("JJ")
						&&!word.toLowerCase().equals("to")
						&&!word.toLowerCase().equals("not")
						&&!word.toLowerCase().equals("n&apos;t")) break;

					result.add(begin);

					GraphNode n=begin.getParent(phrasal_verb_tool);
					if(n!=null) {
						GraphElement elem=n.get_children().get(n.get_children().size()-1);
						result.add(elem);
						break;
					}
					begin=begin.getNextLexeme();
				}
				if(result.isEmpty()) continue;
				//create node
				GraphNode parent=new GraphNode("VERB_GROUP",g);
				
				root.addChildren(parent);
				parent.addParent(root, this);
				for(int j=0;j<result.size();j++) {
					parent.addChildren(result.get(j));
					result.get(j).addParent(parent,this);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
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
			list.add((Stanford_Parser)_gen.get_definition("Stanford_Parser"));
			list.add((Tool_Manual_Phrasal_Verb)_gen.get_definition("Tool_Manual_Phrasal_Verb"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public String get_name() {
		return "Tool_Manual_Verb_Group";
	}

}
