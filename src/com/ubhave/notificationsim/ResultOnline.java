package com.ubhave.notificationsim;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class helps with tracking results of online learning.
 * @author Veljko Pejovic
 *
 */
public class ResultOnline implements Result{

	private HashMap<String, ResultElement> d_resultMap; // per user results
	
	private int d_commonTotalNotif;
	
	private int d_commonSuccNotif;
	
	private class ResultElement {
		
		int d_totalNotif;
		int d_succNotif;
		
		public ResultElement(int a_succNotif, int a_totalNotif) {
			d_totalNotif = a_totalNotif;
			d_succNotif = a_succNotif;			
		}		
		public double getAccuracy() {
			if (d_totalNotif > 0) {
				return ((double) d_succNotif)/d_totalNotif;
			}
			else {
				return 0;
			}
		}
		public int getTotalNotif() {
			return d_totalNotif;
		}
		public int getSuccNotif() {
			return d_succNotif;
		}
		public void incTotalNotif() {
			d_totalNotif++;			
		}
		public void incSuccNotif() {
			d_succNotif++;
		}
	}
	
	public ResultOnline(){
		d_resultMap = new HashMap<String, ResultOnline.ResultElement>();
	}
		
	public void incTotalNotif() {
		d_commonTotalNotif++;
	}

	public void incSuccNotif() {
		d_commonSuccNotif++;
	}
	public void addUser(String a_user){
		if (!d_resultMap.containsKey(a_user)){
			d_resultMap.put(a_user, new ResultElement(d_commonSuccNotif, d_commonTotalNotif));
		}		
	}
	
	public void incTotalNotif(String a_user) {
		if (!d_resultMap.containsKey(a_user)){
			d_resultMap.put(a_user, new ResultElement(d_commonSuccNotif, d_commonTotalNotif));
		}
		ResultElement elem = d_resultMap.get(a_user);		
		elem.incTotalNotif();
	}

	public void incSuccNotif(String a_user) {
		if (!d_resultMap.containsKey(a_user)){
			d_resultMap.put(a_user, new ResultElement(d_commonSuccNotif, d_commonTotalNotif));
		}
		ResultElement elem = d_resultMap.get(a_user);		
		elem.incSuccNotif();
	}
	
	@Override
	public double getMeanNotif() {
		
		if (d_resultMap.isEmpty()) {
			return ((double) d_commonTotalNotif);
		} else {		
			double meanNotif = 0;
			int numElems = 0;
			Iterator iter = d_resultMap.entrySet().iterator();		
			while (iter.hasNext()) {
				Map.Entry pair = (Map.Entry) iter.next();
				ResultElement elem = (ResultElement) pair.getValue();
				meanNotif += elem.getTotalNotif();
				numElems++;
			}
			return meanNotif/numElems;
		}
	}
	
	@Override
	public double getMeanSuccNotif() {
		
		if (d_resultMap.isEmpty()) {
			return ((double) d_commonTotalNotif);
		} else {		
			double succNotif = 0;
			int numElems = 0;
			Iterator iter = d_resultMap.entrySet().iterator();		
			while (iter.hasNext()) {
				Map.Entry pair = (Map.Entry) iter.next();
				ResultElement elem = (ResultElement) pair.getValue();
				succNotif += elem.getSuccNotif();
				numElems++;
			}
			return succNotif/numElems;
		}
	}
	
	@Override
	public double getMeanAccuracy() {
		
		if (d_resultMap.isEmpty()) {
			if (d_commonTotalNotif > 0) {
				return ((double) d_commonSuccNotif)/d_commonTotalNotif;
			}
			else {
				return 0;
			}
		} else {		
			double meanAcc = 0;
			int numElems = 0;
			Iterator iter = d_resultMap.entrySet().iterator();		
			while (iter.hasNext()) {
				Map.Entry pair = (Map.Entry) iter.next();
				ResultElement elem = (ResultElement) pair.getValue();
				meanAcc += elem.getAccuracy();
				numElems++;
			}
			return meanAcc/numElems;
		}
	}
	
	@Override
	public double getStdDevAccuracy() {
		
		if (d_resultMap.isEmpty()) {
			return 0;
		} else {
			double meanAcc = getMeanAccuracy();
			double sumSqrAcc = 0;
			int numElems = 0;
			Iterator iter = d_resultMap.entrySet().iterator();		
			while (iter.hasNext()) {
				Map.Entry pair = (Map.Entry) iter.next();
				ResultElement elem = (ResultElement) pair.getValue();
				sumSqrAcc += Math.pow((elem.getAccuracy() - meanAcc), 2);
				numElems++;
			}
			return Math.sqrt(sumSqrAcc/numElems);
		}
	}

	@Override
	public int numOfUsers() {
		return d_resultMap.size();
	}

	
}
