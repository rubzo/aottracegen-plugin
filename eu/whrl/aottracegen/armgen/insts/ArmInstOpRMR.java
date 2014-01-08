package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRMR extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg;
	public ArmRegister memreg1;
	public ArmRegister memreg2;

	public ArmInstOpRMR(String opcode, ArmRegister reg, ArmRegister memreg1, ArmRegister memreg2) {
		super(opcode);
		this.reg = reg;
		this.memreg1 = memreg1;
		this.memreg2 = memreg2;
	}

	@Override
	public String print() {
		return String.format("%s %s, [%s, %s]", getOpcodeAsString(), reg.toString(), memreg1.toString(), memreg2.toString());
	}
	
	private Pattern regex;
	
	public ArmInstOpRMR() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.lbrace + h.reg + h.commaSpace + h.reg + h.rbrace + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRMR(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), h.readReg(match.group(4)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRMR";
	}
}