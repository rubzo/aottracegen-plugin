package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRRRS extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public ArmRegister reg3;
	public String shift;
	public int shiftAmount;

	public ArmInstOpRRRS(String opcode, ArmRegister reg1, ArmRegister reg2, ArmRegister reg3, String shift, int shiftAmount) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.reg3 = reg3;
		this.shift = shift;
		this.shiftAmount = shiftAmount;
	}

	@Override
	public String print() {
		return String.format("%s %s, %s, %s, %s #%d", getOpcodeAsString(), reg1.toString(), reg2.toString(), reg3.toString(), shift, shiftAmount);
	}
	
	private Pattern regex;
	
	public ArmInstOpRRRS() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.reg + h.commaSpace + h.reg + h.commaSpace + h.shiftOpcode + h.space + h.imm + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRRRS(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), 
					h.readReg(match.group(4)), match.group(5), h.readImm(match.group(6)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRRRS";
	}
}
