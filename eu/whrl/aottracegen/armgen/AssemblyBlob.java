package eu.whrl.aottracegen.armgen;


import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.armgen.insts.*;

public class AssemblyBlob {
	private ArmInst insts;
	private AssemblyProcessor processor;
	
	public AssemblyBlob(List<String> instsList) {
		parse(instsList);
		processor = new AssemblyProcessor();
	}
	
	private boolean lineIsImportant(String line) {
		if (line.startsWith("@")) {
			return false;
		}
		if (line.startsWith("#APP")) {
			return false;
		}
		return true;
	}
	
	private void parse(List<String> instsList) {
		ArmInst latestInst = new ArmInstComment("-- Begin parsed AssemblyBlob --");
		
		insts = latestInst;

		for (String line : instsList) {
			if (lineIsImportant(line)) {
				try {
					ArmInst newInst = ArmInstParser.parse(line);
					newInst.linkToPrevious(latestInst);
					latestInst = newInst;
				} catch (NotParsableException e) {
					System.out.println("Unable to parse: " + line);
				}
			}
		}
	}
	
	public void cleanup(CodeGenContext context) {
		processor.modifyPrologueEpilogueCode(context, insts);
		processor.renameLabels(context, insts);
		if (!context.config.armMode && context.config.enableRemoveCBZs) {
			processor.removeCBZ(context, insts);
		}
	}
	
	public void writeOut(FileWriter writer) throws IOException {
		for (ArmInst curInst : insts) {
			if (curInst instanceof ArmInstPseudoLabel) {
				writer.write( ((IArmInstPrintable)curInst).print() + "\n" );
			} else {
				writer.write( "\t" + ((IArmInstPrintable)curInst).print() + "\n" );
			}
		}
	}
}
