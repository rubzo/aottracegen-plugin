package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.ArmConditionCode;
import eu.whrl.aottracegen.armgen.ArmOpcode;
import eu.whrl.aottracegen.armgen.RegexHelper;

public class ArmInstOp extends ArmInst implements IArmInstPrintable, IArmInstParsable {
	public ArmOpcode opcode;
	public ArmConditionCode cc;
	public boolean setsFlags;
	public String specifier;
	
	public ArmInstOp(String opcodeString) {
		super();
		setsFlags = false;
		specifier = "";
		parseOpcode(opcodeString);
		if (isInvalid()) {
			System.out.println("Couldn't read opcode: " + opcodeString);
			System.exit(1);
		}
	}
	
	public boolean isInvalid() {
		return (opcode == ArmOpcode.INVALID || cc == ArmConditionCode.INVALID);
	}
	
	private void parseOpcode(String opcodeString) {
		if (opcodeString.contains(".")) {
			String[] elems = opcodeString.split("\\.");
			opcodeString = elems[0];
			specifier = elems[1];
		}
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
		if (!specifier.equals("")) {
			opcodeString += "." + specifier;
		}
		return opcodeString;
	}
	
	@Override
	public String print() {
		return getOpcodeAsString();
	}
	
	private Pattern regex;
	
	public ArmInstOp() {
		valid = false;
	}
	
	@Override 
	public void setupRegex(RegexHelper h) {
		regex = Pattern.compile(h.start + h.word + h.end);
	}
	
	@Override
	public Pattern getRegex() {
		return regex;
	}
	
	@Override
	public ArmInst getInst(Matcher match, RegexHelper h) {
		return new ArmInstOp(match.group(1));
	}
	
	@Override
	public String getName() {
		return "Op";
	}
}
