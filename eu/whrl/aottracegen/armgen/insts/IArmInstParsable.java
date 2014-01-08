package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.RegexHelper;

public interface IArmInstParsable {
	public void setupRegex(RegexHelper h);
	public Pattern getRegex();
	public ArmInst getInst(Matcher match, RegexHelper h);
	public String getName();
}
