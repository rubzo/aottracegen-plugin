package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpL extends ArmInstOp implements ArmInstPrintable {
	public String label;

	public ArmInstOpL(String opcode, String label) {
		super(opcode);
		this.label = label;
	}

	public String print() {
		return String.format("%s %s", getOpcodeAsString(), label);
	}
}
