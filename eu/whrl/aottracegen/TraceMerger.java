package eu.whrl.aottracegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import eu.whrl.aottracegen.exceptions.TraceMergingException;

public class TraceMerger {

	public void mergeTraces(Region region, Config config) throws TraceMergingException {
		
		TraceFinder traceFinder = new TraceFinder();
		
		Map<Integer,Trace> traceMap = new HashMap<Integer, Trace>();
		
		traceFinder.generateTracesFromCodeAddress(region, traceMap, region.entryOffset);
		
		Trace initialTrace = traceMap.get(region.entryOffset);
		
		for (int mergeOffset : region.merges) {
			
			traceFinder.generateTracesFromCodeAddress(region, traceMap, mergeOffset);
			
			Trace traceToBeMerged = traceMap.get(mergeOffset);
			
			if (traceToBeMerged == null) {
				System.out.println("Trying to find " + mergeOffset);
				System.out.println(traceMap.toString());
				throw new TraceMergingException();
			}
			
			merge(initialTrace, traceToBeMerged);
		}
		
		region.trace = initialTrace;
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
