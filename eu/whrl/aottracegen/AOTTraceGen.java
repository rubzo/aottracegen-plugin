package eu.whrl.aottracegen;

import java.io.IOException;
import java.util.List;

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
	
	private void populateMethodLookup(DexFile dexFile, List<String> libNames) {
		
		MethodLookup lookup = MethodLookup.getMethodLookup();
		
		lookup.initClassPath(dexFile);
		
		// Add all the methods declared in our original dex file
		for (ClassDefItem clazz : dexFile.ClassDefsSection.getItems()) {
			if (clazz.getClassData() != null) {
				for (EncodedMethod method : clazz.getClassData().getDirectMethods()) {
					lookup.addMethod(method.method.getMethodString(), method);
				}
				for (EncodedMethod method : clazz.getClassData().getVirtualMethods()) {
					lookup.addMethod(method.method.getMethodString(), method);
				}
			}
		}
		
		// Add all the methods declared in specified library dex files
		for (String libName : libNames) {
			try {
				DexFile libDexFile = new DexFile("framework/" + libName, false, false);
				for (ClassDefItem clazz : libDexFile.ClassDefsSection.getItems()) {
					if (clazz.getClassData() != null) {
						for (EncodedMethod method : clazz.getClassData().getDirectMethods()) {
							lookup.addMethod(method.method.getMethodString(), method);
						}
						for (EncodedMethod method : clazz.getClassData().getVirtualMethods()) {
							lookup.addMethod(method.method.getMethodString(), method);
						}
					}
				}
			} catch (IOException e) {
				System.err.println("Couldn't find " + libName + " to load!");
			}
		}
		
	}
	
	/*
	 * Based on the config that we've loaded, get the correct EncodedMethod.
	 */
	private EncodedMethod findTargetMethod(DexFile dexFile, Region region) {
		String methodName = region.clazz + ";->" + region.method + region.signature;
		return MethodLookup.getMethodLookup().getMethodByName(methodName);
	}
	
	/*
	 * Called from the interface methods to actually generate AOT traces based on the DexFile that
	 * baksmali generated.
	 */
	private void generateAOTTraces(DexFile dexFile) {
		config.printConfig();
		
		populateMethodLookup(dexFile, config.extraLibs);
		
		for (Region region : config.regions) {
			region.encodedMethod = findTargetMethod(dexFile, region);
		}
		
		CodeGenContext context = new CodeGenContext(dexFile, config);
		
		for (int i = 0; i < config.regions.size(); i++) {
			context.selectCurrentRegion(i);
			
			TraceCreator traceCreator = new TraceCreator();
			
			if (context.currentRegion.entireMethod) {
				traceCreator.createMethodTrace(context.currentRegion);
			} else {
				try {
					traceCreator.mergeTraces(context.currentRegion);
				} catch (TraceMergingException e) {
					System.err.println("Couldn't merge traces. Could not continue.");
					return;
				}
			}
			
			context.currentRegion.trace.print(context);
		}
		
		// Generate code for the selected traces 
		CodeGenerator codegen = new CodeGenerator();
		codegen.generateCodeFromContext(context);
	}
}
