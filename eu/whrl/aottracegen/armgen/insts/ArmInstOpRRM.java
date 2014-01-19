package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRRM extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public ArmRegister memreg;
	public boolean autoIndex;

	public ArmInstOpRRM(String opcode, ArmRegister reg1, ArmRegister reg2, ArmRegister memreg) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.memreg = memreg;
		this.autoIndex = false;
	}

	@Override
	public String print() {
		String autoIndexString = "";
		if (autoIndex) {
			autoIndexString = "!";
		}
		return String.format("%s %s, %s, [%s]%s", getOpcodeAsString(), reg1.toString(), reg2.toString(), memreg.toString(), autoIndexString);
	}
	
	private Pattern regex;
	
	public ArmInstOpRRM() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.reg + h.commaSpace + h.lbrace + h.reg + h.rbrace + h.possibleBang + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			ArmInstOpRRM newInst = new ArmInstOpRRM(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), h.readReg(match.group(4)));
			if (match.group(5).equals("!")) {
				newInst.autoIndex = true;
			}
			return newInst;
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRRM";
	}
}
