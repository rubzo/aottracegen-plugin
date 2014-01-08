package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRRRR extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public ArmRegister reg3;
	public ArmRegister reg4;

	public ArmInstOpRRRR(String opcode, ArmRegister reg1, ArmRegister reg2, ArmRegister reg3, ArmRegister reg4) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.reg3 = reg3;
		this.reg4 = reg4;
	}

	@Override
	public String print() {
		return String.format("%s %s, %s, %s, %s", getOpcodeAsString(), reg1.toString(), reg2.toString(), reg3.toString(), reg4.toString());
	}
	
	private Pattern regex;
	
	public ArmInstOpRRRR() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.reg + h.commaSpace + h.reg + h.commaSpace + h.reg + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRRRR(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), 
					h.readReg(match.group(4)), h.readReg(match.group(5)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRRRR";
	}
}
