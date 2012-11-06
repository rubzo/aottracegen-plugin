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
	
	private int insertLiteralPoolEntry(LiteralPoolType type, int value) {
		int loc = literalPoolSize;
		literalPoolSize++;
		literalPoolIndices.add(value);
		literalPoolTypes.add(type);
		return loc;
	}
	
	public int addLiteralPoolTypeAndValue(LiteralPoolType type, int value) {
		// Search, do we already have a literal pool entry with this type? 
		for (int i = 0; i < literalPoolSize; i++) {
			if (literalPoolTypes.get(i) == type && literalPoolIndices.get(i) == value) {
				return i;
			}
		}

		return insertLiteralPoolEntry(type, value);
	}
	
	public int addLiteralPoolType(LiteralPoolType type) {
		// Search, do we already have a literal pool entry with this type? 
		for (int i = 0; i < literalPoolSize; i++) {
			if (literalPoolTypes.get(i) == type) {
				return i;
			}
		}
		
		return insertLiteralPoolEntry(type, 0);
	}
	
	public int getLiteralPoolLocationForType(LiteralPoolType type) {
		for (int i = 0; i < literalPoolSize; i++) {
			if (literalPoolTypes.get(i) == type) {
				return i;
			}
		}
		
		return -1;
	}
}
