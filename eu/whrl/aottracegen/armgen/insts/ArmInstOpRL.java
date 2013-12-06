package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpRL extends ArmInstOp implements ArmInstPrintable {
	public ArmRegister reg;
	public String label;

	public ArmInstOpRL(String opcode, ArmRegister reg, String label) {
		super(opcode);
		this.reg = reg;
		this.label = label;
	}

	public String print() {
		return String.format("%s %s, %s", opcode, reg.toString(), label);
	}
}
