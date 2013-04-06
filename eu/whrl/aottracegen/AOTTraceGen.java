package eu.whrl.aottracegen;

import java.io.IOException;
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
	
	private void createLibraryMethodLookupStructure(DexFile dexFile, String libName) {
		
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
	private EncodedMethod findTargetMethod(DexFile dexFile, Region region) {
		boolean foundClass = false;
		
		String methodName = region.method + region.signature;
		String clazzName = region.clazz + ";";
		
		EncodedMethod methodToUse = null;
		
		// Search all the classes
		for (ClassDefItem clazz : dexFile.ClassDefsSection.getItems()) {
			
			// Have we found the right class?
			//System.out.println(clazzName);
			//System.out.println(clazz.getClassType().getTypeDescriptor());
			if (clazz.getClassType().getTypeDescriptor().equals(clazzName)) {
				
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
			createLibraryMethodLookupStructure(dexFile, coreLib);
		}
		
		for (Region region : config.regions) {
			region.encodedMethod = findTargetMethod(dexFile, region);
		}
		
		CodeGenContext context = new CodeGenContext(dexFile, config);
		
		for (int i = 0; i < config.regions.size(); i++) {
			context.selectCurrentRegion(i);
			
			TraceMerger traceMerger = new TraceMerger();
			try {
				traceMerger.mergeTraces(context.currentRegion, config);
			} catch (TraceMergingException e) {
				System.err.println("Couldn't merge traces. Could not continue.");
				return;
			}
			
			context.currentRegion.trace.print(context);
		}
		
		// Generate code for the selected traces 
		CodeGenerator codegen = new CodeGenerator();
		codegen.generateCodeFromContext(context);
	}
}
