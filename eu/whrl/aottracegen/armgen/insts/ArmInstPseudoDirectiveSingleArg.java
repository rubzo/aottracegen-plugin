package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstPseudoDirectiveSingleArg extends ArmInstPseudoDirective implements IArmInstPrintable, IArmInstParsable {
	public String arg;

	public ArmInstPseudoDirectiveSingleArg(String name, String arg) {
		super(name);
		this.arg = arg;
	}

	@Override
	public String print() {
		return "." + name + " " + arg;
	}
	
	private Pattern regex;
	
	public ArmInstPseudoDirectiveSingleArg() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.dot + h.word + h.space + h.word + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		return new ArmInstPseudoDirectiveSingleArg(match.group(1), match.group(2));
	}
	
	@Override
	public String getName() {
		return "DirectiveSingleArg";
	}
}
