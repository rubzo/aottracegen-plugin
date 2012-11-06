package eu.whrl.aottracegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/*
 * Config represents the trace selection config we've passed in as an argument to the plugin.
 */
public class Config {
	private boolean validConfigFileLoaded = false;
	
	public String app = "";
	public String clazz = "";
	public String method = "";
	public String signature = "";
	public int numTraces = 0;
	public List<Integer> traceEntries = new LinkedList<Integer>();
	
	public boolean doMerging = false;
	public boolean traceAll = false;
	public boolean sortTraces = false;
	public boolean produceUnsafe = false;
	public boolean onlyPrintTraces = false;
	public boolean emitDebugFunction = false;
	public boolean emitProfiling = false; // not yet implemented
	
	public void addEntry(int e) {
		numTraces++;
		traceEntries.add(e);
	}
	
	public void loadConfigFile(String filename) {
		File file = new File(filename);
		FileReader reader = null;
		try {
			reader = new FileReader(file);
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't find config file");
			return;
		}
		
		BufferedReader buff = new BufferedReader(reader);
		int lineCounter = 0;
		try {
			while (buff.ready()) {
				String line = buff.readLine();
				if (line.startsWith("#")) {
					continue;
			    } else if (line.startsWith("app")) {
					app = line.substring(4, line.length());
				} else if (line.startsWith("class")) {
					clazz = line.substring(6, line.length());
				} else if (line.startsWith("method")) {
					method = line.substring(7, line.length());
				} else if (line.startsWith("signature")) {
					signature = line.substring(10, line.length());
				} else if (line.startsWith("merge")) {
					doMerging = true;
				} else if (line.startsWith("sort")) {
					sortTraces = true;
				} else if (line.startsWith("unsafe")) {
					produceUnsafe = true;
				} else if (line.startsWith("print")) {
					onlyPrintTraces = true;
				} else if (line.startsWith("debugfunc")) {
					emitDebugFunction = true;
				} else if (line.startsWith("trace all")) {
					traceAll = true;
				} else if (line.startsWith("trace")) {
					addEntry(Integer.parseInt(line.substring(8, line.length()), 16));
				} else {
					System.err.println("Couldn't make sense of line " + lineCounter + ": " + line);
				}
				lineCounter++;
			}
		} catch (IOException e) {
			System.err.println("Couldn't read config file");
		}
		
		if (app != "" && clazz != "" && method != "" && signature != "" && (numTraces > 0 || traceAll)) {
			validConfigFileLoaded = true;
		}
	}
	
	/*
	 * Checks if the config file we tried to load in loadConfigFile() is valid.
	 */
	public boolean isConfigFileValid() {
		return validConfigFileLoaded;
	}
	
	/*
	 * Prints the loaded config, if it's valid.
	 */
	public void printConfig() {
		if (isConfigFileValid()) {
			System.out.println("Printing Config...");
			System.out.println("  App: " + app);
			System.out.println("  Class: " + clazz);
			System.out.println("  Method: " + method);
			System.out.println("  Signature: " + signature);
			if (!traceAll) {
				System.out.println("  Number of traces: " + numTraces);
			} else {
				System.out.println("  Number of traces: all");
			}
			System.out.println();
		} else {
			System.out.println("Cannot print config, as it's invalid!");
		}
	}
}
