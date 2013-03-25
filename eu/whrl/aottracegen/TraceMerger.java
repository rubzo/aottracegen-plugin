package eu.whrl.aottracegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import eu.whrl.aottracegen.exceptions.TraceMergingException;

public class TraceMerger {

	public Map<Integer, Trace> mergeTraces(CodeGenContext context, Config config, Map<Integer,Trace> traceMap) throws TraceMergingException {
		
		Map<Integer,Trace> tracesToBeMerged = new HashMap<Integer,Trace>();
		
		for (int traceEntry : config.traceEntries) {
			if (!traceMap.containsKey(traceEntry)) {
				System.err.println("Unable to find a requested trace for merging in this method.");
				throw new TraceMergingException();
			}
			tracesToBeMerged.put(traceEntry, traceMap.get(traceEntry));
		}
		
		// Merge all traces into the first one.
		Trace mergedTrace = tracesToBeMerged.get(config.traceEntries.get(0));
		tracesToBeMerged.remove(config.traceEntries.get(0));
		
		System.err.println("Initial trace " + mergedTrace.entry);
		
		int tracesLeft = tracesToBeMerged.size();
		
		while (!tracesToBeMerged.isEmpty()) {
			findCandidateTraceFromMapAndMerge(mergedTrace, tracesToBeMerged);
			
			cleanupTraces(mergedTrace, tracesToBeMerged);
			
			if (tracesToBeMerged.size() == tracesLeft) {
				// i.e. nothing has changed
				System.err.println("Unable to merge some of the selected traces together.");
				System.err.println("Traces left to be merged: " + tracesToBeMerged.size());
				System.err.println("Entry addresses for these traces: " + Util.toHexString(tracesToBeMerged.keySet()));
				System.err.println("Merged trace addresses: " + Util.toHexString(mergedTrace.addresses));
				System.err.println("Merged trace successors: " + Util.toHexString(mergedTrace.successors));
				
				throw new TraceMergingException();
			} else {
				tracesLeft = tracesToBeMerged.size();
			}
			
		}
		
		traceMap.clear();
		traceMap.put(mergedTrace.entry, mergedTrace);
		
		config.numTraces = 1;
		config.traceEntries.clear();
		config.traceEntries.add(mergedTrace.entry);
		
		return traceMap;
	}

	private void cleanupTraces(Trace mergedTrace, Map<Integer, Trace> tracesToBeMerged) {
		Set<Integer> tracesToBeRemoved = new HashSet<Integer>();
		for (int entry : tracesToBeMerged.keySet()) {
			if (mergedTrace.containsCodeAddress(entry)) {
				tracesToBeRemoved.add(entry);
			}
		}
		for (int entry : tracesToBeRemoved) {
			tracesToBeMerged.remove(entry);
		}
	}
	
	private void findCandidateTraceFromMapAndMerge(Trace mergedTrace, Map<Integer, Trace> tracesToBeMerged) {
		
		boolean foundNextTrace = false;
		int nextSuccessor = 0;
		for (int successor : mergedTrace.successors) {
			if (tracesToBeMerged.containsKey(successor)) {
				foundNextTrace = true;
				nextSuccessor = successor;
				break;
			}
		}
		
		if (!foundNextTrace) {
			System.err.println("Couldn't find the next mergable trace from the current successors.");
			return;
		}
		
		merge(mergedTrace, tracesToBeMerged.get(nextSuccessor));
		
		tracesToBeMerged.remove(nextSuccessor);
	}

	private void merge(Trace mergedTrace, Trace nextTrace) {
		//
		// Combine addresses
		//
		for (int addr : nextTrace.addresses) {
			mergedTrace.addresses.add(addr);
		}
		
		// Remove all duplicate addresses
		Set<Integer> uniqueAddresses = new LinkedHashSet<Integer>();
		for (int addr : mergedTrace.addresses) {
			uniqueAddresses.add(addr);
		}
		mergedTrace.addresses.clear();
		for (int addr : uniqueAddresses) {
			mergedTrace.addresses.add(addr);
		}
		
		// Sort all the addresses
		//Collections.sort(mergedTrace.addresses);
				
		//
		// Combine successors
		//
		Set<Integer> newSuccessors = new HashSet<Integer>();
		for (int s : mergedTrace.successors) {
			if (!mergedTrace.containsCodeAddress(s)) {
				newSuccessors.add(s);
			}
		}
		for (int s : nextTrace.successors) {
			if (!mergedTrace.containsCodeAddress(s)) {
				newSuccessors.add(s);
			}
		}
		
		mergedTrace.successors = newSuccessors;	
	}
}
