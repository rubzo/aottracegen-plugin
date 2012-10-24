package eu.whrl.aottracegen;

/*
 * Config represents the trace selection config we've passed in as an argument to the plugin.
 */
public class Config {
	public String app = "";
	public String clazz = "";
	public String method = "";
	public String signature = "";
	public int numTraces = 0;
	public int[] traceEntries = null;
	public boolean produceMerged = false;
	public boolean traceAll = false;
	public boolean produceUnsafe = false;
	
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
