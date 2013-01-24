package eu.whrl.aottracegen;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jf.dexlib.Code.Instruction;

public class Trace {
	public boolean valid;
	public List<Integer> addresses;
	
	public Set<Integer> successors;
	
	public int entry;
	
	public TraceMetadata meta;
	
	public Trace() {
		valid = false;
		addresses = new LinkedList<Integer>();
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
		addresses.add(codeAddress);
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
		for (int i = 0; i < addresses.size(); i++) {
			if (addresses.get(i) == codeAddress) {
				return true;
			}
		}
		return false;
	}
	
	public int getLength() {
		return addresses.size();
	}
	
	public void calculateRegisterInteraction(CodeGenContext context) {
		/*
		for (int address : addresses) {
			Instruction inst = context.getInstructionAtCodeAddress(address);
			OpcodeParser.calculateRegisterInteraction(inst, meta.readRegs, meta.dirtyRegs);
		}
		*/
	}
	
	/*
	 * Print out the trace.
	 */
	public void print(CodeGenContext context) {
		System.out.println(String.format("Trace starting at %#x", entry));
    	System.out.println();

    	for (int i = 0; i < addresses.size(); i++) {
    		int codeAddress = addresses.get(i);
    		Instruction inst = context.getInstructionAtCodeAddress(codeAddress);
    		System.out.println(String.format("%#x: %s", codeAddress, inst.opcode.name));
    	}
    	
    	String successorString = "Successors: [";
    	int numSuccessorsSeen = 0;
    	for (int successor : successors) {
    		numSuccessorsSeen++;
    		if (numSuccessorsSeen != successors.size()) {
    			successorString += String.format("%#x, ", successor);
    		} else {
    			successorString += String.format("%#x", successor);
    		}
    	}
    	successorString += "]";
    	System.out.println(successorString);
    	
    	System.out.println();
    	System.out.println();
	}
}
