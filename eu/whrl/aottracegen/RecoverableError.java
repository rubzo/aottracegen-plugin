package eu.whrl.aottracegen;

public class RecoverableError {

	public boolean branchOutOfRange = false;
	
	public int lineNumber = 0; 
	
	public RecoverableError setBranchOutOfRange() {
		branchOutOfRange = true;
		return this;
	}
	
	public RecoverableError setLineNumber(int n) {
		lineNumber = n;
		return this;
	}
}
