package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpR extends ArmInstOp implements ArmInstPrintable {
	public ArmRegister reg;

	public ArmInstOpR(String opcode, ArmRegister reg) {
		super(opcode);
		this.reg = reg;
	}

	public String print() {
		return String.format("%s %s", opcode, reg.toString());
	}
}
