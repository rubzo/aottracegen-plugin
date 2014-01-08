package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmLabelReference;
import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpRL extends ArmInstOp implements IArmInstPrintable, IArmInstHasLabel, IArmInstParsable {
	public ArmRegister reg;
	public ArmLabelReference label;

	public ArmInstOpRL(String opcode, ArmRegister reg, String label) {
		super(opcode);
		this.reg = reg;
		this.label = new ArmLabelReference(label);
	}

	@Override
	public String print() {
		return String.format("%s %s, %s", getOpcodeAsString(), reg.toString(), label.getLabelAsString());
	}
	
	@Override
	public ArmLabelReference getLabel() {
		return label;
	}
	
	private Pattern regex;
	
	public ArmInstOpRL() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.reg + h.commaSpace + h.word + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpRL(match.group(1), h.readReg(match.group(2)), match.group(3));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpRL";
	}
}
