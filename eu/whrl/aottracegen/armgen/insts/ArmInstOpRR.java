package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRR extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	/* only used in vmov */
	public int reg1Index;
	public int reg2Index;

	public ArmInstOpRR(String opcode, ArmRegister reg1, ArmRegister reg2) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.reg1Index = -1;
		this.reg2Index = -1;
	}

	@Override
	public String print() {
		String reg1IndexString = "";
		String reg2IndexString = "";
		if (reg1Index >= 0) {
			reg1IndexString = String.format("[%d]", reg1Index);
		}
		if (reg2Index >= 0) {
			reg2IndexString = String.format("[%d]", reg2Index);
		}
		return String.format("%s %s, %s", getOpcodeAsString(), reg1.toString() + reg1IndexString, reg2.toString() + reg2IndexString);
	}
	
	private Pattern regex;
	
	public ArmInstOpRR() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.reg + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		String reg1String = match.group(2);
		String reg2String = match.group(3);
		try {
			ArmInstOpRR newInst = new ArmInstOpRR(match.group(1), h.readReg(reg1String), h.readReg(reg2String));
			
			if (reg1String.endsWith("]")) {
				newInst.reg1Index = Integer.parseInt(reg1String.substring(reg1String.indexOf('[') + 1, reg1String.length() - 1));
			}
			if (reg2String.endsWith("]")) {
				newInst.reg2Index = Integer.parseInt(reg1String.substring(reg2String.indexOf('[') + 1, reg2String.length() - 1));
			}
			
			return newInst;
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRR";
	}
}
