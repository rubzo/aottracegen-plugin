package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpR extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg;

	public ArmInstOpR(String opcode, ArmRegister reg) {
		super(opcode);
		this.reg = reg;
	}

	@Override
	public String print() {
		return String.format("%s %s", getOpcodeAsString(), reg.toString());
	}
	
	private Pattern regex;
	
	public ArmInstOpR() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpR(match.group(1), h.readReg(match.group(2)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpR";
	}
}
