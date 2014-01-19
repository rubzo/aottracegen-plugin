package eu.whrl.aottracegen.armgen.insts;

import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRMultiple extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmRegister reg1;
	public List<ArmRegister> registers;
	public boolean autoIndex;

	public ArmInstOpRMultiple(String opcode, ArmRegister reg1) {
		super(opcode);
		this.reg1 = reg1;
		this.registers = new LinkedList<ArmRegister>();
		this.autoIndex = false;
	}

	public void addRegister(ArmRegister register) {
		registers.add(register);
	}
	
	public boolean usesRegister(ArmRegister register) {
		return registers.contains(register);
	}

	@Override
	public String print() {
		String regsString = "";
		for (int i = 0; i < registers.size(); i++) {
			regsString += registers.get(i).toString();
			if (i != registers.size()-1) {
				regsString += ", ";
			}
		}
		String autoIndexString = "";
		if (autoIndex) {
			autoIndexString = "!";
		}
		return String.format("%s %s, {%s}", getOpcodeAsString(), reg1.toString() + autoIndexString, regsString);
	}
	
	private Pattern regex;
	
	public ArmInstOpRMultiple() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.lcurly + h.groupInnards + h.rcurly + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		String baseRegString = match.group(2);
		try {
			ArmInstOpRMultiple newInst = new ArmInstOpRMultiple(match.group(1), h.readReg(baseRegString));
			if (baseRegString.trim().endsWith("!")) {
				newInst.autoIndex = true;
			}
			for (ArmRegister reg : h.readRegisterGroup(match.group(3))) {
				newInst.addRegister(reg);
			}
			return newInst;
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRMultiple";
	}
}
