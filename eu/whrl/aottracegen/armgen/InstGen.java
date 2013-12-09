package eu.whrl.aottracegen.armgen;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.LiteralPoolType;
import eu.whrl.aottracegen.Trace;
import eu.whrl.aottracegen.armgen.insts.ArmInst;
import eu.whrl.aottracegen.armgen.insts.ArmInstComment;
import eu.whrl.aottracegen.armgen.insts.ArmInstOp;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpL;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpMultiple;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpR;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRI;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRL;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRMO;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRMultiple;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRR;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRRI;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRRR;
import eu.whrl.aottracegen.armgen.insts.ArmInstPseudoLabel;
import eu.whrl.aottracegen.armgen.insts.ArmRegister;

public class InstGen {
	private ArmInst first;
	private ArmInst last;
	
	public InstGen() {
		first = null;
		last = null;
	}
		
	private void addInst(ArmInst inst) {
		if (first == null) {
			first = inst;
			last = first;
		} else {
			last.linkToNext(inst);
			last = inst;
		}
	}
	
	public ArmInst getFirst() {
		return first;
	}
	
	public ArmInst getLast() {
		return last;
	}
	
	private void addArglessInstruction(String mnemonic) {
		ArmInstOp op = new ArmInstOp(mnemonic);
		addInst(op);
	}
	
	private void addLabelInstruction(String mnemonic, String label) {
		ArmInstOpL op = new ArmInstOpL(mnemonic, label);
		addInst(op);
	}
	
	private void addRegGroupInstruction(String mnemonic, ArmRegister ... registers) {
		ArmInstOpMultiple op = new ArmInstOpMultiple(mnemonic);
		for (ArmRegister reg : registers) {
			op.addRegister(reg);
		}
		addInst(op);
	}
	
	private void addRegRegGroupInstruction(String mnemonic, ArmRegister reg1, ArmRegister ... registers) {
		ArmInstOpRMultiple op = new ArmInstOpRMultiple(mnemonic, reg1);
		for (ArmRegister reg : registers) {
			op.addRegister(reg);
		}
		addInst(op);
	}
	
	private void addRegLabelInstruction(String mnemonic, ArmRegister reg1, String label) {
		ArmInstOpRL op = new ArmInstOpRL(mnemonic, reg1, label);
		addInst(op);
	}
	
	private void addRegImmInstruction(String mnemonic, ArmRegister reg1, int imm) {
		ArmInstOpRI op = new ArmInstOpRI(mnemonic, reg1, imm);
		addInst(op);
	}
	
	private void addRegInstruction(String mnemonic, ArmRegister reg1) {
		ArmInstOpR op = new ArmInstOpR(mnemonic, reg1);
		addInst(op);
	}
	
	private void addRegRegOffsetInstruction(String mnemonic, ArmRegister reg1, ArmRegister reg2, int imm) {
		ArmInstOpRMO op = new ArmInstOpRMO(mnemonic, reg1, reg2, imm);
		addInst(op);
	}
	
	private void addRegRegImmInstruction(String mnemonic, ArmRegister reg1, ArmRegister reg2, int imm) {
		ArmInstOpRRI op = new ArmInstOpRRI(mnemonic, reg1, reg2, imm);
		addInst(op);
	}
	
	private void addRegRegInstruction(String mnemonic, ArmRegister reg1, ArmRegister reg2) {
		ArmInstOpRR op = new ArmInstOpRR(mnemonic, reg1, reg2);
		addInst(op);
	}
	
	private void addRegRegRegInstruction(String mnemonic, ArmRegister reg1, ArmRegister reg2, ArmRegister reg3) {
		ArmInstOpRRR op = new ArmInstOpRRR(mnemonic, reg1, reg2, reg3);
		addInst(op);
	}
	
	private void addComment(String comment) {
		ArmInstComment commentInst = new ArmInstComment(comment);
		addInst(commentInst);
	}
	
	private void addLabel(String labelName) {
		ArmInstPseudoLabel label = new ArmInstPseudoLabel(labelName);
		addInst(label);
	}
	
	/* should only use the methods below */
	
	public void insertComment(String comment) {
		addComment(comment);
	}
	
	public void stackPush(ArmRegister ... registers) {
		addRegGroupInstruction("push", registers);
	}
	
	public void stackPop(ArmRegister ... registers) {
		addRegGroupInstruction("pop", registers);
	}
	
	public void memoryWrite(ArmRegister src, ArmRegister dest, int offset) {
		addRegRegOffsetInstruction("str", src, dest, offset);
	}
	
	public void memoryRead(ArmRegister dest, ArmRegister src, int offset) {
		addRegRegOffsetInstruction("ldr", dest, src, offset);
	}
	
	public void memoryWriteMultiple(ArmRegister dest, ArmRegister ... registers) {
		addRegRegGroupInstruction("stmia", dest, registers);
	}
	
	public void loadConstant(ArmRegister reg, long constant) {
		if (constant < 127 && constant > -128) {
			/* encoding T1 */
			addRegImmInstruction("mov", reg, (int) constant);
		} else if (constant < 32767 && constant > -32768) {
			/* force encoding T3 */
			addRegImmInstruction("movw", reg, (int) constant);
		} else {
			/* shift 1 to a close enough number */
			double scaling = (Math.log(constant) / Math.log(2));
			long base = (long) Math.pow(scaling, 2.0);
			long difference = constant - base;
			
			if (difference < 1023 && difference > -1024) {
				addComment("Calculating " + constant);
				addRegImmInstruction("mov", reg, 1);
				addRegRegImmInstruction("lsl", reg, reg, (int) scaling);
				addRegRegImmInstruction("add", reg, reg, (int) difference);
			} else {
				addComment("ERROR difference will not fit into add immediate, need to implement two registers system");
				addRegImmInstruction("mov", reg, 1);
				addRegRegImmInstruction("lsl", reg, reg, (int) scaling);
				addRegRegImmInstruction("add", reg, reg, (int) difference);
			}
		}
	}
	
	public void doMath(String op, ArmRegister reg1, ArmRegister reg2, int constant) {
		addRegRegImmInstruction(op, reg1, reg2, constant);
	}
	
	public void doMath(String op, ArmRegister reg1, ArmRegister reg2, ArmRegister reg3) {
		addRegRegRegInstruction(op, reg1, reg2, reg3);
	}
	
	public void doComparisonAndJump(String op, ArmRegister reg, int constant, String label) {
		addRegImmInstruction("cmp", reg, constant);
		addLabelInstruction("b" + op, label);
	}
	
	public void copyRegister(ArmRegister reg1, ArmRegister reg2) {
		addRegRegInstruction("mov", reg1, reg2);
	}
	
	public void loadLabel(ArmRegister reg, String label) {
		addRegLabelInstruction("adr", reg, label);
	}
	
	public void jumpToReg(ArmRegister reg) {
		addRegInstruction("blx", reg);
	}
	
	public void jumpToLabel(String label) {
		addLabelInstruction("b", label);
	}
	
	public void addMemoryBarrier() {
		addArglessInstruction("dmb");
	}
	
	public void jumpToFunction(CodeGenContext context, ArmRegister reg, LiteralPoolType function, String name) {
		Trace curTrace = context.currentRegion.trace;
		int literalPoolLoc = curTrace.meta.addLiteralPoolType(function);
		insertComment(String.format("load and call %s()", name));
		loadLabel(reg, String.format("LiteralPool_T%d", context.currentRegionIndex));
		memoryRead(reg, reg, literalPoolLoc * 4);
		jumpToReg(reg);
	}
	
	public void insertLabel(String label) {
		addLabel(label);
	}
	
	public void calleeSavePush() {
		stackPush(ArmRegister.r4, ArmRegister.r5, ArmRegister.r6, 
				ArmRegister.r7, ArmRegister.r8, ArmRegister.r9, 
				ArmRegister.r10, ArmRegister.r11);
	}
	
	public void calleeSavePop() {
		stackPop(ArmRegister.r4, ArmRegister.r5, ArmRegister.r6, 
				ArmRegister.r7, ArmRegister.r8, ArmRegister.r9, 
				ArmRegister.r10, ArmRegister.r11);
	}
}
