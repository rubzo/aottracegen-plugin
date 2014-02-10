package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRMRS extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg;
	public ArmRegister memreg1;
	public ArmRegister memreg2;
	public String shift;
	public int shiftAmount;

	public ArmInstOpRMRS(String opcode, ArmRegister reg, ArmRegister memreg1, ArmRegister memreg2, String shift, int shiftAmount) {
		super(opcode);
		this.reg = reg;
		this.memreg1 = memreg1;
		this.memreg2 = memreg2;
		this.shift = shift;
		this.shiftAmount = shiftAmount;
	}

	@Override
	public String print() {
		return String.format("%s %s, [%s, %s, %s #%d]", getOpcodeAsString(), reg.toString(), memreg1.toString(), memreg2.toString(), shift, shiftAmount);
	}
	
	private Pattern regex;
	
	public ArmInstOpRMRS() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.lbrace + h.reg + h.commaSpace + h.reg + h.commaSpace + h.shiftOpcode + h.space + h.imm + h.rbrace + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRMRS(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), h.readReg(match.group(4)), match.group(5), h.readImm(match.group(6)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRMRS";
	}
}