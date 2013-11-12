package eu.whrl.aottracegen;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.OdexedInvokeVirtual;
import org.jf.dexlib.Code.Analysis.ClassPath;
import org.jf.dexlib.Code.Analysis.DeodexUtil;
import org.jf.dexlib.Code.Format.Format;
import org.jf.dexlib.Code.Format.Instruction35ms;

import eu.whrl.aottracegen.armgen.InstGen;

public class ASMTrace {
	enum ConditionCode {
		EQ,
		NE,
		CS,
		CC,
		MI,
		PL,
		VS,
		VC,
		HI,
		LS,
		GE,
		LT,
		GT,
		LE,
		AL
	}
	
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
		InstGen.setTraceBody(traceBody);
	}

	/*
	 * Modifies the loaded traceBody
	 */
	public void cleanupTrace(CodeGenContext context) {
		modifyOriginalProEpiCode(context);
		renameLabels(context);
		generalCleanup(context);
		emitHandlers(context);
		if (!context.config.armMode && context.config.enableRemoveCBZs) {
			removeCBZ(context);
		}
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
	
	private ConditionCode lineContainsConditionCode(String line, String instruction) {
		
		for (ConditionCode cc : ConditionCode.values()) {
			if (line.startsWith(cc.toString().toLowerCase(), instruction.length())) {
				return cc;
			}
		}
		
		return ConditionCode.AL;
	}
	
	private int findPopInstruction() {
		int idx = -1;
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.startsWith("\tldm") || line.startsWith("\tpop")) {
				if (!line.contains("{r5, r6}")) {
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
			
			// Check if instructions we're replacing contain condition codes
			ConditionCode cc = ConditionCode.AL;
			if (context.config.armMode) {
				if (line.startsWith("\tpop")) {
					cc = lineContainsConditionCode(line, "\tpop");
				} else if (line.startsWith("\tldm")) {
					cc = lineContainsConditionCode(line, "\tldm");
				}
			}
			String conditionCodeSuffix = "";
			if (cc != ConditionCode.AL) {
				conditionCodeSuffix = cc.toString().toLowerCase();
			}
			
			// Does this pop instruction actually branch as well, by popping the stored LR onto the PC reg?
			if (line.endsWith("pc}")) {
				cl = popIdx;
				cl = replaceLine(cl, String.format("\tpop%s\t{r5, r6}", conditionCodeSuffix));
			} else {
				cl = popIdx;
				cl = replaceLine(cl, String.format("\tpop%s\t{r5, r6}", conditionCodeSuffix));
				line = traceBody.get(cl);
				if (line.startsWith("\tbx\tlr")) {
					cl = removeLine(cl);
				}
			}
			cl = addLineAfter(cl,
					String.format("\tb%s\tLeave_T%d", conditionCodeSuffix, context.currentRegionIndex));

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
		// together with other asm traces to form an injectable trace.
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
			} else if (line.contains("invoke_interface")) {
				cl = handleInvokeInterface(context, cl);
			} else if (line.contains("invoke_singleton")) {
				cl = handleInvokeSingleton(context, cl);
			} else if (line.contains("execute_inline")) {
				cl = handleExecuteInline(context, cl);
			} else if (line.contains("single_step")) {
				cl = handleSingleStep(context, cl);
			} else if (line.contains("instanceof")) {
				cl = handleInstanceof(context, cl);
			} else if (line.contains("new_instance")) {
				cl = handleNewInstance(context, cl);
			} else if (line.contains("barrier")) {
				cl = handleBarrier(context, cl);
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
		
		int thisOffsetLPL = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.DPC_OFFSET, thisOffset);
		int nextOffsetLPL = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.DPC_OFFSET, nextOffset);
		
		InstGen.setLinePtr(cl);
		
		InstGen.removeCurrentLine();
		
		InstGen.insertComment("Single stepping...");
		InstGen.insertComment("Save callee-save regs");
		
		InstGen.stackPush("{r4-r11}");
		
		InstGen.insertComment("Restore interpreter regs");
		InstGen.copyRegister("r5", "r1");
		InstGen.copyRegister("r6", "r2");
		
		InstGen.insertComment("Load dPC for this bytecode and the next");
		InstGen.memoryRead("r1", "r0", nextOffsetLPL * 4);
		InstGen.memoryRead("r0", "r0", thisOffsetLPL * 4);
		
		InstGen.insertComment("Jump to dvmJitToInterpSingleStep");
		InstGen.memoryRead("r2", "r6", 112 /* dvmJitToInterpSingleStep */);
		InstGen.jumpToReg("r2");
		InstGen.insertComment("...should return here.");
		
		InstGen.insertComment("Restore callee-save regs");
		InstGen.stackPop("{r4-r11}");

		return InstGen.getLinePtr();
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
		
		int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.EXECUTE_INLINE, inlineIndex);
		
		InstGen.setLinePtr(cl);
		
		InstGen.removeCurrentLine();
		
		InstGen.insertComment("load and call execute-inline");
		InstGen.loadLabel("r2", String.format("LiteralPool_T%d", context.currentRegionIndex));
		InstGen.memoryRead("r2", "r2", literalPoolLoc * 4);
		InstGen.jumpToReg("r2");

		return InstGen.getLinePtr();
	}
	
	private int handleInstanceof(CodeGenContext context, int cl) {
		InstGen.setLinePtr(cl);
		
		InstGen.removeCurrentLine();
		
		/* C code ensures the following: */
		/* r0 contains reference, already null-checked */
		/* r1 contains class pointer */
		
		/* call dvmInstanceofNonTrivial */
		
		/* function expects r0 to actually be r0->clazz, so get it */
		InstGen.memoryRead("r0", "r0", 0);
		
		/* note this is a C function, so it will be do the callee-saved regs saving */
		InstGen.jumpToFunction(context, "r2", LiteralPoolType.CALL_INSTANCEOF_NON_TRIVIAL, "dvmInstanceofNonTrivial");
		
		/* result will be returned in r0, C code expects this, so carry on! */
		return InstGen.getLinePtr();
	}
	
	private int handleBarrier(CodeGenContext context, int cl) {
		InstGen.setLinePtr(cl);
		InstGen.removeCurrentLine();
		InstGen.addMemoryBarrier();
		return InstGen.getLinePtr();
	}
	
	private int handleNewInstance(CodeGenContext context, int cl) {
		InstGen.setLinePtr(cl);
		
		InstGen.removeCurrentLine();
		
		/* C code ensures the following: */
		/* r0 contains class pointer, already null-checked */
		/* r1 contains ALLOC_DONT_TRACK (1) */
		
		/* call dvmAllocObject */
		/* note this is a C function, so it will be do the callee-saved regs saving */
		InstGen.jumpToFunction(context, "r2", LiteralPoolType.CALL_ALLOC_OBJECT, "dvmAllocObject");
		
		/* result will be returned in r0, C code expects this, so carry on! */
		return InstGen.getLinePtr();
	}
	
	private int handleInvokeInterface(CodeGenContext context, int cl) {
		String line = traceBody.get(cl);

		int codeAddress = 0;
		Pattern p = Pattern
				.compile("bl\tinvoke_interface_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}
		
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);

		InstGen.setLinePtr(cl);
		
		// Remove the bl placeholder instruction
		//
		InstGen.removeCurrentLine();

		InstGen.insertComment("START INVOKE INTERFACE");
		InstGen.insertComment("Save callee-save regs");
		InstGen.stackPush("{r4-r11}");

		handleArgumentLoading(context, codeAddress, true, "r1");
		
		// move v
		InstGen.copyRegister("r3", "r1");
		// move self
		InstGen.copyRegister("r4", "r2");
		// load dPCoffset
		InstGen.loadConstant("r1", codeAddress);
		// load vB (method reference)
		InstGen.loadConstant("r2", ((InstructionWithReference) instruction).getReferencedItem().getIndex());
		// load &predictedChainingCell
		InstGen.loadLabel("r5", String.format("ChainingCell_T%d_M%#x", context.currentRegionIndex, codeAddress));
		
		InstGen.jumpToFunction(context, "r6", LiteralPoolType.CALL_AOT_INVOKE_INTERFACE, "dvmCompiler_AOT_INVOKE_INTERFACE");
		
		InstGen.jumpToLabel(String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress));

		InstGen.insertLabel(String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));

		InstGen.loadConstant("r0", 0);

		InstGen.insertLabel(String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress));

		InstGen.insertComment("Restore callee-save regs");
		InstGen.stackPop("{r4-r11}");
		InstGen.insertComment("END INVOKE INTERFACE");
		
		return InstGen.getLinePtr();
	}
	
	private int handleInvokeVirtualQuick(CodeGenContext context, int cl) {
		String line = traceBody.get(cl);

		int codeAddress = 0;
		Pattern p = Pattern
				.compile("bl\tinvoke_virtual_quick_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}

		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);

		InstGen.setLinePtr(cl);
		
		// Remove the bl placeholder instruction
		//
		InstGen.removeCurrentLine();

		InstGen.insertComment("START INVOKE VIRTUAL QUICK");
		InstGen.insertComment("Save callee-save regs");
		InstGen.stackPush("{r4-r11}");

		handleArgumentLoading(context, codeAddress, true, "r1");
		
		// r5 now contains object, move it to r0
		InstGen.copyRegister("r0", "r5");
		// move v
		InstGen.copyRegister("r3", "r1");
		// move self
		InstGen.copyRegister("r4", "r2");
		// load dPCoffset
		InstGen.loadConstant("r1", codeAddress);
		// load vtable offset
		InstGen.loadConstant("r2", ((OdexedInvokeVirtual)instruction).getVtableIndex());
		// load &predictedChainingCell
		InstGen.loadLabel("r5", String.format("ChainingCell_T%d_M%#x", context.currentRegionIndex, codeAddress));
		
		InstGen.jumpToFunction(context, "r6", LiteralPoolType.CALL_AOT_INVOKE_VIRTUAL_QUICK, "dvmCompiler_AOT_INVOKE_VIRTUAL_QUICK");
		
		InstGen.jumpToLabel(String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress));
		
		InstGen.insertLabel(String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));

		InstGen.loadConstant("r0", 0);

		InstGen.insertLabel(String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress));

		InstGen.insertComment("Restore callee-save regs");
		InstGen.stackPop("{r4-r11}");
		InstGen.insertComment("END INVOKE VIRTUAL QUICK");
		
		return InstGen.getLinePtr();
	}
	
	private int handleInvokeSingleton(CodeGenContext context, int cl) {
		String line = traceBody.get(cl);
		
		int codeAddress = 0;
		boolean nullCheckArgs = false;
		Pattern p = Pattern.compile("bl\tinvoke_singleton_(.*)_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(2), 16);
			if (m.group(1) == "nullcheck") {
				nullCheckArgs = true;
			}
		} else {
			System.out.println("Failure to parse invoke_singleton label in .S file. Investigate.");
		}

		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		
		boolean isInvokeSuperQuick = false;
		EncodedMethod method = null;
		if (instruction.getFormat() == Format.Format35ms) {
			isInvokeSuperQuick = true;
		}
		
		if (isInvokeSuperQuick) {
			method = MethodLookup.getMethodLookup().getSuperQuickMethodFromInstruction(instruction, context); 
		} else {
			method = MethodLookup.getMethodLookup().getCalleeMethodFromInstruction(instruction,
					context);
		}
		
		if ((method.accessFlags & 0x100) != 0 /* native? */) {
			return handleInvokeSingletonNative(context, cl, codeAddress, method, nullCheckArgs);
		} else {
			return handleInvokeSingletonJava(context, cl, codeAddress, method, nullCheckArgs);
		}
	}

	private int handleInvokeSingletonJava(CodeGenContext context, int cl, int codeAddress, EncodedMethod method, boolean nullCheckArgs) {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		int methodIndex = 0;
		String vtablePrefix = "";
		if (instruction.getFormat() == Format.Format35ms) {
			methodIndex = ((Instruction35ms) instruction).getVtableIndex();
			vtablePrefix = "V";
		} else {
			methodIndex = ((InstructionWithReference) instruction)
					.getReferencedItem().getIndex();
		}

		InstGen.setLinePtr(cl);
		
		// Remove the bl placeholder instruction
		//
		InstGen.removeCurrentLine();

		InstGen.insertComment("START INVOKE SINGLETON (JAVA)");
		InstGen.insertComment("Save callee-save regs");
		InstGen.stackPush("{r4-r11}");
		
		handleArgumentLoading(context, codeAddress, nullCheckArgs, "r2");

		InstGen.loadLabel("r4", String.format("ChainingCell_T%d_M%s%#x", context.currentRegionIndex, vtablePrefix, methodIndex));
		
		InstGen.jumpToFunction(context, "r5", LiteralPoolType.CALL_AOT_INVOKE_SINGLETON_JAVA, "dvmCompiler_AOT_INVOKE_SINGLETON_JAVA");

		InstGen.insertComment("Restore callee-save regs");
		InstGen.stackPop("{r4-r11}");
		InstGen.insertComment("END INVOKE SINGLETON (JAVA)");
		
		return InstGen.getLinePtr();
	}
	
	private int handleInvokeSingletonNative(CodeGenContext context, int cl, int codeAddress, EncodedMethod method, boolean nullCheckArgs) {
		InstGen.setLinePtr(cl);
		
		// Remove the bl placeholder instruction
		//
		InstGen.removeCurrentLine();

		InstGen.insertComment("START INVOKE SINGLETON (NATIVE)");
		InstGen.insertComment("Save callee-save regs");
		InstGen.stackPush("{r4-r11}");
	
		handleArgumentLoading(context, codeAddress, nullCheckArgs, "r2");

		InstGen.jumpToFunction(context, "r5", LiteralPoolType.CALL_AOT_INVOKE_SINGLETON_NATIVE, "dvmCompiler_AOT_INVOKE_SINGLETON_NATIVE");
		
		InstGen.insertComment("Restore callee-save regs");
		InstGen.stackPop("{r4-r11}");
		InstGen.insertComment("END INVOKE SINGLETON (NATIVE)");
		
		return InstGen.getLinePtr();
	}

	private void handleArgumentLoading(CodeGenContext context, int codeAddress, boolean nullCheck, String fpReg) {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);

		InstGen.insertComment("Begin argument loading");
		int regCount = ((FiveRegisterInstruction) instruction).getRegCount();

		if (regCount > 0) {
			InstGen.memoryRead("r5", fpReg, ((FiveRegisterInstruction) instruction).getRegisterD() * 4);
		}
		if (regCount > 1) {
			InstGen.memoryRead("r6", fpReg, ((FiveRegisterInstruction) instruction).getRegisterE() * 4);
		}
		if (regCount > 2) {
			InstGen.memoryRead("r7", fpReg, ((FiveRegisterInstruction) instruction).getRegisterF() * 4);
		}
		if (regCount > 3) {
			InstGen.memoryRead("r8", fpReg, ((FiveRegisterInstruction) instruction).getRegisterG() * 4);
		}
		if (regCount > 4) {
			InstGen.memoryRead("r9", fpReg, ((FiveRegisterInstruction) instruction).getRegisterA() * 4);
		}

		InstGen.doMath("sub", "r10", fpReg, 20 /* size of StackSaveArea */+ ((FiveRegisterInstruction) instruction).getRegCount() * 4);

		if (regCount > 0) {
			if (nullCheck) {
				InstGen.insertComment("null-check (method is not static)");
				InstGen.doComparisonAndJump("eq", "r5", 0, String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));
			}

			String argsToPush = "{";
			for (int i = 0; i < regCount; i++) {
				argsToPush += String.format("r%d", i + 5);
				if (i != (regCount - 1)) {
					argsToPush += ",";
				}
			}
			argsToPush += "}";

			InstGen.memoryWriteMultiple("r10", argsToPush);
		}
		InstGen.insertComment("Finish argument loading");
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
