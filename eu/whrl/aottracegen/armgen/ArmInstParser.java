package eu.whrl.aottracegen.armgen;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.insts.ArmInst;
import eu.whrl.aottracegen.armgen.insts.ArmInstOp;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpIfThen;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpL;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpMultiple;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpR;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRI;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRL;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRM;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRMI;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRMO;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRMR;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRMultiple;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRR;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRRI;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRRR;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRRRR;
import eu.whrl.aottracegen.armgen.insts.ArmInstOpRRRS;
import eu.whrl.aottracegen.armgen.insts.ArmInstPseudoDirective;
import eu.whrl.aottracegen.armgen.insts.ArmInstPseudoDirectiveSingleArg;
import eu.whrl.aottracegen.armgen.insts.ArmInstPseudoLabel;
import eu.whrl.aottracegen.armgen.insts.IArmInstParsable;
import eu.whrl.aottracegen.armgen.insts.NotParsableException;

public class ArmInstParser {
	private List<IArmInstParsable> parsers;
	private RegexHelper regexHelper;

	public ArmInstParser() {
		regexHelper = new RegexHelper();
		
		/* this defines the parsing order */
		parsers = new LinkedList<IArmInstParsable>();
		parsers.add(new ArmInstPseudoLabel());
		parsers.add(new ArmInstPseudoDirective());
		parsers.add(new ArmInstPseudoDirectiveSingleArg());
		parsers.add(new ArmInstOp());
		parsers.add(new ArmInstOpMultiple());
		parsers.add(new ArmInstOpRMultiple());
		parsers.add(new ArmInstOpIfThen());
		parsers.add(new ArmInstOpR());
		parsers.add(new ArmInstOpRI());
		parsers.add(new ArmInstOpRM());
		parsers.add(new ArmInstOpRRI());
		parsers.add(new ArmInstOpRMO());
		parsers.add(new ArmInstOpRMI());
		parsers.add(new ArmInstOpRMR());
		parsers.add(new ArmInstOpRRRS());
		parsers.add(new ArmInstOpRRRR());
		parsers.add(new ArmInstOpRRR());
		parsers.add(new ArmInstOpRR());
		parsers.add(new ArmInstOpL());
		parsers.add(new ArmInstOpRL());
		
		/* tell these parsers to setup their regexes */
		for (IArmInstParsable parser : parsers) {
			parser.setupRegex(regexHelper);
		}
	}
	
	public ArmInst parse(String line) throws NotParsableException {
		/* go through each parser in turn */
		for (IArmInstParsable parser : parsers) {
			Pattern regex = parser.getRegex();
			/* does this parser have a regex set? */
			if (regex == null) {
				continue;
			}
			/* does its regex match this line? */
			Matcher match = regex.matcher(line);
			if (match.find()) {
				/* tell the parser to construct an instruction */
				/* NB: parsing could still fail at this point, e.g. if an illegal register is used */
				ArmInst inst = parser.getInst(match, regexHelper);
				if (inst != null) {
					/* parsing was successful */
					System.out.println(String.format("PARSE => %s: '%s'", parser.getName(), line));
					return inst;
				}
			}
		}
		
		/* we tried every parser, and got nothing */
		throw new NotParsableException();
	}
}
