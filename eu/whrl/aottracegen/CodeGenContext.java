package eu.whrl.aottracegen;

import java.util.ArrayList;
import java.util.List;

import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Util.SparseIntArray;

public class CodeGenContext {
	public List<Trace> traces;
	public List<Integer> traceEntryAddresses;
	private int currentTraceIdx;
	
	// These are global to the method
	public Instruction[] instructions;
	public SparseIntArray packedSwitchMap;
	public SparseIntArray sparseSwitchMap;
	public SparseIntArray instructionMap;
	
	public DexFile dexFile;
	
	public CodeGenContext(DexFile dexFile, EncodedMethod method) {
		this.dexFile = dexFile;
		traces = new ArrayList<Trace>();
		traceEntryAddresses = new ArrayList<Integer>();
		currentTraceIdx = 0;

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
	}
	
	public Instruction getInstructionAtCodeAddress(int codeAddress) {
		return instructions[instructionMap.get(codeAddress)];
	}
	
	public void addTrace(Trace trace, int traceEntryAddress) {
		traces.add(trace);
		traceEntryAddresses.add(new Integer(traceEntryAddress));
	}
	
	public void setCurrentTraceIndex(int idx) {
		currentTraceIdx = idx;
	}
	
	public Trace getCurrentTrace() {
		return traces.get(currentTraceIdx);
	}
	
	public int getCurrentTraceEntryAddress() {
		return traceEntryAddresses.get(currentTraceIdx).intValue();
	}
}
