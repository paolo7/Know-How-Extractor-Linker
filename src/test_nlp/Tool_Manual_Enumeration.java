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

public class Tool_Manual_Enumeration extends Outil_Traitement {
	@SuppressWarnings("rawtypes")
	public Tool_Manual_Enumeration(TAL_generator gen) {
		super(gen);
	}
	@SuppressWarnings("rawtypes")
	@Override
	public boolean match_criteria(Graph g) {
		try {
			if(g.isEmpty()) return false;
			Stanford_Parser stanford=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
			GraphNode root=g.getFirst().getRoot(stanford);
			ArrayList<Lexeme> result=new ArrayList<Lexeme>();
			root.findTagInLeaf(result, 'V',stanford);
			if(result.isEmpty()) return false;
			if(!g.getTexte().contains(",")
			&&!g.getTexte().contains("and")
			&&!g.getTexte().contains("or")
			&&!g.getTexte().contains("/"))
				return false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	@SuppressWarnings("rawtypes")
	@Override
	public void apply(Graph g) {
		GraphNode root=new GraphNode("ROOT",g);
		try {
			Stanford_Parser stanford=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
			//-- '/' case
			{
				LexemeArray[] split=g.split("/");
				
			}
			//-- 'and' case
			{
				parse(g,"and",root);
			}
			//-- 'or' case
			{
				parse(g,"or",root);
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
	private void parse(Graph g, String separator,GraphNode root) throws Exception {
		Stanford_Parser stanford=(Stanford_Parser)_gen.get_definition("Stanford_Parser");
		//-- premade case
		{
			GraphNode root_syntax=g.getFirst().getRoot(stanford);
			ArrayList<GraphElement> potential_premade_enumerations=new ArrayList<GraphElement>();
			root_syntax.findTagInNode(potential_premade_enumerations,"NP",stanford);
			
			for(int i=1;i<potential_premade_enumerations.size();i++) {
				LexemeArray potential_enumeration=new LexemeArray(potential_premade_enumerations.get(i));
				if(potential_enumeration.contains(separator)) {
					ArrayList<String> array_tag=new ArrayList<String>(); array_tag.add(","); array_tag.add(separator);
					LexemeArray[] split=potential_enumeration.split(array_tag);
					if(split.length>1) {
						ArrayList<LexemeArray> result=new ArrayList<LexemeArray>();
						for(int j=0;j<split.length;j++) {
							result.add(split[j]);
						}
						//System.out.println("CASE 1 "+g.getTexte()+": "+result);
						//System.out.println(Stanford_Parser.last_result);
						GraphNode grp=new GraphNode("list",g);
				    	
						root.addChildren(grp);
						grp.addParent(root, this);
						
						for(int element_i=0;element_i<result.size();element_i++) {
							GraphNode element=new GraphNode("list_element",g);
							grp.addChildren(element);
							element.addParent(grp, this);
							
							LexemeArray array=result.get(element_i);
							for(int child=0;child<array.size();child++) {
								element.addChildren(array.get(child));
								array.get(child).addParent(element, this);
							}
						}
						//grp.print("");
					}
				}
			}
		}
		//-- nature case
		/*{
			LexemeArray[] split=g.split(separator);
			for(int i=1;i<split.length;i++) {
				GraphNode after_separator=split[i].getFirst().getRootWithType(stanford,"NP");
				GraphNode sentence_container=split[i].getFirst().getRootWithType(stanford,"S");
				if(after_separator!=null&&after_separator.getFirst().getParent(this)==null) {
					LexemeArray[] second_split=split[i-1].split(",");
					ArrayList<LexemeArray> result=new ArrayList<LexemeArray>();
					result.add(new LexemeArray(after_separator));
					for(int j=second_split.length-1;j>=0;j--) {
						LexemeArray potential_element=second_split[j];
						GraphNode node=potential_element.getLast().getRootWithType(stanford, "NP");
						GraphNode sentence=potential_element.getLast().getRootWithType(stanford, "S");
						if(sentence!=sentence_container) break;
						boolean contained=true;
						for(int k=0;k<potential_element.size();k++) {
							if(potential_element.get(k).getRootWithType(stanford, "NP")!=node) {
								contained=false;
								break;
							}
						}
						if(!contained) {
							result.add(new LexemeArray(node));
							break;
						}
						result.add(potential_element);
					}
					System.out.println("CASE 2 "+g.getTexte()+": "+result);
					//System.out.println(Stanford_Parser.last_result);
					GraphNode grp=new GraphNode("list",g);
			    	
					root.addChildren(grp);
					grp.addParent(root, this);
					
					for(int element_i=0;element_i<result.size();element_i++) {
						GraphNode element=new GraphNode("list_element",g);
						grp.addChildren(element);
						element.addParent(grp, this);
						
						LexemeArray array=result.get(element_i);
						for(int child=0;child<array.size();child++) {
							element.addChildren(array.get(child));
							array.get(child).addParent(element, this);
						}
					}
				}
			}
		}*/
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public String get_name() {
		return "Tool_Manual_Enumeration";
	}

}
