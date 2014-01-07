package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpRMR extends ArmInstOp implements IArmInstPrintable {
	public ArmRegister reg;
	public ArmRegister memreg1;
	public ArmRegister memreg2;

	public ArmInstOpRMR(String opcode, ArmRegister reg, ArmRegister memreg1, ArmRegister memreg2) {
		super(opcode);
		this.reg = reg;
		this.memreg1 = memreg1;
		this.memreg2 = memreg2;
	}

	public String print() {
		return String.format("%s %s, [%s, %s]", getOpcodeAsString(), reg.toString(), memreg1.toString(), memreg2.toString());
	}
}