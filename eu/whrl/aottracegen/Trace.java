package eu.whrl.aottracegen;

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
}
