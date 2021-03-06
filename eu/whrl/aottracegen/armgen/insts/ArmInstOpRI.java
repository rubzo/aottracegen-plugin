package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRI extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg;
	public int imm;

	public ArmInstOpRI(String opcode, ArmRegister reg, int imm) {
		super(opcode);
		this.reg = reg;
		this.imm = imm;
	}

	@Override
	public String print() {
		return String.format("%s %s, #%d", getOpcodeAsString(), reg.toString(), imm);
	}
	
	private Pattern regex;
	
	public ArmInstOpRI() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.imm + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRI(match.group(1), h.readReg(match.group(2)), h.readImm(match.group(3)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRI";
	}
}
