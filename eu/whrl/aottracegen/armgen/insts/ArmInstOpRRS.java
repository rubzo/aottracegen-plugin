package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRRS extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public String shift;
	public int shiftAmount;

	public ArmInstOpRRS(String opcode, ArmRegister reg1, ArmRegister reg2, String shift, int shiftAmount) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.shift = shift;
		this.shiftAmount = shiftAmount;
	}

	@Override
	public String print() {
		return String.format("%s %s, %s, %s #%d", getOpcodeAsString(), reg1.toString(), reg2.toString(), shift, shiftAmount);
	}
	
	private Pattern regex;
	
	public ArmInstOpRRS() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.reg + h.commaSpace + h.shiftOpcode + h.space + h.imm + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRRS(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), 
					match.group(4), h.readImm(match.group(5)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRRS";
	}
}
