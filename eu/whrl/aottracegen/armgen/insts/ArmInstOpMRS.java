package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpMRS extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister memreg;
	public ArmRegister reg;
	public String shift;
	public int shiftAmount;

	public ArmInstOpMRS(String opcode, ArmRegister memreg, ArmRegister reg, String shift, int shiftAmount) {
		super(opcode);
		this.memreg = memreg;
		this.reg = reg;
		this.shift = shift;
		this.shiftAmount = shiftAmount;
	}

	@Override
	public String print() {
		return String.format("%s [%s, %s, %s #%d]", getOpcodeAsString(), memreg.toString(), reg.toString(), shift, shiftAmount);
	}
	
	private Pattern regex;
	
	public ArmInstOpMRS() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.lbrace + h.reg + h.commaSpace + h.reg + h.commaSpace + h.shiftOpcode + h.space + h.imm + h.rbrace + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpMRS(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), match.group(4), h.readImm(match.group(5)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpMRS";
	}
}
