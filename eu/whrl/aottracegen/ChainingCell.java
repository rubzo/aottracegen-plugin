package eu.whrl.aottracegen;

public class ChainingCell {
	public enum Type {
		NORMAL,
		INVOKE_PREDICTED,
		HOT,
		INVOKE_SINGLETON,
		BACKWARD_BRANCH
	}
	
	public Type type;
	public int codeAddress; // in INVOKE_SINGLE, this is actually the methodIndex
	public boolean vtable; // this is used by INVOKE_SUPER_QUICK
	
	public ChainingCell(Type t, int pc, boolean v) {
		type = t;
		codeAddress = pc;
		vtable = v;
	}
	
	public ChainingCell(Type t, int pc) {
		this(t, pc, false);
	}
}
