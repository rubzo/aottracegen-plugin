package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpRR extends ArmInstOp implements ArmInstPrintable {
	public ArmRegister reg1;
	public ArmRegister reg2;

	public ArmInstOpRR(String opcode, ArmRegister reg1, ArmRegister reg2) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
	}

	public String print() {
		return String.format("%s %s, %s", opcode, reg1.toString(), reg2.toString());
	}
}
