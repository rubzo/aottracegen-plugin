package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpMR extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister memreg;
	public ArmRegister reg;

	public ArmInstOpMR(String opcode, ArmRegister memreg, ArmRegister reg) {
		super(opcode);
		this.memreg = memreg;
		this.reg = reg;
	}

	@Override
	public String print() {
		return String.format("%s [%s, %s]", getOpcodeAsString(), memreg.toString(), reg.toString());
	}
	
	private Pattern regex;
	
	public ArmInstOpMR() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.lbrace + h.reg + h.commaSpace + h.reg + h.rbrace + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpMR(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpMR";
	}
}
