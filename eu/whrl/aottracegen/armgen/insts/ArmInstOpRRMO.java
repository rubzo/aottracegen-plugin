package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRRMO extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public ArmRegister memreg;
	public int offset;
	public boolean autoIndex;

	public ArmInstOpRRMO(String opcode, ArmRegister reg1, ArmRegister reg2, ArmRegister memreg, int offset) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.memreg = memreg;
		this.offset = offset;
		this.autoIndex = false;
	}

	@Override
	public String print() {
		String autoIndexString = "";
		if (autoIndex) {
			autoIndexString = "!";
		}
		return String.format("%s %s, %s, [%s, #%d]%s", getOpcodeAsString(), reg1.toString(), reg2.toString(), memreg.toString(), offset, autoIndexString);
	}
	
	private Pattern regex;
	
	public ArmInstOpRRMO() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.reg + h.commaSpace + h.lbrace + h.reg + h.commaSpace + h.imm + h.rbrace + h.possibleBang + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			ArmInstOpRRMO newInst = new ArmInstOpRRMO(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), h.readReg(match.group(4)), h.readImm(match.group(5)));
			if (match.group(6).equals("!")) {
				newInst.autoIndex = true;
			}
			return newInst;
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRRMO";
	}
}
