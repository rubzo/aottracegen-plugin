package eu.whrl.aottracegen;

public class Config {
	public String clazz = "";
	public String method = "";
	public String signature = "";
	public int numTraces = 0;
	public int[] traceEntries = null;
	public boolean produceMerged = false;
	
	public void addEntry(int e) {
		numTraces++;
		int[] newTraceEntries = new int[numTraces];
		for (int i = 0; i < numTraces-1; i++) {
			newTraceEntries[i] = traceEntries[i];
		}
		newTraceEntries[numTraces-1] = e;
		traceEntries = newTraceEntries;
	}
}
