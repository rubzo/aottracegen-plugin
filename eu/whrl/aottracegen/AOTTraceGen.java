package eu.whrl.aottracegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.jf.baksmali.Plugin;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InvokeInstruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.Code.Format.SparseSwitchDataPseudoInstruction;
import org.jf.dexlib.Util.SparseIntArray;

public class AOTTraceGen implements Plugin {
	public class Config {
		public String clazz = "";
		public String method = "";
		public String signature = "";
		public int numTraces = 0;
		public int[] traceEntries = null;
		
		public void addEntry(int e) {
			numTraces++;
			int[] newTraceEntries = new int[numTraces];
			for (int i = 0; i < numTraces-1; i++) {
				newTraceEntries[i] = traceEntries[i];
			}
			newTraceEntries[numTraces-1] = e;
			traceEntries = newTraceEntries;
		}
	}
	
	boolean validConfigFileLoaded = false;
	Config config = null;
	
	// These data structures are used during the trace generation stage.
	Instruction[] instructions;
	SparseIntArray packedSwitchMap;
	SparseIntArray sparseSwitchMap;
	SparseIntArray instructionMap;
	
	public void init(String pluginArgs) {
		System.out.println("Initing plugin");
		System.out.println("Args: " + pluginArgs);
	}
	
	public void run(DexFile dexFile) {
		System.out.println("Running plugin");
		
		/*// If we've disassembled a dexfile, then check if we want to jump into the injectable trace generation code.
        if (generateCTraces) {
        	System.out.println("Generating C traces...");
        	CTraceGenerator generator = new CTraceGenerator();
        	generator.loadConfigFile(generateCTracesFile);
        	if (generator.isConfigFileValid()) {
        		generator.generateCTraces(dexFile);
        	}         
        }*/
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
	
	public boolean isConfigFileValid() {
		return validConfigFileLoaded;
	}
	
	public void generateCTraces(DexFile dexFile) {
		printConfig();
		
		boolean foundClass = false;
		
		String methodName = config.method + config.signature;
		
		EncodedMethod methodToUse = null;
		
		for (ClassDefItem clazz : dexFile.ClassDefsSection.getItems()) {
			if (clazz.getClassType().getTypeDescriptor().equals(config.clazz)) {
				
				foundClass = true;
				
				boolean foundMethod = false;
						
				// Search the direct methods first.
				for (EncodedMethod method : clazz.getClassData().getDirectMethods()) {
					if (method.method.getShortMethodString().equals(methodName)) {
						methodToUse = method;
						foundMethod = true;
					}
				}
				
				// Then, if we haven't found it, search the virtual ones.
				if (!foundMethod) {
					for (EncodedMethod method : clazz.getClassData().getVirtualMethods()) {
						if (method.method.getShortMethodString().equals(methodName)) {
							methodToUse = method;
							foundMethod = true;
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
		
		if (methodToUse != null) {
			generateTracesFromMethod(methodToUse);
		}
	}
	
	private void generateTracesFromMethod(EncodedMethod method) {
		
		instructions = method.codeItem.getInstructions();
		
		// MAP <switch data location> -> <switch inst code address>
		packedSwitchMap = new SparseIntArray(1);
		// MAP <switch data location> -> <switch inst code address>
		sparseSwitchMap = new SparseIntArray(1);
		// MAP <inst code address> -> <index in instructions list>
		instructionMap = new SparseIntArray(instructions.length);

		// Create the packed switch, sparse switch and instruction maps.
        int currentCodeAddress = 0;
        for (int i=0; i<instructions.length; i++) {
            Instruction instruction = instructions[i];
            if (instruction.opcode == Opcode.PACKED_SWITCH) {
                packedSwitchMap.append(
                        currentCodeAddress +
                                ((OffsetInstruction)instruction).getTargetAddressOffset(),
                        currentCodeAddress);
            } else if (instruction.opcode == Opcode.SPARSE_SWITCH) {
                sparseSwitchMap.append(
                        currentCodeAddress +
                                ((OffsetInstruction)instruction).getTargetAddressOffset(),
                        currentCodeAddress);
            }
            instructionMap.append(currentCodeAddress, i);
            currentCodeAddress += instruction.getSize(currentCodeAddress);
        }
        
        // Now generate all the traces.
        HashMap<Integer,Trace> traceMap = new HashMap<Integer,Trace>();
        generateAllTracesInMethod(traceMap);
        
        // Now look for the traces we want.
        printTraces(traceMap);
	}
	
	private void printTraces(HashMap<Integer,Trace> traceMap) {
		for (Trace trace : traceMap.values()) {

        	System.out.println(String.format("Trace starting at 0x%x", trace.addresses[0]));
        	System.out.println();

        	for (int i = 0; i < trace.length; i++) {
        		int codeAddress = trace.addresses[i];
        		Instruction inst = getInstructionAtCodeAddress(codeAddress);
        		System.out.println(String.format("0x%x: %s", codeAddress, inst.opcode.name));
        	}
        	
        	System.out.println();
        	System.out.println();
        }
	}
	
	private Instruction getInstructionAtCodeAddress(int codeAddress) {
		return instructions[instructionMap.get(codeAddress)];
	}
	
	private boolean isInvokeInstruction(Instruction i) {
		if (i instanceof InvokeInstruction) {
			return true;
		}
		return false;
	}
	
	private boolean isTraceEndingInstruction(int codeAddress, Instruction i) {
		if (i.opcode == Opcode.IF_EQ ||
				i.opcode == Opcode.IF_EQZ ||
				i.opcode == Opcode.IF_GE ||
				i.opcode == Opcode.IF_GEZ ||
				i.opcode == Opcode.IF_GT ||
				i.opcode == Opcode.IF_GTZ ||
				i.opcode == Opcode.IF_LE ||
				i.opcode == Opcode.IF_LEZ ||
				i.opcode == Opcode.IF_LT ||
				i.opcode == Opcode.IF_LTZ ||
				i.opcode == Opcode.IF_NE ||
				i.opcode == Opcode.IF_NEZ) {
			return true;
		}
		
		if (i instanceof InvokeInstruction) {
			return true;
		}
		
		if (i.opcode == Opcode.PACKED_SWITCH || i.opcode == Opcode.SPARSE_SWITCH) {
			return true;
		}
		
		if (i.opcode == Opcode.RETURN ||
				i.opcode == Opcode.RETURN_OBJECT ||
				i.opcode == Opcode.RETURN_VOID ||
				i.opcode == Opcode.RETURN_VOID_BARRIER ||
				i.opcode == Opcode.RETURN_WIDE) {
			return true;
		}
		
		if (i.opcode == Opcode.THROW) {
			return true;
		}
		
		int nextCodeAddress = getNextCodeAddress(codeAddress, i);
		Instruction nextInstruction = getInstructionAtCodeAddress(nextCodeAddress);
		if (nextInstruction.opcode == Opcode.PACKED_SWITCH ||
				nextInstruction.opcode == Opcode.SPARSE_SWITCH) {
			return true;
		}
		
		return false;
	}
	
	private int getNextCodeAddress(int currentCodeAddress, Instruction instruction) {
		int nextCodeAddress = currentCodeAddress + instruction.getSize(currentCodeAddress);
		if (instruction.opcode == Opcode.GOTO || 
				instruction.opcode == Opcode.GOTO_16 || 
				instruction.opcode == Opcode.GOTO_32) {
			nextCodeAddress = currentCodeAddress + ((OffsetInstruction) instruction).getTargetAddressOffset();
		} 
		if (nextCodeAddress == currentCodeAddress) {
			// We have a loop
			return -1;
		}
		return nextCodeAddress;
	}
	
	private void handleSuccessors(Trace trace, int currentCodeAddress, Instruction currentInstruction) {
		// Get the address where we'll be going next.
		int fallthruCodeAddress = getNextCodeAddress(currentCodeAddress, currentInstruction);
		
		if (currentInstruction.opcode == Opcode.IF_EQ ||
				currentInstruction.opcode == Opcode.IF_EQZ ||
				currentInstruction.opcode == Opcode.IF_GE ||
				currentInstruction.opcode == Opcode.IF_GEZ ||
				currentInstruction.opcode == Opcode.IF_GT ||
				currentInstruction.opcode == Opcode.IF_GTZ ||
				currentInstruction.opcode == Opcode.IF_LE ||
				currentInstruction.opcode == Opcode.IF_LEZ ||
				currentInstruction.opcode == Opcode.IF_LT ||
				currentInstruction.opcode == Opcode.IF_LTZ ||
				currentInstruction.opcode == Opcode.IF_NE ||
				currentInstruction.opcode == Opcode.IF_NEZ) {
			
			trace.allocSuccessors(2);
			trace.addSuccessor(fallthruCodeAddress);
			trace.addSuccessor(currentCodeAddress + ((OffsetInstruction)currentInstruction).getTargetAddressOffset());
			return;
		}
		
		if (currentInstruction.opcode == Opcode.PACKED_SWITCH) {
			int switchDataAddress = currentCodeAddress + 
					((OffsetInstruction)currentInstruction).getTargetAddressOffset();
			PackedSwitchDataPseudoInstruction switchData = (PackedSwitchDataPseudoInstruction) getInstructionAtCodeAddress(switchDataAddress);
			
			trace.allocSuccessors(switchData.getTargetCount() + 1);
			trace.addSuccessor(fallthruCodeAddress);
			for (int target : switchData.getTargets()) {
				trace.addSuccessor(currentCodeAddress + target);
			}
			return;
		}
		
		if (currentInstruction.opcode == Opcode.SPARSE_SWITCH) {
			int switchDataAddress = currentCodeAddress + 
					((OffsetInstruction)currentInstruction).getTargetAddressOffset();
			SparseSwitchDataPseudoInstruction switchData = (SparseSwitchDataPseudoInstruction) getInstructionAtCodeAddress(switchDataAddress);
			
			trace.allocSuccessors(switchData.getTargetCount() + 1);
			trace.addSuccessor(fallthruCodeAddress);
			for (int target : switchData.getTargets()) {
				trace.addSuccessor(currentCodeAddress + target);
			}
			return;
		}
		
		// If we get here, there's only the fallthru successor.
		trace.allocSuccessors(1);
		trace.addSuccessor(fallthruCodeAddress);
		return;
	}
	
	private void generateAllTracesInMethod(HashMap<Integer,Trace> traceMap) {
		generateTracesFromCodeAddress(traceMap, 0x0);
	}
	
	private void generateTracesFromCodeAddress(HashMap<Integer,Trace> traceMap, int codeAddress) {
		// First check, have we already generated the traces starting from this point?
		if (traceMap.containsKey(new Integer(codeAddress))) {
			return;
		}
		
		int currentCodeAddress = codeAddress;
		Instruction currentInstruction = null;
		
		// We haven't, so create a new trace.
		Trace trace = new Trace();
		
		// Initialise the trace
		trace.extend(currentCodeAddress);
		currentInstruction = getInstructionAtCodeAddress(currentCodeAddress);
		
		// Trace!
		while (!isTraceEndingInstruction(currentCodeAddress, currentInstruction)) {
			currentCodeAddress = getNextCodeAddress(currentCodeAddress, currentInstruction);
			if (currentCodeAddress == -1) {
				// We have a loop, bailout
				return;
			}
			
			trace.extend(currentCodeAddress);
			currentInstruction = getInstructionAtCodeAddress(currentCodeAddress);
		}
		
		// If the last instruction in the trace is an invoke, extend it to include the
		// return-* instruction.
		if (isInvokeInstruction(currentInstruction)) {
			currentCodeAddress = getNextCodeAddress(currentCodeAddress, currentInstruction);
			if (currentCodeAddress == -1) {
				// We have a loop, bailout
				return;
			}
			trace.extend(currentCodeAddress);
			currentInstruction = getInstructionAtCodeAddress(currentCodeAddress);
		}
		
		
		
		// Deal with successors
		handleSuccessors(trace, currentCodeAddress, currentInstruction);
		
		// Add to the map
		traceMap.put(new Integer(codeAddress), trace);
		
		// Recurse into the successors to get their traces
		for (int successor : trace.successors) {
			generateTracesFromCodeAddress(traceMap, successor);
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
