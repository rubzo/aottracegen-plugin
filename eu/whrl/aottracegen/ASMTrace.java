package eu.whrl.aottracegen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OdexedInvokeVirtual;

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
		
		// Find the push/pop instructions,
		// as well as where the exit label is (just before pop)
		//
		int pushIdx = 0;
		int popIdx = 0;
		String exitLabel = "";

		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);
			
			if (line.startsWith("\tpop\t{") || line.startsWith("\tldmfd\t")) {
				// The exit label must be the line before...
				exitLabel = traceBody.get(cl-1);
				// Cut off the :
				exitLabel = exitLabel.substring(0, exitLabel.length() - 1);
				
				popIdx = cl;
			} else if (line.startsWith("\tpush\t{") || line.startsWith("\tstmfd\t")) {
				pushIdx = cl;
			}
	
		}
		
		// Remove push/pop
		//
		traceBody.remove(pushIdx);
		traceBody.remove(popIdx-1);
		
		// Remove any branches to the exit label
		//
		String branchToExit = "\tb\t" + exitLabel;
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);
			
			if (line.equals(branchToExit)) {
				traceBody.remove(cl);
				cl--;
			}
		}
		
		// Rename all the .L labels, so they don't clash if we're pasting them together
		// with other asm traces to form an injectable trace.
		//
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);
			
			if (line.contains(".L")) {
				line = line.replaceAll(".L(\\d+)", String.format(".LT%#x_$1", curTrace.entry));
			}
			
			traceBody.remove(cl);
			traceBody.add(cl, line);
		}
		
		// Find all clobbered registers
		//
		Set<Integer> clobberedRegs = new HashSet<Integer>();
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.contains("r4")) {
				clobberedRegs.add(4);
			} 
			if (line.contains("r5")) {
				clobberedRegs.add(5);
			} 
			if (line.contains("r6")) {
				clobberedRegs.add(6);
			} 
			if (line.contains("r7")) {
				clobberedRegs.add(7);
			} 
			if (line.contains("r8")) {
				clobberedRegs.add(8);
			} 
		}		
		if (!clobberedRegs.isEmpty()) {
			curTrace.meta.hasClobberedRegisters = true;
			curTrace.meta.clobberedRegisters = new int[clobberedRegs.size()];

			int i = 0;
			for (int reg : clobberedRegs) {
				curTrace.meta.clobberedRegisters[i] = reg;
				i++;
			}

			Arrays.sort(curTrace.meta.clobberedRegisters);
		}
		
		// Remove #APP and @ signs - general cleanup, basically.
		// 
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);
			
			if (line.startsWith("#APP")) {
				traceBody.remove(cl);
				cl--;
			} else if (line.startsWith("@")) {
				traceBody.remove(cl);
				cl--;
			} else if (line.contains(".thumb")) {
				traceBody.remove(cl);
				cl--;
			}
		}

		// Replace all the bl's to exit/exception functions with b's to labels we'll generate
		// in the injectable trace.
		//
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);
			
			if (line.contains("bl\texception")) {
				cl = handleException(context, cl);
			} else if (line.contains("bl\texit")) {
				cl = handleExit(context, cl);
			} else if (line.contains("bl\treturn")) {
				cl = handleReturn(context, cl);
			} else if (line.contains("# invoke_virtual_quick")) {
				cl = handleInvokeVirtualQuick(context, cl);
			}
		}
		
		// Remove redundant labels
		//
		// NB: don't do this for traces containing switches just now - some teething troubles need ironed out.
		if (!curTrace.meta.containsSwitch) {
			Set<String> referencedLabels = new HashSet<String>();
			for (int cl = 0; cl < traceBody.size(); cl++) {
				String line = traceBody.get(cl);

				if (line.matches(".*\\.L.*[^:]$")) {
					Pattern p = Pattern.compile(".*(\\.L.*)$");
					Matcher m = p.matcher(line);
					if (m.find()) {
						referencedLabels.add(m.group(1));
					}
				}
			}
			for (int cl = 0; cl < traceBody.size(); cl++) {
				String line = traceBody.get(cl);

				if (line.matches("^\\.L.*:$") && !referencedLabels.contains(line.substring(0, line.length() - 1))) {
					traceBody.remove(cl);
					cl--;
				}
			}
		}		
	}
	
	private int handleExit(CodeGenContext context, int cl) {
		Trace curTrace = context.getCurrentTrace();
		
		cl = replaceLine(cl, traceBody.get(cl).replaceFirst("\tbl\texit_L(.+)", String.format("\tb\tLT%#x_CC_$1", curTrace.entry)));
		
		return cl;
	}
	
	private int handleException(CodeGenContext context, int cl) {
		Trace curTrace = context.getCurrentTrace();
		
		cl = replaceLine(cl, traceBody.get(cl).replaceFirst("\tbl\texception_L(.+)", String.format("\tb\tLT%#x_EH_$1", curTrace.entry)));
		
		return cl;
	}
	
	private int handleReturn(CodeGenContext context, int cl) {
		Trace curTrace = context.getCurrentTrace();
		
		// If we add an entry to the literal pool, then the return 
		// handler will be loaded into this literal pool location.
		//
		int literalPoolLoc = curTrace.meta.literalPoolSize;
		curTrace.meta.addLiteralPoolEntry(LiteralPoolType.RETURN_HANDLER, 0);
		
		// Create the jump to the return handler
		cl = addLine(cl, String.format("\tadr.w\tr2, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr0, [r2, #%d]", literalPoolLoc*4));
		cl = addLine(cl, "\tblx\tr0");
		
		
		cl = replaceLine(cl, traceBody.get(cl).replaceFirst("\tbl\treturn_L(.+)", String.format("\tb\tLT%#x_EH_$1", curTrace.entry)));
		
		return cl;
	}
	
	private int handleInvokeVirtualQuick(CodeGenContext context, int cl) {
		Trace curTrace = context.getCurrentTrace();
		
		String line = traceBody.get(cl);
		
		int codeAddress = 0;
		Pattern p = Pattern.compile("# invoke_virtual_quick_L0x(.*)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}
		
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		// Remove the bl placeholder instruction
		//
		cl = removeLine(cl);
		
		// Handle arguments
		//
		cl = handleArgumentLoading(context, cl, codeAddress);
		
		// Load DPC from literal pool into r4
		//
		int literalPoolLoc = curTrace.meta.literalPoolSize;
		curTrace.meta.addLiteralPoolEntry(LiteralPoolType.DPC_OFFSET, codeAddress);
		cl = addLine(cl, String.format("\tadr.w\tr2, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr4, [r2, #%d]", literalPoolLoc*4));
		
		// Load the address of the beginning of the next instruction into r1
		//
		cl = addLine(cl, String.format("\tadr.w\tr1, JumpAfter_%#x", codeAddress));
		
		// Load the address of the predicted chaining cell into r2
		//
		cl = addLine(cl, String.format("\tadr.w\tr2, LT%#x_CC_%#x", curTrace.entry, codeAddress));
		
		// Load the method predicted chain handler's address, blx to it
		//
		literalPoolLoc = curTrace.meta.literalPoolSize;
		curTrace.meta.addLiteralPoolEntry(LiteralPoolType.INVOKE_METHOD_PREDICTED_CHAIN_HANDLER, 0);
		cl = addLine(cl, String.format("\tadr.w\tr3, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr3, [r3, #%d]", literalPoolLoc*4));
		cl = addLine(cl, "\tblx\tr3");
		
		// Jump to the predicted chaining cell
		//
		cl = addLine(cl, String.format("\tb\tLT%#x_CC_%#x", curTrace.entry, codeAddress));
		
		// Jump to the exception handler
		//
		cl = addLine(cl, String.format("\tb\tLT%#x_EH_%#x", curTrace.entry, codeAddress));
		
		// Load vtable[methodIdx] into r0
		//
		cl = addLine(cl, String.format("\tldr\tr0, [r7, #%d]", ((OdexedInvokeVirtual)instruction).getVtableIndex() * 4));
		
		// Compare r1 to 0
		//
		cl = addLine(cl, "\tcmp\tr1, #0");
		
		// If gt, jump past this next function call
		//
		cl = addLine(cl, String.format("\tbgt\tJumpAfterChainCall_%#x", codeAddress));
		
		// load dvmJitToPatchPredictedChain pointer
		//
		literalPoolLoc = curTrace.meta.literalPoolSize;
		curTrace.meta.addLiteralPoolEntry(LiteralPoolType.JIT_TO_PATCH_PREDICTED_CHAIN_HANDLER, 0);
		cl = addLine(cl, String.format("\tadr.w\tr7, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr7, [r7, #%d]", literalPoolLoc*4));
		
		// Load self pointer into r1
		cl = addLine(cl, "\tmovs\tr1, r6");
		
		// blx to method
		//
		cl = addLine(cl, "\tblx\tr7");
		
		// Emit this label that we use to avoid the dvmJitToPatchPredictedChain call
		//
		cl = addLine(cl, String.format("JumpAfterChainCall_%#x:", codeAddress));
		
		// load address of next instruction into r1
		//
		cl = addLine(cl, String.format("\tadr.w\tr1, JumpAfter_%#x", codeAddress));
		
		// load the invoke method no opt handler, blx to it
		//
		literalPoolLoc = curTrace.meta.literalPoolSize;
		curTrace.meta.addLiteralPoolEntry(LiteralPoolType.INVOKE_METHOD_NO_OPT_HANDLER, 0);
		cl = addLine(cl, String.format("\tadr.w\tr7, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr7, [r7, #%d]", literalPoolLoc*4));
		cl = addLine(cl, "\tblx\tr7");
		
		// If there's an exception in the handler, we'll return here, so jump to our exception handler.
		//
		cl = addLine(cl, String.format("\tb\tLT%#x_EH_%#x", curTrace.entry, codeAddress));
		
		// Emit this label that we use as the return point after function invocation.
		//
		cl = addLine(cl, String.format("JumpAfter_%#x:", codeAddress));
		
		// Finally, restore the trace function's arguments.
		//
		cl = addLine(cl, "\tmov\tr0, r5");
		cl = addLine(cl, "\tmov\tr1, r6");
		if (curTrace.meta.literalPoolSize > 0) {
			cl = addLine(cl, String.format("\tadr.w\tr2, ITrace_%#x_LiteralPool", curTrace.entry));
		}
		
		return cl;
	}
	
	private int handleArgumentLoading(CodeGenContext context, int cl, int codeAddress) {
		Trace curTrace = context.getCurrentTrace();
		
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		cl = addLine(cl, "  # begin arg loading");
		
		int regCount = ((FiveRegisterInstruction)instruction).getRegCount();
		
		if (regCount > 0) {
			cl = addLine(cl, String.format("\tldr\tr0, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterD()*4 ) );
		}
		if (regCount > 1) {
			cl = addLine(cl, String.format("\tldr\tr1, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterE()*4 ) );
		}
		if (regCount > 2) {
			cl = addLine(cl, String.format("\tldr\tr2, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterF()*4 ) );
		}
		if (regCount > 3) {
			cl = addLine(cl, String.format("\tldr\tr3, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterG()*4 ) );
		}
		if (regCount > 4) {
			cl = addLine(cl, String.format("\tldr\tr4, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterA()*4 ) );
		}
		
		cl = addLine(cl, String.format("\tsub\tr7, r5, #%d", 20 /*size of StackSaveArea*/ + ((FiveRegisterInstruction)instruction).getRegCount()*4));
		
		if (regCount > 0) {
			cl = addLine(cl, "\tcmp\tr0, #0");
			
			cl = addLine(cl, String.format("\tbeq\tLT%#x_EH_%#x", curTrace.entry, codeAddress));
			
			String argsToPush = "";
			for (int i = 0; i < regCount; i++) {
				argsToPush += String.format("r%d",i);
				if (i != (regCount-1)) {
					argsToPush += ",";
				}
			}
			
			cl = addLine(cl, String.format("\tstmia\tr7, {%s}", argsToPush));
		}
		cl = addLine(cl, "  # end arg loading");
		
		return cl;
	}
	
	private int removeLine(int cl) {
		traceBody.remove(cl);
		
		return cl;
	}
	
	private int replaceLine(int cl, String line) {
		traceBody.remove(cl);
		traceBody.add(cl, line);
		
		return cl;
	}
	
	private int addLine(int cl, String line) {
		traceBody.add(cl, line);
		
		return cl + 1;
	}
}
