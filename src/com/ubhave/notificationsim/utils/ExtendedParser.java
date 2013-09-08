package com.ubhave.notificationsim.utils;

import java.util.ListIterator;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.ParseException;

public class ExtendedParser extends BasicParser {

	private boolean d_ignoreUnrecognizedOptions;
	
	public ExtendedParser(final boolean a_ignoreUnrecognizedOptions){
		d_ignoreUnrecognizedOptions = a_ignoreUnrecognizedOptions;	
	}

	@Override
	protected void processOption(String a_arg0, ListIterator a_arg1) throws ParseException {

		boolean hasOption = getOptions().hasOption(a_arg0);
		
		if (hasOption || !d_ignoreUnrecognizedOptions) {
			super.processOption(a_arg0, a_arg1);
		}
	}
}

