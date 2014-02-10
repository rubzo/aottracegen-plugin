package eu.whrl.aottracegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
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
	
	public static Set<Opcode> opcodesThatCanReturn;
	static {
		opcodesThatCanReturn = new TreeSet<Opcode>();
		opcodesThatCanReturn.add(Opcode.RETURN);
		opcodesThatCanReturn.add(Opcode.RETURN_OBJECT);
		opcodesThatCanReturn.add(Opcode.RETURN_VOID);
		opcodesThatCanReturn.add(Opcode.RETURN_VOID_BARRIER);
		opcodesThatCanReturn.add(Opcode.RETURN_WIDE);
	}
	
	public static Set<Opcode> opcodesThatCanBranch;
	static {
		opcodesThatCanBranch = new TreeSet<Opcode>();
		opcodesThatCanBranch.add(Opcode.IF_EQ);
		opcodesThatCanBranch.add(Opcode.IF_EQZ);
		opcodesThatCanBranch.add(Opcode.IF_NE);
		opcodesThatCanBranch.add(Opcode.IF_NEZ);
		opcodesThatCanBranch.add(Opcode.IF_LT);
		opcodesThatCanBranch.add(Opcode.IF_LTZ);
		opcodesThatCanBranch.add(Opcode.IF_GE);
		opcodesThatCanBranch.add(Opcode.IF_GEZ);
		opcodesThatCanBranch.add(Opcode.IF_GT);
		opcodesThatCanBranch.add(Opcode.IF_GTZ);
		opcodesThatCanBranch.add(Opcode.IF_LE);
		opcodesThatCanBranch.add(Opcode.IF_LEZ);
		opcodesThatCanBranch.add(Opcode.PACKED_SWITCH);
		opcodesThatCanBranch.add(Opcode.SPARSE_SWITCH);
		opcodesThatCanBranch.add(Opcode.GOTO);
		opcodesThatCanBranch.add(Opcode.GOTO_16);
		opcodesThatCanBranch.add(Opcode.GOTO_32);
	}
	
	private static Set<Opcode> opcodesWithHotChainingCells;
	static {
		opcodesWithHotChainingCells = new TreeSet<Opcode>();
		opcodesWithHotChainingCells.add(Opcode.MOVE_RESULT);
		opcodesWithHotChainingCells.add(Opcode.MOVE_RESULT_WIDE);
		opcodesWithHotChainingCells.add(Opcode.MOVE_RESULT_OBJECT);
	}
	
	// This is a set of Opcodes that will throw exceptions.
	private static Set<Opcode> opcodesThatThrowExceptions;
	static {
		opcodesThatThrowExceptions = new TreeSet<Opcode>();
		opcodesThatThrowExceptions.add(Opcode.AGET);
		opcodesThatThrowExceptions.add(Opcode.AGET_WIDE);
		opcodesThatThrowExceptions.add(Opcode.AGET_BYTE);
		opcodesThatThrowExceptions.add(Opcode.APUT);
		opcodesThatThrowExceptions.add(Opcode.APUT_WIDE);
		opcodesThatThrowExceptions.add(Opcode.RETURN);
		opcodesThatThrowExceptions.add(Opcode.RETURN_OBJECT);
		opcodesThatThrowExceptions.add(Opcode.RETURN_VOID);
		opcodesThatThrowExceptions.add(Opcode.RETURN_VOID_BARRIER);
		opcodesThatThrowExceptions.add(Opcode.RETURN_WIDE);
		opcodesThatThrowExceptions.add(Opcode.IGET_QUICK);
		opcodesThatThrowExceptions.add(Opcode.IGET_WIDE_QUICK);
		opcodesThatThrowExceptions.add(Opcode.IGET_OBJECT_QUICK);
		opcodesThatThrowExceptions.add(Opcode.IPUT_QUICK);
		opcodesThatThrowExceptions.add(Opcode.IPUT_WIDE_QUICK);
		opcodesThatThrowExceptions.add(Opcode.IPUT_OBJECT_QUICK);
		opcodesThatThrowExceptions.add(Opcode.INVOKE_VIRTUAL_QUICK);
		opcodesThatThrowExceptions.add(Opcode.INVOKE_STATIC);
		opcodesThatThrowExceptions.add(Opcode.INVOKE_SUPER_QUICK);
		opcodesThatThrowExceptions.add(Opcode.INVOKE_DIRECT);
		opcodesThatThrowExceptions.add(Opcode.INVOKE_INTERFACE);
		opcodesThatThrowExceptions.add(Opcode.INSTANCE_OF);
		opcodesThatThrowExceptions.add(Opcode.NEW_INSTANCE);
		opcodesThatThrowExceptions.add(Opcode.CHECK_CAST);
		opcodesThatThrowExceptions.add(Opcode.ARRAY_LENGTH);
		
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
		stringConverter = new BytecodeToPrettyConverter(false /* not LLVM mode */);
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
		
		Trace curTrace = context.currentRegion.trace;
		
		curTrace.calculateRegisterInteraction(context);
		
		for (int successor : curTrace.successors) {
			if (curTrace.containsCodeAddress(successor)) {
				curTrace.meta.chainingCells.put(successor, new ChainingCell(ChainingCell.Type.BACKWARD_BRANCH, successor));
			} else {
				curTrace.meta.chainingCells.put(successor, new ChainingCell(ChainingCell.Type.NORMAL, successor));
			}
		}
		
		try {
			// Everything that this calls MUST throw the IOException back up here!
			determineInstructionsThatThrowExceptions();
			emitFunctionStart();
			
			for (int i = 0; i < curTrace.getLength(); i++) {
				emitForCodeAddress(curTrace.addresses.get(i));
				
				// If we're the last instruction, make sure we jump to the correct exit.
				int codeAddress = curTrace.addresses.get(i);
				Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
				int nextAddress = context.currentRegion.getNextCodeAddress(codeAddress, instruction);
				
				if (needControlFlow(curTrace, i, nextAddress)) {
					writer.write("  " + converter.getGotoLabel(curTrace, nextAddress) + ";\n\n");
				}
				
			 	updateChainingCells(curTrace, instruction, codeAddress, nextAddress);
			}
			
			emitFunctionEnd();
			
		} catch (IOException e) {
			System.err.println("Couldn't write to C file!");
			throw new CGeneratorFaultException();
		}
	}

	private void updateChainingCells(Trace curTrace, Instruction instruction, int codeAddress, int nextAddress) {
		if (instruction.opcode == Opcode.INVOKE_VIRTUAL_QUICK || instruction.opcode == Opcode.INVOKE_INTERFACE ||
				instruction.opcode == Opcode.INVOKE_VIRTUAL_QUICK_RANGE || instruction.opcode == Opcode.INVOKE_INTERFACE_RANGE) {
			curTrace.meta.chainingCells.put(codeAddress, (new ChainingCell(ChainingCell.Type.INVOKE_PREDICTED, codeAddress)));
		} else if (opcodesWithHotChainingCells.contains(instruction.opcode) && !curTrace.containsCodeAddress(nextAddress)) {
			curTrace.meta.chainingCells.get(nextAddress).type = ChainingCell.Type.HOT;
		}
	}

	private boolean needControlFlow(Trace trace, int currentAddressIdx, int nextAddress) {
		if (opcodesThatCanReturn.contains(context.currentRegion.getInstructionAtCodeAddress(trace.addresses.get(currentAddressIdx)).opcode)) {
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
	 * Emit the function signature, basically.
	 */
	private void emitFunctionStart() throws IOException {
		writer.write("#define TRACE_EXIT(a) { return a+1; }\n");
		writer.write("#define TRACE_EXCEPTION(a) { return -1-a; }\n");
		writer.write("#define TRACE_RETURN(a) { return 0; }\n");
		writer.write("#define TRACE_DEPARTURE_INFO int\n");
		writer.write("\n");
		writer.write("extern double __hiya_sin(double v, int *lit);\n");
		writer.write("extern double __hiya_cos(double v, int *lit);\n");
		writer.write("extern double __hiya_sqrt(double v, int *lit);\n");
		writer.write("\n");
		
		writer.write(String.format("// --- TRACE %2d START (%s;%s;%s) ---\n", context.currentRegionIndex, context.currentRegion.clazz, context.currentRegion.method, context.currentRegion.signature));
		writer.write("TRACE_DEPARTURE_INFO trace(int *lit, int *v, char *self) {\n");
		if (context.config.forceEarlyExit) {
			writer.write("\tint tripCount = 0;\n\n");
		}
		if (context.config.printVregsMode) {
			writer.write("\tcount_invoke_for_print_vregs(lit);\n");
		}
	}
	
	/*
	 * Emit the functions that exit labels call. This will be required to correctly identify where the trace is going
	 * when we're generating our injectable trace.
	 */
	private void determineInstructionsThatThrowExceptions() throws IOException {
		Trace curTrace = context.currentRegion.trace;

		// Generate the exception function prototypes.
		for (int i = 0; i < curTrace.getLength(); i++) {
			int codeAddress = curTrace.addresses.get(i);

			Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);

			if (opcodesThatThrowExceptions.contains(instruction.opcode)) {
				curTrace.meta.codeAddressesThatThrowExceptions.add(codeAddress);
			}

			
		}
	}
	
	/*
	 * Emit the comment, label and actual C for the given instruction.
	 */
	private void emitForCodeAddress(int codeAddress) throws IOException, UnimplementedInstructionException {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		
		if (instruction.opcode != Opcode.NOP) {
			writer.write(stringConverter.convert(context, codeAddress));
			writer.write(String.format("  __L%#x:\n", codeAddress));
			
			if (context.config.forceEarlyExit && (codeAddress == context.config.forceEarlyExitCodeAddress)) {
				writer.write("  // Forced to exit early...\n");
				writer.write(String.format("  if (++tripCount == %d) TRACE_EXCEPTION(%#x)\n\n", context.config.forceEarlyExitTripCount, codeAddress));
				return;
			}
			
			if (context.config.breakFlagCheckMode && context.currentRegion.trace.meta.backwardsBranchTargets.contains(codeAddress)) {
				writer.write("  // Backwards Branch Target - must check flags!\n");
				writer.write(String.format("  if ( *((int*)(self+40)) != 0 ) TRACE_EXCEPTION(%#x)\n", codeAddress));
			}	
			
			writer.write(converter.convert(context, codeAddress));
			writer.write("\n");
		}
	}
	
	/*
	 * Emit the end of the function, basically!
	 */
	private void emitFunctionEnd() throws IOException {
		writer.write("}\n");
	}
}
