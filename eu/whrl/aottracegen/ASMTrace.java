package eu.whrl.aottracegen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ASMTrace {
	private List<String> traceBody;
	
	public String getFullStringTraceBody() {
		String result = "";
		for (String line : traceBody) {
			result += line + "\n";
		}
		return result;
	}
	
	public void setTraceBody(List<String> traceBody) {
		this.traceBody = traceBody;
	}
	
	/*
	 * Modifies the loaded traceBody, doing the following:
	 * 
	 * - remove push/pop instructions
	 * - remove any branches to the original exit label
	 * - rename all line labels to be unique when output with other traces
	 * - replace all bl's to exit/exception functions with b's to our exit/exception labels
	 * - remove any redundant labels
	 */
	public void cleanupTrace(CodeGenContext context) {
		// Find the push/pop instructions,
		// as well as where the exit label is (just before pop)
		//
		int pushIdx = 0;
		int popIdx = 0;
		String exitLabel = "";
		
		Trace curTrace = context.getCurrentTrace();
		int curTraceEntry = curTrace.entries[0];
		
		for (int i = 0; i < traceBody.size(); i++) {
			String line = traceBody.get(i);
			
			if (line.equals("\tpop\t{r4, pc}")) {
				// The exit label must be the line before...
				exitLabel = traceBody.get(i-1);
				// Cut off the :
				exitLabel = exitLabel.substring(0, exitLabel.length() - 1);
				
				popIdx = i;
			} else if (line.equals("\tpush\t{r4, lr}")) {
				pushIdx = i;
			}
	
		}
		
		// Remove push/pop
		//
		traceBody.remove(pushIdx);
		traceBody.remove(popIdx-1);
		
		// Remove any branches to the exit label
		//
		String branchToExit = "\tb\t" + exitLabel;
		for (int i = 0; i < traceBody.size(); i++) {
			String line = traceBody.get(i);
			
			if (line.equals(branchToExit)) {
				traceBody.remove(i);
				i--;
			}
		}
		
		// Rename all the .L labels, so they don't clash if we're pasting them together
		// with other asm traces to form an injectable trace.
		//
		for (int i = 0; i < traceBody.size(); i++) {
			String line = traceBody.get(i);
			
			if (line.contains(".L")) {
				line = line.replaceFirst(".L(\\d+)", String.format(".LT0x%x_$1", curTraceEntry));
			}
			
			traceBody.remove(i);
			traceBody.add(i, line);
		}
		
		// Replace all the bl's to exit/exception functions with b's to labels we'll generate
		// in the injectable trace.
		//
		for (int i = 0; i < traceBody.size(); i++) {
			String line = traceBody.get(i);
			
			if (line.contains("bl\texception")) {
				line = line.replaceFirst("\tbl\texception_L(.+)", String.format("\tb\tLT0x%x_EH_$1", curTraceEntry));
			} else if (line.contains("bl\texit")) {
				line = line.replaceFirst("\tbl\texit_L(.+)", String.format("\tb\tLT0x%x_CC_$1", curTraceEntry));
			}
			
			traceBody.remove(i);
			traceBody.add(i, line);
		}
		
		// Remove redundant labels
		//
		Set<String> referencedLabels = new HashSet<String>();
		for (int i = 0; i < traceBody.size(); i++) {
			String line = traceBody.get(i);
			
			if (line.matches(".*\\.L.*[^:]$")) {
				Pattern p = Pattern.compile(".*(\\.L.*)$");
				Matcher m = p.matcher(line);
				if (m.find()) {
					referencedLabels.add(m.group(1));
				}
			}
		}
		for (int i = 0; i < traceBody.size(); i++) {
			String line = traceBody.get(i);
			
			if (line.matches("^\\.L.*:$") && !referencedLabels.contains(line.substring(0, line.length() - 1))) {
				traceBody.remove(i);
				i--;
			}
		}
	}
}
