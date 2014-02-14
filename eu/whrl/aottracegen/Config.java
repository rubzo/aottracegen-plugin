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
	
	public List<Region> regions = new LinkedList<Region>();
	public Region currentRegion = null;
	
	public boolean emitDebugFunctions = false;
	public boolean emitEHCounter = false;
	
	public boolean announceMode = false;
	public boolean blockingMode = false;
	public boolean printVregsMode = false;
	public int printVregsInvokeLimit = 0;
	
	public boolean trailMode = false;
	
	public boolean breakFlagCheckMode = false;
	
	public boolean vultureMode = false;
	
	public boolean forceEarlyExit = false;
	public int forceEarlyExitCodeAddress = 0;
	public int forceEarlyExitTripCount = 1;
	
	public boolean singleStepAll = false;
	
	public boolean armMode = false;
	
	public boolean emulateJitMode = false;
	
	public List<String> extraLibs = new LinkedList<String>();
	
	public String cflags = "-O3";
	
	public boolean enableRemoveCBZs = true;
	
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
				if (line.startsWith("#") || line.isEmpty()) {
					continue;
			    } else if (line.startsWith("app")) {
					app = line.substring(4, line.length());
					
			    } else if (line.startsWith("region")) {
			    	currentRegion = new Region();
			    	currentRegion.id = regions.size();
			    	regions.add(currentRegion);
				} else if (line.startsWith("method")) {
					boolean endsWithColon = false;
					String fullMethodName = line.substring(7, line.length());
					if (fullMethodName.endsWith(";")) {
						endsWithColon = true;
					}
					String[] splits = fullMethodName.split(";");
					currentRegion.clazz = splits[0];
					currentRegion.method = splits[1];
					currentRegion.signature = splits[2];
					if (splits.length > 3) {
						for (int i = 3; i < splits.length; i++) {
							currentRegion.signature += (";" + splits[i]);
						}
					}
					if (endsWithColon) {
						currentRegion.signature += ";";
					}
					if (singleStepAll) {
						currentRegion.singleStepOnly = true;
					}
				} else if (line.startsWith("entry")) {
					int entryOffset = Integer.parseInt(line.substring(8, line.length()), 16);
					currentRegion.entryOffset = entryOffset;
				} else if (line.startsWith("merge")) {
					int mergeOffset = Integer.parseInt(line.substring(8, line.length()), 16);
					currentRegion.merges.add(mergeOffset);
				} else if (line.startsWith("entire")) {
					currentRegion.entireMethod = true;
				} else if (line.startsWith("disablefp")) {
					currentRegion.disableFP = true;
				} else if (line.startsWith("end_region")) {
					currentRegion.completed = true;
					currentRegion = null;
				} else if (line.startsWith("debugfuncs")) {
					emitDebugFunctions = true;
				} else if (line.startsWith("thissinglestep")) {
					currentRegion.singleStepOnly = true;
				} else if (line.startsWith("alternative")) {
					currentRegion.useAlternative = true;
				} else if (line.startsWith("ehcounter")) {
					emitEHCounter = true;
					// Emitting the counter at every exception handler exit point requires the debug function to be emitted.
					emitDebugFunctions = true;
				} else if (line.startsWith("announce")) {
					announceMode = true;
				} else if (line.startsWith("blocking")) {
					blockingMode = true;
				} else if (line.startsWith("trail")) {
					trailMode = true;
				} else if (line.startsWith("vulture")) {
					vultureMode = true;
				} else if (line.startsWith("breakflag")) {
					breakFlagCheckMode = true;
				} else if (line.startsWith("printvregs")) {
					printVregsInvokeLimit = Integer.parseInt(line.substring(11, line.length()));
					printVregsMode = true;
				} else if (line.startsWith("earlyexittripcount")) {
					forceEarlyExitTripCount = Integer.parseInt(line.substring(19, line.length()));
				} else if (line.startsWith("earlyexit")) {
					forceEarlyExitCodeAddress = Integer.parseInt(line.substring(12, line.length()), 16);
					forceEarlyExit = true;
				
				} else if (line.startsWith("allsinglestep")) {
					singleStepAll = true;
				} else if (line.startsWith("arm")) {
					armMode = true;
				} else if (line.startsWith("emulatejit")) {
					emulateJitMode = true;
				} else if (line.startsWith("no-remove-cbz")) {
					enableRemoveCBZs = false;
				} else if (line.startsWith("cflags")) {
					cflags = line.substring(6, line.length());
				} else if (line.startsWith("libs")) {
					extraLibs.add(line.substring(5, line.length()));
				} else {
					System.err.println("Couldn't make sense of line " + lineCounter + ": " + line);
				}
				lineCounter++;
			}
		} catch (IOException e) {
			System.err.println("Couldn't read config file");
		}
		
		if (regions.size() > 0 && app != "") {
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
			System.out.println("  Number of regions: " + regions.size());
			for (Region region : regions) {
				if (region.entireMethod) {
					System.out.println(String.format("  Region (Method) %s;%s;%s", region.clazz, region.method, region.signature));
				} else {
					System.out.println(String.format("  Region %s;%s;%s + %#x", region.clazz, region.method, region.signature, region.entryOffset));
				}
			}
			System.out.println();
			
		} else {
			System.out.println("Cannot print config, as it's invalid!");
		}
	}
}
