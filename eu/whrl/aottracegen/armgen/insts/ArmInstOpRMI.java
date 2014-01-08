package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRMI extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg;
	public ArmRegister memreg;
	public int imm;

	public ArmInstOpRMI(String opcode, ArmRegister reg, ArmRegister memreg, int imm) {
		super(opcode);
		this.reg = reg;
		this.memreg = memreg;
		this.imm = imm;
	}

	@Override
	public String print() {
		return String.format("%s %s, [%s], #%d", getOpcodeAsString(), reg.toString(), memreg.toString(), imm);
	}
	
	private Pattern regex;
	
	public ArmInstOpRMI() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.lbrace + h.reg + h.rbrace + h.commaSpace + h.imm + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRMI(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), h.readImm(match.group(4)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRMI";
	}
}
