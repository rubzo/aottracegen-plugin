package eu.whrl.aottracegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;

public class CTraceGenerator {
	private CodeGenContext context;
	private FileWriter writer;
	private boolean prepared;
	private BytecodeToCConverter converter;
	
	private static Set<Opcode> opcodesThatNeedFunctions;
	static {
		opcodesThatNeedFunctions = new TreeSet<Opcode>();
		opcodesThatNeedFunctions.add(Opcode.IGET_QUICK);
		
		// ...
	}
	
	private static Map<Opcode,String> opcodeFunctionHelpers;
	static {
		opcodeFunctionHelpers = new TreeMap<Opcode,String>();
		opcodeFunctionHelpers.put(Opcode.IGET_QUICK, 
				"inline int iget_quick(int obj, int idx) {\n" +
				"  return *((int*) (((char*)obj) + idx));\n" +
				"}\n");
		
		// ...
	}
	
	private static Set<Opcode> opcodesThatRaiseExceptions;
	static {
		opcodesThatRaiseExceptions = new TreeSet<Opcode>();
		opcodesThatRaiseExceptions.add(Opcode.AGET);
		
		// ...
	}
	
	public CTraceGenerator(CodeGenContext context) {
		this.context = context;
		writer = null;
		prepared = false;
		converter = new BytecodeToCConverter();
	}
	
	public void prepare() {
		try {
			File cFile = new File("aottrace_output.c");
			writer = new FileWriter(cFile);			
			prepared = true;	
		} catch (IOException e) {
			System.err.println("Couldn't open C file for writing!");
		}
	}
	
	public void finish() {
		if (!prepared) {
			return;
		}
		
		try {
			writer.close();
		} catch (IOException e) {
			System.err.println("Couldn't close C file?");
		}
	}
	
	public void generate() {
		if (!prepared) {
			return;
		}
		
		try {
			// Everything that this calls MUST throw the IOException back up here!
			emitFunctions(writer);
			emitExitFunctionPrototypes(writer);
			emitFunctionStart(writer);
			for (int i = 0; i < context.trace.length; i++) {
				emitForCodeAddress(writer, context.trace.addresses[i]);
				// If we're the last instruction...
				if (i == context.trace.length - 1 ) {
					int codeAddress = context.trace.addresses[i];
					Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
					int fallThroughAddress = codeAddress + instruction.getSize(codeAddress);
					writer.write(String.format("  goto __exit_L0x%x;\n\n", fallThroughAddress));
				}
			}
			emitExitLabels(writer);
			emitFunctionEnd(writer);
			
			
		} catch (IOException e) {
			System.err.println("Couldn't write to C file!");
		}
	}
	
	private void emitFunctions(Writer writer) throws IOException {
		writer.write("// FUNCTIONS\n");
		
		for (int i = 0; i < context.trace.length; i++) {
			Instruction instruction = context.getInstructionAtCodeAddress(context.trace.addresses[i]);
			
			if (opcodesThatNeedFunctions.contains(instruction.opcode) && !context.generatedFunctionOpcodes.contains(instruction.opcode)) {
				context.generatedFunctionOpcodes.add(instruction.opcode);
				writer.write(opcodeFunctionHelpers.get(instruction.opcode));
			}
		}
		
		
		writer.write("\n");
	}
	
	private void emitExitFunctionPrototypes(Writer writer) throws IOException {
		writer.write("// EXIT FUNCTION PROTOTYPES\n");
		
		for (int i = 0; i < context.trace.length; i++) {
			int codeAddress = context.trace.addresses[i];
			
			Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
			if (opcodesThatRaiseExceptions.contains(instruction.opcode)) {
				writer.write(String.format("void exception_L0x%x() {return;}\n", codeAddress));
			}
		}
		
		for (int i = 0; i < context.trace.successorsCount; i++) {
			int successorAddress = context.trace.successors[i];
			
			writer.write(String.format("void exit_L0x%x() {return;}\n", successorAddress));
		}
		
		writer.write("\n");
	}
	
	private void emitFunctionStart(Writer writer) throws IOException {
		writer.write("void trace(int* v, char* self, int *lit) {\n");
	}
	
	private void emitForCodeAddress(Writer writer, int codeAddress) throws IOException {
		writer.write(String.format("  // BYTECODE AT 0x%x\n", codeAddress));
		writer.write(String.format("  __L0x%x:\n", codeAddress));
		
		writer.write(converter.convert(context, codeAddress));
		
		writer.write("\n");
	}
	
	private void emitExitLabels(Writer writer) throws IOException {
		writer.write("  // EXIT LABELS\n");
		
		for (int i = 0; i < context.trace.length; i++) {
			int codeAddress = context.trace.addresses[i];
			
			Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
			if (opcodesThatRaiseExceptions.contains(instruction.opcode)) {
				writer.write(String.format("  __exception_L0x%1$x: exception_L0x%1$x(); return;\n", codeAddress));
			}
		}
		
		for (int i = 0; i < context.trace.successorsCount; i++) {
			int successorAddress = context.trace.successors[i];
			
			writer.write(String.format("  __exit_L0x%1$x: exit_L0x%1$x(); return;\n", successorAddress));
		}
		
		writer.write("\n");
	}
	
	private void emitFunctionEnd(Writer writer) throws IOException {
		writer.write("}\n");
	}
}
