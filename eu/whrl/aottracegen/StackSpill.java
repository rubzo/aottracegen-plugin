package eu.whrl.aottracegen;

public class StackSpill {
	enum Type {
		SUM,
		LITPOOL_POINTER,
		LITPOOL_POINTER_SUM,
	}
	
	Type type;
	int reg;
	int offset;
}
