package eu.whrl.aottracegen.armgen.insts;

public class ArmInstOp extends ArmInst implements ArmInstPrintable {
	public ArmOpcode opcode;
	public ArmConditionCode cc;
	public boolean setsFlags;
	
	public ArmInstOp(String opcodeString) {
		super();
		setsFlags = false;
		parseOpcode(opcodeString);
		if (isInvalid()) {
			System.out.println("Couldn't read opcode: " + opcodeString);
			System.exit(1);
		}
	}
	
	public boolean isInvalid() {
		return (opcode == ArmOpcode.INVALID || cc == ArmConditionCode.INVALID);
	}

	public String print() {
		return getOpcodeAsString();
	}
	
	private void parseOpcode(String opcodeString) {
		for (ArmConditionCode condition : ArmConditionCode.values()) {
			if (opcodeString.endsWith(condition.toString())) {		
				for (ArmOpcode legitOpcode : ArmOpcode.values()) {
					if ((legitOpcode.toString() + condition.toString()).equals(opcodeString)) {
						cc = condition;
						opcode = legitOpcode;
						return;
					}
					if ((legitOpcode.toString() + "s" + condition.toString()).equals(opcodeString)) {
						cc = condition;
						opcode = legitOpcode;
						setsFlags = true;
						return;
					}
				}
				
			}
		}
		for (ArmOpcode legitOpcode : ArmOpcode.values()) {
			if (legitOpcode.toString().equals(opcodeString)) {
				cc = ArmConditionCode.al;
				opcode = legitOpcode;
				return;
			}
			if ((legitOpcode.toString() + "s").equals(opcodeString)) {
				cc = ArmConditionCode.al;
				opcode = legitOpcode;
				setsFlags = true;
				return;
			}
		}
		cc = ArmConditionCode.INVALID;
		opcode = ArmOpcode.INVALID;
	}
	
	public String getOpcodeAsString() {
		String opcodeString = opcode.toString();
		if (setsFlags) {
			opcodeString += "s";
		}
		if (cc != ArmConditionCode.al) {
			opcodeString += cc.toString();
		}
		return opcodeString;
	}
}
