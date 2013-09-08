package com.ubhave.notificationsim.utils;

public class SimulatorException extends Exception {

	private static final long serialVersionUID = 1L;

	public static final int TIMESTAMP_ATTRIBUTE_MISSING = 1;

	private final int errorCode;
	
	public SimulatorException(int code){
		errorCode = code;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
}
