package eu.whrl.aottracegen;

import java.util.Arrays;
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
		Trace curTrace = context.getCurrentTrace();
		int curTraceEntry = curTrace.getPrimaryEntry();
		
		// Find the push/pop instructions,
		// as well as where the exit label is (just before pop)
		//
		int pushIdx = 0;
		int popIdx = 0;
		String exitLabel = "";

		for (int i = 0; i < traceBody.size(); i++) {
			String line = traceBody.get(i);
			
			if (line.startsWith("\tpop\t{") || line.startsWith("\tldmfd\t")) {
				// The exit label must be the line before...
				exitLabel = traceBody.get(i-1);
				// Cut off the :
				exitLabel = exitLabel.substring(0, exitLabel.length() - 1);
				
				popIdx = i;
			} else if (line.startsWith("\tpush\t{") || line.startsWith("\tstmfd\t")) {
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
				line = line.replaceAll(".L(\\d+)", String.format(".LT0x%x_$1", curTraceEntry));
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
				i = handleException(context, i);
			} else if (line.contains("bl\texit")) {
				i = handleExit(context, i);
			} else if (line.contains("bl\treturn")) {
				i = handleReturn(context, i);
			}
		}
		
		// Remove redundant labels
		//
		// NB: don't do this for traces containing switches just now - some teething troubles need ironed out.
		if (!curTrace.meta.containsSwitch) {
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
		
		// Find all clobbered registers
		//
		Set<Integer> clobberedRegs = new HashSet<Integer>();
		for (int i = 0; i < traceBody.size(); i++) {
			String line = traceBody.get(i);
			
			if (line.contains("r4")) {
				clobberedRegs.add(new Integer(4));
			} 
			if (line.contains("r5")) {
				clobberedRegs.add(new Integer(5));
			} 
			if (line.contains("r6")) {
				clobberedRegs.add(new Integer(6));
			} 
			if (line.contains("r7")) {
				clobberedRegs.add(new Integer(7));
			} 
			if (line.contains("r8")) {
				clobberedRegs.add(new Integer(8));
			} 
		}		
		if (!clobberedRegs.isEmpty()) {
			curTrace.meta.hasClobberedRegisters = true;
			curTrace.meta.clobberedRegisters = new int[clobberedRegs.size()];
			
			int i = 0;
			for (Integer elem : clobberedRegs) {
				curTrace.meta.clobberedRegisters[i] = elem.intValue();
				i++;
			}
			
			Arrays.sort(curTrace.meta.clobberedRegisters);
		}
	}
	
	private int handleExit(CodeGenContext context, int currentLineIdx) {
		Trace curTrace = context.getCurrentTrace();
		int curTraceEntry = curTrace.getPrimaryEntry();
		
		String line = traceBody.get(currentLineIdx);
		
		line = line.replaceFirst("\tbl\texit_L(.+)", String.format("\tb\tLT0x%x_CC_$1", curTraceEntry));
		
		traceBody.remove(currentLineIdx);
		traceBody.add(currentLineIdx, line);
		
		return currentLineIdx;
	}
	
	private int handleException(CodeGenContext context, int currentLineIdx) {
		Trace curTrace = context.getCurrentTrace();
		int curTraceEntry = curTrace.getPrimaryEntry();
		
		String line = traceBody.get(currentLineIdx);
		
		line = line.replaceFirst("\tbl\texception_L(.+)", String.format("\tb\tLT0x%x_EH_$1", curTraceEntry));
		
		traceBody.remove(currentLineIdx);
		traceBody.add(currentLineIdx, line);
		
		return currentLineIdx;
	}
	
	private int handleReturn(CodeGenContext context, int currentLineIdx) {
		return currentLineIdx;
	}
}
