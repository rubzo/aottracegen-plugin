package eu.whrl.aottracegen.armgen.insts;

import java.util.List;
import java.util.LinkedList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmInstOpMultiple extends ArmInstOp implements ArmInstPrintable {
	public List<ArmRegister> registers;

	public ArmInstOpMultiple(String opcode) {
		super(opcode);
		this.registers = new LinkedList<ArmRegister>();
	}

	public void addRegister(ArmRegister register) {
		registers.add(register);
	}

	public String print() {
		String regsString = "";
		for (int i = 0; i < registers.size(); i++) {
			regsString += registers.get(i).toString();
			if (i != registers.size()-1) {
				regsString += ", ";
			}
		}
		return String.format("%s {%s}", this.opcode, regsString);
	}
}
