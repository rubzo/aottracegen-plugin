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
	public List<Integer> traceEntryAddresses;
	private int currentTraceIdx;
	
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
	public CodeGenContext(DexFile df, EncodedMethod method) {
		dexFile = df;
		traces = new ArrayList<Trace>();
		traceEntryAddresses = new ArrayList<Integer>();
		currentTraceIdx = 0;

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
	public void addTrace(Trace trace, int traceEntryAddress) {
		traces.add(trace);
		traceEntryAddresses.add(new Integer(traceEntryAddress));
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
	 * Get the currently selected trace entry address during generateC() or compileC().
	 */
	public int getCurrentTraceEntryAddress() {
		return traceEntryAddresses.get(currentTraceIdx).intValue();
	}
}
