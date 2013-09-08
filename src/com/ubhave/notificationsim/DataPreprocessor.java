package com.ubhave.notificationsim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.attributeSelection.WrapperSubsetEval;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.MathExpression;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class DataPreprocessor {

	Instances d_instances;
	
	public void readData(String a_fileName, String a_className) throws Exception {
		
		d_instances = DataSource.read(a_fileName);        
		Attribute classAttribute = d_instances.attribute(a_className);
		d_instances.setClass(classAttribute);
	    
		/*if (a_classIndex.length() == 0)
	    	a_classIndex = "last";
	    if (a_classIndex.equals("first"))
	    	d_instances.setClassIndex(0);
	    else if (a_classIndex.equals("last"))
	    	d_instances.setClassIndex(d_instances.numAttributes() - 1);	    	
	    else
	    	d_instances.setClassIndex(Integer.parseInt(a_classIndex) - 1);*/	   
	}
	
	public void classAttributeTholding(int a_thold, boolean a_presence) throws Exception {
		
		MathExpression tholdFilter = new MathExpression();
		if (a_presence) {
			tholdFilter.setExpression("ifelse(A<"+a_thold+",1,0)"); // This is for presence
		} else {
			tholdFilter.setExpression("ifelse(A<"+a_thold+",0,1)"); // This is for sentiment
		}
		tholdFilter.setIgnoreClass(true);
		tholdFilter.setInvertSelection(true);
		tholdFilter.setIgnoreRange("last");
		tholdFilter.setInputFormat(d_instances);
		d_instances =Filter.useFilter(d_instances, tholdFilter);
		
		// After this "1" indicates that the answer came within a_thold minutes
		NumericToNominal numToNom = new NumericToNominal();
		numToNom.setAttributeIndices("last");
		numToNom.setInputFormat(d_instances);
		d_instances = Filter.useFilter(d_instances, numToNom);
	}
	
	public void filterData(ArrayList<String> a_preserveAttributes) throws Exception{
		
		ArrayList<Integer> removeIndices = new ArrayList<Integer>();		
		
		Enumeration<Attribute> iterator = d_instances.enumerateAttributes();
		while (iterator.hasMoreElements()){
			Attribute atr = (Attribute) iterator.nextElement();
			if (!(a_preserveAttributes.contains(atr.name()) || atr.name()==d_instances.classAttribute().name())) {
				removeIndices.add(atr.index());
			}
		}
		
		Remove remove = new Remove();
		remove.setAttributeIndicesArray(convertIntegers(removeIndices));
		remove.setInputFormat(d_instances);
		d_instances = Filter.useFilter(d_instances, remove);
		
		if (a_preserveAttributes.contains("user_id")) {
			NumericToNominal numToNom = new NumericToNominal();
			Attribute userIDAttribute = d_instances.attribute("user_id");
			int filterIndices[] = {userIDAttribute.index()};
			numToNom.setAttributeIndicesArray(filterIndices);
			numToNom.setInputFormat(d_instances);
			d_instances = Filter.useFilter(d_instances, numToNom);
		}
	}
	
	public void filterNonAnsweredData() throws Exception {
		RemoveWithValues remove = new RemoveWithValues();
		Attribute userIDAttribute = d_instances.attribute("hour_of_day_false");
		remove.setAttributeIndex(String.valueOf(userIDAttribute.index()+1));
		remove.setSplitPoint(-1.0);
		remove.setInvertSelection(false);
		remove.setMatchMissingValues(true);
		remove.setInputFormat(d_instances);
		d_instances = Filter.useFilter(d_instances, remove);
		
		//weka.filters.unsupervised.instance.RemoveWithValues -S 1439.0 -C last -L first-last -V
	}
	
	
	public Instances getData() {
		return d_instances;
	}
	
	public void saveToArff(String a_filename) throws IOException {
		ArffSaver saver = new ArffSaver();
		saver.setInstances(d_instances);
		saver.setFile(new File(a_filename));
		saver.writeBatch();
	}
	
	public static int[] convertIntegers(ArrayList<Integer> integers)
	{
	    int[] ret = new int[integers.size()];
	    for (int i=0; i < ret.length; i++)
	    {
	        ret[i] = integers.get(i).intValue();
	    }
	    return ret;
	}

	public void featureSelection() throws Exception {
		
		ArrayList<Integer> removeIndices = new ArrayList<Integer>();	
		Attribute timestampAttribute = d_instances.attribute("timestamp");
		Attribute userIDAttribute = d_instances.attribute("user_id");						

		Remove remove = new Remove();
		if (timestampAttribute != null) removeIndices.add(timestampAttribute.index());
		if (userIDAttribute != null) removeIndices.add(userIDAttribute.index());
		remove.setAttributeIndicesArray(convertIntegers(removeIndices));
		remove.setInputFormat(d_instances);
		Instances tmpInstances = Filter.useFilter(d_instances, remove);
		
		System.out.println("All attributes:\n");
		for (int i=0; i<tmpInstances.numAttributes(); i++){
			System.out.println(tmpInstances.attribute(i).name());
		}
		
		weka.attributeSelection.AttributeSelection attsel = new weka.attributeSelection.AttributeSelection();  // package weka.attributeSelection!
		// OPTION1: WrapperSubsetEval eval = new WrapperSubsetEval();    
		// OPTION1: eval.setClassifier(new NaiveBayes());   		 
		CfsSubsetEval eval = new CfsSubsetEval();  // OPTION2:
		GreedyStepwise search = new GreedyStepwise();
		search.setSearchBackwards(true);
		attsel.setEvaluator(eval);
		attsel.setSearch(search);
		attsel.SelectAttributes(tmpInstances);
		int[] indices = attsel.selectedAttributes();
		System.out.println("selected attribute indices (starting with 0):\n" + Utils.arrayToString(indices));
		for (int i=0; i<indices.length; i++) {
			System.out.println(tmpInstances.attribute(indices[i]).name());
		}
		
	}
	
	public void featureSelectionAndFiltering() throws Exception {
		
		ArrayList<Integer> removeIndices = new ArrayList<Integer>();	
		Attribute timestampAttribute = d_instances.attribute("timestamp");
		Attribute userIDAttribute = d_instances.attribute("user_id");						

		Remove remove = new Remove();
		if (timestampAttribute != null) removeIndices.add(timestampAttribute.index());
		if (userIDAttribute != null) removeIndices.add(userIDAttribute.index());
		remove.setAttributeIndicesArray(convertIntegers(removeIndices));
		remove.setInputFormat(d_instances);
		Instances tmpInstances = Filter.useFilter(d_instances, remove);
		
		AttributeSelection filter = new AttributeSelection();  // package weka.filters.supervised.attribute!		
		// OPTION1: WrapperSubsetEval eval = new WrapperSubsetEval();    
		// OPTION1: eval.setClassifier(new NaiveBayes());   		 
		CfsSubsetEval eval = new CfsSubsetEval();  // OPTION2:  
		GreedyStepwise search = new GreedyStepwise();
		search.setSearchBackwards(true);
		filter.setEvaluator(eval);
		filter.setSearch(search);
		filter.setInputFormat(tmpInstances);
		d_instances = Filter.useFilter(tmpInstances, filter);		
	}
	
}
