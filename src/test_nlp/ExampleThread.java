package test_nlp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.derby.tools.sysinfo;

import nlp.ParsedPhrase2;
import Data.Output;
import Data.WebLabel;

public class ExampleThread implements Runnable {
	private NLP_BOX parser;
	private WebLabel lbl;
	private BufferedWriter output;
	public ExampleThread(NLP_BOX parser,WebLabel lbl,BufferedWriter output) {
		this.parser=parser;
		this.lbl=lbl;
		this.output=output;
	}
	@Override
	public void run() {
		try {
			ArrayList<Output<ParsedPhrase2> > parsed_sentences=parser.parse(lbl,2,false);
			for(int i=0;i<parsed_sentences.size();i++) {
				//System.out.println(parsed_sentences.get(i).arg);
				output.write(parsed_sentences.get(i).arg.toString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
