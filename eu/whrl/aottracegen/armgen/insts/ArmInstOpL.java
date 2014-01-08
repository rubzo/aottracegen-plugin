package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmLabelReference;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpL extends ArmInstOp implements IArmInstPrintable, IArmInstHasLabel, IArmInstParsable {
	public ArmLabelReference label;

	public ArmInstOpL(String opcode, String label) {
		super(opcode);
		this.label = new ArmLabelReference(label);
	}
	
	public ArmInstOpL(String opcode, ArmLabelReference label) {
		super(opcode);
		this.label = label;
	}

	@Override
	public String print() {
		return String.format("%s %s", getOpcodeAsString(), label.getLabelAsString());
	}
	
	@Override
	public ArmLabelReference getLabel() {
		return label;
	}

	private Pattern regex;
	
	public ArmInstOpL() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.space + h.word + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		return new ArmInstOpL(match.group(1), match.group(2));
	}
	
	@Override
	public String getName() {
		return "OpL";
	}
}
