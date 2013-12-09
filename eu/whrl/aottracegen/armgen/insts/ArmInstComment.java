package eu.whrl.aottracegen.armgen.insts;

public class ArmInstComment extends ArmInst implements IArmInstPrintable {
	public String comment;

	public ArmInstComment(String comment) {
		super();
		this.comment = comment;
	}

	public String print() {
		return "# " + comment;
	}
}
