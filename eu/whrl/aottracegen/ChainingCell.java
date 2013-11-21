package eu.whrl.aottracegen;

public class ChainingCell {
	public enum Type {
		NORMAL,
		INVOKE_PREDICTED,
		HOT,
		INVOKE_SINGLETON,
		INVOKE_SUPER_SINGLETON,
		BACKWARD_BRANCH
	}
	
	public Type type;
	public int codeAddress; // in INVOKE_SINGLE, this is actually the methodIndex
	
	public ChainingCell(Type t, int pc) {
		type = t;
		codeAddress = pc;
	}
}
