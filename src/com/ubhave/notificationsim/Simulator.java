package com.ubhave.notificationsim;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.ubhave.notificationsim.utils.SimulatorException;

import moa.streams.ArffFileStream;
import moa.streams.CachedInstancesStream;

import weka.classifiers.AbstractClassifier;
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

public class Simulator {

	private Instances d_instances;
	
	// Classifier name => actual classifier
	private HashMap<String, Object> d_classifierMap; //for each user
	
	private HashMap<String, HashMap<String, Object>> d_userMap; //for all users 
	
	private HashMap<String, Result> d_classifierResults; // for each classifier
	
	private HashMap<String, HashMap<String, Long>> d_lastSent; //for each user

	private long d_deadline; //deadline for sending a msg (in long) 
	
	private double d_acceptanceThold; //we only fire a notification if we are this much sure it will be successful
	
	private boolean d_isOnline = false;
	
	private boolean d_seconds = false;
	
	private int d_cutTime;
	
	private int d_burnInTime;

	private String d_answerThold;
		
	public HashMap<String, Object> deepCopy(HashMap<String, Object> a_map) {
		HashMap<String, Object> copy = new HashMap<String, Object>();
		Iterator iter = a_map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry pair = (Map.Entry) iter.next();	        	
	        String clsName = (String) pair.getKey();
	        if (d_isOnline){
	        	moa.classifiers.Classifier cls = (moa.classifiers.Classifier) pair.getValue();
	        	moa.classifiers.Classifier clsCopy = cls.copy();
	        	copy.put(clsName, clsCopy);
	        } 
	        else {
	        	Classifier cls = (Classifier) pair.getValue();
	        	Classifier clsCopy;
				try {
					clsCopy = AbstractClassifier.makeCopy(cls);
					copy.put(clsName, clsCopy);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	        	
	        }
		}		
		return copy;
	}
	
	public Simulator (boolean a_online, double a_cutoffTime, double a_burnInTime, boolean a_seconds) {
		d_classifierMap = new HashMap<String, Object>();
		d_classifierResults = new HashMap<String, Result>();			
		d_lastSent = new HashMap<String, HashMap<String,Long>>();
		d_isOnline = a_online;
		d_seconds = a_seconds;
		if (d_seconds) {
			d_cutTime = (int) (a_cutoffTime * 86400);
			d_burnInTime = (int) (a_burnInTime * 86400);
		}
		else  {
			d_cutTime = (int) (a_cutoffTime * 86400000);
			d_burnInTime = (int) (a_burnInTime * 86400000);
		}
	}
		
	public void setDeadline(double a_deadlineHrs) {
		if (d_seconds){
			d_deadline = (long) (a_deadlineHrs*60*60);
		}
		else {
			d_deadline = (long) (a_deadlineHrs*60*60*1000);
		}
	}
	
	public void setAcceptanceThold(double a_acceptanceThold){
		d_acceptanceThold = a_acceptanceThold;
	}
	
	public void instantiateClassifiers(String[] a_classifiers) throws Exception {
				
		for(String classifer : a_classifiers) {
			String[] tmpOptions = Utils.splitOptions(classifer);
			String classname = tmpOptions[0];
			tmpOptions[0]="";
			if (d_isOnline) {
				moa.classifiers.Classifier cls = (moa.classifiers.Classifier) Utils.forName(moa.classifiers.Classifier.class, classname, tmpOptions);
				d_classifierMap.put(classname, cls);//.copy());
				d_lastSent.put(classname, new HashMap<String, Long>());
			} else {
				Classifier cls = (Classifier) Utils.forName(Classifier.class, classname, tmpOptions);
				d_classifierMap.put(classname, cls);//Classifier.makeCopy(cls));				
			}
		}
	}
	
	public void setData(Instances a_instances) throws SimulatorException{
		d_instances = new Instances (a_instances);	
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
				
			PrintWriter writer = new PrintWriter("/home/staff/pejovicv/Desktop/H1PresenceOnlineSampleMe_1day.txt", "UTF-8");
			
			Attribute userIDAttribute = d_instances.attribute("user_id");						
			CachedInstancesStream streamAll = new CachedInstancesStream(d_instances);
			Iterator iter = d_classifierMap.entrySet().iterator();
		    while (iter.hasNext()) {
		    	Map.Entry pair = (Map.Entry) iter.next();	        	
	        	String clsName = (String) pair.getKey();
	        	moa.classifiers.Classifier cls = (moa.classifiers.Classifier) pair.getValue();
	        	cls.setModelContext(streamAll.getHeader());
	        	cls.prepareForUse();
		    }

		    Attribute timestampAttribute = d_instances.attribute("timestamp");
		    	    
		    long startTime = -1;		    

		    boolean burnInPeriod = true;
		    
		    boolean trainFromAll = true;
		    
		    while (streamAll.hasMoreInstances() && burnInPeriod && (d_burnInTime > 0)) {
		    	
		    	Instance currentInstance = streamAll.nextInstance();		    	
		    	long timestamp = (long)currentInstance.value(timestampAttribute);
		    	
		    	if (startTime == -1){
                    startTime = timestamp;
                }
		    	
		    	if (timestamp >= startTime + d_burnInTime) {
		    		burnInPeriod = false;
		    	}
		    	System.out.println("Skipped");
		    }
		    
		    
		    while (streamAll.hasMoreInstances() && trainFromAll) {
		    	
		    	Instance currentInstance = streamAll.nextInstance();		    	
		    	long timestamp = (long)currentInstance.value(timestampAttribute);
		    	
		    	currentInstance.setMissing(userIDAttribute);
		    	currentInstance.setMissing(timestampAttribute);
		    	
		    	if (startTime == -1){
                    startTime = timestamp;
                }
		    	
		    	if (timestamp >= startTime + d_cutTime) {
                    trainFromAll = false;
                    System.out.println("Start time "+startTime+" cut off time "+d_cutTime+" time "+timestamp);
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
			        	
			        	// During the training period the simulator uses every opportunity to
			        	// train a classifier
			        	double outcome = 1;
			        	/*
			        	if (clsName.contains("MajorityClass")){
		        			
		        			double dist[] = cls.getVotesForInstance(currentInstance);
			    			Random flip = new Random();
			    			double probNo;
			    			if (dist == null || dist.length<2) {
			    				probNo = 0;
			    			} else {
			    				probNo = dist[0]/(dist[0]+dist[1]); 
			    			}
			    		
			    			double draw = flip.nextDouble();
			    			if (draw > probNo) {
			    				outcome = 1;
			    			}
			    			else {
			    				outcome = 0;
			    			}
		        		}
			        	else {			        		
			        		double dist[] = cls.getVotesForInstance(currentInstance);
			        		if (dist == null || dist.length<2) {
			        			outcome = 1;
			        		}
			        		else {
				        		if (Utils.maxIndex(cls.getVotesForInstance(currentInstance)) == 1){
				        			outcome = 1;
				        		}
			        		}
			        	}*/
			        	if (outcome == 1) {
			        		 result.incTotalNotif();			        		
			        		if (actual == 1){
			        			result.incSuccNotif();
			        		}
				        	cls.trainOnInstance(currentInstance);	
				        	//if (result.getMeanNotif() > )
				        	writer.println(timestamp+"\t"+clsName+"\t"+result.getMeanAccuracy()+"\t"+
				    				result.getStdDevAccuracy()+"\t"+actual);
			        	}
				    }			    
		    	}
		    } // end common training
			
		    d_userMap = new HashMap<String, HashMap<String,Object>>();		    		   
		    
		    Iterator resultsIter = d_classifierResults.entrySet().iterator();
		    while(resultsIter.hasNext()){
		    	Enumeration<String> uidIter = userIDAttribute.enumerateValues();			    
		    	Map.Entry pair = (Map.Entry) resultsIter.next();
		    	String classifierName = (String) pair.getKey();
		    	ResultOnline res = (ResultOnline) pair.getValue();
		    	while (uidIter.hasMoreElements()){
		    		String userID = uidIter.nextElement();
		    		res.addUser(userID);
		    		d_lastSent.get(classifierName).put(userID, (long) 0);
		    	}
		    }
		    	    
		    while (streamAll.hasMoreInstances()){ // for each instance
		    	
		    	Instance currentInstance = streamAll.nextInstance();
		    	String userID = userIDAttribute.value((int)currentInstance.value(userIDAttribute));
		    	double actual = currentInstance.classValue();
		    	long timestamp = (long)currentInstance.value(timestampAttribute);
		    	currentInstance.setMissing(userIDAttribute);
		    	
		    	if (!d_userMap.containsKey(userID)) {		    		
		    		d_userMap.put(userID, deepCopy(d_classifierMap));	        	
		    	}		    	
		    	HashMap<String, Object> userClassifiers = d_userMap.get(userID);
		    	
		    	Iterator classifierIterator = userClassifiers.entrySet().iterator();
		    	
		    	while (classifierIterator.hasNext()) { // for each classifier		    		
		    		
		    		Map.Entry pair = (Map.Entry)classifierIterator.next();
		    		moa.classifiers.Classifier cls = (moa.classifiers.Classifier) pair.getValue();
		    		String clsName = (String) pair.getKey();
		    		
		        	if (!d_classifierResults.containsKey(clsName)){
		        		d_classifierResults.put(clsName, new ResultOnline());		        		
		        	}
		        	
		    		ResultOnline result = (ResultOnline) d_classifierResults.get(clsName);
		    		
		    		double outcome = 0;
		        	if (clsName.contains("MajorityClass")){
	        			
	        			double dist[] = cls.getVotesForInstance(currentInstance);
		    			Random flip = new Random();
		    			double probNo;
		    			if (dist == null || dist.length<2) {
		    				probNo = 0;
		    			} else {
		    				probNo = dist[0]/(dist[0]+dist[1]); 
		    			}
		    			double draw = flip.nextDouble();
		    			if (draw > probNo) {
		    				outcome = 1;
		    			}
		    			else {
		    				outcome = 0;
		    			}		    
	        		}
		        	else {
		        		
		        		if (d_acceptanceThold > 0) {
		        			double dist[] = cls.getVotesForInstance(currentInstance);
			        		double sum = 0;
			        		for (double elem : dist){
			        			sum += elem;
			        		}
			        		for (int i=0; i<dist.length; i++){
			        			dist[i] = dist[i]/sum;
			        		}
			        		//System.out.println("Vote distribution "+Arrays.toString(dist)+clsName);
			        		if (dist[1] > d_acceptanceThold) {
			        			outcome = 1;
			        		}
		        		}
		        		else {
		        			if (Utils.maxIndex(cls.getVotesForInstance(currentInstance)) == 1){
		        				outcome = 1;
		        			}
		        		}		        		
		        	}
		        	if (d_deadline > 0) {
	        			if (timestamp - d_lastSent.get(clsName).get(userID) > d_deadline){
	        				outcome = 1;
	        			}
	        		}
		        	if (outcome == 1) {
		        		long lastSent = d_lastSent.get(clsName).get(userID);
		        		if (lastSent == 0) {
		        			lastSent = 0;
		        		}
		        		
		        		d_lastSent.get(clsName).put(userID, timestamp);

		        		long diffSent = timestamp - lastSent;
		        		
		        		result.incTotalNotif(userID);			        		
				    	cls.trainOnInstance(currentInstance);	
		        		if (actual == 1){
		        			result.incSuccNotif(userID);
		        		}

				    	writer.println(timestamp+"\t"+clsName+"\t"+result.getMeanAccuracy()+"\t"+
			    				result.getStdDevAccuracy()+"\t"+actual);
		        	}

		    	}
		    	
		    }
		    writer.close();
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
			        	Classifier clsCopy = AbstractClassifier.makeCopy(cls);	        	
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
			        			/*double dist[] = clsCopy.distributionForInstance(currentInstance);
			        			System.out.println("Vote distribution "+Arrays.toString(dist)+clsName);
			        			Random flip = new Random();
				    			double draw = flip.nextDouble();
				    			if (draw > dist[0]) {
				    				result = 1;
				    			}
				    			else {
				    				result = 0;
				    			}*/	
			        			
			        			result = clsCopy.classifyInstance(currentInstance);
			        		}
			        		
			        		if (result == 1) {
			        			numTotal++;
			        			if (result == actual){
			        				numCorrect++;
			        			}
			        		
			        		}
			        		
			        		//System.out.println("Predicted "+result+" actual "+actual);
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
	        Result res = (Result) pair.getValue();	        
	        /*System.out.println("\nClassifier "+clsName);
	        System.out.println("\t number of users "+res.numOfUsers());
	        System.out.println("\t mean accuracy "+res.getMeanAccuracy());
	        System.out.println("\t accuracy variance "+res.getStdDevAccuracy());
	        System.out.println("\t average successful (total) notifications: "+res.getMeanSuccNotif()+" ("+res.getMeanNotif()+")");
	        System.out.println("\t total instances "+d_instances.size());*/
	        System.out.println(clsName + "\t" + d_answerThold + "\t"+ res.getMeanAccuracy() +
	        		"\t"+res.getStdDevAccuracy()+"\t"+res.getMeanSuccNotif()+"\t"+res.getMeanNotif());
		}
	}

	public void setAnswerThold(String a_answerThold) {
		d_answerThold = a_answerThold;
		
	}
}
