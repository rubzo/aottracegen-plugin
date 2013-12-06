package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmInstOp extends ArmInst implements ArmInstPrintable {
	public String opcode;

	public ArmInstOp(String opcode) {
		super();
		this.opcode = opcode;
	}

	public String print() {
		return opcode;
	}
}
