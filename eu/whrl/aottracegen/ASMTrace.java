package eu.whrl.aottracegen;

import java.util.List;
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
	 * Modifies the loaded traceBody
	 */
	public void cleanupTrace(CodeGenContext context) {
		modifyOriginalProEpiCode(context);		
		renameLabels(context);
		generalCleanup(context);
		emitHandlers(context);
	}
	
	private void modifyOriginalProEpiCode(CodeGenContext context) {
		// Find the push/pop instructions,
		//
		int pushIdx = 0;
		int popIdx = 0;
		int cl = 0;
		
		for (cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);
			
			if (line.startsWith("\tldmfd") || line.startsWith("\tpop")) {
				popIdx = cl;
			}
		}
		
		cl = popIdx;
		cl = replaceLine(cl, "\tpop\t{r5, r6}");
		cl = addLineAfter(cl, String.format("\tb\tLeave_T%d", context.currentTraceIdx));
		if (cl != traceBody.size()) {
			String line = traceBody.get(cl);
			if (line.startsWith("\tbx\tlr")) {
				cl = removeLine(cl);
			}
		}
		
		for (cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);
			
			if (line.startsWith("\tpush") || line.startsWith("\tstmfd")) {
				pushIdx = cl;
			}
		}
		
		cl = pushIdx;
		cl = replaceLine(cl, "\tpush\t{r5, r6}");
	}
	
	private void renameLabels(CodeGenContext context) {
		// Rename all the .L labels, so they don't clash if we're pasting them together
		// with other asm traces to form an injectable trace.
		//
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.contains(".L")) {
				line = line.replaceAll(".L(\\d+)", String.format(".T%d_L$1", context.currentTraceIdx));
			}

			traceBody.remove(cl);
			traceBody.add(cl, line);
		}
	}
	
	private void generalCleanup(CodeGenContext context) {
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
	}
	
	private void emitHandlers(CodeGenContext context) {
		// Replace all the bl's to exit/exception functions with b's to labels we'll generate
		// in the injectable trace.
		//
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.contains("invoke_virtual_quick")) {
				cl = handleInvokeVirtualQuick(context, cl);
			} else if (line.contains("invoke_static")) {
				cl = handleInvokeStatic(context, cl);
			}
		}
	}
	
	private int handleInvokeVirtualQuick(CodeGenContext context, int cl) {
		Trace curTrace = context.getCurrentTrace();
		
		String line = traceBody.get(cl);
		
		int codeAddress = 0;
		Pattern p = Pattern.compile("bl\tinvoke_virtual_quick_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}
		
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		// Remove the bl placeholder instruction
		//
		cl = removeLine(cl);
		
		cl = addLine(cl, "###");
		cl = addLine(cl, "### START INVOKE VIRTUAL QUICK ###");
		cl = addLine(cl, "###");
		
		if (context.config.armMode) {
			cl = addLine(cl, "# Must enter Thumb2 execution mode!");
			cl = addLine(cl, String.format("\tadr\tr0, Invoke_ThumbCode_T%d_A%#x", context.currentTraceIdx, codeAddress));
			cl = addLine(cl, "\tadd\tr0, r0, #1");
			cl = addLine(cl, "\tbx\tr0");
			cl = addLine(cl, String.format("Invoke_ThumbCode_T%d_A%#x:", context.currentTraceIdx, codeAddress));
			cl = addLine(cl, "\t.thumb");
		}
		
		// Handle arguments
		//
		cl = handleArgumentLoading(context, cl, codeAddress);
		
		// Load DPC from literal pool into r4
		//
		int literalPoolLoc = 0;
		literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(LiteralPoolType.DPC_OFFSET, codeAddress);
		
		cl = addLine(cl, String.format("\tadr.w\tr2, LiteralPool_T%d", context.currentTraceIdx));
		cl = addLine(cl, String.format("\tldr\tr4, [r2, #%d]", literalPoolLoc*4));
		
		// Load the address of the beginning of the next instruction into r1
		//
		cl = addLine(cl, String.format("\tadr.w\tr1, JumpAfter_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		// Load the address of the predicted chaining cell into r2
		//
		cl = addLine(cl, String.format("\tadr.w\tr2, ChainingCell_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		// Load the method predicted chain handler's address, blx to it
		//
		literalPoolLoc = curTrace.meta.addLiteralPoolType(LiteralPoolType.INVOKE_METHOD_PREDICTED_CHAIN_HANDLER);
		cl = addLine(cl, String.format("\tadr.w\tr3, LiteralPool_T%d", context.currentTraceIdx));
		cl = addLine(cl, String.format("\tldr\tr3, [r3, #%d]", literalPoolLoc*4));
		cl = addLine(cl, "\tblx\tr3");
		
		// Jump to the predicted chaining cell
		//
		cl = addLine(cl, String.format("\tb\tCCLP_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		// Jump to the exception handler
		//
		cl = addLine(cl, String.format("\tb\tEHLP_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		// Load vtable[methodIdx] into r0
		//
		cl = addLine(cl, String.format("\tldr\tr0, [r7, #%d]", ((OdexedInvokeVirtual)instruction).getVtableIndex() * 4));
		
		cl = addLine(cl, String.format("\tb\tALP_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		cl = addLine(cl, String.format("CCLP_T%d_A%#x:", context.currentTraceIdx, codeAddress));
				
		// Jump to the predicted chaining cell
		//
		cl = addLine(cl, String.format("\tb\tChainingCell_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		cl = addLine(cl, String.format("EHLP_T%d_A%#x:", context.currentTraceIdx, codeAddress));
		
		// Jump to the exception handler
		//
		cl = addLine(cl, "\tmov\tr1, #0"); // 0 indicates exception must be raised
		cl = addLine(cl, String.format("\tb\tJumpAfterBad_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		cl = addLine(cl, String.format("ALP_T%d_A%#x:", context.currentTraceIdx, codeAddress));
		
		
		// Compare r1 to 0
		//
		cl = addLine(cl, "\tcmp\tr1, #0");
		
		// If gt, jump past this next function call
		//
		cl = addLine(cl, String.format("\tbgt\tJumpAfterChainCall_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		// load dvmJitToPatchPredictedChain pointer
		//
		literalPoolLoc = curTrace.meta.addLiteralPoolType(LiteralPoolType.JIT_TO_PATCH_PREDICTED_CHAIN_HANDLER);
		cl = addLine(cl, String.format("\tadr.w\tr7, LiteralPool_T%d", context.currentTraceIdx));
		cl = addLine(cl, String.format("\tldr\tr7, [r7, #%d]", literalPoolLoc*4));
		
		// Load self pointer into r1
		cl = addLine(cl, "\tmovs\tr1, r6");
		
		// blx to method
		//
		cl = addLine(cl, "\tblx\tr7");
		
		// Emit this label that we use to avoid the dvmJitToPatchPredictedChain call
		//
		cl = addLine(cl, String.format("JumpAfterChainCall_T%d_A%#x:", context.currentTraceIdx, codeAddress));
		
		// load address of next instruction into r1
		//
		cl = addLine(cl, String.format("\tadr.w\tr1, JumpAfter_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		// load the invoke method no opt handler, blx to it
		//
		literalPoolLoc = curTrace.meta.addLiteralPoolType(LiteralPoolType.INVOKE_METHOD_NO_OPT_HANDLER);		
		cl = addLine(cl, String.format("\tadr.w\tr7, LiteralPool_T%d", context.currentTraceIdx));
		cl = addLine(cl, String.format("\tldr\tr7, [r7, #%d]", literalPoolLoc*4));
		cl = addLine(cl, "\tblx\tr7");
		
		// If there's an exception in the handler, we'll return here, so jump to our exception handler.
		//
		cl = addLine(cl, "\tmov\tr1, #0"); // 0 indicates exception must be raised
		cl = addLine(cl, String.format("\tb\tJumpAfterBad_T%d_A%#x", context.currentTraceIdx, codeAddress));
		
		cl = addLine(cl, "\t.align 4");
		
		// Emit this label that we use as the return point after function invocation.
		//
		cl = addLine(cl, String.format("JumpAfter_T%d_A%#x:", context.currentTraceIdx, codeAddress));
		cl = addLine(cl, "\tmov\tr1, #1"); // 1 indicates everything went okay
		
		cl = addLine(cl, String.format("JumpAfterBad_T%d_A%#x:", context.currentTraceIdx, codeAddress));
		
		if (context.config.armMode) {
			cl = addLine(cl, "# Must enter ARM execution mode!");
			cl = addLine(cl, String.format("\tadr\tr0, Invoke_ARMCode_T%d_A%#x", context.currentTraceIdx, codeAddress));
			cl = addLine(cl, "\tbx\tr0");
			cl = addLine(cl, String.format("Invoke_ARMCode_T%d_A%#x:", context.currentTraceIdx, codeAddress));
			cl = addLine(cl, "\t.arm");
		}
		cl = addLine(cl, "\tmov\tr0, r1");
		cl = addLine(cl, "###");
		cl = addLine(cl, "### END INVOKE VIRTUAL QUICK ###");
		cl = addLine(cl, "###");
		
		return cl;
	}
	
	private int handleInvokeStatic(CodeGenContext context, int cl) {
		
		Trace curTrace = context.getCurrentTrace();
		
		String line = traceBody.get(cl);
		
		int codeAddress = 0;
		Pattern p = Pattern.compile("bl\tinvoke_static_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}
		
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		// Remove the bl placeholder instruction
		//
		cl = removeLine(cl);
		
		cl = addLine(cl, "###");
		cl = addLine(cl, "### START INVOKE STATIC ###");
		cl = addLine(cl, "###");
		cl = addLine(cl, "   #....");
		cl = addLine(cl, "###");
		cl = addLine(cl, "### END INVOKE STATIC ###");
		cl = addLine(cl, "###");
		
		return cl;
		
	}
	
	private int handleArgumentLoading(CodeGenContext context, int cl, int codeAddress) {
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		cl = addLine(cl, "# begin arg loading");
		cl = addLine(cl, "\tmov\tr5, r1");
		
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
			// normally we'd need to set r0 to 0 to indicate exception, luckily r0 already contains 0 if this happens!
			cl = addLine(cl, String.format("\tbeq\tJumpAfterBad_T%d_A%#x", context.currentTraceIdx, codeAddress)); 
			
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
	
	private int addLineAfter(int cl, String line) {
		traceBody.add(cl+1, line);
		
		return cl + 2;
	}
}
