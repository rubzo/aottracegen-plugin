package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpRM extends ArmInstOp implements ArmInstPrintable {
	public ArmRegister reg;
	public ArmRegister memreg;

	public ArmInstOpRM(String opcode, ArmRegister reg, ArmRegister memreg) {
		super(opcode);
		this.reg = reg;
		this.memreg = memreg;
	}

	public String print() {
		return String.format("%s %s, [%s]", opcode, reg.toString(), memreg.toString());
	}
}
