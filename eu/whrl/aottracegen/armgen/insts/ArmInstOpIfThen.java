package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpIfThen extends ArmInstOp implements IArmInstPrintable {
	public ArmConditionCode cc;

	public ArmInstOpIfThen(String opcode, ArmConditionCode cc) {
		super(opcode);
		this.cc = cc;
	}

	public String print() {
		return String.format("%s %s", getOpcodeAsString(), cc.toString());
	}
}
