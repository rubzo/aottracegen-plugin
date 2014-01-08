package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmLabelReference;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstPseudoLabel extends ArmInst implements IArmInstPrintable, IArmInstHasLabel, IArmInstParsable {
	public ArmLabelReference label;
	
	public ArmInstPseudoLabel(String name) {
		super();
		this.label = new ArmLabelReference(name);
	}

	@Override
	public String print() {
		return label.getLabelAsString() + ":";
	}
	
	@Override
	public ArmLabelReference getLabel() {
		return label;
	}
	
	private Pattern regex;
	
	public ArmInstPseudoLabel() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.colon + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		return new ArmInstPseudoLabel(match.group(1));
	}
	
	@Override
	public String getName() {
		return "Label";
	}
}
