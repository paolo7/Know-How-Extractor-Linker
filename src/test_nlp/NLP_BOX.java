package test_nlp;

import java.util.ArrayList;

import Data.Output;
import Data.TAL_generator;
import Data.WebLabel;
import Outils.Stanford_Parser;
import nlp.ParsedPhrase2;

public class NLP_BOX {
	private TAL_generator<ParsedPhrase2> gen;
	
	public NLP_BOX() throws Exception {
		gen=new TAL_generator<ParsedPhrase2>();
		gen.define(new Stanford_Parser(gen));
		gen.define(new Tool_Manual_Enumeration(gen));
		gen.define(new Tool_Manual_Context(gen));
		gen.define(new Tool_Manual_Verb_Group(gen));
		gen.define(new Rule_Parsing_Sentence(gen));
		gen.define(new Tool_Stanford_Lemmatizer(gen));
		gen.define(new Tool_Manual_Phrasal_Verb(gen));
		gen.ajouterRegle((Rule_Parsing_Sentence)gen.get_definition("Rule_Parsing_Sentence"));
	}
	
	public ArrayList<Output<ParsedPhrase2> > parse(WebLabel label,int max_sentences,boolean fast) {
		ArrayList<WebLabel> _sentences=new ArrayList<WebLabel>();
		
		boolean modify=true;
		String text=label.label;
		while(modify) {
			modify=false;
			for(int i=0;i<text.length();i++) {
				if(max_sentences==0) break;
				if(text.charAt(i)=='.'||text.charAt(i)=='?'||text.charAt(i)=='!') {
					//case 1 : normal sentence
					if(i<text.length()-2) {
						if(text.charAt(i+1)==' '||text.charAt(i+1)=='"') {
							String first_char_from_sentence=""; first_char_from_sentence+=text.charAt(0);
							String first_char_from_next_sentence=""; first_char_from_next_sentence+=text.charAt(i+2);
							//if(first_char_from_sentence.toUpperCase().equals(first_char_from_sentence))
								//if(first_char_from_next_sentence.toUpperCase().equals(first_char_from_next_sentence)) {
									_sentences.add(new WebLabel(removeAllIncise(text.substring(0,i+1)).toLowerCase(),label.entity));
									//System.out.println("case 1 : "+removeAllIncise(text.substring(0,i+1)).toLowerCase());
									max_sentences--;
									text=text.substring(i+2);
									modify=true;
									break;
								//}
						}
					}
					// case 2 : end sentence
					if(i==text.length()-1) {
						//String first_char_from_sentence=""; first_char_from_sentence+=text.charAt(0);
						//if(first_char_from_sentence.toUpperCase().equals(first_char_from_sentence)) {
							_sentences.add(new WebLabel(removeAllIncise(text.substring(0,i+1)).toLowerCase(),label.entity));
							//System.out.println("case 2 : "+removeAllIncise(text.substring(0,i+1)).toLowerCase());
							max_sentences--;
							text=text.substring(i+1);
							break;
						//}
					}
				}
			}
		}
		// case 3 : sentence still have a last phrase with no punctuation
		if(text.length()>0&&max_sentences>0) {
			//String first_char_from_sentence=""; first_char_from_sentence+=text.charAt(0);
			//if(first_char_from_sentence.toUpperCase().equals(first_char_from_sentence)) {
				String s=removeAllIncise(text).toLowerCase();
				if(s.contains("--")){
					String[] split_trash=s.split("--");
					s=split_trash[0];
				}
				else {
					_sentences.add(new WebLabel(s,label.entity));
				}
				//System.out.println("case 3 : "+text);
				max_sentences--;
				text="";
			//}
		}
		float moyenne=0.0f;
		int count=0;
		try {
			ArrayList<WebLabel> _labels = new ArrayList<WebLabel>();
			
			for(int i=0;i<_sentences.size();i++) {
				count++;
				moyenne=(_sentences.get(i).label.length()+moyenne*count)/(float)(count+1);

				if(_sentences.get(i).label.length()<2)
					continue;
				
				if(_sentences.get(i).label.charAt(1)==')')
					continue;

				if(_sentences.get(i).label.length()<2)
					continue;
				
				_labels.add(new WebLabel(_sentences.get(i).label,_sentences.get(i).entity));
			}
			ArrayList<Output<ParsedPhrase2> > result=new ArrayList<Output<ParsedPhrase2> >();
			gen.process(result,_labels);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<Output<ParsedPhrase2> >();
		}
	}
	public String removeAllIncise(String sentence) {
		sentence=removeIncise(sentence,'-','-');
		sentence=removeIncise(sentence,'(',')');
		return sentence;
	}
	public String removeIncise(String str,char begin_token,char end_token) {
		String result="";
		String subtext="";
		boolean incise=false;
		for(int i=0;i<str.length();i++) {
			if(str.charAt(i)==begin_token&&!incise) {
				incise=true;
				subtext="";
				continue;
			}
			if(str.charAt(i)==end_token&&incise) {
				incise=false;
				subtext="";
				continue;
			}
			if(!incise)
				result+=str.charAt(i);
			else
				subtext+=str.charAt(i);
			
			if(i==str.length()-1&&incise) {
				result+=subtext;
			}
		}
		
		return result.replaceAll("  ", " ");
	}
}
