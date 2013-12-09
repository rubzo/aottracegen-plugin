package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpL extends ArmInstOp implements IArmInstPrintable, IArmInstHasLabel {
	public ArmLabelReference label;

	public ArmInstOpL(String opcode, String label) {
		super(opcode);
		this.label = new ArmLabelReference(label);
	}
	
	public ArmInstOpL(String opcode, ArmLabelReference label) {
		super(opcode);
		this.label = label;
	}

	public String print() {
		return String.format("%s %s", getOpcodeAsString(), label.getLabelAsString());
	}
	
	public ArmLabelReference getLabel() {
		return label;
	}
}
