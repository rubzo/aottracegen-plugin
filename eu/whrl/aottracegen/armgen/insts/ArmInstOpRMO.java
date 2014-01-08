package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRMO extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg;
	public ArmRegister memreg;
	public int offset;
	public boolean autoIndex;

	public ArmInstOpRMO(String opcode, ArmRegister reg, ArmRegister memreg, int offset) {
		super(opcode);
		this.reg = reg;
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
		return String.format("%s %s, [%s, #%d]%s", getOpcodeAsString(), reg.toString(), memreg.toString(), offset, autoIndexString);
	}
	
	private Pattern regex;
	
	public ArmInstOpRMO() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.lbrace + h.reg + h.commaSpace + h.imm + h.rbrace + h.possibleBang + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			ArmInstOpRMO newInst = new ArmInstOpRMO(match.group(1), h.readReg(match.group(2)), h.readReg(match.group(3)), h.readImm(match.group(4)));
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
		return "OpRMO";
	}
}
