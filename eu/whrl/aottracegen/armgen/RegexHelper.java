package eu.whrl.aottracegen.armgen;

import eu.whrl.aottracegen.armgen.insts.NotParsableException;

public class RegexHelper {
	public final String start = "^";
	public final String end = "$";
	public final String imm = "#?(-?0x[0-9a-fA-F]+?|-?[0-9]+?)";
	public final String reg = "((?:r\\d+|s\\d+|d\\d+|q\\d+|ip|sp|fp|lr|pc)(?:!|\\[\\d+\\])?)";
	public final String word = "([^\\s]+?)";
	public final String itOpcode = "(it|itt|ite|ittt|itte|itet|itee|itttt|ittte|ittet|ittee|itett|itete|iteet|iteee)"; // yep
	public final String cc = "(eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al)";
	public final String shiftOpcode = "(asr|lsr|lsl)";
	public final String groupInnards = "([^\\}]+?)";
	public final String space = " ";
	public final String colon = ":";
	public final String commaSpace = ", ";
	public final String dot = "\\.";
	public final String lbrace = "\\[";
	public final String rbrace = "\\]";
	public final String lcurly = "\\{";
	public final String rcurly = "\\}";
	public final String possibleBang = "(!?)";
	
	public ArmRegister readReg(String reg) throws NotParsableException {
		try {
			if (reg.endsWith("!")) {
				reg = reg.substring(0, reg.length() - 1);
			} else if (reg.endsWith("]")) {
				reg = reg.substring(0, reg.indexOf('['));
			}
			return ArmRegister.valueOf(reg.trim());
		} catch (IllegalArgumentException e) {
			throw new NotParsableException();
		}
	}

	public ArmConditionCode readCC(String cc) throws NotParsableException {
		try {
			return ArmConditionCode.valueOf(cc.trim());
		} catch (IllegalArgumentException e) {
			throw new NotParsableException();
		}
	}

	public int readImm(String imm) throws NotParsableException {
		try {
			if (imm.startsWith("0x")) {
				String trimmedImm = imm.trim();
				trimmedImm = trimmedImm.substring(2, trimmedImm.length());
				if (trimmedImm.equals("ffffffff")) {
					return -1;
				}
				return Integer.parseInt(trimmedImm, 16);
			}
			return Integer.parseInt(imm.trim());
		} catch (NumberFormatException e) {
			throw new NotParsableException();
		}
	}
}
