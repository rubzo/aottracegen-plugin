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

import eu.whrl.aottracegen.converters.BytecodeToCConverter;
import eu.whrl.aottracegen.converters.BytecodeToStringConverter;
import eu.whrl.aottracegen.exceptions.UnimplementedInstructionException;

public class CTraceGenerator {
	private CodeGenContext context;
	private FileWriter writer;
	private boolean prepared;
	private BytecodeToCConverter converter;
	private BytecodeToStringConverter stringConverter;
	
	private static Set<Opcode> opcodesThatNeedHelperFunctions;
	static {
		opcodesThatNeedHelperFunctions = new TreeSet<Opcode>();
		opcodesThatNeedHelperFunctions.add(Opcode.IGET_QUICK);
		
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
		opcodesThatRaiseExceptions.add(Opcode.AGET_BYTE);
		opcodesThatRaiseExceptions.add(Opcode.APUT);
		
		// ...
	}
	
	//
	// Actual methods start here...
	//
	public CTraceGenerator(CodeGenContext context) {
		this.context = context;
		writer = null;
		prepared = false;
		converter = new BytecodeToCConverter();
		stringConverter = new BytecodeToStringConverter();
	}
	
	public void prepare(String name) {
		try {
			File cFile = new File(name);
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
	
	public void generate() throws UnimplementedInstructionException {
		if (!prepared) {
			return;
		}
		
		Trace curTrace = context.getCurrentTrace();
		
		try {
			// Everything that this calls MUST throw the IOException back up here!
			emitFunctions(writer);
			emitExitFunctionPrototypes(writer);
			emitFunctionStart(writer);
			
			for (int i = 0; i < curTrace.length; i++) {
				emitForCodeAddress(writer, curTrace.addresses[i]);
				
				// If we're the last instruction...
				if (i == curTrace.length - 1 ) {
					int codeAddress = curTrace.addresses[i];
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
	
	/*
	 * Emit the helper functions that will be used within the trace function.
	 */
	private void emitFunctions(Writer writer) throws IOException {
		writer.write("// --- FUNCTIONS ---\n");
		
		Trace curTrace = context.getCurrentTrace();
		
		for (int i = 0; i < context.getCurrentTrace().length; i++) {
			Instruction instruction = context.getInstructionAtCodeAddress(curTrace.addresses[i]);
			
			if (opcodesThatNeedHelperFunctions.contains(instruction.opcode) && !curTrace.meta.generatedFunctionOpcodes.contains(instruction.opcode)) {
				curTrace.meta.generatedFunctionOpcodes.add(instruction.opcode);
				writer.write(opcodeFunctionHelpers.get(instruction.opcode));
			}
		}
		
		writer.write("\n");
	}
	
	private void emitExitFunctionPrototypes(Writer writer) throws IOException {
		writer.write("// --- EXIT FUNCTION PROTOTYPES ---\n");
		
		Trace curTrace = context.getCurrentTrace();
		
		for (int i = 0; i < curTrace.length; i++) {
			int codeAddress = curTrace.addresses[i];
			
			Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
			if (opcodesThatRaiseExceptions.contains(instruction.opcode)) {
				writer.write(String.format("void exception_L0x%x() {return;}\n", codeAddress));
			}
		}
		
		for (int i = 0; i < curTrace.successorsCount; i++) {
			int successorAddress = curTrace.successors[i];
			
			writer.write(String.format("void exit_L0x%x() {return;}\n", successorAddress));
		}
		
		writer.write("\n");
	}
	
	private void emitFunctionStart(Writer writer) throws IOException {
		writer.write(String.format("// --- TRACE 0x%x START ---\n", context.getCurrentTraceEntryAddress()));
		writer.write("void trace(int* v, char* self, int *lit) {\n");
	}
	
	private void emitForCodeAddress(Writer writer, int codeAddress) throws IOException, UnimplementedInstructionException {
		writer.write(stringConverter.convert(context, codeAddress));
		writer.write(String.format("  __L0x%x:\n", codeAddress));
		
		writer.write(converter.convert(context, codeAddress));
		
		writer.write("\n");
	}
	
	private void emitExitLabels(Writer writer) throws IOException {
		writer.write("  // --- EXIT LABELS ---\n");
		
		Trace curTrace = context.getCurrentTrace();
		
		for (int i = 0; i < curTrace.length; i++) {
			int codeAddress = curTrace.addresses[i];
			
			Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
			if (opcodesThatRaiseExceptions.contains(instruction.opcode)) {
				writer.write(String.format("  __exception_L0x%1$x: exception_L0x%1$x(); return;\n", codeAddress));
			}
		}
		
		for (int i = 0; i < curTrace.successorsCount; i++) {
			int successorAddress = curTrace.successors[i];
			
			writer.write(String.format("  __exit_L0x%1$x: exit_L0x%1$x(); return;\n", successorAddress));
		}
		
		writer.write("\n");
	}
	
	private void emitFunctionEnd(Writer writer) throws IOException {
		writer.write("}\n");
	}
}
