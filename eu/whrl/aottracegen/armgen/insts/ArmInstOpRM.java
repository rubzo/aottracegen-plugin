package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRM extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg;
	public ArmRegister memreg;

	public ArmInstOpRM(String opcode, ArmRegister reg, ArmRegister memreg) {
		super(opcode);
		this.reg = reg;
		this.memreg = memreg;
	}

	@Override
	public String print() {
		return String.format("%s %s, [%s]", getOpcodeAsString(), reg.toString(), memreg.toString());
	}
	
	private Pattern regex;
	
	public ArmInstOpRM() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.lbrace + h.reg + h.rbrace + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRM(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRM";
	}
}
