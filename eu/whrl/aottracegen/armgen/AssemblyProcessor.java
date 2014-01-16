package eu.whrl.aottracegen.armgen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.InvokeInstruction;
import org.jf.dexlib.Code.OdexedInvokeVirtual;
import org.jf.dexlib.Code.RegisterRangeInstruction;
import org.jf.dexlib.Code.Format.Format;
import org.jf.dexlib.Code.Format.Instruction35ms;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.LiteralPoolType;
import eu.whrl.aottracegen.MethodLookup;
import eu.whrl.aottracegen.Trace;
import eu.whrl.aottracegen.armgen.insts.*;

public class AssemblyProcessor {
	
	private ArmInstOpMultiple findPopInstruction(ArmInst insts) {
		for (ArmInst inst : insts) {
			ArmOpcode opcode = inst.getOpcode();
			if (opcode == ArmOpcode.ldm || opcode == ArmOpcode.pop) {
				ArmInstOpMultiple op = (ArmInstOpMultiple) inst;
				if (op.registers.size() != 3 || op.registers.get(0) != ArmRegister.r2 || 
						op.registers.get(1) != ArmRegister.r5 || 
						op.registers.get(2) != ArmRegister.r6) {
					return op;
				}
			}
		}
		return null;
	}
	
	private ArmInstOpMultiple findPushInstruction(ArmInst insts) {
		for (ArmInst inst : insts) {
			ArmOpcode opcode = inst.getOpcode();
			if (opcode == ArmOpcode.stm || opcode == ArmOpcode.push) {
				ArmInstOpMultiple op = (ArmInstOpMultiple) inst;
				if (op.registers.size() != 3 || op.registers.get(0) != ArmRegister.r0 ||
						op.registers.get(1) != ArmRegister.r1 ||
						op.registers.get(2) != ArmRegister.r2) {
					return op;
				}
			}
		}
		return null;
	}
	
	/*
	 * Returns a pointer to the first ArmInst that makes up the constant pool, if it finds one.
	 */
	public ArmInst containsConstantPool(CodeGenContext context, ArmInst insts) {
		ArmInst constantPoolStart = null;
		
		boolean investigating = false;
		int currentSize = 0;
		
		final int minimumConstantPoolSize = 16;
		
		for (ArmInst inst : insts) {
			if (inst instanceof ArmInstPseudoDirectiveSingleArg && !investigating) {
				ArmInstPseudoDirectiveSingleArg directive = (ArmInstPseudoDirectiveSingleArg) inst;
				
				if (directive.name.equals("word")) {
					investigating = true;
					currentSize = 1;
					constantPoolStart = inst;
				}
			} else if (inst instanceof ArmInstPseudoDirectiveSingleArg && investigating) {
				ArmInstPseudoDirectiveSingleArg directive = (ArmInstPseudoDirectiveSingleArg) inst;
				
				if (directive.name.equals("word")) {
					currentSize++;
				} else {
					if (currentSize >= minimumConstantPoolSize) {
						return constantPoolStart;
					} else {
						investigating = false;
						currentSize = 0;
					}
				}
			} else if (investigating) {
				if (currentSize >= minimumConstantPoolSize) {
					return constantPoolStart;
				} else {
					investigating = false;
					currentSize = 0;
				}
			}
		}
		
		if (investigating && currentSize >= minimumConstantPoolSize) {
			return constantPoolStart;
		}
		
		return null;
	}
	
	public ArmInst getEndOfConstantPool(ArmInst constantPoolStart) {
		for (ArmInst inst : constantPoolStart) {
			if (inst instanceof ArmInstPseudoDirectiveSingleArg) {
				ArmInstPseudoDirectiveSingleArg directive = (ArmInstPseudoDirectiveSingleArg) inst;
				
				if (!directive.name.equals("word")) {
					return inst;
				}
			} else {
				return inst;
			}
		}
		
		return null;
	}
	
	public ArmInst findAndFixConstantPools(CodeGenContext context, ArmInst insts) {
		
		boolean searching = true;
		
		ArmInst currentHead = insts;
		
		while (searching) {
			ArmInst potentialConstantPool = containsConstantPool(context, currentHead);
		
			if (potentialConstantPool != null) {
				/* for now, just label the constant pools */
				ArmInstComment comment = new ArmInstComment("A constant pool");
				potentialConstantPool.insertBefore(comment);
				currentHead = getEndOfConstantPool(potentialConstantPool);
				if (currentHead == null) {
					searching = false;
				}
			} else {
				searching = false;
			}
		}
		
		return insts;
	}
	
	public ArmInst fixupTableBranchLabels(CodeGenContext context, ArmInst insts) {
		
		boolean doFixup = false;
		boolean foundTable = false;
		
		String regionPrefix = "T" + context.currentRegionIndex + "_";
		
		for (ArmInst inst : insts) {
			ArmOpcode opcode = inst.getOpcode();
			if ((opcode == ArmOpcode.tbb || opcode == ArmOpcode.tbh) && !doFixup) {
				doFixup = true;
			} else if (doFixup && !foundTable) {
				if (inst instanceof ArmInstPseudoDirectiveSingleArg) {
					ArmInstPseudoDirectiveSingleArg directive = (ArmInstPseudoDirectiveSingleArg) inst;
					if (directive.name.equals("byte") || directive.name.equals("2byte")) {
						foundTable = true;
					}
				}
			} 
			
			if (foundTable) {
				if (inst instanceof ArmInstPseudoDirectiveSingleArg) {
					ArmInstPseudoDirectiveSingleArg directive = (ArmInstPseudoDirectiveSingleArg) inst;
					if (directive.name.equals("byte") || directive.name.equals("2byte")) {
						directive.arg = directive.arg.replaceAll("L", regionPrefix + "L");
					} else {
						doFixup = false;
						foundTable = false;
					}
				}
			}
			
		}
		
		return insts;
	}
	
	public ArmInst fixupEntryAndExits(CodeGenContext context, ArmInst insts) {
		boolean foundAnExit = false;
		
		/* deal with the potentially multiple pop instructions */
		ArmInstOpMultiple popInst = findPopInstruction(insts);
		while (popInst != null) {
			
			/* remove the following bx lr instruction, if it exists */
			if (!popInst.usesRegister(ArmRegister.pc)) {
				if (popInst.next != null && popInst.next.getOpcode() == ArmOpcode.bx) {
					popInst.removeNext();
				}
			}
			
			/* pop only r5, r6 */
			popInst.registers.clear();
			popInst.addRegister(ArmRegister.r2);
			popInst.addRegister(ArmRegister.r5);
			popInst.addRegister(ArmRegister.r6);
			
			/* add branch to our exit code */
			ArmInstOpL leaveInst = new ArmInstOpL("b", "Leave_T" + context.currentRegionIndex);
			if (popInst.cc != ArmConditionCode.al) {
				/* pass on CC */
				leaveInst.cc = popInst.cc;
				popInst.cc = ArmConditionCode.al;
			}
			popInst.insertAfter(leaveInst);
			
			foundAnExit = true;

			/* there might be another pop instruction, look for it */
			popInst = findPopInstruction(insts);
		}
		
		if (!foundAnExit) {
			for (ArmInst inst : insts) {
				ArmOpcode opcode = inst.getOpcode();
				if (opcode == ArmOpcode.bx) {
					ArmInstOpR exitInst = (ArmInstOpR) inst;
					if (exitInst.reg == ArmRegister.lr) {
						ArmInstOpMultiple newInstStart = new ArmInstOpMultiple("pop");
						newInstStart.addRegister(ArmRegister.r2);
						newInstStart.addRegister(ArmRegister.r5);
						newInstStart.addRegister(ArmRegister.r6);
						
						ArmInstOpL newInstEnd = new ArmInstOpL("b", "Leave_T" + context.currentRegionIndex);
						
						newInstStart.linkToNext(newInstEnd);
						
						inst.replaceChain(newInstStart, newInstEnd);
						foundAnExit = true;
					} else {
						System.err.println("Found a bx instruction that doesn't use lr?");
						System.exit(1);
					}
				}
			}
		}
		
		if (!foundAnExit) {
			System.err.println("Failed to find any exits from Region " + context.currentRegionIndex);
			System.exit(1);
		}

		/* deal with the single push instruction at the entry */
		ArmInstOpMultiple pushInst = findPushInstruction(insts);
		
		if (pushInst != null) {
			pushInst.registers.clear();
			pushInst.addRegister(ArmRegister.r0);
			pushInst.addRegister(ArmRegister.r1);
			pushInst.addRegister(ArmRegister.r2);
		} else {
			/* there were no push insts at the start of the trace, so put our one for r5+r6 in */
			ArmInstOpMultiple newPushInst = new ArmInstOpMultiple("push");
			newPushInst.addRegister(ArmRegister.r0);
			newPushInst.addRegister(ArmRegister.r1);
			newPushInst.addRegister(ArmRegister.r2);
			insts.insertBefore(newPushInst);
			insts = newPushInst;
		}
		
		return insts;
	}
	
	public ArmInst renameLabels(CodeGenContext context, ArmInst insts) {
		/* put T<region index> on the front of all labels, so they don't clash if we output multiple regions */
		String regionPrefix = "T" + context.currentRegionIndex + "_";
		for (ArmInst inst : insts) {
			if (inst instanceof IArmInstHasLabel) {
				ArmLabelReference label = ((IArmInstHasLabel)inst).getLabel();
				if (label.isLocal()) {
					label.rename(regionPrefix + label.getLabelNameOnly());
				}
			}
		}
		
		return insts;
	}
	
	public ArmInst removeCBZ(CodeGenContext context, ArmInst insts) {
		/* find cbz and cbnzs, and replace whem with a cmp and b */
		for (ArmInst inst : insts) {
			ArmOpcode opcode = inst.getOpcode();
			if (opcode == ArmOpcode.cbz || opcode == ArmOpcode.cbnz) {
				/* get arguments to original cb inst */
				ArmInstOpRL conditionalBranchInst = (ArmInstOpRL) inst;
				ArmRegister reg = conditionalBranchInst.reg;
				ArmLabelReference label = conditionalBranchInst.label;
				
				/* construct new insts */
				// TODO: use InstGen class
				ArmInstOpRI compareInst = new ArmInstOpRI("cmp", reg, 0);
				ArmInstOpL branchInst = new ArmInstOpL("b", label);
				if (opcode == ArmOpcode.cbz) {
					branchInst.cc = ArmConditionCode.eq;
				} else {
					branchInst.cc = ArmConditionCode.ne;
				}
				
				/* chain them, and insert them into the inst list */
				compareInst.linkToNext(branchInst);
				inst.replaceChain(compareInst, branchInst);
			}
		}
		
		return insts;
	}
	
	public ArmInst emitFunctionCalls(CodeGenContext context, ArmInst insts) {
		for (ArmInst inst : insts) {
			ArmOpcode opcode = inst.getOpcode();
			if (opcode == ArmOpcode.bl) {
				ArmInstOpL branchInst = (ArmInstOpL) inst;
				String dest = branchInst.label.getLabelAsString();
				if (dest.contains("invoke_virtual_quick")) {
					handleInvokeVirtualQuick(context, branchInst);
				} else if (dest.contains("invoke_interface")) {
					handleInvokeInterface(context, branchInst);
				} else if (dest.contains("invoke_singleton")) {
					handleInvokeSingleton(context, branchInst);
				} else if (dest.contains("execute_inline")) {
					handleExecuteInline(context, branchInst);
				} else if (dest.contains("single_step")) {
					handleSingleStep(context, branchInst);
				} else if (dest.contains("instanceof")) {
					handleInstanceof(context, branchInst);
				} else if (dest.contains("new_instance")) {
					handleNewInstance(context, branchInst);
				} else if (dest.contains("new_array")) {
					handleNewArray(context, branchInst);
				} else if (dest.contains("barrier")) {
					handleBarrier(context, branchInst);
				} else {
					System.out.println("Unhandled bl label: " + dest);
				}
				
			}
		}
		
		return insts;
	}
	
	private void handleSingleStep(CodeGenContext context, ArmInstOpL inst) {
		String label = inst.label.getLabelAsString();
		Trace curTrace = context.currentRegion.trace;
		int thisOffset = 0;
		int nextOffset = 0;
		Pattern p = Pattern
				.compile("single_step_0x(.*)_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(label);
		if (m.find()) {
			thisOffset = Integer.parseInt(m.group(1), 16);
			nextOffset = Integer.parseInt(m.group(2), 16);
		}

		int thisOffsetLPL = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.DPC_OFFSET, thisOffset);
		int nextOffsetLPL = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.DPC_OFFSET, nextOffset);

		InstGen gen = new InstGen();

		gen.insertComment("Single stepping...");
		gen.insertComment("Save callee-save regs");

		gen.stackPush(ArmRegister.r4, ArmRegister.r5, ArmRegister.r6, 
				ArmRegister.r7, ArmRegister.r8, ArmRegister.r9, 
				ArmRegister.r10, ArmRegister.r11);

		gen.insertComment("Restore interpreter regs");
		gen.copyRegister(ArmRegister.r5, ArmRegister.r6);
		gen.copyRegister(ArmRegister.r6, ArmRegister.r2);

		gen.insertComment("Load dPC for this bytecode and the next");
		gen.memoryRead(ArmRegister.r1, ArmRegister.r0, nextOffsetLPL * 4);
		gen.memoryRead(ArmRegister.r0, ArmRegister.r0, thisOffsetLPL * 4);

		gen.insertComment("Jump to dvmJitToInterpSingleStep");
		gen.memoryRead(ArmRegister.r2, ArmRegister.r6, 112);
		gen.jumpToReg(ArmRegister.r2);
		gen.insertComment("...should return here.");

		gen.insertComment("Restore callee-save regs");
		gen.stackPop(ArmRegister.r4, ArmRegister.r5, ArmRegister.r6, 
				ArmRegister.r7, ArmRegister.r8, ArmRegister.r9, 
				ArmRegister.r10, ArmRegister.r11);

		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleNewInstance(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.jumpToFunction(context, ArmRegister.r2, LiteralPoolType.CALL_ALLOC_OBJECT, "dvmAllocObject");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleNewArray(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.jumpToFunction(context, ArmRegister.r3, LiteralPoolType.CALL_ALLOC_ARRAY_BY_CLASS, "dvmAllocArrayByClass");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleExecuteInline(CodeGenContext context, ArmInstOpL inst) {
		String label = inst.label.getLabelAsString();
		Trace curTrace = context.currentRegion.trace;
		
		int inlineIndex = 0;
		Pattern p = Pattern
				.compile("execute_inline_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(label);
		if (m.find()) {
			inlineIndex = Integer.parseInt(m.group(1), 16);
		}
		
		int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.EXECUTE_INLINE, inlineIndex);
		
		InstGen gen = new InstGen();
		gen.insertComment("load and call execute-inline");
		gen.loadLabel(ArmRegister.r2, String.format("LiteralPool_T%d", context.currentRegionIndex));
		gen.memoryRead(ArmRegister.r2, ArmRegister.r2, literalPoolLoc * 4);
		gen.jumpToReg(ArmRegister.r2);
		
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleInstanceof(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.memoryRead(ArmRegister.r0, ArmRegister.r0, 0);
		gen.jumpToFunction(context, ArmRegister.r2, LiteralPoolType.CALL_INSTANCEOF_NON_TRIVIAL, "dvmInstanceofNonTrivial");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleBarrier(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.addMemoryBarrier();
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}

	private void handleInvokeInterface(CodeGenContext context, ArmInstOpL inst) {
		String label = inst.label.getLabelAsString();

		int codeAddress = 0;
		Pattern p = Pattern
				.compile("invoke_interface_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(label);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}

		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);

		InstGen gen = new InstGen();

		gen.insertComment("START INVOKE INTERFACE");
		gen.insertComment("Save callee-save regs");
		gen.calleeSavePush();

		handleArgumentLoading(gen, context, codeAddress, true, ArmRegister.r1);

		// move v
		gen.copyRegister(ArmRegister.r3, ArmRegister.r1);
		// move self
		gen.copyRegister(ArmRegister.r4, ArmRegister.r2);
		// load dPCoffset
		gen.loadConstant(ArmRegister.r1, codeAddress);
		// load vB (method reference)
		gen.loadConstant(ArmRegister.r2, ((InstructionWithReference) instruction).getReferencedItem().getIndex());
		// load &predictedChainingCell
		gen.loadLabel(ArmRegister.r5, String.format("ChainingCell_T%d_M%#x", context.currentRegionIndex, codeAddress));

		gen.jumpToFunction(context, ArmRegister.r6, LiteralPoolType.CALL_AOT_INVOKE_INTERFACE, "dvmCompiler_AOT_INVOKE_INTERFACE");

		gen.jumpToLabel(String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress));

		gen.insertLabel(String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));

		gen.loadConstant(ArmRegister.r0, 0);

		gen.insertLabel(String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress));

		gen.insertComment("Restore callee-save regs");
		gen.calleeSavePop();
		gen.insertComment("END INVOKE INTERFACE");

		inst.replaceChain(gen.getFirst(), gen.getLast());
	}

	private void handleInvokeVirtualQuick(CodeGenContext context, ArmInstOpL inst) {
		String label = inst.label.getLabelAsString();

		int codeAddress = 0;
		Pattern p = Pattern
				.compile("invoke_virtual_quick_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(label);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}

		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);

		InstGen gen = new InstGen();

		gen.insertComment("START INVOKE VIRTUAL QUICK");
		gen.insertComment("Save callee-save regs");
		gen.calleeSavePush();

		handleArgumentLoading(gen, context, codeAddress, true, ArmRegister.r1);
	
		// r5 now contains object, move it to r0
		gen.copyRegister(ArmRegister.r0, ArmRegister.r5);
		// move v
		gen.copyRegister(ArmRegister.r3, ArmRegister.r1);
		// move self
		gen.copyRegister(ArmRegister.r4, ArmRegister.r2);
		// load dPCoffset
		gen.loadConstant(ArmRegister.r1, codeAddress);
		// load vtable offset
		gen.loadConstant(ArmRegister.r2, ((OdexedInvokeVirtual)instruction).getVtableIndex());
		// load &predictedChainingCell
		gen.loadLabel(ArmRegister.r5, String.format("ChainingCell_T%d_M%#x", context.currentRegionIndex, codeAddress));

		gen.jumpToFunction(context, ArmRegister.r6, LiteralPoolType.CALL_AOT_INVOKE_VIRTUAL_QUICK, "dvmCompiler_AOT_INVOKE_VIRTUAL_QUICK");

		String jumpAfterLabel = String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress);
		gen.jumpToLabel(jumpAfterLabel);
	
		gen.insertLabel(String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));

		gen.loadConstant(ArmRegister.r0, 0);

		gen.insertLabel(jumpAfterLabel);

		gen.insertComment("Restore callee-save regs");
		gen.calleeSavePop();
		gen.insertComment("END INVOKE VIRTUAL QUICK");
	
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleInvokeSingleton(CodeGenContext context, ArmInstOpL inst) {
		String label = inst.label.getLabelAsString();
	
		int codeAddress = 0;
		boolean nullCheckArgs = false;
		Pattern p = Pattern.compile("invoke_singleton_(.*)_0x(.*)\\(PLT\\)$");
		Matcher m = p.matcher(label);
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
			handleInvokeSingletonNative(context, inst, codeAddress, method, nullCheckArgs);
		} else {
			handleInvokeSingletonJava(context, inst, codeAddress, method, nullCheckArgs);
		}
	}
	
	private void handleInvokeSingletonJava(CodeGenContext context, ArmInstOpL inst, int codeAddress, EncodedMethod method, boolean nullCheckArgs) {
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

		InstGen gen = new InstGen();

		gen.insertComment("START INVOKE SINGLETON (JAVA)");
		gen.insertComment("Save callee-save regs");
		gen.calleeSavePush();

		handleArgumentLoading(gen, context, codeAddress, nullCheckArgs, ArmRegister.r2);

		gen.loadLabel(ArmRegister.r4, String.format("ChainingCell_T%d_M%s%#x", context.currentRegionIndex, vtablePrefix, methodIndex));

		gen.jumpToFunction(context, ArmRegister.r5, LiteralPoolType.CALL_AOT_INVOKE_SINGLETON_JAVA, "dvmCompiler_AOT_INVOKE_SINGLETON_JAVA");

		gen.insertComment("Restore callee-save regs");
		gen.calleeSavePop();
		gen.insertComment("END INVOKE SINGLETON (JAVA)");

		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleInvokeSingletonNative(CodeGenContext context, ArmInstOpL inst, int codeAddress, EncodedMethod method, boolean nullCheckArgs) {
		InstGen gen = new InstGen();

		gen.insertComment("START INVOKE SINGLETON (NATIVE)");
		gen.insertComment("Save callee-save regs");
		gen.calleeSavePush();

		handleArgumentLoading(gen, context, codeAddress, nullCheckArgs, ArmRegister.r2);

		gen.jumpToFunction(context, ArmRegister.r5, LiteralPoolType.CALL_AOT_INVOKE_SINGLETON_NATIVE, "dvmCompiler_AOT_INVOKE_SINGLETON_NATIVE");

		gen.insertComment("Restore callee-save regs");
		gen.calleeSavePop();
		gen.insertComment("END INVOKE SINGLETON (NATIVE)");

		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleArgumentRangeLoading(InstGen gen, CodeGenContext context, int codeAddress, boolean nullCheck, ArmRegister fpReg) {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		
		gen.insertComment("Begin range argument loading");
		if (fpReg != ArmRegister.r5) {
			gen.copyRegister(fpReg, ArmRegister.r5);
		}
		int regCount = ((InvokeInstruction) instruction).getRegCount();
		int startReg = ((RegisterRangeInstruction) instruction).getStartRegister();
		
		gen.doMath("add", ArmRegister.r4, ArmRegister.r5, startReg * 4);
		
		int regMask = regCount;
		if (regCount >= 4) {
			regMask = 4;
		}
		
		gen.doReadWriteMultipleWithRegMask(ArmRegister.r4, false, regMask);
		
		gen.doMath("sub", ArmRegister.r7, ArmRegister.r5, regCount * 4 + 20 /* size of StackSaveArea */);
		
		if (regCount > 0 && nullCheck) {
			gen.insertComment("null-check (method is not static)");
			gen.doComparisonAndJump("eq", ArmRegister.r0, 0, String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));
		}
		
		if (regCount >= 8) {
			gen.stackPush(ArmRegister.r0, ArmRegister.r5);
			
			String loopHeaderLabel = String.format("ArgRangeLoopHeader_T%d_A%#x", context.currentRegionIndex, codeAddress);
			
			if (regCount > 11) {
				gen.loadConstant(ArmRegister.r5, (((regCount - 4) >> 2) << 2));
				gen.insertLabel(loopHeaderLabel);
			}
			
			gen.doReadWriteMultipleWithRegMask(ArmRegister.r7, true, regMask);
			
			gen.doReadWriteMultipleWithRegMask(ArmRegister.r4, false, regMask);
			
			if (regCount > 11) {
				gen.doMath("sub", ArmRegister.r5, ArmRegister.r5, 4);
				gen.doComparisonAndJump("ne", ArmRegister.r5, 0, loopHeaderLabel);
			}
		}
		
		if (regCount != 0) {
			gen.doReadWriteMultipleWithRegMask(ArmRegister.r7, true, regMask);
		}
		
		if (regCount > 4 && (regCount % 4 != 0)) {
			regMask = ((1 << (regCount & 0x3)) - 1) << 1;
			gen.doReadWriteMultipleWithRegMask(ArmRegister.r4, false, regMask);
		}
		
		if (regCount >= 8) {
			gen.stackPop(ArmRegister.r0, ArmRegister.r5);
		}
		
		if (regCount > 4 && (regCount % 4 != 0)) {
			regMask = ((1 << (regCount & 0x3)) - 1) << 1;
			gen.doReadWriteMultipleWithRegMask(ArmRegister.r7, true, regMask);
		}
		
		if (fpReg != ArmRegister.r5) {
			gen.copyRegister(ArmRegister.r5, fpReg);
		}
		gen.insertComment("End range argument loading");
	}
	
	private void handleArgumentLoading(InstGen gen, CodeGenContext context, int codeAddress, boolean nullCheck, ArmRegister fpReg) {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		
		if (instruction instanceof RegisterRangeInstruction) {
			handleArgumentRangeLoading(gen, context, codeAddress, nullCheck, fpReg);
			return;
		}

		gen.insertComment("Begin argument loading");
		int regCount = ((FiveRegisterInstruction) instruction).getRegCount();

		if (regCount > 0) {
			gen.memoryRead(ArmRegister.r5, fpReg, ((FiveRegisterInstruction) instruction).getRegisterD() * 4);
		}
		if (regCount > 1) {
			gen.memoryRead(ArmRegister.r6, fpReg, ((FiveRegisterInstruction) instruction).getRegisterE() * 4);
		}
		if (regCount > 2) {
			gen.memoryRead(ArmRegister.r7, fpReg, ((FiveRegisterInstruction) instruction).getRegisterF() * 4);
		}
		if (regCount > 3) {
			gen.memoryRead(ArmRegister.r8, fpReg, ((FiveRegisterInstruction) instruction).getRegisterG() * 4);
		}
		if (regCount > 4) {
			gen.memoryRead(ArmRegister.r9, fpReg, ((FiveRegisterInstruction) instruction).getRegisterA() * 4);
		}

		gen.doMath("sub", ArmRegister.r10, fpReg, 20 /* size of StackSaveArea */+ ((FiveRegisterInstruction) instruction).getRegCount() * 4);

		if (regCount > 0) {
			if (nullCheck) {
				gen.insertComment("null-check (method is not static)");
				gen.doComparisonAndJump("eq", ArmRegister.r5, 0, String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));
			}

			gen.memoryWriteMultiple(ArmRegister.r10);
			ArmInstOpRMultiple inst = (ArmInstOpRMultiple) gen.getLast();

			inst.addRegister(ArmRegister.r5);
			if (regCount > 1) {
				inst.addRegister(ArmRegister.r6);
			}
			if (regCount > 2) {
				inst.addRegister(ArmRegister.r7);
			}
			if (regCount > 3) {
				inst.addRegister(ArmRegister.r8);
			}
			if (regCount > 4) {
				inst.addRegister(ArmRegister.r9);
			}
		}
		gen.insertComment("Finished argument loading");
	}
}
