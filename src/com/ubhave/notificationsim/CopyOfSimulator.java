package com.ubhave.notificationsim;

import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.ubhave.notificationsim.utils.SimulatorException;

import moa.streams.ArffFileStream;
import moa.streams.CachedInstancesStream;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;

public class CopyOfSimulator {

	private Instances d_instances;
	
	private Hashtable<String, Object> d_classifierMap;
	
	private Hashtable<String, Result> d_classifierResults; // for each classifier
	
	private boolean d_isOnline = false;
	
	private int d_cutTime;
		
	public CopyOfSimulator (boolean a_online, double a_cutoffTime, boolean a_seconds) {
		d_classifierMap = new Hashtable<String, Object>();
		d_classifierResults = new Hashtable<String, Result>();			
		d_isOnline = a_online;
		if (a_seconds) {
			d_cutTime = (int) (a_cutoffTime * 86400);
		}
		else  {
			d_cutTime = (int) (a_cutoffTime * 86400000);
		}
	}
		
	public void instantiateClassifiers(String[] a_classifiers) throws Exception {
				
		for(String classifer : a_classifiers) {
			String[] tmpOptions = Utils.splitOptions(classifer);
			String classname = tmpOptions[0];
			tmpOptions[0]="";
			if (d_isOnline) {
				moa.classifiers.Classifier cls = (moa.classifiers.Classifier) Utils.forName(moa.classifiers.Classifier.class, classname, tmpOptions);
				d_classifierMap.put(classname, cls.copy());
			} else {
				Classifier cls = (Classifier) Utils.forName(Classifier.class, classname, tmpOptions);
				d_classifierMap.put(classname, cls);//Classifier.makeCopy(cls));				
			}
		}
	}
	
	public void setData(Instances a_instances) throws SimulatorException{
		d_instances = a_instances;	
		if (d_isOnline) {
			if (d_instances.attribute("timestamp") != null) {
				d_instances.sort(d_instances.attribute("timestamp"));
			}
			else {
				throw new SimulatorException(SimulatorException.TIMESTAMP_ATTRIBUTE_MISSING);
			}
		}
	}	
	
	public void run() throws Exception {
		
		if (d_isOnline) {
		
			Attribute userIDAttribute = d_instances.attribute("user_id");
			int removeIndices[] = {userIDAttribute.index()};
			Remove remove = new Remove();
			remove.setAttributeIndicesArray(removeIndices);
			remove.setInputFormat(d_instances);
			Instances allData = Filter.useFilter(d_instances, remove);
			
			CachedInstancesStream streamAll = new CachedInstancesStream(allData);
			Iterator iter = d_classifierMap.entrySet().iterator();
		    while (iter.hasNext()) {
		    	Map.Entry pair = (Map.Entry) iter.next();	        	
	        	String clsName = (String) pair.getKey();
	        	moa.classifiers.Classifier cls = (moa.classifiers.Classifier) pair.getValue();
	        	cls.setModelContext(streamAll.getHeader());
	        	cls.prepareForUse();
		    }

		    Attribute timestampAttribute = allData.attribute("timestamp");
		    	    
		    long startTime = 0;		    

		    boolean trainFromAll = true;
		    
		    while (streamAll.hasMoreInstances() && trainFromAll) {
		    	
		    	Instance currentInstance = streamAll.nextInstance();	
		    	
		    	if (startTime == 0){
                    startTime = (long)currentInstance.value(timestampAttribute);
                }
		    	
		    	if ((long)currentInstance.value(0) >= startTime + d_cutTime) {
                    trainFromAll = false;
                    System.out.println("Start time "+startTime+" cut off time "+d_cutTime+" time "+(long)currentInstance.value(0));
                    System.out.println("No more common training");
		    	}
		    	
		    	if (trainFromAll) {		    				    				   		    	
		    		
			    	double actual = currentInstance.classValue();
			    	
			    	Iterator iterClassifier = d_classifierMap.entrySet().iterator();		    	
			    	
				    while (iterClassifier.hasNext()) {
				    	Map.Entry pair = (Map.Entry) iterClassifier.next();
				    	String clsName = (String) pair.getKey();
			        	moa.classifiers.Classifier cls = (moa.classifiers.Classifier) pair.getValue();
			        	
			        	if (!d_classifierResults.containsKey(clsName)){
			        		d_classifierResults.put(clsName, new ResultOnline());
			        	}
			        	ResultOnline result = (ResultOnline) d_classifierResults.get(clsName);
			        	
			        	double votes[] = cls.getVotesForInstance(currentInstance);
			        	for (int i=0; i<votes.length; i++) {
			        		System.out.print(i+" votes "+votes[i]+"; ");
			        	}
			        	System.out.println();
			        	System.out.println(currentInstance.classValue());
			        	if (cls.correctlyClassifies(currentInstance) && actual == 1) {
			        		// predicted YES and it was YES			        		
			        		result.incSuccNotif();
			        		result.incTotalNotif();			        		
			        		System.out.println("Classifier "+clsName+" correct");	    
			        	} else if ((!cls.correctlyClassifies(currentInstance)) && actual == 0){
			        		System.out.println("Classifier "+clsName+" incorrect");
			        		result.incTotalNotif();
			        	}
			        	
			        	cls.trainOnInstance(currentInstance);		        	
				    }			    
		    	}
		    } // end common training
					   
		    

			/*while (iteratorX.hasMoreElements()) {
				String location = iteratorX.nextElement();
				System.out.println(location);
				System.out.println(locationAttribute.indexOfValue(location));
			}
			System.out.println("shouldnt change " +d_instances.numAttributes());*/

		    /*
		    ArffSaver saver = new ArffSaver();
		    saver.setInstances(d_instances);
		    saver.setFile(new File("testNew.arff"));
		    saver.writeBatch();*/
		    		 
		    // For each userID
			Enumeration<String> iterator = userIDAttribute.enumerateValues();
			System.out.println(userIDAttribute.name()+" "+userIDAttribute.index()+" "+userIDAttribute.numValues());
			while (iterator.hasMoreElements()) {
				
				String userID = iterator.nextElement();
				//System.out.println("User "+userID+" "+String.valueOf(userIDAttribute.indexOfValue(userID)));
				
				RemoveWithValues removeAllButOne = new RemoveWithValues();
				removeAllButOne.setAttributeIndex(String.valueOf(userIDAttribute.index()+1));	
				removeAllButOne.setNominalIndices(String.valueOf(userIDAttribute.indexOfValue(userID)+1));
				removeAllButOne.setInputFormat(d_instances);

				removeAllButOne.setInvertSelection(true);
				removeAllButOne.setMatchMissingValues(false);
				removeAllButOne.setModifyHeader(false);				
				
				//System.out.println(userIDAttribute.indexOfValue(userIDAttribute.value(Integer.parseInt(userID))));
				//int toRemove[] = {userIDAttribute.indexOfValue(userID)};
				//removeAllButOne.setNominalIndicesArr(toRemove);				
				//System.out.println("filter attribute "+removeAllButOne.getAttributeIndex());
				//System.out.println("filter value "+removeAllButOne.getNominalIndices());				 
				//(String.valueOf(userIDAttribute.indexOfValue(userID)));
			    //System.out.println("Values "+d_instances.attribute(userIDAttribute.index()).numValues());

				Instances singleUserData = Filter.useFilter(d_instances, removeAllButOne);
			
				Attribute userIDAttributeSingle = singleUserData.attribute("user_id");
				int removeIndicesSingle[] = {userIDAttributeSingle.index()};
				Remove removeSingle = new Remove();
				removeSingle.setAttributeIndicesArray(removeIndicesSingle);
				removeSingle.setInputFormat(singleUserData);
				singleUserData = Filter.useFilter(singleUserData, removeSingle);
				
			    Attribute timestampAttributeSingle = singleUserData.attribute("timestamp");
				CachedInstancesStream streamSingle = new CachedInstancesStream(singleUserData);
				Iterator iterSingle = d_classifierMap.entrySet().iterator();
				
				// Create a copy of half-trained classifers for each of the users
				Hashtable<String, Object> singleClassifierMap = new Hashtable<String, Object>();
				while(iterSingle.hasNext()) {
					Map.Entry pair = (Map.Entry) iterSingle.next();	        	
		        	String clsName = (String) pair.getKey();
		        	moa.classifiers.Classifier cls = (moa.classifiers.Classifier) pair.getValue();
		        	moa.classifiers.Classifier clsCopy = cls.copy();
		        	singleClassifierMap.put(clsName, clsCopy);		        	
				}
				
				System.out.println("Copied classifiers");
				// Fast forward single streams to the splitting time point
				Instance currentInstance;
				do {
					currentInstance = streamSingle.nextInstance();
				} while (currentInstance.value(timestampAttributeSingle) < startTime + d_cutTime);
				System.out.println("Rewind");
				
				while (streamSingle.hasMoreInstances()) {

					currentInstance = streamSingle.nextInstance();
					double actual = currentInstance.classValue();	
					
	        		//System.out.println("Iterating instance with value "+actual);	    
	        		
					Iterator iterSingleCopies = singleClassifierMap.entrySet().iterator();
					while (iterSingleCopies.hasNext()) {

				    	Map.Entry pair = (Map.Entry) iterSingleCopies.next();	        	
			        	String clsName = (String) pair.getKey();
			        	moa.classifiers.Classifier cls = (moa.classifiers.Classifier) pair.getValue();
		        	
			        	if (!d_classifierResults.containsKey(clsName)) {
			        		d_classifierResults.put(clsName, new ResultOnline());
			        	}
			        	ResultOnline result = (ResultOnline) d_classifierResults.get(clsName);
			        	
			        	
			        	if (cls.correctlyClassifies(currentInstance) && actual == 1) {
			        		// predicted YES and it was YES
			        		System.out.println("Classifier "+clsName+" correct");	   
			        		result.incSuccNotif(userID);
			        		result.incTotalNotif(userID);	
			        	} else if ((!cls.correctlyClassifies(currentInstance)) && actual == 0){
			        		System.out.println("Classifier "+clsName+" incorrect");
			        		result.incTotalNotif(userID);
			        	}		        		
			        	//System.out.println("Classifier "+clsName+" avg accuracy "+currentResult.getAvgAccuracy()+
				        //		" total succ "+currentResult.getSuccN()+" total tries "+currentResult.getTotalN()+" total opportunities "+totalCnt++);
			        	cls.trainOnInstance(currentInstance);
					} //end of specific classifier per instance
		        	
			    } // end of single stream
			
			} // end of single user
		}
			
		else {
	        // TODO: fixed folding for now
			int folds = 10, runs=10;
			for (int currentRun = 0; currentRun < runs; currentRun++) {
				
				Random rand = new Random(currentRun+1);
				Instances randData = new Instances(d_instances);
		        randData.randomize(rand);
		        
		        if (randData.classAttribute().isNominal()) {
		        	randData.stratify(folds);
		        }
		        
		        for (int currentFold = 0; currentFold < folds; currentFold++) {
		        	
		            Instances train = randData.trainCV(folds, currentFold);
		            Instances test = randData.testCV(folds, currentFold);
		            
			        Iterator iter = d_classifierMap.entrySet().iterator();
			        
			        while (iter.hasNext()) {
			        
			        	Map.Entry pair = (Map.Entry) iter.next();	        	
			        	String clsName = (String) pair.getKey();
			        	Classifier cls = (Classifier) pair.getValue();	        	
			        	Classifier clsCopy = cls;// TODO: make a copy here
			        	clsCopy.buildClassifier(train);
			        	
			        	int numCorrect = 0;
			        	int numTotal = 0;
			        	double avgAccuracy = 0;
			        	
			        	for (int i=0; i<test.numInstances(); i++) {
			        		Instance currentInstance = test.instance(i);
			        		double result;
			        		double actual = currentInstance.classValue();        		
		
			        		if (clsName.contains("ZeroR")){
			        			
			        			double dist[] = clsCopy.distributionForInstance(currentInstance);
				    			Random flip = new Random();
				    			double draw = flip.nextDouble();
				    			if (draw > dist[0]) {
				    				result = 1;
				    			}
				    			else {
				    				result = 0;
				    			}	
			        		}
			        		else {
			        			result = clsCopy.classifyInstance(currentInstance);
			        		}
			        		
			        		if (result == 1) {
			        			numTotal++;
			        			if (result == actual){
			        				numCorrect++;
			        			}
			        			//System.out.println("Predicted "+result+" actual "+actual);
			        		}
			        	}
			        	
			        	if (numTotal>0){
			        		avgAccuracy = numCorrect/(double)numTotal;
			        	}
			        	else {
			        		avgAccuracy = 0;
			        	}
			        	//System.out.println("Total accurracy " + avgAccuracy);
			        	
			        	if (!d_classifierResults.containsKey(clsName)) {
			        		d_classifierResults.put(clsName, new ResultBatch(runs, folds));
			        	}
			        	ResultBatch currentResult = (ResultBatch) d_classifierResults.get(clsName);
			        	currentResult.setAccuracy(currentRun, currentFold, avgAccuracy);
			        	currentResult.setSuccNotif(currentRun, currentFold, numCorrect);
			        	currentResult.setTotalNotif(currentRun, currentFold, numTotal);
			        	//d_classifierResults.put(clsName, currentResult);
			        	//Evaluation eval = new Evaluation(randData);
			        	//eval.evaluateModel(cls, test);
			        	//System.out.println(eval.toSummaryString());
			        }
		        }	    	
			}
		}
        this.printResults();
        
	}
	
	public void printResults(){
		Iterator iter = d_classifierResults.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry pair = (Map.Entry) iter.next();	        	
	        String clsName = (String) pair.getKey();
	        ResultBatch res = (ResultBatch) pair.getValue();	        
	        System.out.println("Classifier "+clsName+" avg accuracy "+res.getMeanAccuracy()+
	        		" ["+res.getStdDevAccuracy()+"]"+
	        		" total succ "+res.getMeanSuccNotif()+" total tries "+res.getMeanNotif());	        
		}
	}
}
