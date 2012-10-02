package eu.whrl.aottracegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.whrl.aottracegen.exceptions.TraceMergingException;

public class TraceMerger {

	public Map<Integer, Trace> mergeTraces(CodeGenContext context, Config config, Map<Integer,Trace> traceMap) throws TraceMergingException {
		
		Map<Integer,Trace> tracesToBeMerged = new HashMap<Integer,Trace>();
		
		for (int traceEntry : config.traceEntries) {
			if (!traceMap.containsKey(new Integer(traceEntry))) {
				System.err.println("Unable to find a requested trace for merging in this method.");
				throw new TraceMergingException();
			}
			tracesToBeMerged.put(new Integer(traceEntry), traceMap.get(new Integer(traceEntry)));
		}
		
		// Merge all traces into the first one.
		Trace mergedTrace = tracesToBeMerged.get(new Integer(config.traceEntries[0]));
		tracesToBeMerged.remove(new Integer(config.traceEntries[0]));
		
		int tracesLeft = tracesToBeMerged.size();
		
		while (!tracesToBeMerged.isEmpty()) {
			mergeTraces(mergedTrace, tracesToBeMerged);
			
			if (tracesToBeMerged.size() == tracesLeft) {
				// i.e. nothing has changed
				System.err.println("Unable to merge some of the selected traces together.");
				throw new TraceMergingException();
			}
		}
		
		traceMap.clear();
		traceMap.put(new Integer(mergedTrace.entry), mergedTrace);
		
		config.numTraces = 1;
		config.traceEntries = new int[] {mergedTrace.entry};
		
		return traceMap;
	}

	private void mergeTraces(Trace mergedTrace, Map<Integer, Trace> tracesToBeMerged) {
		
		boolean foundNextTrace = false;
		int nextSuccessor = 0;
		for (int successor : mergedTrace.successors) {
			if (tracesToBeMerged.containsKey(new Integer(successor))) {
				foundNextTrace = true;
				nextSuccessor = successor;
			}
		}
		
		if (!foundNextTrace) {
			System.err.println("Couldn't find the next mergable trace from the current successors.");
			return;
		}
		
		merge(mergedTrace, tracesToBeMerged.get(new Integer(nextSuccessor)));
		
		tracesToBeMerged.remove(new Integer(nextSuccessor));
	}

	private void merge(Trace mergedTrace, Trace nextTrace) {
		int newSuccessorsCount = mergedTrace.successorsCount + nextTrace.successorsCount - 1;
		int[] newSuccessors = new int[newSuccessorsCount];
		int i = 0;
		for (int s : mergedTrace.successors) {
			if (s != nextTrace.entry) {
				newSuccessors[i] = s;
				i++;
			}
		}
		for (int s : nextTrace.successors) {
			newSuccessors[i] = s;
			i++;
		}
		
		newSuccessors = removeDuplicates(newSuccessors);
		newSuccessorsCount = newSuccessors.length;
		
		mergedTrace.successors = newSuccessors;
		mergedTrace.successorsCount = newSuccessorsCount;
		
		
		int newLength = mergedTrace.length + nextTrace.length;
		int[] newAddresses = new int[newLength];
		
		for (int idx = 0; idx < mergedTrace.length; idx++) {
			newAddresses[idx] = mergedTrace.addresses[idx];
		}
		for (int idx = 0; idx < nextTrace.length; idx++) {
			newAddresses[mergedTrace.length + idx] = nextTrace.addresses[idx];
		}
		
		newAddresses = removeDuplicates(newAddresses);
		newLength = newAddresses.length;
		
		mergedTrace.addresses = newAddresses;
		mergedTrace.length = newLength;
	}
	
	/*
	 * Removes duplicates from a list, preserves ordering.
	 */
	private int[] removeDuplicates(int[] list) {
		Set<Integer> s = new HashSet<Integer>();
		
		int[] tempList = new int[list.length];
		int idx = 0;
		for (int i : list) {
			if (!s.contains(new Integer(i))) {
				s.add(new Integer(i));
				tempList[idx] = i;
				idx++;
			}
		}
		
		// Copy slightly too large array into a snug array.
		int[] newList = new int[s.size()];
		for (int i = 0; i < s.size(); i++) {
			newList[i] = tempList[i];
		}
		
		return newList;
	}
}
