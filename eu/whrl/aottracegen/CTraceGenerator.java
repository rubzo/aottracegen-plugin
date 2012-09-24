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
import eu.whrl.aottracegen.converters.BytecodeToPrettyConverter;
import eu.whrl.aottracegen.exceptions.CGeneratorFaultException;
import eu.whrl.aottracegen.exceptions.UnimplementedInstructionException;

public class CTraceGenerator {
	private CodeGenContext context;
	private FileWriter writer;
	private boolean prepared;
	private BytecodeToCConverter converter;
	private BytecodeToPrettyConverter stringConverter;
	
	//
	// Section where we define...
	//
	
	// This is a set of Opcodes that need helper functions to be generated.
	private static Set<Opcode> opcodesThatNeedHelperFunctions;
	static {
		opcodesThatNeedHelperFunctions = new TreeSet<Opcode>();
		opcodesThatNeedHelperFunctions.add(Opcode.IGET_QUICK);
		opcodesThatNeedHelperFunctions.add(Opcode.IPUT_QUICK);
		
		// ...
	}
	
	// This is a mapping from the Opcodes that need helper functions
	// to the string that contains that helper function.
	private static Map<Opcode,String> opcodeFunctionHelpers;
	static {
		opcodeFunctionHelpers = new TreeMap<Opcode,String>();
		opcodeFunctionHelpers.put(Opcode.IGET_QUICK, 
				"inline int iget_quick(int obj, int idx) {\n" +
				"  return *((int*) (((char*)obj) + idx));\n" +
				"}\n");
		opcodeFunctionHelpers.put(Opcode.IPUT_QUICK, 
				"inline int iput_quick(int value, int obj, int idx) {\n" +
				"  *((int*) (((char*)obj) + idx)) = value;\n" +
				"}\n");
		
		// ...
	}
	
	// This is a set of Opcodes that will raise exceptions.
	private static Set<Opcode> opcodesThatRaiseExceptions;
	static {
		opcodesThatRaiseExceptions = new TreeSet<Opcode>();
		opcodesThatRaiseExceptions.add(Opcode.AGET);
		opcodesThatRaiseExceptions.add(Opcode.AGET_BYTE);
		opcodesThatRaiseExceptions.add(Opcode.APUT);
		
		// ...
	}
	
	/*
	 * Constructor for the CTraceGenerator.
	 */
	public CTraceGenerator(CodeGenContext context) {
		this.context = context;
		writer = null;
		prepared = false;
		converter = new BytecodeToCConverter();
		stringConverter = new BytecodeToPrettyConverter();
	}
	
	/*
	 * Opens the 'name' file to write C to it.
	 */
	public void prepare(String name) {
		try {
			File cFile = new File(name);
			writer = new FileWriter(cFile);			
			prepared = true;	
		} catch (IOException e) {
			System.err.println("Couldn't open C file for writing!");
		}
	}
	
	/*
	 * Closes the file we've been writing C to.
	 */
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
	
	/*
	 * Perform the actual generation of C.
	 */
	public void generate() throws UnimplementedInstructionException, CGeneratorFaultException {
		if (!prepared) {
			System.err.println("C generator wasn't prepared?");
			throw new CGeneratorFaultException();
		}
		
		Trace curTrace = context.getCurrentTrace();
		
		try {
			// Everything that this calls MUST throw the IOException back up here!
			emitFunctions(writer);
			emitExitFunctionPrototypes(writer);
			emitFunctionStart(writer);
			
			for (int i = 0; i < curTrace.length; i++) {
				emitForCodeAddress(writer, curTrace.addresses[i]);
				
				// If we're the last instruction, make sure we jump to the correct exit.
				int codeAddress = curTrace.addresses[i];
				Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
				int nextAddress = context.getNextCodeAddress(codeAddress, instruction);
				
				if (i == curTrace.length - 1 || curTrace.addresses[i+1] != nextAddress) {
					String labelPrefix = "__";
					if (!curTrace.containsCodeAddress(nextAddress)) {
						labelPrefix = "__exit_";
					}
					writer.write(String.format("  goto %sL0x%x;\n", labelPrefix, nextAddress));
				}
			}
			
			emitExitLabels(writer);
			emitFunctionEnd(writer);
			
		} catch (IOException e) {
			System.err.println("Couldn't write to C file!");
			throw new CGeneratorFaultException();
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
			
			if (opcodesThatNeedHelperFunctions.contains(instruction.opcode) && !curTrace.meta.opcodesUsedThatNeedHelperFunctions.contains(instruction.opcode)) {
				curTrace.meta.opcodesUsedThatNeedHelperFunctions.add(instruction.opcode);
				writer.write(opcodeFunctionHelpers.get(instruction.opcode));
			}
		}
		
		writer.write("\n");
	}
	
	/*
	 * Emit the functions that exit labels call. This will be required to correctly identify where the trace is going
	 * when we're generating our injectable trace.
	 */
	private void emitExitFunctionPrototypes(Writer writer) throws IOException {
		writer.write("// --- EXIT FUNCTION PROTOTYPES ---\n");
		
		Trace curTrace = context.getCurrentTrace();
		
		// Generate the exception function prototypes.
		for (int i = 0; i < curTrace.length; i++) {
			int codeAddress = curTrace.addresses[i];
			
			Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
			if (opcodesThatRaiseExceptions.contains(instruction.opcode)) {
				writer.write(String.format("void exception_L0x%x() {return;}\n", codeAddress));
				curTrace.meta.codeAddressesRaisingExceptions.add(new Integer(codeAddress));
			}
		}
		
		// Generate the exit function prototypes.
		for (int i = 0; i < curTrace.successorsCount; i++) {
			int successorAddress = curTrace.successors[i];
			
			writer.write(String.format("void exit_L0x%x() {return;}\n", successorAddress));
		}
		
		writer.write("\n");
	}
	
	/*
	 * Emit the function signature, basically.
	 */
	private void emitFunctionStart(Writer writer) throws IOException {
		writer.write(String.format("// --- TRACE 0x%x START ---\n", context.getCurrentTrace().getPrimaryEntry()));
		writer.write("void trace(int pc, int* v, char* self, int *lit) {\n");
	}
	
	/*
	 * Emit the comment, label and actual C for the given instruction.
	 */
	private void emitForCodeAddress(Writer writer, int codeAddress) throws IOException, UnimplementedInstructionException {
		writer.write(stringConverter.convert(context, codeAddress));
		writer.write(String.format("  __L0x%x:\n", codeAddress));
		
		writer.write(converter.convert(context, codeAddress));
		
		
		
		writer.write("\n");
	}
	
	/*
	 * Emit the exit labels that call functions. This will be required to correctly identify where the trace is going
	 * when we're generating our injectable trace.
	 */
	private void emitExitLabels(Writer writer) throws IOException {
		writer.write("  // --- EXIT LABELS ---\n");
		
		Trace curTrace = context.getCurrentTrace();
		
		// Generate the exception labels.
		for (int i = 0; i < curTrace.length; i++) {
			int codeAddress = curTrace.addresses[i];
			
			Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
			if (opcodesThatRaiseExceptions.contains(instruction.opcode)) {
				writer.write(String.format("  __exception_L0x%1$x: exception_L0x%1$x(); return;\n", codeAddress));
			}
		}
		
		// Generate the exit labels.
		for (int i = 0; i < curTrace.successorsCount; i++) {
			int successorAddress = curTrace.successors[i];
			
			writer.write(String.format("  __exit_L0x%1$x: exit_L0x%1$x(); return;\n", successorAddress));
		}
		
		writer.write("\n");
	}
	
	/*
	 * Emit the end of the function, basically!
	 */
	private void emitFunctionEnd(Writer writer) throws IOException {
		writer.write("}\n");
	}
}
