package eu.whrl.aottracegen.armgen.insts;

import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpMultiple extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public List<ArmRegister> registers;

	public ArmInstOpMultiple(String opcode) {
		super(opcode);
		this.registers = new LinkedList<ArmRegister>();
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
		return String.format("%s {%s}", getOpcodeAsString(), regsString);
	}
	
	private Pattern regex;
	
	public ArmInstOpMultiple() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.lcurly + h.groupInnards + h.rcurly + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		ArmInstOpMultiple newInst = new ArmInstOpMultiple(match.group(1));
		String regsString = match.group(2);
		try {
			for (String reg : regsString.split(",")) {
				newInst.addRegister(h.readReg(reg));
			}
		} catch (NotParsableException e) {
			return null;
		}
		return newInst;
	}
	
	@Override
	public String getName() {
		return "OpMultiple";
	}
}
