package eu.whrl.aottracegen;

import org.jf.dexlib.Code.Instruction;

public class Trace {
	public static final int MAX_LENGTH = 100;
	
	public boolean valid;
	public int[] addresses;
	public int length;
	
	public int[] successors;
	public int successorsCount;
	private int successorsMax;
	
	public int entry;
	
	public TraceMetadata meta;
	
	public Trace() {
		valid = false;
		addresses = new int[MAX_LENGTH];
		length = 0;
		successors = null;
		successorsCount = 0;
		entry = 0;
		meta = new TraceMetadata();
	}
	
	/*
	 * Extend this trace with a new codeAddress.
	 */
	public boolean extend(int codeAddress) {
		if (!valid) {
			valid = true;
		}
		if (length == MAX_LENGTH) {
			return false;
		}
		addresses[length] = codeAddress;
		length++;
		return true;
	}
	
	/*
	 * Allocate array space for all the successors of this trace.
	 */
	public void allocSuccessors(int count) {
		successors = new int[count];
		successorsMax = count;
	}
	
	public void shrinkSuccessors(int count) {
		int[] newSuccessors = new int[count];
		for (int i = 0; i < count; i++) {
			newSuccessors[i] = successors[i];
		}
		successorsMax = count;
		successorsCount = count;
		successors = newSuccessors;
	}
	
	public void markHasNoSuccessors() {
		successorsMax = 0;
		successorsCount = 0;
		successors = null;
	}
	
	/*
	 * Add a successor address to this trace.
	 */
	public void addSuccessor(int codeAddress) {
		if (successors != null && successorsCount != successorsMax) {
			successors[successorsCount] = codeAddress;
			successorsCount++;
		}
	}
	
	public void setEntry(int codeAddress) {
		entry = codeAddress;
	}
	
	
	/*
	 * Check if this trace contains a given code address.
	 */
	public boolean containsCodeAddress(int codeAddress) {
		for (int i = 0; i < length; i++) {
			if (addresses[i] == codeAddress) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Print out the trace.
	 */
	public void print(CodeGenContext context) {
		System.out.println(String.format("Trace starting at 0x%x", entry));
    	System.out.println();

    	for (int i = 0; i < length; i++) {
    		int codeAddress = addresses[i];
    		Instruction inst = context.getInstructionAtCodeAddress(codeAddress);
    		System.out.println(String.format("0x%x: %s", codeAddress, inst.opcode.name));
    	}
    	
    	System.out.println();
    	System.out.println();
	}
}
