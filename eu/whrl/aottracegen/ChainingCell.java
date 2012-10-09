package eu.whrl.aottracegen;

public class ChainingCell {
	enum Type {
		NORMAL,
		INVOKE_PREDICTED,
		HOT,
		INVOKE_SINGLETON,
		BACKWARD_BRANCH
	}
	
	public Type type;
	public int codeAddress;
	
	public ChainingCell(Type t, int pc) {
		type = t;
		codeAddress = pc;
	}
}
