package eu.whrl.aottracegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jf.dexlib.Code.Opcode;

public class TraceMetadata {
	public Set<Opcode> generatedFunctionOpcodes;
	public List<Integer> codeAddressesRaisingExceptions;
	
	public int literalPoolSize;
	public List<Integer> literalPoolIndices;
	public List<Opcode> literalPoolOpcodes;
	
	public TraceMetadata() {
		generatedFunctionOpcodes = new TreeSet<Opcode>();
		codeAddressesRaisingExceptions = new ArrayList<Integer>();
		literalPoolSize = 0;
		literalPoolIndices = new ArrayList<Integer>();
		literalPoolOpcodes = new ArrayList<Opcode>();
	}
}
