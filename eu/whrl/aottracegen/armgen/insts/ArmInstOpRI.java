package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpRI extends ArmInstOp implements ArmInstPrintable {
	public ArmRegister reg;
	public int imm;

	public ArmInstOpRI(String opcode, ArmRegister reg, int imm) {
		super(opcode);
		this.reg = reg;
		this.imm = imm;
	}

	public String print() {
		return String.format("%s %s, #%d", opcode, reg.toString(), imm);
	}
}
