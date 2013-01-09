package eu.whrl.aottracegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;

import eu.whrl.aottracegen.converters.BytecodeToCConverter;
import eu.whrl.aottracegen.converters.BytecodeToPrettyConverter;
import eu.whrl.aottracegen.converters.BytecodeToUnsafeCConverter;
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
		
		// ...
	}
	
	private static Set<Opcode> opcodesThatCanReturn;
	static {
		opcodesThatCanReturn = new TreeSet<Opcode>();
		opcodesThatCanReturn.add(Opcode.RETURN);
		opcodesThatCanReturn.add(Opcode.RETURN_OBJECT);
		opcodesThatCanReturn.add(Opcode.RETURN_VOID);
		opcodesThatCanReturn.add(Opcode.RETURN_VOID_BARRIER);
		opcodesThatCanReturn.add(Opcode.RETURN_WIDE);
	}
	
	private static Set<Opcode> opcodesWithHotChainingCells;
	static {
		opcodesWithHotChainingCells = new TreeSet<Opcode>();
		opcodesWithHotChainingCells.add(Opcode.MOVE_RESULT);
	}
	
	// This is a mapping from the Opcodes that need helper functions
	// to the string that contains that helper function.
	private static Map<Opcode,String> opcodeFunctionHelpers;
	static {
		opcodeFunctionHelpers = new TreeMap<Opcode,String>();
		// ...
	}
	
	// This is a set of Opcodes that will throw exceptions.
	private static Set<Opcode> opcodesThatThrowExceptions;
	static {
		opcodesThatThrowExceptions = new TreeSet<Opcode>();
		opcodesThatThrowExceptions.add(Opcode.AGET);
		opcodesThatThrowExceptions.add(Opcode.AGET_BYTE);
		opcodesThatThrowExceptions.add(Opcode.APUT);
		opcodesThatThrowExceptions.add(Opcode.RETURN);
		opcodesThatThrowExceptions.add(Opcode.RETURN_OBJECT);
		opcodesThatThrowExceptions.add(Opcode.RETURN_VOID);
		opcodesThatThrowExceptions.add(Opcode.RETURN_VOID_BARRIER);
		opcodesThatThrowExceptions.add(Opcode.RETURN_WIDE);
		opcodesThatThrowExceptions.add(Opcode.IGET_QUICK);
		opcodesThatThrowExceptions.add(Opcode.IGET_WIDE_QUICK);
		opcodesThatThrowExceptions.add(Opcode.IPUT_QUICK);
		opcodesThatThrowExceptions.add(Opcode.IPUT_WIDE_QUICK);
		opcodesThatThrowExceptions.add(Opcode.INVOKE_VIRTUAL_QUICK);
		
		// ...
	}
	
	/*
	 * Constructor for the CTraceGenerator.
	 */
	public CTraceGenerator(CodeGenContext context) {
		this.context = context;
		writer = null;
		prepared = false;
		if (context.config.produceUnsafe) {
			converter = new BytecodeToUnsafeCConverter(context);
		} else {
			converter = new BytecodeToCConverter(context);
		}
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
		
		curTrace.calculateRegisterInteraction(context);
		
		for (int successor : curTrace.successors) {
			curTrace.meta.chainingCells.put(successor, new ChainingCell(ChainingCell.Type.NORMAL, successor));
		}
		
		try {
			// Everything that this calls MUST throw the IOException back up here!
			if (context.config.avoidVirtualRegs) {
				emitSaveVregsMacro();
				emitLoadVregsMacro();
			}
			emitHelperFunctions();
			emitExitFunctionPrototypes();
			emitFunctionStart();
			if (context.config.avoidVirtualRegs) {
				emitVirtualRegs();
			}
			
			for (int i = 0; i < curTrace.getLength(); i++) {
				emitForCodeAddress(curTrace.addresses.get(i));
				
				// If we're the last instruction, make sure we jump to the correct exit.
				int codeAddress = curTrace.addresses.get(i);
				Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
				int nextAddress = context.getNextCodeAddress(codeAddress, instruction);
				
				if (needControlFlow(curTrace, i, nextAddress)) {
					writer.write("  " + converter.getGotoLabel(curTrace, nextAddress) + ";\n\n");
				}
				
			 	updateChainingCells(curTrace, instruction, codeAddress, nextAddress);
			}
			
			emitExitLabels();
			emitFunctionEnd();
			
		} catch (IOException e) {
			System.err.println("Couldn't write to C file!");
			throw new CGeneratorFaultException();
		}
	}
	
	private void emitSaveVregsMacro() throws IOException {
		Trace curTrace = context.getCurrentTrace();
		
		writer.write("#define SAVE_VREGS \\\n");
		String continueLine = "\\";
		
		int i = 0;
		for (int reg : curTrace.meta.dirtyRegs) {
			if (i == (curTrace.meta.dirtyRegs.size()-1)) {
				continueLine = "";
			}
			writer.write(String.format("v[%1$d] = v%1$d;%2$s\n", reg, continueLine));
		}
		writer.write("\n");
		
	}
	
	private void emitLoadVregsMacro() throws IOException {
		Trace curTrace = context.getCurrentTrace();
	
		writer.write("#define LOAD_VREGS \\\n");
		String continueLine = "\\";
		
		int i = 0;
		for (int reg : curTrace.meta.readRegs) {
			if (i == (curTrace.meta.readRegs.size()-1)) {
				continueLine = "";
			}
			writer.write(String.format("v%1$d = v[%1$d];%2$s\n", reg, continueLine));
		}
		writer.write("\n");
		
	}

	private void emitVirtualRegs() throws IOException {
		Trace curTrace = context.getCurrentTrace();
		
		writer.write("  // Copy virtual register array into local vars\n");
		for (int i = 0; i < context.virtualRegisterCount; i++) {
			if (curTrace.meta.readRegs.contains(i)) {
				writer.write(String.format("  int v%1$d = v[%1$d];\n", i));
			} else {
				writer.write(String.format("  int v%1$d;\n", i));
			}
		}
		writer.write("\n");
	}

	private void updateChainingCells(Trace curTrace, Instruction instruction, int codeAddress, int nextAddress) {
		if (instruction.opcode == Opcode.INVOKE_VIRTUAL_QUICK) {
			curTrace.meta.chainingCells.put(codeAddress, (new ChainingCell(ChainingCell.Type.INVOKE_PREDICTED, codeAddress)));
		} else if (opcodesWithHotChainingCells.contains(instruction.opcode) && !curTrace.containsCodeAddress(nextAddress)) {
			curTrace.meta.chainingCells.get(nextAddress).type = ChainingCell.Type.HOT;
		}
	}

	private boolean needControlFlow(Trace trace, int currentAddressIdx, int nextAddress) {
		if (opcodesThatCanReturn.contains(context.getInstructionAtCodeAddress(trace.addresses.get(currentAddressIdx)).opcode)) {
			return false;
		}
		if (currentAddressIdx == (trace.getLength() - 1) && !trace.successors.isEmpty()) {
			return true;
		}
		if (currentAddressIdx == (trace.getLength() - 1) && trace.successors.isEmpty()) {
			return false;
		}
		if (trace.addresses.get(currentAddressIdx+1) != nextAddress) {
			return true;
		}
		return false;
	}
	
	/*
	 * Emit the helper functions that will be used within the trace function.
	 */
	private void emitHelperFunctions() throws IOException {
		writer.write("// --- FUNCTIONS ---\n");
		
		Trace curTrace = context.getCurrentTrace();
		
		for (int i = 0; i < context.getCurrentTrace().getLength(); i++) {
			Instruction instruction = context.getInstructionAtCodeAddress(curTrace.addresses.get(i));
			
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
	private void emitExitFunctionPrototypes() throws IOException {
		writer.write("// --- EXIT FUNCTION PROTOTYPES ---\n");
		
		Trace curTrace = context.getCurrentTrace();
		
		// Generate the exception function prototypes.
		for (int i = 0; i < curTrace.getLength(); i++) {
			int codeAddress = curTrace.addresses.get(i);
			
			Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
			if (opcodesThatThrowExceptions.contains(instruction.opcode)) {
				writer.write(String.format("void exception_L%#x() {return;}\n", codeAddress));
				curTrace.meta.codeAddressesThatThrowExceptions.add(codeAddress);
			}
			
			if (opcodesThatCanReturn.contains(instruction.opcode)) {
				writer.write(String.format("void return_L%#x() {return;}\n", codeAddress));
			}
		}
		
		// Generate the exit function prototypes.
		for (int successorAddress : curTrace.successors) {
			writer.write(String.format("void exit_L%#x() {return;}\n", successorAddress));
		}
		
		writer.write("\n");
	}
	
	/*
	 * Emit the function signature, basically.
	 */
	private void emitFunctionStart() throws IOException {
		writer.write("// --- INTERPRETER REGISTER PROTECTION ---\n");

		// Taken from vm/compiler/codegen/arm/Thumb2/Factory.cpp
		writer.write("register int *v asm (\"r5\");\n");
		writer.write("register char *self asm (\"r6\");\n");
		writer.write("register int *reserved1 asm (\"r13\");\n");
		writer.write("register int *reserved2 asm (\"r14\");\n");
		writer.write("register int *reserved3 asm (\"r15\");\n");

		writer.write("\n");
		writer.write(String.format("// --- TRACE %#x START ---\n", context.getCurrentTrace().entry));
		writer.write("void trace(int *lit) {\n");
		
	}
	
	/*
	 * Emit the comment, label and actual C for the given instruction.
	 */
	private void emitForCodeAddress(int codeAddress) throws IOException, UnimplementedInstructionException {
		writer.write(stringConverter.convert(context, codeAddress));
		writer.write(String.format("  __L%#x:\n", codeAddress));
		writer.write(converter.convert(context, codeAddress));
		writer.write("\n");
	}
	
	/*
	 * Emit the exit labels that call functions. This will be required to correctly identify where the trace is going
	 * when we're generating our injectable trace.
	 */
	private void emitExitLabels() throws IOException {
		writer.write("  // --- EXIT LABELS ---\n");
		
		Trace curTrace = context.getCurrentTrace();
		
		String saveVRegsString = "";
		if (context.config.avoidVirtualRegs) {
			saveVRegsString = "SAVE_VREGS";
		}
		
		// Generate the exception labels.
		for (int i = 0; i < curTrace.getLength(); i++) {
			int codeAddress = curTrace.addresses.get(i);
			
			Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
			if (opcodesThatThrowExceptions.contains(instruction.opcode)) {
				writer.write(String.format("  __exception_L%1$#x: %2$s exception_L%1$#x(); return;\n", codeAddress, saveVRegsString));
			}
			
			if (opcodesThatCanReturn.contains(instruction.opcode)) {
				writer.write(String.format("  __return_L%1$#x: return_L%1$#x(); return;\n", codeAddress));
			}
		}
		
		// Generate the exit labels.
		for (int successorAddress : curTrace.successors) {
			writer.write(String.format("  __exit_L%1$#x: %2$s exit_L%1$#x(); return;\n", successorAddress, saveVRegsString));
		}
		
		writer.write("\n");
	}
	
	/*
	 * Emit the end of the function, basically!
	 */
	private void emitFunctionEnd() throws IOException {
		writer.write("}\n");
	}
}
