package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpRL extends ArmInstOp implements IArmInstPrintable, IArmInstHasLabel {
	public ArmRegister reg;
	public ArmLabelReference label;

	public ArmInstOpRL(String opcode, ArmRegister reg, String label) {
		super(opcode);
		this.reg = reg;
		this.label = new ArmLabelReference(label);
	}

	public String print() {
		return String.format("%s %s, %s", getOpcodeAsString(), reg.toString(), label.getLabelAsString());
	}
	
	public ArmLabelReference getLabel() {
		return label;
	}
}
