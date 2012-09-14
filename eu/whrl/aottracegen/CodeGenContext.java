package eu.whrl.aottracegen;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Util.SparseIntArray;

public class CodeGenContext {
	public Trace trace;
	
	public Instruction[] instructions;
	public SparseIntArray packedSwitchMap;
	public SparseIntArray sparseSwitchMap;
	public SparseIntArray instructionMap;
	
	public DexFile dexFile;
	
	
	
	
	
}
