package eu.whrl.aottracegen.armgen;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.whrl.aottracegen.armgen.insts.*;

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
		parsers.add(new ArmInstOpMultipleM());
		parsers.add(new ArmInstOpIfThen());
		parsers.add(new ArmInstOpR());
		parsers.add(new ArmInstOpRII());
		parsers.add(new ArmInstOpRI());
		parsers.add(new ArmInstOpRM());
		parsers.add(new ArmInstOpMRS());
		parsers.add(new ArmInstOpMR());
		parsers.add(new ArmInstOpRRS());
		parsers.add(new ArmInstOpRRII());
		parsers.add(new ArmInstOpRRI());
		parsers.add(new ArmInstOpRRMO());
		parsers.add(new ArmInstOpRRM());
		parsers.add(new ArmInstOpRMO());
		parsers.add(new ArmInstOpRMI());
		parsers.add(new ArmInstOpRMR());
		parsers.add(new ArmInstOpRMRS());
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
					//System.out.println("PARSE: " + parser.getName() + " => " + line);
					
					/* but did it work? */
					String input = line;
					String output = ((IArmInstPrintable) inst).print();
					if ( !input.equals(output) ) {
						
						input = input.replace("#", "");
						output = output.replace("#", "");
						
						input = input.replaceAll("\\{[^\\}]+?\\}", "");
						output = output.replaceAll("\\{[^\\}]+?\\}", "");
						
						input = input.replace("0xffffffff", "-1");
						output = output.replace("0xffffffff", "-1");
						
						input = input.replace("0x0", "0");
						output = output.replace("0x0", "0");
						
						if (!input.equals(output)) {
							System.out.println("Mismatch after using: " + parser.getName());
							System.out.println("INPUT : '" + line + "'");
							System.out.println("OUTPUT: '" + ((IArmInstPrintable) inst).print() + "'");
							System.exit(1);
						}
					}
					
					/* yes, it worked */
					return inst;
				}
			}
		}
		
		/* we tried every parser, and got nothing */
		throw new NotParsableException();
	}
}
