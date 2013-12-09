package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpRRI extends ArmInstOp implements IArmInstPrintable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public int imm;

	public ArmInstOpRRI(String opcode, ArmRegister reg1, ArmRegister reg2, int imm) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.imm = imm;
	}

	public String print() {
		return String.format("%s %s, %s, #%d", getOpcodeAsString(), reg1.toString(), reg2.toString(), imm);
	}
}
