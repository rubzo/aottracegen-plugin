package eu.whrl.aottracegen;

import java.util.HashSet;
import java.util.Set;

import org.jf.dexlib.Code.Instruction;

public class Trace {
	public static final int MAX_LENGTH = 100;
	
	public boolean valid;
	public int[] addresses;
	public int length;
	
	public Set<Integer> successors;
	
	public int entry;
	
	public TraceMetadata meta;
	
	public Trace() {
		valid = false;
		addresses = new int[MAX_LENGTH];
		length = 0;
		successors = new HashSet<Integer>();
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
	 * Add a successor address to this trace.
	 */
	public void addSuccessor(int codeAddress) {
		successors.add(codeAddress);
	}
	
	public void removeSuccessor(int codeAddress) {
		successors.remove(codeAddress);
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
		System.out.println(String.format("Trace starting at %#x", entry));
    	System.out.println();

    	for (int i = 0; i < length; i++) {
    		int codeAddress = addresses[i];
    		Instruction inst = context.getInstructionAtCodeAddress(codeAddress);
    		System.out.println(String.format("%#x: %s", codeAddress, inst.opcode.name));
    	}
    	
    	System.out.println();
    	System.out.println();
	}
}
