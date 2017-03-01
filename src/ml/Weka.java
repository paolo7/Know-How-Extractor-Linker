package ml;

import java.util.List;

import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

public class Weka {

	private Instances wekaInstanceSet;
	private FastVector attrInfo = new FastVector();

	public Weka(String[] features) {
		for (String feature : features) {
			Attribute attribute = new Attribute(feature);
			attrInfo.addElement(attribute);
		}
		
		FastVector targetValues = new FastVector();
		targetValues.addElement("true");
		targetValues.addElement("false");
		Attribute target = new Attribute("target", targetValues);
		attrInfo.addElement(target);

		SMO scheme = new SMO();
		 // set options
		 try {
			scheme.setOptions(Utils.splitOptions("-C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\""));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void addInstance(List<Integer> featureValues){
		Instance wekaInstance = new Instance(attrInfo.size());
		for (int i = 0; i < featureValues.size(); i++) {
		    if (featureValues.get(i) != null) {
		        wekaInstance.setValue((Attribute) attrInfo.elementAt(i), featureValues.get(i));
		    }
		}
		wekaInstanceSet.add(wekaInstance);
	}
	
	public void classify(){
		String[] options = new String[1];
		 options[0] = "-U";            // unpruned tree
		 J48 tree = new J48();         // new instance of tree
		 try {
			 tree.setOptions(options);     // set the options
			tree.buildClassifier(wekaInstanceSet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   // build classifier
	}

}
