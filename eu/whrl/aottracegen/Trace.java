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
	
	public Trace() {
		valid = false;
		addresses = new int[MAX_LENGTH];
		length = 0;
		successors = null;
		successorsCount = 0;
	}
	
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
	
	public void allocSuccessors(int count) {
		successors = new int[count];
		successorsMax = count;
	}
	
	public void addSuccessor(int codeAddress) {
		if (successors != null && successorsCount != successorsMax) {
			successors[successorsCount] = codeAddress;
			successorsCount++;
		}
	}
	
	public boolean containsCodeAddress(int codeAddress) {
		for (int i = 0; i < length; i++) {
			if (addresses[i] == codeAddress) {
				return true;
			}
		}
		return false;
	}
	
	public void print(CodeGenContext context) {
		System.out.println(String.format("Trace starting at 0x%x", addresses[0]));
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