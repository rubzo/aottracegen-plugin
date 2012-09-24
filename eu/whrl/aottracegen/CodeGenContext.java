package eu.whrl.aottracegen;

import java.util.ArrayList;
import java.util.List;

import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Util.SparseIntArray;

/*
 * NB: A CodeGenContext only works within one method!
 */
public class CodeGenContext {
	public List<Trace> traces;
	private int currentTraceIdx;
	
	public int methodIndex;
	public Config config;
	
	//
	// These are local to this method only!
	//
	// ARRAY <instructions in method>
	public Instruction[] instructions;
	// MAP <switch data location> -> <switch inst code address>
	public SparseIntArray packedSwitchMap;
	// MAP <switch data location> -> <switch inst code address>
	public SparseIntArray sparseSwitchMap;
	// MAP <inst code address> -> <index in instructions list>
	public SparseIntArray instructionMap;
	
	public DexFile dexFile;
	
	/*
	 * NB: A CodeGenContext only works within one method!
	 * Hence, we pass in the method to the constructor.
	 */
	public CodeGenContext(DexFile df, EncodedMethod method, Config config) {
		dexFile = df;
		traces = new ArrayList<Trace>();
		currentTraceIdx = 0;
		
		this.config = config;
		methodIndex = method.method.getIndex();

		// Store all the instructions the method contains.
		instructions = method.codeItem.getInstructions();

		// Create the packed switch, sparse switch and instruction maps.
        packedSwitchMap = new SparseIntArray(1);
		sparseSwitchMap = new SparseIntArray(1);
		instructionMap = new SparseIntArray(instructions.length);
		
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
	}
	
	/*
	 * Get the instruction at the provided code address.
	 */
	public Instruction getInstructionAtCodeAddress(int codeAddress) {
		return instructions[instructionMap.get(codeAddress)];
	}
	
	/*
	 * Add a trace to this context.
	 */
	public void addTrace(Trace trace) {
		traces.add(trace);
	}
	
	/*
	 * This method must be called before calling generateC() or compileC()!
	 */
	public void setCurrentTraceIndex(int idx) {
		currentTraceIdx = idx;
	}
	
	/*
	 * Get the currently selected trace during generateC() or compileC().
	 */
	public Trace getCurrentTrace() {
		return traces.get(currentTraceIdx);
	}
		
	/*
	 * Get the index of the currently selected trace.
	 */
	public int getCurrentTraceIndex() {
		return currentTraceIdx;
	}
	
	/*
	 * Calculates, based on the current code address and instruction, where the next code address is.
	 * (Basically handling things like GOTO.)
	 */
	public int getNextCodeAddress(int currentCodeAddress, Instruction instruction) {
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
}
