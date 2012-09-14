package eu.whrl.aottracegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Util.SparseIntArray;

public class CodeGenContext {
	public Trace trace;
	
	public Instruction[] instructions;
	public SparseIntArray packedSwitchMap;
	public SparseIntArray sparseSwitchMap;
	public SparseIntArray instructionMap;
	
	public DexFile dexFile;
	
	public Set<Opcode> generatedFunctionOpcodes;
	public List<Integer> codeAddressesRaisingExceptions;
	
	public CodeGenContext() {
		generatedFunctionOpcodes = new TreeSet<Opcode>();
		codeAddressesRaisingExceptions = new ArrayList<Integer>();
	}
	
	public Instruction getInstructionAtCodeAddress(int codeAddress) {
		return instructions[instructionMap.get(codeAddress)];
	}
}
