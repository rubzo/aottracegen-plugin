package eu.whrl.aottracegen.armgen.insts;

import java.util.List;
import java.util.LinkedList;

public class ArmInstOpRMultiple extends ArmInstOp implements IArmInstPrintable {
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
}
