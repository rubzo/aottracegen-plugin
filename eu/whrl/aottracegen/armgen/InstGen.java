package eu.whrl.aottracegen.armgen;

import java.util.List;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.LiteralPoolType;
import eu.whrl.aottracegen.Trace;

public class InstGen {
	private static List<String> traceBody;
	private static int linePtr;
	
	public static void setTraceBody(List<String> traceBody) {
		InstGen.traceBody = traceBody;
		InstGen.linePtr = 0;
	}
	
	public static void setLinePtr(int linePtr) {
		InstGen.linePtr = linePtr;
	}
	
	public static int getLinePtr() {
		return linePtr;
	}
	
	private static void removeLine() {
		traceBody.remove(linePtr);
	}

	private static void replaceLine(String line) {
		traceBody.remove(linePtr);
		traceBody.add(linePtr, line);
	}

	private static void addLine(String line) {
		traceBody.add(linePtr, line);
		linePtr++;
	}

	private static void addLineAfter(String line) {
		traceBody.add(linePtr + 1, line);
		linePtr += 2;
	}
		
	private static void addArglessInstruction(String mnemonic) {
		addLine(String.format("\t%s", mnemonic));
	}
	
	private static void addLabelInstruction(String mnemonic, String label) {
		addLine(String.format("\t%s\t%s", mnemonic, label));
	}
	
	private static void addRegGroupInstruction(String mnemonic, String regGroup) {
		addLine(String.format("\t%s\t%s", mnemonic, regGroup));
	}
	
	private static void addRegRegGroupInstruction(String mnemonic, String reg1, String regGroup) {
		addLine(String.format("\t%s\t%s, %s", mnemonic, reg1, regGroup));
	}
	
	private static void addRegLabelInstruction(String mnemonic, String reg1, String label) {
		addLine(String.format("\t%s\t%s, %s", mnemonic, reg1, label));
	}
	
	private static void addRegImmInstruction(String mnemonic, String reg1, int imm) {
		addLine(String.format("\t%s\t%s, #%d", mnemonic, reg1, imm));
	}
	
	private static void addRegInstruction(String mnemonic, String reg1) {
		addLine(String.format("\t%s\t%s", mnemonic, reg1));
	}
	
	private static void addRegRegOffsetInstruction(String mnemonic, String reg1, String reg2, int imm) {
		addLine(String.format("\t%s\t%s, [%s, #%d]", mnemonic, reg1, reg2, imm));
	}
	
	private static void addRegRegImmInstruction(String mnemonic, String reg1, String reg2, int imm) {
		addLine(String.format("\t%s\t%s, %s, #%d", mnemonic, reg1, reg2, imm));
	}
	
	private static void addRegRegInstruction(String mnemonic, String reg1, String reg2) {
		addLine(String.format("\t%s\t%s, %s", mnemonic, reg1, reg2));
	}
	
	private static void addRegRegRegInstruction(String mnemonic, String reg1, String reg2, String reg3) {
		addLine(String.format("\t%s\t%s, %s", mnemonic, reg1, reg2));
	}
	
	private static void addComment(String comment) {
		addLine(String.format("\t# %s", comment));
	}
	
	private static void addLabel(String label) {
		addLine(String.format("%s:", label));
	}
	
	/* should only use the methods below */
	
	public static void removeCurrentLine() {
		removeLine();
	}
	
	public static void insertComment(String comment) {
		addComment(comment);
	}
	
	public static void stackPush(String regGroup) {
		addRegGroupInstruction("push", regGroup);
	}
	
	public static void stackPop(String regGroup) {
		addRegGroupInstruction("pop", regGroup);
	}
	
	public static void memoryWrite(String src, String dest, int offset) {
		addRegRegOffsetInstruction("str", src, dest, offset);
	}
	
	public static void memoryRead(String dest, String src, int offset) {
		addRegRegOffsetInstruction("ldr", dest, src, offset);
	}
	
	public static void memoryWriteMultiple(String dest, String regGroup) {
		addRegRegGroupInstruction("stmia", dest, regGroup);
	}
	
	public static void loadConstant(String reg, long constant) {
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
	
	public static void doMath(String op, String reg1, String reg2, int constant) {
		addRegRegImmInstruction(op, reg1, reg2, constant);
	}
	
	public static void doMath(String op, String reg1, String reg2, String reg3) {
		addRegRegRegInstruction(op, reg1, reg2, reg3);
	}
	
	public static void doComparisonAndJump(String op, String reg, int constant, String label) {
		addRegImmInstruction("cmp", reg, constant);
		addLabelInstruction("b" + op, label);
	}
	
	public static void copyRegister(String reg1, String reg2) {
		addRegRegInstruction("mov", reg1, reg2);
	}
	
	public static void loadLabel(String reg, String label) {
		addRegLabelInstruction("adr", reg, label);
	}
	
	public static void jumpToReg(String reg) {
		addRegInstruction("blx", reg);
	}
	
	public static void jumpToLabel(String label) {
		addLabelInstruction("b", label);
	}
	
	public static void addMemoryBarrier() {
		addArglessInstruction("dmb");
	}
	
	public static void jumpToFunction(CodeGenContext context, String reg, LiteralPoolType function, String name) {
		Trace curTrace = context.currentRegion.trace;
		int literalPoolLoc = curTrace.meta.addLiteralPoolType(function);
		InstGen.insertComment(String.format("load and call %s()", name));
		InstGen.loadLabel(reg, String.format("LiteralPool_T%d", context.currentRegionIndex));
		InstGen.memoryRead(reg, reg, literalPoolLoc * 4);
		InstGen.jumpToReg(reg);
	}
	
	public static void insertLabel(String label) {
		addLabel(label);
	}
}
