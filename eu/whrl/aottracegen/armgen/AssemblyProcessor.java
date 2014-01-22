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

import eu.whrl.aottracegen.ChainingCell;
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
				if (opcode == ArmOpcode.tbb) {
					ArmInstOp tableBranchInst = (ArmInstOp) inst;
					tableBranchInst.opcode = ArmOpcode.tbh;
					ArmInstComment comment = new ArmInstComment("Was promoted from tbb to tbh");
					inst.insertBefore(comment);
				}
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
						if (directive.name.equals("byte")) {
							directive.name = "2byte";
						}
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
			pushInst.removeSelf();
		}
		
		/* Put a new push instruction right at the beginning */
		ArmInstOpMultiple newPushInst = new ArmInstOpMultiple("push");
		newPushInst.addRegister(ArmRegister.r0);
		newPushInst.addRegister(ArmRegister.r1);
		newPushInst.addRegister(ArmRegister.r2);
		insts.insertBefore(newPushInst);

		return newPushInst;
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
				} else if (dest.contains("count_invoke_for_print_vregs")) {
					handleCountInvokeForPrintVregs(context, branchInst);
				} else if (dest.contains("print_vregs")) {
					handlePrintVregs(context, branchInst);
				} else if (dest.contains("instanceof")) {
					handleInstanceof(context, branchInst);
				} else if (dest.contains("new_instance")) {
					handleNewInstance(context, branchInst);
				} else if (dest.contains("new_array")) {
					handleNewArray(context, branchInst);
				} else if (dest.contains("barrier")) {
					handleBarrier(context, branchInst);
				} else if (dest.contains("sqrt")) {
					handleSqrt(context, branchInst);
				} else if (dest.contains("__aeabi_l2d")) {
					handleAeabiL2D(context, branchInst);
				} else if (dest.contains("__aeabi_l2f")) {
					handleAeabiL2F(context, branchInst);
				} else if (dest.contains("__aeabi_idivmod")) {
					handleAeabiIDivMod(context, branchInst);
				} else if (dest.contains("__aeabi_idiv")) {
					handleAeabiIDiv(context, branchInst);
				} else if (dest.contains("__hiya_cos")) {
					handleCos(context, branchInst);
				} else if (dest.contains("__hiya_sin")) {
					handleSin(context, branchInst);
				} else {
					System.out.println("Unhandled bl label: " + dest);
					System.exit(1);
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

		gen.insertComment("--- SINGLE STEP START");
		gen.insertComment("Save callee-save regs");

		gen.stackPush(ArmRegister.r4, ArmRegister.r5, ArmRegister.r6, 
				ArmRegister.r7, ArmRegister.r8, ArmRegister.r9, 
				ArmRegister.r10, ArmRegister.r11);

		gen.insertComment("Restore interpreter regs");
		gen.copyRegister(ArmRegister.r5, ArmRegister.r1);
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
		
		gen.insertComment("--- SINGLE STEP END");

		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleCountInvokeForPrintVregs(CodeGenContext context, ArmInstOpL inst) {
		/* literal pool pointer is in r0 */
		InstGen gen = new InstGen();
		gen.insertComment("--- COUNT INVOKE FOR PRINT VREGS START");
		gen.jumpToFunction(context, ArmRegister.r0, ArmRegister.r0, LiteralPoolType.CALL_DVMPRINTVREGSCOUNTINVOKE, "dvmPrintVregsCountInvoke");
		gen.insertComment("--- COUNT INVOKE PRINT VREGS END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handlePrintVregs(CodeGenContext context, ArmInstOpL inst) {
		/* literal pool pointer is in r3 */
		InstGen gen = new InstGen();
		gen.insertComment("--- PRINT VREGS START");
		gen.jumpToFunction(context, ArmRegister.r3, ArmRegister.r3, LiteralPoolType.CALL_DVMPRINTVREGS, "dvmPrintVregs");
		gen.insertComment("--- PRINT VREGS END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleNewInstance(CodeGenContext context, ArmInstOpL inst) {
		/* literal pool pointer is in r2 */
		InstGen gen = new InstGen();
		gen.insertComment("--- NEW INSTANCE START");
		gen.jumpToFunction(context, ArmRegister.r2, ArmRegister.r2, LiteralPoolType.CALL_ALLOC_OBJECT, "dvmAllocObject");
		gen.insertComment("--- NEW INSTANCE END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleNewArray(CodeGenContext context, ArmInstOpL inst) {
		/* literal pool pointer is in r3 */
		InstGen gen = new InstGen();
		gen.insertComment("--- NEW ARRAY START");
		gen.jumpToFunction(context, ArmRegister.r3, ArmRegister.r3, LiteralPoolType.CALL_ALLOC_ARRAY_BY_CLASS, "dvmAllocArrayByClass");
		gen.insertComment("--- NEW ARRAY END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleExecuteInline(CodeGenContext context, ArmInstOpL inst) {
		String label = inst.label.getLabelAsString();
		Trace curTrace = context.currentRegion.trace;
		
		int inlineIndex = 0;
		int numArgs = 0;
		Pattern p = Pattern
				.compile("execute_inline_args_(\\d+)_index_(\\d+)");
		Matcher m = p.matcher(label);
		if (m.find()) {
			numArgs = Integer.parseInt(m.group(1));
			inlineIndex = Integer.parseInt(m.group(2));
		}
		
		int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.EXECUTE_INLINE, inlineIndex);
		
		InstGen gen = new InstGen();
		gen.insertComment("--- EXECUTE INLINE START");
		gen.stackPush(ArmRegister.r4);
		/* copy the literal pool */
		gen.copyRegister(ArmRegister.r4, ArmRegister.r0);
		
		/* shift the args down */
		if (numArgs > 0) {
			gen.copyRegister(ArmRegister.r0, ArmRegister.r1);
		}
		if (numArgs > 1) {
			gen.copyRegister(ArmRegister.r1, ArmRegister.r2);
		}
		if (numArgs > 2) {
			gen.copyRegister(ArmRegister.r2, ArmRegister.r3);
		}
		
		/* load the execute inline function, and jump to it */
		gen.memoryRead(ArmRegister.r4, ArmRegister.r4, literalPoolLoc * 4);
		gen.jumpToReg(ArmRegister.r4);
		
		gen.stackPop(ArmRegister.r4);
		
		/* r0 is where the C expects the result to be */
		gen.insertComment("--- EXECUTE INLINE END");
		
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleInstanceof(CodeGenContext context, ArmInstOpL inst) {
		/* literal pool pointer is in r2 */
		InstGen gen = new InstGen();
		gen.insertComment("--- INSTANCE OF START");
		gen.memoryRead(ArmRegister.r0, ArmRegister.r0, 0);
		gen.jumpToFunction(context, ArmRegister.r2, ArmRegister.r2, LiteralPoolType.CALL_INSTANCEOF_NON_TRIVIAL, "dvmInstanceofNonTrivial");
		gen.insertComment("--- INSTANCE OF END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleBarrier(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.insertComment("--- BARRIER START");
		gen.addMemoryBarrier();
		gen.insertComment("--- BARRIER END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleSqrt(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.insertComment("--- BOMB START");
		gen.addBomb();
		gen.insertComment("--- END START");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleAeabiL2D(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.insertComment("--- AEABI_L2D START");
		gen.jumpToFunctionHardcodedLiteralPool(context, ArmRegister.r2, LiteralPoolType.CALL___AEABI_L2D, "__aeabi_l2d", inst);
		gen.insertComment("--- AEABI_L2D END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleAeabiL2F(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.insertComment("--- AEABI_L2F START");
		gen.jumpToFunctionHardcodedLiteralPool(context, ArmRegister.r2, LiteralPoolType.CALL___AEABI_L2F, "__aeabi_l2f", inst);
		gen.insertComment("--- AEABI_L2F END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleAeabiIDivMod(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.insertComment("--- AEABI_IDIVMOD START");
		gen.jumpToFunctionHardcodedLiteralPool(context, ArmRegister.r2, LiteralPoolType.CALL___AEABI_IDIVMOD, "__aeabi_idivmod", inst);
		gen.insertComment("--- AEABI_IDIVMOD END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleAeabiIDiv(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.insertComment("--- AEABI_IDIV START");
		gen.jumpToFunctionHardcodedLiteralPool(context, ArmRegister.r2, LiteralPoolType.CALL___AEABI_IDIVMOD, "__aeabi_idiv", inst);
		gen.insertComment("--- AEABI_IDIV END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleCos(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.insertComment("--- COS START");
		// double will be in d0, lit will be in r0, see pg19 of Arm Procedure Call Standard
		gen.jumpToFunction(context, ArmRegister.r0, ArmRegister.r0, LiteralPoolType.CALL_COS, "cos");
		gen.insertComment("--- COS END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleSin(CodeGenContext context, ArmInstOpL inst) {
		InstGen gen = new InstGen();
		gen.insertComment("--- SIN START");
		// double will be in d0, lit will be in r0, see pg19 of Arm Procedure Call Standard
		gen.jumpToFunction(context, ArmRegister.r0, ArmRegister.r0, LiteralPoolType.CALL_SIN, "sin");
		gen.insertComment("--- SIN END");
		inst.replaceChain(gen.getFirst(), gen.getLast());
	}

	private void handleInvokeInterface(CodeGenContext context, ArmInstOpL inst) {
		/* literal pool pointer is in r0 */
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

		gen.insertComment("--- INVOKE INTERFACE START");
		gen.insertComment("Save callee-save regs");
		gen.calleeSavePush();

		handleArgumentLoading(gen, context, codeAddress, true, ArmRegister.r1);

		// save the literal pool pointer in r6... 
		gen.copyRegister(ArmRegister.r6, ArmRegister.r0);

		// r5 now contains object, move it to r0
		gen.copyRegister(ArmRegister.r0, ArmRegister.r5);
		
		// move v
		gen.copyRegister(ArmRegister.r3, ArmRegister.r1);
		// move self
		gen.copyRegister(ArmRegister.r4, ArmRegister.r2);
		// load dPCoffset
		gen.loadConstant(ArmRegister.r1, codeAddress);
		// load vB (method reference)
		gen.loadConstant(ArmRegister.r2, ((InstructionWithReference) instruction).getReferencedItem().getIndex());
		// load &predictedChainingCell
		gen.loadLabel(ArmRegister.r5, String.format("ChainingCell_T%d_M%#x", context.currentRegionIndex, codeAddress), inst, true);

		gen.jumpToFunction(context, ArmRegister.r6, ArmRegister.r6, LiteralPoolType.CALL_AOT_INVOKE_INTERFACE, "dvmCompiler_AOT_INVOKE_INTERFACE");

		gen.jumpToLabel(String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress));

		gen.insertLabel(String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));

		gen.loadConstant(ArmRegister.r0, 0);

		gen.insertLabel(String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress));

		gen.insertComment("Restore callee-save regs");
		gen.calleeSavePop();
		gen.insertComment("--- INVOKE INTERFACE END");

		inst.replaceChain(gen.getFirst(), gen.getLast());
	}

	private void handleInvokeVirtualQuick(CodeGenContext context, ArmInstOpL inst) {
		/* literal pool pointer is now in r6 */
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

		gen.insertComment("--- INVOKE VIRTUAL QUICK START");
		gen.insertComment("Save callee-save regs");
		gen.calleeSavePush();

		handleArgumentLoading(gen, context, codeAddress, true, ArmRegister.r1);
	
		// save the literal pool pointer in r6... 
		gen.copyRegister(ArmRegister.r6, ArmRegister.r0);
		
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
		gen.loadLabel(ArmRegister.r5, String.format("ChainingCell_T%d_M%#x", context.currentRegionIndex, codeAddress), inst, true);

		gen.jumpToFunction(context, ArmRegister.r6, ArmRegister.r6, LiteralPoolType.CALL_AOT_INVOKE_VIRTUAL_QUICK, "dvmCompiler_AOT_INVOKE_VIRTUAL_QUICK");

		String jumpAfterLabel = String.format("JumpAfter_T%d_A%#x", context.currentRegionIndex, codeAddress);
		gen.jumpToLabel(jumpAfterLabel);
	
		gen.insertLabel(String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));

		gen.loadConstant(ArmRegister.r0, 0);

		gen.insertLabel(jumpAfterLabel);

		gen.insertComment("Restore callee-save regs");
		gen.calleeSavePop();
		gen.insertComment("--- INVOKE VIRTUAL QUICK END");
	
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
		
		Trace curTrace = context.currentRegion.trace;
	
		/* literal pool pointer is in r1 */
		/* need to load the right method pointer into r1, and move the literal pool pointer to r4 */
		/* we'll do this later on, though... */
		int methodIndex = 0;
		int literalPoolLoc = 0;
		if (nullCheckArgs) {
			/* load DIRECT METHOD */
			methodIndex = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(LiteralPoolType.DIRECT_METHOD, methodIndex);
			if (!curTrace.meta.chainingCells.containsKey(methodIndex)) {
				curTrace.meta.chainingCells.put(methodIndex, (new ChainingCell(ChainingCell.Type.INVOKE_SINGLETON, methodIndex)));
			}
		} else {
			/* load STATIC METHOD */
			methodIndex = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(LiteralPoolType.STATIC_METHOD, methodIndex);
			if (!curTrace.meta.chainingCells.containsKey(methodIndex)) {
				curTrace.meta.chainingCells.put(methodIndex, (new ChainingCell(ChainingCell.Type.INVOKE_SINGLETON, methodIndex)));
			}
		}
		
		EncodedMethod method = MethodLookup.getMethodLookup().getCalleeMethodFromInstruction(instruction,
				context);

		if ((method.accessFlags & 0x100) != 0 /* native? */) {
			handleInvokeSingletonNative(context, inst, codeAddress, method, nullCheckArgs, methodIndex, literalPoolLoc);
		} else {
			handleInvokeSingletonJava(context, inst, codeAddress, method, nullCheckArgs, methodIndex, literalPoolLoc);
		}
	}
	
	private void handleInvokeSingletonJava(CodeGenContext context, ArmInstOpL inst, int codeAddress, EncodedMethod method, boolean nullCheckArgs, int methodIndex, int literalPoolLoc) {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		String vtablePrefix = "";

		InstGen gen = new InstGen();

		gen.insertComment("--- INVOKE SINGLETON (JAVA) START");
		gen.insertComment("Save callee-save regs");
		gen.calleeSavePush();
		
		/* move literal pool from r1 to r11 */
		gen.copyRegister(ArmRegister.r11, ArmRegister.r1);
		/* read method pointer out of r11[literalpoolloc] into r1 */ 
		gen.memoryRead(ArmRegister.r1, ArmRegister.r11, 4 * literalPoolLoc);

		handleArgumentLoading(gen, context, codeAddress, nullCheckArgs, ArmRegister.r2);

		gen.loadLabel(ArmRegister.r4, String.format("ChainingCell_T%d_M%s%#x", context.currentRegionIndex, vtablePrefix, methodIndex), inst, true);

		gen.jumpToFunction(context, ArmRegister.r11, ArmRegister.r11, LiteralPoolType.CALL_AOT_INVOKE_SINGLETON_JAVA, "dvmCompiler_AOT_INVOKE_SINGLETON_JAVA");

		gen.insertComment("Restore callee-save regs");
		gen.calleeSavePop();
		gen.insertComment("--- INVOKE SINGLETON (JAVA) END");

		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	private void handleInvokeSingletonNative(CodeGenContext context, ArmInstOpL inst, int codeAddress, EncodedMethod method, boolean nullCheckArgs, int methodIndex, int literalPoolLoc) {
		InstGen gen = new InstGen();

		gen.insertComment("--- INVOKE SINGLETON (NATIVE) START");
		gen.insertComment("Save callee-save regs");
		gen.calleeSavePush();

		/* move literal pool from r1 to r5 */
		gen.copyRegister(ArmRegister.r5, ArmRegister.r1);
		/* read method pointer out of r5[literalpoolloc] into r1 */ 
		gen.memoryRead(ArmRegister.r1, ArmRegister.r5, 4 * literalPoolLoc);
		
		handleArgumentLoading(gen, context, codeAddress, nullCheckArgs, ArmRegister.r2);

		gen.jumpToFunction(context, ArmRegister.r5, ArmRegister.r5, LiteralPoolType.CALL_AOT_INVOKE_SINGLETON_NATIVE, "dvmCompiler_AOT_INVOKE_SINGLETON_NATIVE");

		gen.insertComment("Restore callee-save regs");
		gen.calleeSavePop();
		gen.insertComment("--- INVOKE SINGLETON (NATIVE) END");

		inst.replaceChain(gen.getFirst(), gen.getLast());
	}
	
	/* clobbers r3-r9, must ensure first argument loaded is in r5 at the end, if it was nullchecked */
	private void handleArgumentRangeLoading(InstGen gen, CodeGenContext context, int codeAddress, boolean nullCheck, ArmRegister fpReg) {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		
		ArmRegister initialFpReg = fpReg;
		
		gen.insertComment("Begin range argument loading");
		if (fpReg.ordinal() > ArmRegister.r3.ordinal()) {
			gen.copyRegister(ArmRegister.r3, fpReg);
			fpReg = ArmRegister.r3;
		}
		// fpreg must now be in r3 or lower!!
		
		int regCount = ((InvokeInstruction) instruction).getRegCount();
		int startVirtualReg = ((RegisterRangeInstruction) instruction).getStartRegister();
		
		ArmRegister counterReg = ArmRegister.r4;
		ArmRegister readPointerReg = ArmRegister.r5;
		ArmRegister writePointerReg = ArmRegister.r6;
		ArmRegister tempArgStorageFirstReg = ArmRegister.r7;
		
		gen.doMath("add", readPointerReg, fpReg, startVirtualReg * 4);
		
		int numRegsInBatch = regCount;
		if (regCount >= 4) {
			numRegsInBatch = 4;
		}
		
		gen.doReadMultipleRange(readPointerReg, tempArgStorageFirstReg, numRegsInBatch, true);
		
		gen.doMath("sub", writePointerReg, fpReg, regCount * 4 + 20 /* size of StackSaveArea */);
		
		if (regCount > 0 && nullCheck) {
			gen.insertComment("null-check (method is not static)");
			gen.doComparisonAndJump("eq", tempArgStorageFirstReg, 0, String.format("RaiseException_T%d_A%#x", context.currentRegionIndex, codeAddress));
			gen.stackPush(tempArgStorageFirstReg);
		}
		
		if (regCount >= 8) {
			String loopHeaderLabel = String.format("ArgRangeLoopHeader_T%d_A%#x", context.currentRegionIndex, codeAddress);
			
			if (regCount > 11) {
				gen.loadConstant(counterReg, (((regCount - 4) >> 2) << 2));
				gen.insertLabel(loopHeaderLabel);
			}
			
			gen.doWriteMultipleRange(writePointerReg, tempArgStorageFirstReg, numRegsInBatch, true);
			
			gen.doReadMultipleRange(readPointerReg, tempArgStorageFirstReg, numRegsInBatch, true);
			
			if (regCount > 11) {
				gen.doMath("sub", counterReg, counterReg, 4);
				gen.doComparisonAndJump("ne", counterReg, 0, loopHeaderLabel);
			}
		}
		
		if (regCount != 0) {
			gen.doWriteMultipleRange(writePointerReg, tempArgStorageFirstReg, numRegsInBatch, true);
		}
		
		if (regCount > 4 && (regCount % 4 != 0)) {
			numRegsInBatch = regCount % 4;
			gen.doReadMultipleRange(readPointerReg, tempArgStorageFirstReg, numRegsInBatch, true);
		}
		
		if (regCount > 4 && (regCount % 4 != 0)) {
			numRegsInBatch = regCount % 4;
			gen.doWriteMultipleRange(writePointerReg, tempArgStorageFirstReg, numRegsInBatch, true);
		}
		
		if (fpReg != initialFpReg) {
			gen.copyRegister(initialFpReg, fpReg);
		}
		
		if (regCount > 0 && nullCheck) {
			gen.stackPop(ArmRegister.r5);
		}
		gen.insertComment("End range argument loading");
	}
	
	/* could clobber anything from r3 upwards */
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

			gen.memoryWriteMultiple(ArmRegister.r10, false);
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
