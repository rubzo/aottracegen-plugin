package eu.whrl.aottracegen.armgen.insts;

public class ArmInstPseudoLabel extends ArmInst implements ArmInstPrintable {
	public String name;

	public ArmInstPseudoLabel(String name) {
		super();
		this.name = name;
	}

	public String print() {
		return name + ":";
	}
}
