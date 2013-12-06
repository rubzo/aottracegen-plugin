package eu.whrl.aottracegen.armgen.insts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmInstParser {
	private static Pattern regexArmInstPseudoLabel;
	private static Pattern regexArmInstOp;
	private static Pattern regexArmInstOpMultiple;
	private static Pattern regexArmInstOpR;
	private static Pattern regexArmInstOpRR;
	private static Pattern regexArmInstOpRI;
	private static Pattern regexArmInstOpRM;
	private static Pattern regexArmInstOpRMO;

	static {
		regexArmInstPseudoLabel = Pattern.compile("^([^\\s]+?):$");
		regexArmInstOp = Pattern.compile("^([^\\s]+?)$");
		regexArmInstOpMultiple = Pattern.compile("^([^\\s]+)[^\\{]+\\{([^\\}]+?)\\}$");
		regexArmInstOpR = Pattern.compile("^([^\\s]+?) ([^\\s]+?)$");
		regexArmInstOpRR = Pattern.compile("^([^\\s]+?) ([^\\s]+?), ([^\\s]+?)$");
		regexArmInstOpRI = Pattern.compile("^([^\\s]+?) ([^\\s]+?), #(-?\\d+?)$");
		regexArmInstOpRM = Pattern.compile("^([^\\s]+?) ([^\\s]+?), \\[([^\\s]+?)\\]$");
		regexArmInstOpRMO = Pattern.compile("^([^\\s]+?) ([^\\s]+?), \\[([^\\s]+?), #(-?\\d+?)\\]$");
	}

	private static ArmRegister readReg(String reg) {
			return ArmRegister.valueOf(reg.trim());
	}

	private static int readImm(String imm) {
			return Integer.parseInt(imm.trim());
	}

	public static ArmInst parse(String line) throws NotParsableException {
		try { return parseArmInstPseudoLabel(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOp(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpMultiple(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpR(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRI(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRM(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRMO(line); } 
		catch (NotParsableException e) {}
		try { return parseArmInstOpRR(line); } 
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

	private static ArmInst parseArmInstOp(String line) throws NotParsableException {
		Matcher match = regexArmInstOp.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOp newInst = new ArmInstOp(match.group(1));
		System.out.println("PARSE: Op: " + line);
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

	private static ArmInst parseArmInstOpRR(String line) throws NotParsableException {
		Matcher match = regexArmInstOpRR.matcher(line);
		if (!match.find()) {
			throw new NotParsableException();
		}
		ArmInstOpRR newInst = new ArmInstOpRR(match.group(1), readReg(match.group(2)), readReg(match.group(3)));
		System.out.println("PARSE: OpRR: " + line);
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
}
