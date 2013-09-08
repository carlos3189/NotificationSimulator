package com.ubhave.notificationsim;

import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.ubhave.notificationsim.utils.ExtendedParser;
import com.ubhave.notificationsim.utils.SimulatorException;

/**
 * Main tester class for the notification delivery simulator.
 * Command line options allow the selection of relevant context attributes,
 * such as location, accelerometer features, hour of day, weekend indicator.
 * Through a command line option the user can specify the desired target
 * answer time. I.e. anything answered before that time will be considered
 * as a success. Finally, a set of classifiers that we want to evaluate must
 * be provided via classifiers_list command line option.
 *  
 * The output includes the average accuracy, number of tried notifications
 * and the number of successful notifications for each of the classifiers. 
 *  
 * Prerequisite:
 *  - An arff file with information about the context and time to reply
 *  (or a very large int if there is no reply) 
 * 
 * @author Veljko Pejovic (v.pejovic@cs.bham.ac.uk)
 *
 */
public class Main {


	public static void main(String[] args){

		try {
			Options options = new Options();
			options.addOption("timestamp", false, "Seconds since epoch timestamp");
			options.addOption("location", false, "Extract coarse, self-reported location (home, work, public)");
			options.addOption("user_id", false, "Extract user ID number");
			options.addOption("accMean", false, "Extract accelerometer mean value");
			options.addOption("accVar", false, "Extract accelerometer variance value");
			options.addOption("hour_of_day", false, "Extract hour of day");
			options.addOption("weekend", false, "Extract weekend indicator");
			options.addOption("question_type", false, "Extract question type (only in Locshare dataset)");
			
						
			Options otherOptions = new Options();
			Option filename = OptionBuilder.withArgName("file")
								.hasArg()
								.isRequired()
								.withDescription("Data file name")
								.create("filename");
			
			Option answerThold = OptionBuilder.withArgName("thold")
					.hasArg()
					.isRequired()
					.withDescription("Answer threshold in minutes")					
					.create("answer_thold");			
			
			Option classifierList = OptionBuilder.withArgName("classifiers")
					.hasArgs()
					.isRequired()
					.withDescription("List of strings that represent classifiers and their options." +
							" The definitions should be enclosed in double quotes and separated by a single space.")					
					.create("classifier_list");		
			
			Option cutoff = OptionBuilder.withArgName("cutoff")
					.hasArg()
					.withDescription("Cut off in days between group and individual classifier training for online classification.")
					.create("cutoff");	
			
			Option burnin = OptionBuilder.withArgName("burnin")
					.hasArg()
					.withDescription("Burn-in period before online classification starts.")
					.create("burnin");	
			Option acceptanceThold = OptionBuilder.withArgName("acceptance_thold")
					.hasArg()
					.withDescription("Confidence need for a notification to be fired (double [0-1]).")
					.create("acceptance_thold");	
			Option deadline = OptionBuilder.withArgName("deadline")
					.hasArg()
					.withDescription("Deadline in hours for waiting for a notification opportunity (after this this time the notification is fired irrespective of classificaiton")
					.create("deadline");	
			
			otherOptions.addOption("m_time", false, "Features related to time");
			otherOptions.addOption("m_acc", false, "Features related to accelerometer");
			otherOptions.addOption("m_GPS", false, "Features related to GPS");

			otherOptions.addOption("c1", false, "Features related to the context at the time of notification sending");
			otherOptions.addOption("c2", false, "Features related to the context at the time of response");
			otherOptions.addOption("c1_c2", false, "Features related to the context change from notification to response");
			otherOptions.addOption("c1_c1", false, "Features related to the context change from notification to notification");
			otherOptions.addOption("c1_change", false, "Features related to the context change at the time of notification");
			otherOptions.addOption("c2_change", false, "Features related to the context change at the time of reply");

			otherOptions.addOption("o1", false, "Presence: all reactions coded as YES");
			otherOptions.addOption("o2", false, "Sentiment: answer_threshold determines which are labelled as YES");
			otherOptions.addOption("o3", false, "Time to reply: predict time from stimulus to reaction");
			
			
			otherOptions.addOption("online", false, "Online classification.");
			otherOptions.addOption("seconds", false, "Set if timestamps are in seconds, leave it out if they are in milliseconds.");			

			otherOptions.addOption(filename);			
			otherOptions.addOption(answerThold);
			otherOptions.addOption(classifierList);
			otherOptions.addOption(cutoff);
			otherOptions.addOption(burnin);
			otherOptions.addOption(deadline);
			otherOptions.addOption(acceptanceThold);
			CommandLineParser parser = new ExtendedParser(true);
			CommandLine cmd = parser.parse(options, args);
			
			CommandLineParser otherParser = new ExtendedParser(true);
			CommandLine cmdOther = otherParser.parse(otherOptions, args);
			
			ArrayList<String> preserveAttributes = new ArrayList<String>(); 
			boolean filterNonAnswered = false;
			
			for (Option o : cmd.getOptions()) {
				preserveAttributes.add(o.getOpt());
			}
			if (cmdOther.hasOption("c1")){
				preserveAttributes.add("hour_of_day_true");
				preserveAttributes.add("weekend_true");
				preserveAttributes.add("time_into_experiment_true");
				preserveAttributes.add("acc_mean_true");
				preserveAttributes.add("acc_var_true");
				preserveAttributes.add("acc_ZCR_true");
				preserveAttributes.add("coarse_location_true");
			}
			if (cmdOther.hasOption("c2")){
				preserveAttributes.add("hour_of_day_false");
				preserveAttributes.add("weekend_false");
				preserveAttributes.add("time_into_experiment_false");
				preserveAttributes.add("acc_mean_false");
				preserveAttributes.add("acc_var_false");
				preserveAttributes.add("acc_ZCR_false");
				preserveAttributes.add("coarse_location_false");
				preserveAttributes.add("activity");
				preserveAttributes.add("company");
				preserveAttributes.add("emotion_happy");
				preserveAttributes.add("emotion_sad");
				preserveAttributes.add("emotion_angry");
				preserveAttributes.add("emotion_fear");
				preserveAttributes.add("emotion_neutral");
				preserveAttributes.add("engagement_interesting");
				preserveAttributes.add("engagement_challenging");
				preserveAttributes.add("engagement_concentrated");
				preserveAttributes.add("engagement_important");
				preserveAttributes.add("engagement_skilled");
				preserveAttributes.add("engagement_smthgelse");
				filterNonAnswered = true;
			}
			if (cmdOther.hasOption("c1_c2")){
				// TODO: this option should not be used on the datasets after 27.8
				preserveAttributes.add("activity_change");
				preserveAttributes.add("location_change");
				preserveAttributes.add("BT_change");
				preserveAttributes.add("WiFi_change");
				filterNonAnswered = true;
			}
			if (cmdOther.hasOption("c1_c1")){
				preserveAttributes.add("activity_change");
				preserveAttributes.add("location_change");
				preserveAttributes.add("BT_change");
				preserveAttributes.add("WiFi_change");
				filterNonAnswered = false;
			}
			if (cmdOther.hasOption("c1_change")){
				preserveAttributes.add("activity_var_true");
				filterNonAnswered = false;
			}
			if (cmdOther.hasOption("c2_change")){
				preserveAttributes.add("activity_var_false");
				filterNonAnswered = true;
			}
			if (cmdOther.hasOption("m_time")){
				preserveAttributes.add("hour_of_day_true");
				preserveAttributes.add("weekend_true");
				preserveAttributes.add("time_into_experiment_true");
			}
			if (cmdOther.hasOption("m_acc")){
				preserveAttributes.add("acc_mean_true");
				preserveAttributes.add("acc_var_true");
				preserveAttributes.add("acc_ZCR_true");	
				preserveAttributes.add("activity_var_true");
			}
			if (cmdOther.hasOption("m_GPS")){
				preserveAttributes.add("coarse_location_true");
			}
			
			String classAttributeName = "";
			boolean thold = false;
			boolean presence = true;
			if (cmdOther.hasOption("o1")){
				preserveAttributes.add("answer_time");
				classAttributeName = "answer_time";
				thold = true;
				presence = true;
			}
			if (cmdOther.hasOption("o2")){
				preserveAttributes.add("sentiment");
				classAttributeName = "sentiment";
				thold = true;
				presence = false;
			}
			if (cmdOther.hasOption("o3")){
				preserveAttributes.add("answer_time");
				classAttributeName = "answer_time";
				thold = false;
			}
			
			DataPreprocessor preprocessor = new DataPreprocessor();
			
/*			for(Option o:cmdOther.getOptions()){
				System.out.println(o.getOpt()+o.getValues());
			}
*/			
			preprocessor.readData(cmdOther.getOptionValue("filename"), classAttributeName);
					
			if (filterNonAnswered) {
				preprocessor.filterNonAnsweredData();
			}

			preprocessor.filterData(preserveAttributes);
			
			if (thold) {
				preprocessor.classAttributeTholding(Integer.parseInt(cmdOther.getOptionValue("answer_thold")), presence);
			}
			preprocessor.featureSelection();
			preprocessor.featureSelectionAndFiltering();
			// We can save the intermediate dataset to a file for later examination
			preprocessor.saveToArff("/home/staff/pejovicv/Dropbox/MyCode/UBhave/scripts/SampleMeAnalysis/ExtractFromMongoToArff/extracted.arff");
			
			boolean online=false, seconds=false;
			double cutOffTime = 1;
			double burnInTime = 0;
			
			if (cmdOther.hasOption("online")) {
				online = true;				
			}
			if (cmdOther.hasOption("seconds")) {
				seconds = true;			
			}
			if (cmdOther.hasOption("cutoff")) {
				cutOffTime = Double.parseDouble(cmdOther.getOptionValue("cutoff"));
			}
			if (cmdOther.hasOption("burnin")) {
				burnInTime = Double.parseDouble(cmdOther.getOptionValue("burnin"));
			}
			
			Simulator simulator = new Simulator(online, cutOffTime, burnInTime, seconds);
			
			if (cmdOther.hasOption("acceptance_thold")) {
				simulator.setAcceptanceThold(Double.parseDouble(cmdOther.getOptionValue("acceptance_thold")));
			}
			
			if (cmdOther.hasOption("deadline")) {
				simulator.setDeadline(Double.parseDouble(cmdOther.getOptionValue("deadline")));
			}
			
			String[] classifierTypes = cmdOther.getOptionValues("classifier_list");			
			
			simulator.instantiateClassifiers(classifierTypes);
			simulator.setData(preprocessor.getData());
			simulator.setAnswerThold(cmdOther.getOptionValue("answer_thold"));
			simulator.run();
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SimulatorException e) {
			System.err.print("Simulator error: "+e.getErrorCode()+"\n");
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
