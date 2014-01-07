package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmInstParser {
	private static final String start = "^";
	private static final String end = "$";
	private static final String imm = "#?(-?0x[0-9a-fA-F]+?|-?[0-9]+?)";
	private static final String reg = "((?:r\\d+|s\\d+|d\\d+|ip|sp|fp|lr|pc)!?)";
	private static final String word = "([^\\s]+?)";
	private static final String itOpcode = "(it|itt|ite|ittt|itte|itet|itee|itttt|ittte|ittet|ittee|itett|itete|iteet|iteee)"; // yep
	private static final String cc = "(eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al)";
	private static final String shiftOpcode = "(asr|lsr|lsl)";
	private static final String groupInnards = "([^\\}]+?)";
	private static final String space = " ";
	private static final String colon = ":";
	private static final String commaSpace = ", ";
	private static final String dot = "\\.";
	private static final String lbrace = "\\[";
	private static final String rbrace = "\\]";
	private static final String lcurly = "\\{";
	private static final String rcurly = "\\}";
	

	private static Pattern regexArmInstPseudoLabel;
	private static Pattern regexArmInstPseudoDirective;
	private static Pattern regexArmInstPseudoDirectiveSingleArg;
	private static Pattern regexArmInstOp;
	private static Pattern regexArmInstOpMultiple;
	private static Pattern regexArmInstOpRMultiple;
	private static Pattern regexArmInstOpIfThen;
	private static Pattern regexArmInstOpR;
	private static Pattern regexArmInstOpRR;
	private static Pattern regexArmInstOpRRR;
	private static Pattern regexArmInstOpRRRS;
	private static Pattern regexArmInstOpRRRR;
	private static Pattern regexArmInstOpRRI;
	private static Pattern regexArmInstOpRI;
	private static Pattern regexArmInstOpRM;
	private static Pattern regexArmInstOpRMO;
	private static Pattern regexArmInstOpRMR;
	private static Pattern regexArmInstOpRL;
	private static Pattern regexArmInstOpL;

	static {
		regexArmInstPseudoLabel = Pattern.compile(start + word + colon + end);
		regexArmInstPseudoDirective = Pattern.compile(start + dot + word + end);
		regexArmInstPseudoDirectiveSingleArg = Pattern.compile(start + dot + word + space + word + end);
		regexArmInstOp = Pattern.compile(start + word + end);
		regexArmInstOpMultiple = Pattern.compile(start + word + space + lcurly + groupInnards + rcurly + end);
		regexArmInstOpRMultiple = Pattern.compile(start + word + space + reg + commaSpace + lcurly + groupInnards + rcurly + end);
		regexArmInstOpIfThen = Pattern.compile(start + itOpcode + space + cc + end);
		regexArmInstOpR = Pattern.compile(start + word + space + reg + end);
		regexArmInstOpRR = Pattern.compile(start + word + space + reg + commaSpace + reg + end);
		regexArmInstOpRRR = Pattern.compile(start + word + space + reg + commaSpace + reg + commaSpace + reg + end);
		regexArmInstOpRRRR = Pattern.compile(start + word + space + reg + commaSpace + reg + commaSpace + reg + commaSpace + reg + end);
		regexArmInstOpRRRS = Pattern.compile(start + word + space + reg + commaSpace + reg + commaSpace + reg + commaSpace + shiftOpcode + space + imm + end);
		regexArmInstOpRRI = Pattern.compile(start + word + space + reg + commaSpace + reg + commaSpace + imm + end);
		regexArmInstOpRI = Pattern.compile(start + word + space + reg + commaSpace + imm + end);
		regexArmInstOpRM = Pattern.compile(start + word + space + reg + commaSpace + lbrace + reg + rbrace + end);
		regexArmInstOpRMO = Pattern.compile(start + word + space + reg + commaSpace + lbrace + reg + commaSpace + imm + rbrace + end);
		regexArmInstOpRMR = Pattern.compile(start + word + space + reg + commaSpace + lbrace + reg + commaSpace + reg + rbrace + end);
		regexArmInstOpL = Pattern.compile(start + word + space + word + end);
		regexArmInstOpRL = Pattern.compile(start + word + space + reg + commaSpace + word + end);
	}

	private static ArmRegister readReg(String reg) throws NotParsableException {
		try {
			if (reg.endsWith("!")) {
				reg = reg.substring(0, reg.length() - 1);
			}
			return ArmRegister.valueOf(reg.trim());
		} catch (IllegalArgumentException e) {
			throw new NotParsableException();
		}
	}

	private static ArmConditionCode readCC(String cc) throws NotParsableException {
		try {
			return ArmConditionCode.valueOf(cc.trim());
		} catch (IllegalArgumentException e) {
			throw new NotParsableException();
		}
	}

	private static int readImm(String imm) {
		if (imm.startsWith("0x")) {
			String trimmedImm = imm.trim();
			trimmedImm = trimmedImm.substring(2, trimmedImm.length());
			return Integer.parseInt(trimmedImm, 16);
		}
		return Integer.parseInt(imm.trim());
	}

	public static ArmInst parse(String line) throws NotParsableException {
		try { return parseArmInstPseudoLabel(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstPseudoDirective(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstPseudoDirectiveSingleArg(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOp(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpMultiple(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRMultiple(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpIfThen(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpR(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRI(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRM(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRRI(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRMO(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRMR(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRRRS(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRRRR(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRRR(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRR(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpL(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRL(line); } 
		catch (NotParsableException e) {}

		throw new NotParsableException();
	}

	private static ArmInst parseArmInstPseudoLabel(String line) throws NotParsableException {
		Matcher match = regexArmInstPseudoLabel.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstPseudoLabel newInst = new ArmInstPseudoLabel(match.group(1));
		System.out.println("PARSE: Label: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstPseudoDirective(String line) throws NotParsableException {
		Matcher match = regexArmInstPseudoDirective.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstPseudoDirective newInst = new ArmInstPseudoDirective(match.group(1));
		System.out.println("PARSE: Directive: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstPseudoDirectiveSingleArg(String line) throws NotParsableException {
		Matcher match = regexArmInstPseudoDirectiveSingleArg.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstPseudoDirectiveSingleArg newInst = new ArmInstPseudoDirectiveSingleArg(match.group(1), match.group(2));
		System.out.println("PARSE: DirectiveSingleArg: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOp(String line) throws NotParsableException {
		Matcher match = regexArmInstOp.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOp newInst = new ArmInstOp(match.group(1));
		System.out.println("PARSE: Op: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpIfThen(String line) throws NotParsableException {
		Matcher match = regexArmInstOpIfThen.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpIfThen newInst = new ArmInstOpIfThen(match.group(1), readCC(match.group(2)));
		System.out.println("PARSE: OpIfThen: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpR(String line) throws NotParsableException {
		Matcher match = regexArmInstOpR.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpR newInst = new ArmInstOpR(match.group(1), readReg(match.group(2)));
		System.out.println("PARSE: OpR: " + line);
		return newInst;
	}
	
	private static ArmInst parseArmInstOpL(String line) throws NotParsableException {
		Matcher match = regexArmInstOpL.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpL newInst = new ArmInstOpL(match.group(1), match.group(2));
		System.out.println("PARSE: OpL: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpRR(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRR.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRR newInst = new ArmInstOpRR(match.group(1), readReg(match.group(2)), readReg(match.group(3)));
		System.out.println("PARSE: OpRR: " + line);
		return newInst;
	}
	
	private static ArmInst parseArmInstOpRRRR(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRRRR.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRRRR newInst = new ArmInstOpRRRR(match.group(1), readReg(match.group(2)), readReg(match.group(3)), readReg(match.group(4)), readReg(match.group(5)));
		System.out.println("PARSE: OpRRRR: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpRRRS(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRRRS.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRRRS newInst = new ArmInstOpRRRS(match.group(1), readReg(match.group(2)), readReg(match.group(3)), readReg(match.group(4)), match.group(5), readImm(match.group(6)));
		System.out.println("PARSE: OpRRRS: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpRRR(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRRR.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRRR newInst = new ArmInstOpRRR(match.group(1), readReg(match.group(2)), readReg(match.group(3)), readReg(match.group(4)));
		System.out.println("PARSE: OpRRR: " + line);
		return newInst;
	}
	
	private static ArmInst parseArmInstOpRL(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRL.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRL newInst = new ArmInstOpRL(match.group(1), readReg(match.group(2)), match.group(3));
		System.out.println("PARSE: OpRL: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpRRI(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRRI.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRRI newInst = new ArmInstOpRRI(match.group(1), readReg(match.group(2)), readReg(match.group(3)), readImm(match.group(4)));
		System.out.println("PARSE: OpRRI: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpRI(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRI.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRI newInst = new ArmInstOpRI(match.group(1), readReg(match.group(2)), readImm(match.group(3)));
		System.out.println("PARSE: OpRI: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpRM(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRM.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRM newInst = new ArmInstOpRM(match.group(1), readReg(match.group(2)), readReg(match.group(3)));
		System.out.println("PARSE: OpRM: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpRMO(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRMO.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRMO newInst = new ArmInstOpRMO(match.group(1), readReg(match.group(2)), readReg(match.group(3)), readImm(match.group(4)));
		System.out.println("PARSE: OpRMO: " + line);
		return newInst;
	}
	
	private static ArmInst parseArmInstOpRMR(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRMR.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRMR newInst = new ArmInstOpRMR(match.group(1), readReg(match.group(2)), readReg(match.group(3)), readReg(match.group(4)));
		System.out.println("PARSE: OpRMR: " + line);
		return newInst;
	}

	private static ArmInst parseArmInstOpMultiple(String line) throws NotParsableException {
		Matcher match = regexArmInstOpMultiple.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpMultiple newInst = new ArmInstOpMultiple(match.group(1));
		String regsString = match.group(2);
		for (String reg : regsString.split(",")) {
			newInst.addRegister(readReg(reg));
		}
		System.out.println("PARSE: OpMultiple: " + line);
		return newInst;
	}
	
	private static ArmInst parseArmInstOpRMultiple(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRMultiple.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		String baseRegString = match.group(2);
		
		ArmInstOpRMultiple newInst = new ArmInstOpRMultiple(match.group(1), readReg(baseRegString));
		if (baseRegString.trim().endsWith("!")) {
			newInst.autoIndex = true;
		}
		String regsString = match.group(3);
		for (String reg : regsString.split(",")) {
			newInst.addRegister(readReg(reg));
		}
		System.out.println("PARSE: OpRMultiple: " + line);
		return newInst;
	}
}
