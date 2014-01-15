package eu.whrl.aottracegen.armgen.insts;

import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpMultipleM extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public List<ArmRegister> registers;
	public ArmRegister memreg;

	public ArmInstOpMultipleM(String opcode, ArmRegister memreg) {
		super(opcode);
		this.memreg = memreg;
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
		return String.format("%s {%s}, [%s]", getOpcodeAsString(), regsString, memreg.toString());
	}
	
	private Pattern regex;
	
	public ArmInstOpMultipleM() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.lcurly + h.groupInnards + h.rcurly + h.commaSpace + h.lbrace + h.reg + h.rbrace + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			ArmInstOpMultipleM newInst = new ArmInstOpMultipleM(match.group(1), h.readReg(match.group(3)));
			String regsString = match.group(2);

			for (String reg : regsString.split(",")) {
				newInst.addRegister(h.readReg(reg));
			}
			return newInst;
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpMultipleM";
	}
}
