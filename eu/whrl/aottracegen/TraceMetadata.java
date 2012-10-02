package eu.whrl.aottracegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jf.dexlib.Code.Opcode;

public class TraceMetadata {
	public Set<Opcode> opcodesUsedThatNeedHelperFunctions;
	public List<Integer> codeAddressesThatThrowExceptions;
	
	public int literalPoolSize;
	public List<Integer> literalPoolIndices;
	public List<Opcode> literalPoolOpcodes;
	public int[] clobberedRegisters;
	public boolean hasClobberedRegisters;
	public boolean containsSwitch;
	
	public TraceMetadata() {
		opcodesUsedThatNeedHelperFunctions = new TreeSet<Opcode>();
		codeAddressesThatThrowExceptions = new ArrayList<Integer>();
		literalPoolSize = 0;
		literalPoolIndices = new ArrayList<Integer>();
		literalPoolOpcodes = new ArrayList<Opcode>();
		clobberedRegisters = null;
		hasClobberedRegisters = false;
		containsSwitch = false;
	}
}
