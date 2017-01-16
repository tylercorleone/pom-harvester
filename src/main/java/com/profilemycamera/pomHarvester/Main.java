package com.profilemycamera.pomHarvester;

import java.io.IOException;
import java.util.HashMap;

import com.profilemycamera.pomHarvester.PomHarvester;
import com.profilemycamera.pomHarvester.graph.ArtifactsGraph;
import com.profilemycamera.pomHarvester.utils.Logger;

public class Main {
	private static HashMap<String, String> paramsMap = new HashMap<String, String>();
	private static String inDirName, outFileName;

	public static void main(String[] args) throws IOException {
		parseArgs(args);
		PomHarvester.configure(paramsMap.get("gav-filter"), paramsMap.get("ignore-versions") != null,
				paramsMap.get("keep-snapshots") != null, paramsMap.get("wrap-ears") != null,
				paramsMap.get("exclude-filter"), paramsMap.get("html") != null);
		ArtifactsGraph theGraph;
		try {
			theGraph = PomHarvester.analyze(inDirName);
			PomHarvester.render(theGraph, outFileName);
		} catch (PomHarvesterException e) {
			Logger.error("an error occurred:\n\t" + e.getMessage());
			System.exit(1);
		}
	}

	private static void parseArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			try {
				parseArg(args[i]); // reads the parameter and validate
										// the potential value
			} catch (PomHarvesterException e) {
				printUsage(e.getMessage());
				System.exit(1);
			}
		}
		
		if (inDirName == null || outFileName == null) {
			printUsage();
			System.exit(1);
		}
	}

	private static void parseArg(String arg) throws PomHarvesterException {
		// arg should be something like "--filter=org.apache.*"
		if (arg.matches("-h") || arg.matches("--help")) {
			printUsage();
			System.exit(0);
		}
		if (!arg.matches("--.*")) {
			if (inDirName == null) {
				inDirName = arg;
			} else if (outFileName == null ) {
				outFileName = arg;
			} else {
				throw new PomHarvesterException("too many arguments");
			}
		} else {
			// arg is an option
			int equalIndex = arg.indexOf("=");
			String optionName = arg.substring(2, equalIndex != -1 ? equalIndex : arg.length());
			String optionValue = equalIndex != -1 ? arg.substring(equalIndex + 1, arg.length()) : null;
			validateOption(optionName, optionValue); // throws an exception if an
			// illegal value or an unknown option is found
			if (optionValue == null)
				optionValue = "set";
			paramsMap.put(optionName, optionValue);
		}
	}

	private static void validateOption(String name, String value) throws PomHarvesterException {
		String error = "";
		if (name.equals("gav-filter")) { // --gav-filter=REGEX
			if (value == null || value.isEmpty())
				error = "empty gav-filter";
		} else if (name.equals("ignore-versions")) { // --ignore-versions
			if (value != null)
				error = "use --ignore-versions to ignore versions, don't otherwise";
		} else if (name.equals("keep-snapshots")) { // --keep-snapshots
			if (value != null)
				error = "use --keep-snapshots to keep snapshots, don't otherwise";
		} else if (name.equals("wrap-ears")) { // --wrap-ears
			if (value != null)
				error = "use --wrap-ears to wrap ears, don't otherwise";
		} else if (name.equals("exclude-filter")) { // --exclude-filter=REGEX
			if (value.isEmpty())
				error = "empty exclude-filter";
		} else if (name.equals("html")) { // --html
			if (value != null)
				error = "use --html to produce html output, don't otherwise";
		} else {
			error = "unknown option --" + name;
		}

		if (!error.isEmpty())
			throw new PomHarvesterException(error);
	}

	private static void printUsage() {
		printUsage("");
	}

	private static void printUsage(String prepend) {
		String jarName = new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())
				.getName();
		String tab = "  ";
		if (prepend != null && !prepend.isEmpty())
			System.out.println(prepend);
		System.out.println("usage: java -jar " + jarName + " [OPTION]... DIRECTORY OUTPUT_FILE_NAME");
		System.out.println(
				"Renders the dependencies graph of Maven pom files located inside DIRECTORY into OUTPUT_FILE_NAME.ext");
		System.out.println();
		System.out.println("Options:");
		System.out.println(tab + "-h, --help               display this help and exit");

		System.out.println(
				tab + "--gav-filter=REGEX       only artifacts and dependencies whose \"groupId:artifactId:version\"\n"
						+ tab + "                         matches the REGEX will be on the graph");

		System.out.println(
				tab + "--ignore-versions        checking for artifacts equality, versions differences will be ignored");

		// System.out.println(tab + "--allow-missing-versions checking for
		// artifacts equality, versions differences will be ignored");

		System.out.println(tab
				+ "--keep-snapshots         do not substitute snapshot dependencies with stable versione, if available");

		System.out.println(tab
				+ "--wrap-ears              dependencies of ear's pom will be wrapped inside the ear's graph-node\n"
				+ tab + "                         references to those deps will be redirected to the wrapping ear");

		System.out.println(tab
				+ "--exclude-filter=REGEX   exclude artifacts and dependencies whose \"groupId:artifactId:packaging:classifier:version\"\n"
				+ tab + "                         matches the REGEX.\n" + tab
				+ "                         For example to exclude artifacts of type \"pom\" you can use the REGEX \".*:.*:pom:.*:.*\"");
		System.out.println(
				tab + "--html                   write an html table of dependencies instead of a graph diagram");
	}
}
