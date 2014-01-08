package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRRI extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public int imm;

	public ArmInstOpRRI(String opcode, ArmRegister reg1, ArmRegister reg2, int imm) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.imm = imm;
	}

	@Override
	public String print() {
		return String.format("%s %s, %s, #%d", getOpcodeAsString(), reg1.toString(), reg2.toString(), imm);
	}
	
	private Pattern regex;
	
	public ArmInstOpRRI() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.reg + h.commaSpace + h.imm + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRRI(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), h.readImm(match.group(4)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRRI";
	}
}
