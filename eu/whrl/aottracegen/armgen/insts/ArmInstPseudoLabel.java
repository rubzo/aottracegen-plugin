package eu.whrl.aottracegen.armgen.insts;

public class ArmInstPseudoLabel extends ArmInst implements IArmInstPrintable, IArmInstHasLabel {
	public ArmLabelReference label;

	public ArmInstPseudoLabel(String name) {
		super();
		this.label = new ArmLabelReference(name);
	}

	public String print() {
		return label.getLabelAsString() + ":";
	}
	
	public ArmLabelReference getLabel() {
		return label;
	}
}
