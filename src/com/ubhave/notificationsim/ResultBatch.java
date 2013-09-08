package com.ubhave.notificationsim;

public class ResultBatch implements Result {
	
	private int d_runs;
	
	private int d_folds;
	
	private int[][] d_totalNotifMatrix;
	
	private int[][] d_succNotifMatrix;
	
	private double[][] d_accuracyMatrix;
	
	public ResultBatch(int a_runs, int a_folds) {
		d_folds = a_folds;
		d_runs = a_runs;
		d_accuracyMatrix = new double[a_runs][a_folds];
		d_succNotifMatrix = new int[a_runs][a_folds];
		d_totalNotifMatrix = new int[a_runs][a_folds];
	}	
	
	/**
	 * Accuracy is calculated as follows. First, an average value in each run is calcualted.
	 * Then this is averaged over all runs.
	 * 
	 * @return
	 */
	@Override
	public double getMeanAccuracy() {		
		double[] perRunAcc = new double[d_runs];
		double totalAcc = 0;
		for (int i=0; i<d_runs; i++) {
			
			double totalRunAcc = 0;
			for (int j=0; j<d_folds; j++){
				
				totalRunAcc += d_accuracyMatrix[i][j];
			}
			perRunAcc[i] = totalRunAcc/d_folds;
			totalAcc += perRunAcc[i];
		}
		return totalAcc/d_runs;
	}

	@Override
	public double getStdDevAccuracy() {		
		double meanAcc = getMeanAccuracy();		
		double sum = 0;
		for (int i=0; i<d_runs; i++) {
			
			double totalRunAcc = 0;
			for (int j=0; j<d_folds; j++){
				
				totalRunAcc += d_accuracyMatrix[i][j];
			}
			double perRunAcc = totalRunAcc/d_folds;
			
			sum += Math.pow((perRunAcc - meanAcc), 2);			
		}
		return Math.sqrt(sum/d_runs);		
	}
	
	@Override	
	public double getMeanNotif() {
		double perRunNotif = 0;
		double totalNotif = 0;
		for (int i=0; i<d_runs; i++) {
			
			double totalRunNotif = 0;
			for (int j=0; j<d_folds; j++){
				
				totalRunNotif += d_totalNotifMatrix[i][j];
			}
			perRunNotif = totalRunNotif/d_folds;
			totalNotif += perRunNotif;
		}
		return totalNotif/d_runs;
	}
	
	@Override	
	public double getMeanSuccNotif() {
		double perRunNotif = 0;
		double totalNotif = 0;
		for (int i=0; i<d_runs; i++) {
			
			double totalRunNotif = 0;
			for (int j=0; j<d_folds; j++){				
				totalRunNotif += d_succNotifMatrix[i][j];
			}
			perRunNotif = totalRunNotif/d_folds;
			totalNotif += perRunNotif;
		}
		return totalNotif/d_runs;
	}
	
	public void setAccuracy(int a_run, int a_fold, double a_value){
		d_accuracyMatrix[a_run][a_fold] = a_value;
	}
	
	public double getAccuracy(int a_run, int a_fold){
		return d_accuracyMatrix[a_run][a_fold];
	}
	
	public void setTotalNotif(int a_run, int a_fold, int a_value){
		d_totalNotifMatrix[a_run][a_fold] = a_value;
	}
	
	public int getTotalNFold(int a_run, int a_fold){
		return d_totalNotifMatrix[a_run][a_fold];
	}
	
	public void setSuccNotif(int a_run, int a_fold, int a_value){
		d_succNotifMatrix[a_run][a_fold] = a_value;
	}
	
	public int getSuccNotif(int a_run, int a_fold){
		return d_succNotifMatrix[a_run][a_fold];
	}

	@Override
	public int numOfUsers() {		
		return 0;
	}
}
