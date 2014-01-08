package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRRR extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public ArmRegister reg3;

	public ArmInstOpRRR(String opcode, ArmRegister reg1, ArmRegister reg2, ArmRegister reg3) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.reg3 = reg3;
	}

	@Override
	public String print() {
		return String.format("%s %s, %s, %s", getOpcodeAsString(), reg1.toString(), reg2.toString(), reg3.toString());
	}
	
	private Pattern regex;
	
	public ArmInstOpRRR() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.reg + h.commaSpace + h.reg + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRRR(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), h.readReg(match.group(4)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRRR";
	}
}
