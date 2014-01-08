package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmConditionCode;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOpIfThen extends ArmInstOp implements IArmInstPrintable, IArmInstParsable {
	public ArmConditionCode cc;

	public ArmInstOpIfThen(String opcode, ArmConditionCode cc) {
		super(opcode);
		this.cc = cc;
	}

	@Override
	public String print() {
		return String.format("%s %s", getOpcodeAsString(), cc.toString());
	}
	
	private Pattern regex;
	
	public ArmInstOpIfThen() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.itOpcode + h.space + h.cc + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		try {
			return new ArmInstOpIfThen(match.group(1), h.readCC(match.group(2)));
		} catch (NotParsableException e) {
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "OpIfThen";
	}
}
