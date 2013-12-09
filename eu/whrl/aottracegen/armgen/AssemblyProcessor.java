package eu.whrl.aottracegen.armgen;

import java.util.Iterator;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.armgen.insts.ArmConditionCode;
import eu.whrl.aottracegen.armgen.insts.ArmInst;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpL;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpMultiple;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRI;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRL;
import eu.whrl.aottracegen.armgen.insts.ArmLabelReference;
import eu.whrl.aottracegen.armgen.insts.ArmOpcode;
import eu.whrl.aottracegen.armgen.insts.ArmRegister;
import eu.whrl.aottracegen.armgen.insts.IArmInstHasLabel;

public class AssemblyProcessor {
	
	private ArmInstOpMultiple findPopInstruction(ArmInst insts) {
		for (ArmInst inst : insts) {
			ArmOpcode opcode = inst.getOpcode();
			if (opcode == ArmOpcode.ldm || opcode == ArmOpcode.pop) {
				ArmInstOpMultiple op = (ArmInstOpMultiple) inst;
				if (op.registers.size() != 2 || op.registers.get(0) != ArmRegister.r5 || 
						op.registers.get(1) != ArmRegister.r6) {
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
				if (op.registers.size() != 2 || op.registers.get(0) != ArmRegister.r5 || 
						op.registers.get(1) != ArmRegister.r6) {
					return op;
				}
			}
		}
		return null;
	}
	
	public void modifyPrologueEpilogueCode(CodeGenContext context, ArmInst insts) {
		/* deal with the potentially multiple pop instructions */
		ArmInstOpMultiple popInst = findPopInstruction(insts);
		while (popInst != null) {
			
			/* remove the following bx lr instruction, if it exists */
			if (!popInst.usesRegister(ArmRegister.pc)) {
				if (popInst.next != null && popInst.next.getOpcode().equals("bx")) {
					popInst.next = popInst.next.next;
					popInst.next.prev = popInst;
				}
			}
			
			/* pop only r5, r6 */
			popInst.registers.clear();
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

			/* there might be another pop instruction, look for it */
			popInst = findPopInstruction(insts);
		}

		/* deal with the single push instruction at the entry */
		ArmInstOpMultiple pushInst = findPushInstruction(insts);
		
		if (pushInst != null) {
			pushInst.registers.clear();
			pushInst.addRegister(ArmRegister.r5);
			pushInst.addRegister(ArmRegister.r6);
		}
	}
	
	public void renameLabels(CodeGenContext context, ArmInst insts) {
		/* put T<region index> on the front of all labels, so they don't clash if we output multiple regions */
		String regionPrefix = "T" + context.currentRegionIndex + "_";
		for (ArmInst inst : insts) {
			if (inst instanceof IArmInstHasLabel) {
				ArmLabelReference label = ((IArmInstHasLabel)inst).getLabel();
				label.rename(regionPrefix + label.getLabelNameOnly());
			}
		}
	}
	
	public void removeCBZ(CodeGenContext context, ArmInst insts) {
		/* find cbz and cbnzs, and replace whem with a cmp and b */
		for (ArmInst inst : insts) {
			ArmOpcode opcode = inst.getOpcode();
			if (opcode == ArmOpcode.cbz || opcode == ArmOpcode.cbnz) {
				/* get arguments to original cb inst */
				ArmInstOpRL conditionalBranchInst = (ArmInstOpRL) inst;
				ArmRegister reg = conditionalBranchInst.reg;
				ArmLabelReference label = conditionalBranchInst.label;
				
				/* construct new insts */
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
	}
}
