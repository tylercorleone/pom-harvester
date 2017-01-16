package com.profilemycamera.pomHarvester.utils;

public class Logger {
	
	private static boolean DEBUG = true;
	
	/* INFO */
	public static void info() {
		info("", true);
	}

	public static void info(String message) {
		info(message, true);
	}
	
	public static void info(String message, boolean newline) {
		if("<hr/>".equals(message)) message = "------------------------------------------------------------------------";
		System.out.print("[INFO] " + message + (newline ? "\n" : ""));
	}
	
	/* WARNING */
	public static void warning() {
		warning("", true);
	}
	
	public static void warning(String message) {
		warning(message, true);
	}
	
	public static void warning(String message, boolean newline) {
		if("<hr/>".equals(message)) message = "---------------------------------------------------------------------";
		System.out.print("[WARNING] " + message + (newline ? "\n" : ""));
	}
	
	/* ERROR */
	public static void error(String message) {
		error(message, true);
	}
	
	public static void error(String message, boolean newline) {
		if("<hr/>".equals(message)) message = "-----------------------------------------------------------------------";
		System.out.print("[ERROR] " + message + (newline ? "\n" : ""));
	}
	
	/* DEBUG */
	public static void debug() {
		debug("", true);
	}
	
	public static void debug(String message) {
		debug(message, true);
	}
	
	public static void debug(String message, boolean newline) {
		if(!Logger.DEBUG ) return;
		if("<hr/>".equals(message)) message = "-----------------------------------------------------------------------";
		System.out.print("[DEBUG] " + message + (newline ? "\n" : ""));
	}

}
