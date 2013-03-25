package eu.whrl.aottracegen;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.jf.baksmali.Plugin;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;

import eu.whrl.aottracegen.exceptions.TraceMergingException;

public class AOTTraceGen implements Plugin {
	private Config config = null;
	
	//
	// INTERFACE METHODS
	//
	public void init(String pluginArgs) {
		config = new Config();
		config.loadConfigFile(pluginArgs);
	}
	
	public void run(DexFile dexFile) {
		if (config.isConfigFileValid()) {
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
	
	private void createMethodLookupStructure(DexFile dexFile, String libName) {
		
		for (ClassDefItem clazz : dexFile.ClassDefsSection.getItems()) {
			if (clazz.getClassData() != null) {
				for (EncodedMethod method : clazz.getClassData().getDirectMethods()) {
					Util.methodMap.put(method.method.getMethodString(), method);
				}
				for (EncodedMethod method : clazz.getClassData().getVirtualMethods()) {
					Util.methodMap.put(method.method.getMethodString(), method);
				}
			}
		}
		try {
			DexFile libDexFile = new DexFile(libName, false, false);
			for (ClassDefItem clazz : libDexFile.ClassDefsSection.getItems()) {
				if (clazz.getClassData() != null) {
					for (EncodedMethod method : clazz.getClassData().getDirectMethods()) {
						Util.methodMap.put(method.method.getMethodString(), method);
					}
					for (EncodedMethod method : clazz.getClassData().getVirtualMethods()) {
						Util.methodMap.put(method.method.getMethodString(), method);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Couldn't find " + libName + " to load!");
		}
		
		
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
		config.printConfig();
		
		for (String coreLib : config.coreLibs) {
			createMethodLookupStructure(dexFile, coreLib);
		}
		
		EncodedMethod methodToUse = findTargetMethod(dexFile);
		
		if (methodToUse == null) {
			return;
		}
		
		CodeGenContext context = new CodeGenContext(dexFile, methodToUse, config);
		
		// Enumerate all the traces in the method
		TraceFinder traceFinder = new TraceFinder();
		Map<Integer,Trace> traceMap = traceFinder.generateTracesFromMethod(context);
		
		// Add all the traces to the config, now that we've found them, if necessary.
		if (config.traceAll) {
			for (int traceEntry : traceMap.keySet()) {
				config.addEntry(traceEntry);
			}
		}

		if (config.sortTraces) {
			Collections.sort(config.traceEntries);
		}
		
		// Do merging if needed
		if (config.doMerging) {
			TraceMerger traceMerger = new TraceMerger();
			try {
				traceMap = traceMerger.mergeTraces(context, config, traceMap);
			} catch (TraceMergingException e) {
				System.err.println("Couldn't merge traces. Could not continue.");
				return;
			}
			
		}
		
		
		
		// Generate code for the selected traces
		for (int traceEntry : config.traceEntries) {
			Trace trace = traceMap.get(traceEntry);	
			if (trace == null) {
				System.err.println(String.format("Specified trace %#x is not a trace head", traceEntry));
				return;
			}
			trace.print(context);
			context.addTrace(trace);
		}
		
		if (!config.onlyPrintTraces) {
			CodeGenerator codeGen = new CodeGenerator();
			codeGen.generateCodeFromContext(context);
		}
	}
}
