package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpRRRR extends ArmInstOp implements IArmInstPrintable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public ArmRegister reg3;
	public ArmRegister reg4;

	public ArmInstOpRRRR(String opcode, ArmRegister reg1, ArmRegister reg2, ArmRegister reg3, ArmRegister reg4) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.reg3 = reg3;
		this.reg4 = reg4;
	}

	public String print() {
		return String.format("%s %s, %s, %s, %s", getOpcodeAsString(), reg1.toString(), reg2.toString(), reg3.toString(), reg4.toString());
	}
}
