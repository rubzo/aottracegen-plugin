package eu.whrl.aottracegen.armgen.insts;

public class ArmInstPseudoDirective extends ArmInst implements IArmInstPrintable {
	public String name;

	public ArmInstPseudoDirective(String name) {
		super();
		this.name = name;
	}

	public String print() {
		return "." + name;
	}
}