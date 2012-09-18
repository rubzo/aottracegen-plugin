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
		}
	}
	
	//
	// EVERYTHING ELSE
	//
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
		try {
			while (buff.ready()) {
				String line = buff.readLine();
				if (line.startsWith("class")) {
					config.clazz = line.substring(6, line.length());
				} else if (line.startsWith("method")) {
					config.method = line.substring(7, line.length());
				} else if (line.startsWith("signature")) {
					config.signature = line.substring(10, line.length());
				} else if (line.startsWith("merge")) {
					config.produceMerged = true;
				} else if (line.startsWith("trace")) {
					config.addEntry(Integer.parseInt(line.substring(8, line.length()), 16));
				}
			}
		} catch (IOException e) {
			System.err.println("Couldn't read config file");
		}
		
		if (config.clazz != "" && config.method != "" && config.signature != "" && config.numTraces > 0) {
			validConfigFileLoaded = true;
		}
	}
	
	private boolean isConfigFileValid() {
		return validConfigFileLoaded;
	}
	
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
	
	private void generateAOTTraces(DexFile dexFile) {
		printConfig();
		
		EncodedMethod methodToUse = findTargetMethod(dexFile);
		
		if (methodToUse == null) {
			return;
		}
		
		CodeGenContext context = new CodeGenContext();
		context.dexFile = dexFile;
		
		// Enumerate all the traces in the method
		TraceFinder traceFinder = new TraceFinder();
		Map<Integer,Trace> traceMap = traceFinder.generateTracesFromMethod(context, methodToUse);
		
		// Do merging if needed
		TraceMerger traceMerger = new TraceMerger();
		traceMap = traceMerger.mergeTraces(context, config, traceMap);
		
		// Generate code for the selected traces
		CodeGenerator codeGen = new CodeGenerator();
		for (int traceEntry : config.traceEntries) {
			Trace trace = traceMap.get(new Integer(traceEntry));			
			trace.print(context);
			context.refreshAndSetTrace(trace, traceEntry);
			codeGen.generateCodeFromContext(context);
		}
	}
	
	
	
	private void printConfig() {
		System.out.println("Printing Config...");
		System.out.println("  Class: " + config.clazz);
		System.out.println("  Method: " + config.method);
		System.out.println("  Signature: " + config.signature);
		System.out.println("  Number of traces: " + config.numTraces);
		System.out.println();
	}
	
}
