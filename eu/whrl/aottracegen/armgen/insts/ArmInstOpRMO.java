package eu.whrl.aottracegen.armgen.insts;

class ArmInstOpRMO extends ArmInstOp implements IArmInstPrintable {
	public ArmRegister reg;
	public ArmRegister memreg;
	public int offset;

	public ArmInstOpRMO(String opcode, ArmRegister reg, ArmRegister memreg, int offset) {
		super(opcode);
		this.reg = reg;
		this.memreg = memreg;
		this.offset = offset;
	}

	public String print() {
		return String.format("%s %s, [%s, #%d]", getOpcodeAsString(), reg.toString(), memreg.toString(), offset);
	}
}
