package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOpRRRS extends ArmInstOp implements IArmInstPrintable {
	public ArmRegister reg1;
	public ArmRegister reg2;
	public ArmRegister reg3;
	public String shift;
	public int shiftAmount;

	public ArmInstOpRRRS(String opcode, ArmRegister reg1, ArmRegister reg2, ArmRegister reg3, String shift, int shiftAmount) {
		super(opcode);
		this.reg1 = reg1;
		this.reg2 = reg2;
		this.reg3 = reg3;
		this.shift = shift;
		this.shiftAmount = shiftAmount;
	}

	public String print() {
		return String.format("%s %s, %s, %s, %s #%d", getOpcodeAsString(), reg1.toString(), reg2.toString(), reg3.toString(), shift, shiftAmount);
	}
}
