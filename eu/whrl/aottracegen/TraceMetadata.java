package eu.whrl.aottracegen;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jf.dexlib.Code.Opcode;

public class TraceMetadata {
	public Set<Opcode> opcodesUsedThatNeedHelperFunctions;
	public List<Integer> codeAddressesThatThrowExceptions;
	
	public int literalPoolSize;
	public List<Integer> literalPoolIndices;
	public List<LiteralPoolType> literalPoolTypes;
	public boolean containsSwitch;
	public Map<Integer, ChainingCell> chainingCells;
	
	public TraceMetadata() {
		opcodesUsedThatNeedHelperFunctions = new TreeSet<Opcode>();
		codeAddressesThatThrowExceptions = new ArrayList<Integer>();
		literalPoolSize = 0;
		literalPoolIndices = new ArrayList<Integer>();
		literalPoolTypes = new ArrayList<LiteralPoolType>();
		containsSwitch = false;
		chainingCells = new TreeMap<Integer,ChainingCell>();
	}
	
	public void addLiteralPoolEntry(LiteralPoolType type, int value) {
		literalPoolSize++;
		literalPoolIndices.add(value);
		literalPoolTypes.add(type);
	}
}
