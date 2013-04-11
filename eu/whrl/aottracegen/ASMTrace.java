package eu.whrl.aottracegen;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
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

	private int findPopInstruction() {
		int idx = -1;
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.startsWith("\tldm") || line.startsWith("\tpop")) {
				if (!line.equals("\tpop\t{r5, r6}")) {
					idx = cl;
				}
			}
		}
		return idx;
	}

	private int findPushInstruction() {
		int idx = -1;
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.startsWith("\tstm") || line.startsWith("\tpush")) {
				if (!line.equals("\tpush\t{r5, r6}")) {
					idx = cl;
				}
			}
		}
		return idx;
	}

	private void modifyOriginalProEpiCode(CodeGenContext context) {
		// Find the push/pop instructions,
		//

		int popIdx = findPopInstruction();
		int cl = 0;

		// Pop
		while (popIdx != -1) {
			String line = traceBody.get(popIdx);
			if (line.endsWith("pc}")) {
				cl = popIdx;
				cl = replaceLine(cl, "\tpop\t{r5, r6}");
			} else {
				cl = popIdx;
				cl = replaceLine(cl, "\tpop\t{r5, r6}");
				line = traceBody.get(cl);
				if (line.startsWith("\tbx\tlr")) {
					cl = removeLine(cl);
				}
			}
			cl = addLineAfter(cl,
					String.format("\tb\tLeave_T%d", context.currentRegionIndex));

			popIdx = findPopInstruction();
		}

		// Push
		int pushIdx = findPushInstruction();
		

		if (pushIdx != -1) {
			cl = pushIdx;
			cl = replaceLine(cl, "\tpush\t{r5, r6}");
		}
	}

	private void renameLabels(CodeGenContext context) {
		// Rename all the .L labels, so they don't clash if we're pasting them
		// together
		// with other asm traces to form an injectable trace.
		//
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.contains(".L")) {
				line = line.replaceAll(".L(\\d+)",
						String.format(".T%d_L$1", context.currentRegionIndex));
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
		// Replace all the bl's to exit/exception functions with b's to labels
		// we'll generate
		// in the injectable trace.
		//
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.contains("invoke_virtual_quick")) {
				cl = handleInvokeVirtualQuick(context, cl);
			} else if (line.contains("invoke_static")) {
				cl = handleInvokeStatic(context, cl);
			} else if (line.contains("execute_inline")) {
				cl = handleExecuteInline(context, cl);
			} else if (line.contains("single_step")) {
				cl = handleSingleStep(context, cl);
			}
			
		}
	}
	
	private int handleSingleStep(CodeGenContext context, int cl) {
		Trace curTrace = context.currentRegion.trace;
		String line = traceBody.get(cl);
		
		int thisOffset = 0;
		int nextOffset = 0;
		Pattern p = Pattern
				.compile("bl\tsingle_step_0x(.*)_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			thisOffset = Integer.parseInt(m.group(1), 16);
			nextOffset = Integer.parseInt(m.group(2), 16);
		}
		
		cl = removeLine(cl);
		
		int thisOffsetLPL = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.DPC_OFFSET, thisOffset);
		int nextOffsetLPL = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.DPC_OFFSET, nextOffset);
		
		
		cl = addLine(cl, "\t# Single stepping...");
		// preserve regs
		cl = addLine(cl, "\tpush\t{r0-r3}");
		
		cl = enterThumb2Mode(context, cl, thisOffset);
		
		// restore interp regs
		cl = addLine(cl, "\tmov\tr5, r1");
		cl = addLine(cl, "\tmov\tr6, r2");
		
		cl = addLine(cl,
				String.format("\tldr\tr1, [r0, #%d]", nextOffsetLPL * 4));
		cl = addLine(cl,
				String.format("\tldr\tr0, [r0, #%d]", thisOffsetLPL * 4));
		
		cl = addLine(cl, "\tldr\tr2, [r6, #112]");
		cl = addLine(cl, "\tblx\tr2");
		cl = addLine(cl, "\t# ...should return here.");
		
		cl = enterArmMode(context, cl, thisOffset);
		cl = addLine(cl, "\tpop\t{r0-r3}");
		
		return cl;
	}
	
	private int handleExecuteInline(CodeGenContext context, int cl) {
		Trace curTrace = context.currentRegion.trace;

		String line = traceBody.get(cl);

		int inlineIndex = 0;
		Pattern p = Pattern
				.compile("bl\texecute_inline_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			inlineIndex = Integer.parseInt(m.group(1), 16);
		}
		
		cl = removeLine(cl);
		
		int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.EXECUTE_INLINE, inlineIndex);
		cl = addLine(cl, String.format("\tadr\tr2, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr2, [r2, #%d]", literalPoolLoc * 4));
		cl = addLine(cl, "\tblx\tr2");
		
		return cl;
	}
	
	private int enterThumb2Mode(CodeGenContext context, int cl, int codeAddress) {
		if (context.config.armMode) {
			cl = addLine(cl, "# Must enter Thumb2 execution mode!");
			cl = addLine(cl, String.format(
					"\tadr\tr3, Invoke_ThumbCode_T%d_A%#x",
					context.currentRegionIndex, codeAddress));
			cl = addLine(cl, "\tadd\tr3, r3, #1");
			cl = addLine(cl, "\tbx\tr3");
			cl = addLine(cl, String.format("Invoke_ThumbCode_T%d_A%#x:",
					context.currentRegionIndex, codeAddress));
			cl = addLine(cl, "\t.thumb");
		}
		return cl;
	}
	
	private int enterArmMode(CodeGenContext context, int cl, int codeAddress) {
		if (context.config.armMode) {
			cl = addLine(cl, "# Must enter ARM execution mode!");
			cl = addLine(cl, String.format(
					"\tadr\tr3, Invoke_ARMCode_T%d_A%#x",
					context.currentRegionIndex, codeAddress));
			cl = addLine(cl, "\tbx\tr3");
			cl = addLine(cl, String.format("Invoke_ARMCode_T%d_A%#x:",
					context.currentRegionIndex, codeAddress));
			cl = addLine(cl, "\t.arm");
		}
		return cl;
	}

	private int handleInvokeVirtualQuick(CodeGenContext context, int cl) {
		Trace curTrace = context.currentRegion.trace;

		String line = traceBody.get(cl);

		int codeAddress = 0;
		Pattern p = Pattern
				.compile("bl\tinvoke_virtual_quick_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}

		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		
		// Remove the bl placeholder instruction
		//
		cl = removeLine(cl);

		cl = addLine(cl, "###");
		cl = addLine(cl, "### START INVOKE VIRTUAL QUICK ###");
		cl = addLine(cl, "###");

		cl = enterThumb2Mode(context, cl, codeAddress);

		// move interp-special regs back
		cl = addLine(cl, "\tmov\tr5, r1");
		cl = addLine(cl, "\tmov\tr6, r2");

		// Handle arguments
		//
		cl = handleArgumentLoading(context, cl, codeAddress);

		// Load DPC from literal pool into r4
		//
		int literalPoolLoc = 0;
		literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.DPC_OFFSET, codeAddress);

		cl = addLine(cl, String.format("\tadr.w\tr2, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr4, [r2, #%d]", literalPoolLoc * 4));

		// Load the address of the beginning of the next instruction into r1
		//
		cl = addLine(cl, String.format("\tadr.w\tr1, JumpAfter_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		// Load the address of the predicted chaining cell into r2
		//
		cl = addLine(cl, String.format("\tadr.w\tr2, ChainingCell_T%d_M%#x",
				context.currentRegionIndex, codeAddress));

		// Load the method predicted chain handler's address, blx to it
		//
		literalPoolLoc = curTrace.meta
				.addLiteralPoolType(LiteralPoolType.INVOKE_METHOD_PREDICTED_CHAIN_HANDLER);
		cl = addLine(cl, String.format("\tadr.w\tr3, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr3, [r3, #%d]", literalPoolLoc * 4));
		cl = addLine(cl, "\tblx\tr3");

		// Jump to the predicted chaining cell
		//
		cl = addLine(cl, String.format("\tb\tCCLP_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		// Jump to the exception handler
		//
		cl = addLine(cl, String.format("\tb\tEHLP_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		// Load vtable[methodIdx] into r0
		//
		cl = addLine(cl, String.format("\tldr\tr0, [r7, #%d]",
				((OdexedInvokeVirtual) instruction).getVtableIndex() * 4));

		cl = addLine(cl, String.format("\tb\tALP_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		cl = addLine(cl, String.format("CCLP_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));

		// Jump to the predicted chaining cell
		//
		cl = addLine(cl, String.format("\tb\tChainingCell_T%d_M%#x",
				context.currentRegionIndex, codeAddress));

		cl = addLine(cl, String.format("EHLP_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));

		// Jump to the exception handler
		//
		cl = addLine(cl, "\tmov\tr1, #0"); // 0 indicates exception must be
											// raised
		cl = addLine(cl, String.format("\tb\tJumpAfterBad_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		cl = addLine(cl, String.format("ALP_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));

		// Compare r1 to 0
		//
		cl = addLine(cl, "\tcmp\tr1, #0");

		// If gt, jump past this next function call
		//
		cl = addLine(cl, String.format("\tbgt\tJumpAfterChainCall_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		// load dvmJitToPatchPredictedChain pointer
		//
		literalPoolLoc = curTrace.meta
				.addLiteralPoolType(LiteralPoolType.JIT_TO_PATCH_PREDICTED_CHAIN_HANDLER);
		cl = addLine(cl, String.format("\tadr.w\tr7, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr7, [r7, #%d]", literalPoolLoc * 4));

		// Load self pointer into r1
		cl = addLine(cl, "\tmovs\tr1, r6");

		// blx to method
		//
		cl = addLine(cl, "\tblx\tr7");

		// Emit this label that we use to avoid the dvmJitToPatchPredictedChain
		// call
		//
		cl = addLine(cl, String.format("JumpAfterChainCall_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));

		// load address of next instruction into r1
		//
		cl = addLine(cl, String.format("\tadr.w\tr1, JumpAfter_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		// load the invoke method no opt handler, blx to it
		//
		literalPoolLoc = curTrace.meta
				.addLiteralPoolType(LiteralPoolType.INVOKE_METHOD_NO_OPT_HANDLER);
		cl = addLine(cl, String.format("\tadr.w\tr7, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr7, [r7, #%d]", literalPoolLoc * 4));
		cl = addLine(cl, "\tblx\tr7");

		// If there's an exception in the handler, we'll return here, so jump to
		// our exception handler.
		//
		cl = addLine(cl, "\tmov\tr1, #0"); // 0 indicates exception must be
											// raised
		cl = addLine(cl, String.format("\tb\tJumpAfterBad_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		cl = addLine(cl, "\t.align 4");

		// Emit this label that we use as the return point after function
		// invocation.
		//
		cl = addLine(cl, String.format("JumpAfter_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));
		cl = addLine(cl, "\tmov\tr1, #1"); // 1 indicates everything went okay

		cl = addLine(cl, String.format("JumpAfterBad_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));

		cl = enterArmMode(context, cl, codeAddress);
		cl = addLine(cl, "\tmov\tr0, r1");
		cl = addLine(cl, "###");
		cl = addLine(cl, "### END INVOKE VIRTUAL QUICK ###");
		cl = addLine(cl, "###");

		return cl;
	}

	private int handleInvokeStatic(CodeGenContext context, int cl) {
		Trace curTrace = context.currentRegion.trace;

		String line = traceBody.get(cl);

		int codeAddress = 0;
		Pattern p = Pattern.compile("bl\tinvoke_static_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}

		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		EncodedMethod method = Util.getCalleeMethodFromInvoke(instruction,
				context);

		// Remove the bl placeholder instruction
		//
		cl = removeLine(cl);

		cl = addLine(cl, "### START INVOKE STATIC ###");
		cl = addLine(cl, "# Save \"callee\"-save regs");
		cl = addLine(cl, "\tpush\t{r4-r11}");

		int literalPoolLoc = 0;
		if ((method.accessFlags & 0x100) != 0 /* native? */) {
			cl = addLine(cl, "\t# load and call aotInvokeStaticNative()");
			literalPoolLoc = curTrace.meta
					.addLiteralPoolType(LiteralPoolType.CALL_AOT_INVOKE_STATIC_NATIVE);
		} else {
			// can't happen just yet
			cl = addLine(cl, "\t# load and call aotInvokeStatic()");
		}
		cl = addLine(cl, String.format("\tadr\tr4, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr4, [r4, #%d]", literalPoolLoc * 4));
		cl = addLine(cl, "\tblx\tr4");

		cl = addLine(cl, "# Restore \"callee\"-save regs");
		cl = addLine(cl, "\tpop\t{r4-r11}");
		cl = addLine(cl, "### END INVOKE STATIC ###");

		return cl;
	}

	// to be removed
	private int handleInvokeStaticN(CodeGenContext context, int cl) {

		Trace curTrace = context.currentRegion.trace;

		String line = traceBody.get(cl);

		int codeAddress = 0;
		Pattern p = Pattern.compile("bl\tinvoke_static_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}

		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		EncodedMethod method = Util.getCalleeMethodFromInvoke(instruction,
				context);
		int methodIndex = ((InstructionWithReference) instruction)
				.getReferencedItem().getIndex();

		// Remove the bl placeholder instruction
		//
		cl = removeLine(cl);

		cl = addLine(cl, "###");
		cl = addLine(cl, "### START INVOKE STATIC ###");
		cl = addLine(cl, "###");

		cl = addLine(cl, "# Save caller-save regs");
		cl = addLine(cl, "\tpush\t{r4-r11}");

		cl = enterThumb2Mode(context, cl, codeAddress);

		// Handle arguments
		//
		cl = handleArgumentLoading(context, cl, codeAddress);

		// r0 = static method pointer
		//
		cl = addLine(cl, "# r0 = static method's pointer (resolved at runtime)");
		int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.STATIC_METHOD, methodIndex);
		cl = addLine(cl, String.format("\tadr.w\tr2, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr0, [r2, #%d]", literalPoolLoc * 4));

		// r1 = where we jump after this method has been invoked, within this
		// trace
		//
		cl = addLine(cl,
				"# r1 = label to jump to after method has been invoked");
		cl = addLine(cl, String.format("\tadr.w\tr1, JumpAfter_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		// r4 = callsite dPC
		//
		cl = addLine(cl, "# r4 = dalvik PC of call site");
		literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.DPC_OFFSET, codeAddress);
		cl = addLine(cl,
				String.format("\tldr\tr4, [r2, #%d]", literalPoolLoc * 4));

		if ((method.accessFlags & 0x100) != 0 /* native? */) {
			// r7 = # regs in callee method
			//
			cl = addLine(
					cl,
					"# r7 = # regs used by callee (assumed to be 0 currently when executing native. TBC)");
			cl = addLine(cl, "\tmov\tr7, #0");

			// load dvmJitToPatchPredictedChain pointer
			//
			cl = addLine(cl, "# Call TEMPLATE_INVOKE_METHOD_NATIVE");
			literalPoolLoc = curTrace.meta
					.addLiteralPoolType(LiteralPoolType.INVOKE_METHOD_NATIVE_HANDLER);
			cl = addLine(cl,
					String.format("\tldr\tr2, [r2, #%d]", literalPoolLoc * 4));
			// Jump to it
			cl = addLine(cl, "\tblx\tr2");

		} else {
			// r7 = # regs in callee method
			//
			cl = addLine(cl, "# r7 = #Êregs used by callee");
			cl = addLine(
					cl,
					String.format("\tmov\tr7, #%d",
							method.codeItem.getRegisterCount()));

			// load dvmJitToPatchPredictedChain pointer
			//
			cl = addLine(cl, "# Call TEMPLATE_INVOKE_METHOD_CHAIN");
			literalPoolLoc = curTrace.meta
					.addLiteralPoolType(LiteralPoolType.INVOKE_METHOD_CHAIN_HANDLER);
			cl = addLine(cl,
					String.format("\tldr\tr2, [r2, #%d]", literalPoolLoc * 4));
			// Jump to it
			cl = addLine(cl, "\tblx\tr2");

			// r2 = # outs in callee method
			//
			cl = addLine(cl, "# r2 = # outs in callee");
			cl = addLine(
					cl,
					String.format("\tmov\tr2, #%d",
							method.codeItem.getOutWords()));

			// Jump to the chaining cell
			cl = addLine(cl, "# Jump to the Invoke Singleton Chaining Cell");
			cl = addLine(cl, String.format(
					"\tadr.w\tr7, ChainingCell_T%d_M%#x",
					context.currentRegionIndex, methodIndex));
			cl = addLine(cl, "\tblx\tr7");
		}

		// If there's an exception in the handler, we'll return here, so jump to
		// our exception handler.
		//
		cl = addLine(cl, "# If we return to this point, there was an exception");
		cl = addLine(cl, "\tmov\tr1, #0"); // 0 indicates exception must be
											// raised
		cl = addLine(cl, String.format("\tb\tJumpAfterBad_T%d_A%#x",
				context.currentRegionIndex, codeAddress));

		cl = addLine(cl, "\t.align 4");

		// Emit this label that we use as the return point after function
		// invocation.
		//
		cl = addLine(cl, "# If we return to this point, everything went okay");
		cl = addLine(cl, String.format("JumpAfter_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));
		cl = addLine(cl, "\tmov\tr1, #1"); // 1 indicates everything went okay

		cl = addLine(cl, String.format("JumpAfterBad_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));

		cl = enterArmMode(context, cl, codeAddress);
		cl = addLine(cl, "\tmov\tr0, r1");

		cl = addLine(cl, "# Restore caller-save regs");
		cl = addLine(cl, "\tpop\t{r4-r11}");
		cl = addLine(cl, "###");
		cl = addLine(cl, "### END INVOKE STATIC ###");
		cl = addLine(cl, "###");

		return cl;

	}

	private int handleArgumentLoading(CodeGenContext context, int cl,
			int codeAddress) {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);

		cl = addLine(cl, "# begin arg loading");
		int regCount = ((FiveRegisterInstruction) instruction).getRegCount();

		if (regCount > 0) {
			cl = addLine(cl, String.format("\tldr\tr0, [r5, #%d]",
					((FiveRegisterInstruction) instruction).getRegisterD() * 4));
		}
		if (regCount > 1) {
			cl = addLine(cl, String.format("\tldr\tr1, [r5, #%d]",
					((FiveRegisterInstruction) instruction).getRegisterE() * 4));
		}
		if (regCount > 2) {
			cl = addLine(cl, String.format("\tldr\tr2, [r5, #%d]",
					((FiveRegisterInstruction) instruction).getRegisterF() * 4));
		}
		if (regCount > 3) {
			cl = addLine(cl, String.format("\tldr\tr3, [r5, #%d]",
					((FiveRegisterInstruction) instruction).getRegisterG() * 4));
		}
		if (regCount > 4) {
			cl = addLine(cl, String.format("\tldr\tr4, [r5, #%d]",
					((FiveRegisterInstruction) instruction).getRegisterA() * 4));
		}

		cl = addLine(
				cl,
				String.format(
						"\tsub\tr7, r5, #%d",
						20 /* size of StackSaveArea */+ ((FiveRegisterInstruction) instruction)
								.getRegCount() * 4));

		if (regCount > 0) {
			cl = addLine(cl, "\tcmp\tr0, #0");
			// normally we'd need to set r0 to 0 to indicate exception, luckily
			// r0 already contains 0 if this happens!
			cl = addLine(cl, String.format("\tbeq\tJumpAfterBad_T%d_A%#x",
					context.currentRegionIndex, codeAddress));

			String argsToPush = "";
			for (int i = 0; i < regCount; i++) {
				argsToPush += String.format("r%d", i);
				if (i != (regCount - 1)) {
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
		traceBody.add(cl + 1, line);

		return cl + 2;
	}
}
