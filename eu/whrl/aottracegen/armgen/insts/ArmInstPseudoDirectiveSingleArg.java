package eu.whrl.aottracegen.armgen.insts;

public class ArmInstPseudoDirectiveSingleArg extends ArmInstPseudoDirective implements IArmInstPrintable {
	public String arg;

	public ArmInstPseudoDirectiveSingleArg(String name, String arg) {
		super(name);
		this.arg = arg;
	}

	public String print() {
		return "." + name + " " + arg;
	}
}
