package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstPseudoDirective extends ArmInst implements IArmInstPrintable, IArmInstParsable {
	public String name;

	public ArmInstPseudoDirective(String name) {
		super();
		this.name = name;
	}

	@Override
	public String print() {
		return "." + name;
	}

	private Pattern regex;
	
	public ArmInstPseudoDirective() {
		valid = false;
	}
	
	@Override
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.dot + h.word + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		return new ArmInstPseudoDirective(match.group(1));
	}
	
	@Override
	public String getName() {
		return "Directive";
	}
}
