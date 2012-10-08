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
	public List<LiteralPoolType> literalPoolTypes;
	public int[] clobberedRegisters;
	public boolean hasClobberedRegisters;
	public boolean containsSwitch;
	
	public TraceMetadata() {
		opcodesUsedThatNeedHelperFunctions = new TreeSet<Opcode>();
		codeAddressesThatThrowExceptions = new ArrayList<Integer>();
		literalPoolSize = 0;
		literalPoolIndices = new ArrayList<Integer>();
		literalPoolTypes = new ArrayList<LiteralPoolType>();
		clobberedRegisters = null;
		hasClobberedRegisters = false;
		containsSwitch = false;
	}
	
	public void addLiteralPoolEntry(LiteralPoolType type, int value) {
		literalPoolSize++;
		literalPoolIndices.add(value);
		literalPoolTypes.add(type);
	}
}
