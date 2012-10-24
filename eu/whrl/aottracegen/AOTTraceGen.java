package eu.whrl.aottracegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.jf.baksmali.Plugin;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;

import eu.whrl.aottracegen.exceptions.TraceMergingException;

public class AOTTraceGen implements Plugin {
	private boolean validConfigFileLoaded = false;
	private Config config = null;
	
	//
	// INTERFACE METHODS
	//
	public void init(String pluginArgs) {
		loadConfigFile(pluginArgs);
	}
	
	public void run(DexFile dexFile) {
		if (isConfigFileValid()) {
			generateAOTTraces(dexFile);
		} else {
			System.err.println("The config file wasn't valid, cannot continue.");
		}
	}
	//
	// END INTERFACE METHODS
	//
	
	/*
	 * Loads a config file from the filename provided.
	 */
	private void loadConfigFile(String filename) {
		File file = new File(filename);
		FileReader reader = null;
		try {
			reader = new FileReader(file);
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't find config file");
			return;
		}
		
		BufferedReader buff = new BufferedReader(reader);
		config = new Config();
		int lineCounter = 0;
		try {
			while (buff.ready()) {
				String line = buff.readLine();
				if (line.startsWith("app")) {
					config.app = line.substring(4, line.length());
				} else if (line.startsWith("class")) {
					config.clazz = line.substring(6, line.length());
				} else if (line.startsWith("method")) {
					config.method = line.substring(7, line.length());
				} else if (line.startsWith("signature")) {
					config.signature = line.substring(10, line.length());
				} else if (line.startsWith("merge")) {
					config.produceMerged = true;
				} else if (line.startsWith("unsafe")) {
					config.produceUnsafe = true;
				} else if (line.startsWith("trace all")) {
					config.traceAll = true;
				} else if (line.startsWith("trace")) {
					config.addEntry(Integer.parseInt(line.substring(8, line.length()), 16));
				} else {
					System.err.println("Couldn't make sense of line " + lineCounter + ": " + line);
				}
				lineCounter++;
			}
		} catch (IOException e) {
			System.err.println("Couldn't read config file");
		}
		
		if (config.app != "" && config.clazz != "" && config.method != "" && config.signature != "" && (config.numTraces > 0 || config.traceAll)) {
			validConfigFileLoaded = true;
		}
	}
	
	/*
	 * Checks if the config file we tried to load in loadConfigFile() is valid.
	 */
	private boolean isConfigFileValid() {
		return validConfigFileLoaded;
	}
	
	/*
	 * Based on the config that we've loaded, get the correct EncodedMethod.
	 */
	private EncodedMethod findTargetMethod(DexFile dexFile) {
		boolean foundClass = false;
		
		String methodName = config.method + config.signature;
		
		EncodedMethod methodToUse = null;
		
		// Search all the classes
		for (ClassDefItem clazz : dexFile.ClassDefsSection.getItems()) {
			
			// Have we found the right class?
			if (clazz.getClassType().getTypeDescriptor().equals(config.clazz)) {
				
				foundClass = true;
				
				boolean foundMethod = false;
						
				// Search the direct methods first.
				for (EncodedMethod method : clazz.getClassData().getDirectMethods()) {
					if (method.method.getShortMethodString().equals(methodName)) {
						methodToUse = method;
						foundMethod = true;
						break;
					}
				}
				
				// Then, if we haven't found it, search the virtual ones.
				if (!foundMethod) {
					for (EncodedMethod method : clazz.getClassData().getVirtualMethods()) {
						if (method.method.getShortMethodString().equals(methodName)) {
							methodToUse = method;
							foundMethod = true;
							
							// Found the method, so we're done.
							break;
						}
					}
				}
				
				if (!foundMethod) {
					System.err.println("Couldn't find the given method in this class!");
				}
				
				// Found the class, so we're done.
				break;
			}
		}
		
		if (!foundClass) {
			System.err.println("Couldn't find the given class in this DEX file!");
		}
		
		return methodToUse;
	}
	
	/*
	 * Called from the interface methods to actually generate AOT traces based on the DexFile that
	 * baksmali generated.
	 */
	private void generateAOTTraces(DexFile dexFile) {
		printConfig();
		
		EncodedMethod methodToUse = findTargetMethod(dexFile);
		
		if (methodToUse == null) {
			return;
		}
		
		CodeGenContext context = new CodeGenContext(dexFile, methodToUse, config);
		
		// Enumerate all the traces in the method
		TraceFinder traceFinder = new TraceFinder();
		Map<Integer,Trace> traceMap = traceFinder.generateTracesFromMethod(context);
		
		// Do merging if needed
		if (config.produceMerged) {
			TraceMerger traceMerger = new TraceMerger();
			try {
				traceMap = traceMerger.mergeTraces(context, config, traceMap);
			} catch (TraceMergingException e) {
				System.err.println("Couldn't merge traces. Could not continue.");
				return;
			}
			
		}
		
		// Add all the traces to the config, now that we've found them, if necessary.
		if (config.traceAll) {
			for (int traceEntry : traceMap.keySet()) {
				config.addEntry(traceEntry);
			}
		}
		
		// Generate code for the selected traces
		CodeGenerator codeGen = new CodeGenerator();
		for (int traceEntry : config.traceEntries) {
			Trace trace = traceMap.get(traceEntry);	
			if (trace == null) {
				System.err.println(String.format("Specified trace %#x is not a trace head", traceEntry));
				return;
			}
			trace.print(context);
			context.addTrace(trace);
		}
		
		codeGen.generateCodeFromContext(context);
	}
	
	/*
	 * Prints the loaded config, if it's valid.
	 */
	private void printConfig() {
		if (isConfigFileValid()) {
			System.out.println("Printing Config...");
			System.out.println("  App: " + config.app);
			System.out.println("  Class: " + config.clazz);
			System.out.println("  Method: " + config.method);
			System.out.println("  Signature: " + config.signature);
			if (!config.traceAll) {
				System.out.println("  Number of traces: " + config.numTraces);
			} else {
				System.out.println("  Number of traces: all");
			}
			System.out.println();
		} else {
			System.out.println("Cannot print config, as it's invalid!");
		}
	}
	
}
