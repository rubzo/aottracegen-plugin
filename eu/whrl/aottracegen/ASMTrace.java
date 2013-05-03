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
		removeCBZ(context);
	}

	private void removeCBZ(CodeGenContext context) {
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.startsWith("\tcbz") || line.startsWith("\tcbnz")) {
				Pattern p = Pattern
						.compile("(cbz|cbnz)\tr(\\d+), (.*)$");
				Matcher m = p.matcher(line);
				if (m.find()) {
					cl = replaceLine(cl, String.format("\tcmp\tr%s, #0", m.group(2)));
					cl++;
					if (m.group(1).equals("cbz")) {
						cl = addLine(cl, String.format("\tbeq\t%s", m.group(3)));
					} else {
						cl = addLine(cl, String.format("\tbne\t%s", m.group(3)));
					}
				}
			}
		}
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

		cl = addLine(cl, "### START INVOKE VIRTUAL QUICK ###");
		cl = addLine(cl, "# Save \"callee\"-save regs");
		cl = addLine(cl, "\tpush\t{r4-r11}");
		
		cl = enterThumb2Mode(context, cl, codeAddress);

		cl = handleArgumentLoading(context, cl, codeAddress, true, "r1");

		// r5 now contains object, move it to r0
		cl = addLine(cl, "\tmov\tr0, r5");
		
		// move v
		cl = addLine(cl, "\tmov\tr3, r1");
		// move self
		cl = addLine(cl, "\tmov\tr4, r2");
		// load dPCoffset
		cl = addLine(cl, String.format("\tmov\tr1, #%d", codeAddress));
		// load vtable offset
		cl = addLine(cl, String.format("\tmov\tr2, #%d", ((OdexedInvokeVirtual) instruction).getVtableIndex()));
		// load &predictedChainingCell
		cl = addLine(cl, String.format(
				"\tadr.w\tr5, ChainingCell_T%d_M%#x",
				context.currentRegionIndex, codeAddress));
		
		int literalPoolLoc = 0;
		cl = addLine(cl, "\t# load and call aotVirtualQuick()");
		literalPoolLoc = curTrace.meta
				.addLiteralPoolType(LiteralPoolType.CALL_AOT_INVOKE_VIRTUAL_QUICK);

		cl = addLine(cl, String.format("\tadr.w\tr6, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr6, [r6, #%d]", literalPoolLoc * 4));
		cl = addLine(cl, "\tblx\tr6");
		
		cl = addLine(cl, String.format("\tb\tJumpAfter_T%d_A%#x",
				context.currentRegionIndex, codeAddress));
		
		cl = addLine(cl, String.format("RaiseException_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));
		
		cl = addLine(cl, "\tmov\tr0, #0");
		
		cl = addLine(cl, String.format("JumpAfter_T%d_A%#x:",
				context.currentRegionIndex, codeAddress));
		
		cl = enterArmMode(context, cl, codeAddress);

		cl = addLine(cl, "# Restore \"callee\"-save regs");
		cl = addLine(cl, "\tpop\t{r4-r11}");
		cl = addLine(cl, "### END INVOKE VIRTUAL QUICK ###");

		return cl;
	}

	private int handleInvokeVirtualQuickN(CodeGenContext context, int cl) {
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
		cl = handleArgumentLoading(context, cl, codeAddress, true, "r1");

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
		
		if ((method.accessFlags & 0x100) != 0 /* native? */) {
			return handleInvokeStaticNative(context, cl, codeAddress, method);
		} else {
			return handleInvokeStaticJava(context, cl, codeAddress, method);
		}
	}

	private int handleInvokeStaticJava(CodeGenContext context, int cl, int codeAddress, EncodedMethod method) {
		Trace curTrace = context.currentRegion.trace;

		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		int methodIndex = ((InstructionWithReference) instruction)
				.getReferencedItem().getIndex();

		// Remove the bl placeholder instruction
		//
		cl = removeLine(cl);

		cl = addLine(cl, "### START INVOKE STATIC (JAVA) ###");
		cl = addLine(cl, "# Save \"callee\"-save regs");
		cl = addLine(cl, "\tpush\t{r4-r11}");

		cl = handleArgumentLoading(context, cl, codeAddress, false, "r2");

		int literalPoolLoc = 0;
		cl = addLine(cl, "\t# load and call aotInvokeStatic()");
		literalPoolLoc = curTrace.meta
				.addLiteralPoolType(LiteralPoolType.CALL_AOT_INVOKE_STATIC_JAVA);
		cl = addLine(cl, String.format(
				"\tadr.w\tr4, ChainingCell_T%d_M%#x",
				context.currentRegionIndex, methodIndex));

		cl = addLine(cl, String.format("\tadr.w\tr5, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr5, [r5, #%d]", literalPoolLoc * 4));
		cl = addLine(cl, "\tblx\tr5");

		cl = addLine(cl, "# Restore \"callee\"-save regs");
		cl = addLine(cl, "\tpop\t{r4-r11}");
		cl = addLine(cl, "### END INVOKE STATIC (JAVA) ###");

		return cl;
	}
	
	private int handleInvokeStaticNative(CodeGenContext context, int cl, int codeAddress, EncodedMethod method) {
		Trace curTrace = context.currentRegion.trace;

		// Remove the bl placeholder instruction
		//
		cl = removeLine(cl);

		cl = addLine(cl, "### START INVOKE STATIC (NATIVE) ###");
		cl = addLine(cl, "# Save \"callee\"-save regs");
		cl = addLine(cl, "\tpush\t{r4-r11}");

		cl = handleArgumentLoading(context, cl, codeAddress, false, "r2");

		int literalPoolLoc = 0;
		cl = addLine(cl, "# load and call aotInvokeStaticNative()");
		literalPoolLoc = curTrace.meta
				.addLiteralPoolType(LiteralPoolType.CALL_AOT_INVOKE_STATIC_NATIVE);

		cl = addLine(cl, String.format("\tadr\tr5, LiteralPool_T%d",
				context.currentRegionIndex));
		cl = addLine(cl,
				String.format("\tldr\tr5, [r5, #%d]", literalPoolLoc * 4));
		cl = addLine(cl, "\tblx\tr5");

		cl = addLine(cl, "# Restore \"callee\"-save regs");
		cl = addLine(cl, "\tpop\t{r4-r11}");
		cl = addLine(cl, "### END INVOKE STATIC (NATIVE) ###");
		cl = addLine(cl, "# (success or EH check comes next...)");

		return cl;
	}

	private int handleArgumentLoading(CodeGenContext context, int cl,
			int codeAddress, boolean nullCheck, String fpReg) {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);

		cl = addLine(cl, "# begin arg loading");
		int regCount = ((FiveRegisterInstruction) instruction).getRegCount();

		if (regCount > 0) {
			cl = addLine(cl, String.format("\tldr\tr5, [%s, #%d]", fpReg,
					((FiveRegisterInstruction) instruction).getRegisterD() * 4));
		}
		if (regCount > 1) {
			cl = addLine(cl, String.format("\tldr\tr6, [%s, #%d]", fpReg,
					((FiveRegisterInstruction) instruction).getRegisterE() * 4));
		}
		if (regCount > 2) {
			cl = addLine(cl, String.format("\tldr\tr7, [%s, #%d]", fpReg,
					((FiveRegisterInstruction) instruction).getRegisterF() * 4));
		}
		if (regCount > 3) {
			cl = addLine(cl, String.format("\tldr\tr8, [%s, #%d]", fpReg,
					((FiveRegisterInstruction) instruction).getRegisterG() * 4));
		}
		if (regCount > 4) {
			cl = addLine(cl, String.format("\tldr\tr9, [%s, #%d]", fpReg,
					((FiveRegisterInstruction) instruction).getRegisterA() * 4));
		}

		cl = addLine(
				cl,
				String.format(
						"\tsub\tr10, %s, #%d",
						fpReg,
						20 /* size of StackSaveArea */+ ((FiveRegisterInstruction) instruction)
						.getRegCount() * 4));

		if (regCount > 0) {
			if (nullCheck) {
				cl = addLine(cl, "\tcmp\tr5, #0");
				cl = addLine(cl, String.format("\tbeq\tRaiseException_T%d_A%#x",
						context.currentRegionIndex, codeAddress));
			}

			String argsToPush = "";
			for (int i = 0; i < regCount; i++) {
				argsToPush += String.format("r%d", i + 5);
				if (i != (regCount - 1)) {
					argsToPush += ",";
				}
			}

			cl = addLine(cl, String.format("\tstmia\tr10, {%s}", argsToPush));
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
