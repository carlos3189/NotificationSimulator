package com.ubhave.notificationsim;

public interface Result {

	public double getStdDevAccuracy();
	
	public double getMeanAccuracy();
	
	public double getMeanNotif();
	
	public double getMeanSuccNotif();
	
	public int numOfUsers();
}
