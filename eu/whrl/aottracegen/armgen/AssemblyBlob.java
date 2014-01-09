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
		ArmInstParser parser = new ArmInstParser();
		
		ArmInst latestInst = new ArmInstComment("-- Begin parsed AssemblyBlob --");
		
		insts = latestInst;

		for (String line : instsList) {
			if (lineIsImportant(line)) {
				try {
					ArmInst newInst = parser.parse(line);
					newInst.linkToPrevious(latestInst);
					latestInst = newInst;
				} catch (NotParsableException e) {
					System.err.println("Unable to parse: '" + line + "'");
					System.exit(1);
				}
			}
		}
	}
	
	public void cleanup(CodeGenContext context) {
		insts = processor.fixupEntryAndExits(context, insts);
		insts = processor.renameLabels(context, insts);
		insts = processor.emitFunctionCalls(context, insts);
		if (!context.config.armMode && context.config.enableRemoveCBZs) {
			insts = processor.removeCBZ(context, insts);
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
