package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRII extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg;
	public int imm1;
	public int imm2;

	public ArmInstOpRII(String opcode, ArmRegister reg, int imm1, int imm2) {
		super(opcode);
		this.reg = reg;
		this.imm1 = imm1;
		this.imm2 = imm2;
	}

	@Override
	public String print() {
		return String.format("%s %s, #%d, #%d", getOpcodeAsString(), reg.toString(), imm1, imm2);
	}
	
	private Pattern regex;
	
	public ArmInstOpRII() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.imm + h.commaSpace + h.imm + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRII(match.group(1), h.readReg(match.group(2)), h.readImm(match.group(3)), h.readImm(match.group(4)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRII";
	}
}
