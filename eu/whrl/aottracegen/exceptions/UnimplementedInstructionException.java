package eu.whrl.aottracegen.exceptions;

public class UnimplementedInstructionException extends Exception {
	private static final long serialVersionUID = 1L;
	private String name;
	private int address;
	
	public UnimplementedInstructionException(String instructionName, int codeAddress) {
		name = instructionName;
		address = codeAddress;
	}
	
	public String getUnimplementedInstructionName() {
		return name;
	}
	
	public int getUnimplementedInstructionCodeAddress() {
		return address;
	}
}