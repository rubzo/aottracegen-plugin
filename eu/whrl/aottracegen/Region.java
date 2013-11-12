package eu.whrl.aottracegen;

import java.util.LinkedList;
import java.util.List;

import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Util.SparseIntArray;

public class Region {
	public String clazz = "";
	public String method = "";
	public String signature = "";
	
	public int entryOffset = 0;
	public List<Integer> merges = new LinkedList<Integer>();
	
	public int id = 0;
	
	public boolean completed = false;
	
	public boolean disableFP = false;
	public boolean entireMethod = false;
	
	public EncodedMethod encodedMethod;
	
	// the "trace" that represents this region
	public Trace trace;
	
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
	
	/*
	 * Get the instruction at the provided code address.
	 */
	public Instruction getInstructionAtCodeAddress(int codeAddress) {
		return instructions[instructionMap.get(codeAddress)];
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
