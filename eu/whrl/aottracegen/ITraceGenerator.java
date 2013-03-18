package eu.whrl.aottracegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.whrl.aottracegen.exceptions.ITraceGeneratorFaultException;

public class ITraceGenerator {
	private List<ASMTrace> asmTraces;
	
	private boolean prepared;
	private FileWriter writer;

	public ITraceGenerator() {
		asmTraces = new ArrayList<ASMTrace>();
	}
	
	public void loadAsmFiles(CodeGenContext context, String[] asmFileNames) throws ITraceGeneratorFaultException {
		int i = 0;
		for (String asmFileName : asmFileNames) {
			context.setCurrentTraceIndex(i);
			loadAsmFile(context, asmFileName);
			i++;
		}
		
	}

	private void loadAsmFile(CodeGenContext context, String asmFileName) throws ITraceGeneratorFaultException {
		ASMTrace asmTrace = new ASMTrace();
	
		asmTrace.setTraceBody(extractTraceBody(context, asmFileName));
		asmTrace.cleanupTrace(context);
		asmTraces.add(asmTrace);
	}

	/*
	 * Given a filename of a generated .S file, extracts solely the body of the 'trace' function,
	 * and returns this body as an array of strings, one string for each line.
	 */
	private List<String> extractTraceBody(CodeGenContext context, String asmFileName) throws ITraceGeneratorFaultException {
		final String startMarker = "trace:";
		final String endMarker = "\t.size\ttrace, .-trace";
		
		List<String> traceBody = new ArrayList<String>();
		
		try {
			File file = new File(asmFileName);
			BufferedReader buff = new BufferedReader(new FileReader(file));
			
			String line;
			boolean inTraceBody = false;
			
			while (buff.ready()) {
				line = buff.readLine();
				
				if (line.equals(endMarker)) {
					inTraceBody = false;
				}
				
				if (inTraceBody) {
					traceBody.add(line);
				}
				
				if (line.equals(startMarker)) {
					inTraceBody = true;
					// Skip past the two @ lines...
					buff.readLine();
					buff.readLine();
				}
			}
			
			buff.close();
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't find the compiled assembly file: " + asmFileName);
			throw new ITraceGeneratorFaultException();
		} catch (IOException e) {
			System.err.println("Couldn't read the compiled assembly file: " + asmFileName);
			throw new ITraceGeneratorFaultException();
		}
		
		return traceBody;
	}

	public void prepare(String name) throws ITraceGeneratorFaultException {
		try {
			File file = new File(name);
			writer = new FileWriter(file);			
			prepared = true;	
		} catch (IOException e) {
			System.err.println("Couldn't open injectable trace file for writing!");
			throw new ITraceGeneratorFaultException();
		}
	}
	
	/*
	 * Closes the file we've been writing an injectable trace to.
	 */
	public void finish() {
		if (!prepared) {
			return;
		}
		
		try {
			writer.close();
		} catch (IOException e) {
			System.err.println("Couldn't close injectable trace file?");
		}
	}
	
	public void generate(CodeGenContext context) throws ITraceGeneratorFaultException {
		if (!prepared) {
			System.err.println("Injectable trace generator wasn't prepared?");
			throw new ITraceGeneratorFaultException();
		}
		
		// Reset the context.
		context.setCurrentTraceIndex(0);
	
		try {
			
			emitStart();
				
			emitTables(context);
			writer.write("\n");
			
			emitChainingCellsTable(context);
			writer.write("\n");
			
			emitTraces(context);
			
		} catch (IOException e) {
			System.err.println("Couldn't write to injectable trace file!");
			throw new ITraceGeneratorFaultException();
		}
	}
	
	private void emitStart() throws IOException {
		
		writer.write("\t.text\n");
		writer.write("\t.global dvmITraceStartTable\n");
		writer.write("\t.global dvmITraceEndTable\n");
		writer.write("\t.global dvmITraceBasePCTable\n");
		writer.write("\t.global dvmITraceChainingCellsTable\n");
		writer.write("\t.global dvmITraceLiteralPoolTable\n");
		writer.write("\n");
		writer.write("\t.syntax unified\n");
		writer.write("\t.thumb\n");
		writer.write("\t# Magic number used to make sure the ITraceLoader has loaded the right code:\n");
		writer.write("\t.word 0xDEADBEEF\n");
		
	}
	
	private void emitTables(CodeGenContext context) throws IOException {
		int[] traceEntries = new int[context.traces.size()];
		int numTraces = context.traces.size();
		
		writer.write("dvmITraceStartTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word Start_T%d\n", i));
		}
		writer.write("dvmITraceEndTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word End_T%d\n", i));
		}
		writer.write("dvmITraceBasePCTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word BasePC_T%d\n", i));
		}
		writer.write("dvmITraceChainingCellsTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word ChainingCells_T%d\n", i));
		}
		writer.write("dvmITraceLiteralPoolTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word LiteralPool_T%d\n", i));
		}
		
	}
	
	private void emitChainingCellsTable(CodeGenContext context) throws IOException {
		for (int traceIdx = 0; traceIdx < context.traces.size(); traceIdx++) {
			context.setCurrentTraceIndex(traceIdx);
			Trace curTrace = context.getCurrentTrace();
			
			writer.write(String.format("ChainingCells_T%d:\n", traceIdx));
			
			for (ChainingCell cc : curTrace.meta.chainingCells.values()) {
				if (cc.type != ChainingCell.Type.INVOKE_PREDICTED) {
					writer.write(String.format("\t.word ChainingCellValue_T%d_A%#x\n", context.currentTraceIdx, cc.codeAddress));
				}
			}
		}
	}
	
	private void emitTraces(CodeGenContext context) throws IOException {
		for (int traceIdx = 0; traceIdx < context.traces.size(); traceIdx++) {
			context.setCurrentTraceIndex(traceIdx);
			emitTrace(context);
			writer.write("\n");
		}
	}
	
	private void emitTrace(CodeGenContext context) throws IOException {
		Trace curTrace = context.getCurrentTrace();
		ASMTrace curAsmTrace = asmTraces.get(context.getCurrentTraceIndex());
		
		// start of the trace
		writer.write(String.format("Start_T%d:\n", context.currentTraceIdx));
		
		if (context.config.armMode) {
			// Code that enters ARM execution mode
			writer.write("# Code that enters ARM exceution mode.\n");
			writer.write(String.format("\tadr.w\tr0, Start_ARMCode_T%d\n", context.currentTraceIdx));
			writer.write("\tbx\tr0\n");
			writer.write(String.format("Start_ARMCode_T%d:\n", context.currentTraceIdx));
			writer.write("\t.arm\n");
			writer.write("# Load inputs to the trace code.\n");
		}
		writer.write(String.format("\tadr\tr0, LiteralPool_T%d\n", context.currentTraceIdx));
		writer.write("\tmov\tr1, r5\n");
		writer.write("\tmov\tr2, r6\n");
		
		// and the actual trace body now...
		writer.write("# Actual trace code begins now:\n");
		writer.write(curAsmTrace.getFullStringTraceBody());
		writer.write("\n");
		
		// Exit code! r0 = {1 = exit, 2 = exception, 3 = return}, r1 = code address
		writer.write("# End of actual trace code, now for code to determine how we leave the trace:\n");
		writer.write(String.format("Leave_T%d:\n", context.currentTraceIdx));
		
		if (context.config.armMode) {
			writer.write("# First thing we need to do is enter Thumb2 code again\n");
			writer.write(String.format("\tadr\tr2, End_ThumbCode_T%d\n", context.currentTraceIdx));
			writer.write("\tadd\tr2, r2, #1\n");
			writer.write("\tbx\tr2\n");
			writer.write(String.format("End_ThumbCode_T%d:\n", context.currentTraceIdx));
			writer.write(".thumb\n");
		}
		
		writer.write("\tcmp\tr0, #1\n");
		writer.write(String.format("\tbeq\tExits_T%d\n", context.currentTraceIdx));
		writer.write("\tcmp\tr0, #2\n");
		writer.write(String.format("\tbeq\tExceptions_T%d\n", context.currentTraceIdx));
		writer.write("\tcmp\tr0, #3\n");
		writer.write(String.format("\tbeq\tReturns_T%d\n", context.currentTraceIdx));
		
		writer.write(String.format("Exits_T%d:\n", context.currentTraceIdx));
		for (ChainingCell cc : curTrace.meta.chainingCells.values()) {
			writer.write(String.format("\tcmp\tr1, #%d\n", cc.codeAddress));
			writer.write(String.format("\tbeq\tChainingCell_T%d_A%#x\n", context.currentTraceIdx, cc.codeAddress));
		}
		
		writer.write(String.format("Exceptions_T%d:\n", context.currentTraceIdx));
		for (int exceptionCodeAddress : curTrace.meta.codeAddressesThatThrowExceptions) {
			writer.write(String.format("\tcmp\tr1, #%d\n", exceptionCodeAddress));
			writer.write(String.format("\tbeq\tExceptionHandler_T%d_A%#x\n", context.currentTraceIdx, exceptionCodeAddress));
		}
		
		writer.write(String.format("Returns_T%d:\n", context.currentTraceIdx));
		
		writer.write(".align 4\n");
		
		
		// base pc location
		// NB: this MUST come just before the literal pool!
		writer.write(String.format("BasePC_T%d:\n", context.currentTraceIdx));
		writer.write("\t.word 0x00000000\n");
		writer.write("\n");
		
		// Add an entry to the literal pool for our AOTDebug function, if we ever need to use it
		if (context.config.emitDebugFunctions) {
			curTrace.meta.addLiteralPoolType(LiteralPoolType.AOT_DEBUG_COUNTER_FUNCTION);
			curTrace.meta.addLiteralPoolType(LiteralPoolType.AOT_DEBUG_LOG_MESSAGE_FUNCTION);
		}
		
		// its literal pool
		writer.write(String.format("LiteralPool_T%d:\n", context.currentTraceIdx));
		for (int litPoolIdx = 0; litPoolIdx < curTrace.meta.literalPoolSize; litPoolIdx++) {
			if (context.config.emitDebugFunctions && curTrace.meta.literalPoolTypes.get(litPoolIdx) == LiteralPoolType.AOT_DEBUG_COUNTER_FUNCTION) {
				writer.write(String.format("AOTDebugCounter_T%d:\n", context.currentTraceIdx));
			} else if (context.config.emitDebugFunctions && curTrace.meta.literalPoolTypes.get(litPoolIdx) == LiteralPoolType.AOT_DEBUG_LOG_MESSAGE_FUNCTION) {
				writer.write(String.format("AOTDebugLogMessage_T%d:\n", context.currentTraceIdx));
			}
			writer.write("\t.word 0x00000000\n");
		}
		writer.write("\n");
		
		// exception handlers
		if (curTrace.meta.codeAddressesThatThrowExceptions.size() > 0) {
			for (int exceptionCodeAddress : curTrace.meta.codeAddressesThatThrowExceptions) {
				writer.write(String.format("ExceptionHandler_T%d_A%#x:\n", context.currentTraceIdx, exceptionCodeAddress));
				
				if (context.config.emitEHCounter) {
					writer.write(String.format("\tadr.w\tr1, AOTDebugCounter_T%d\n", context.currentTraceIdx));
					writer.write("\tldr\tr1, [r1]\n");
					writer.write(String.format("\tmovw\tr0, #%d\n", exceptionCodeAddress));
					writer.write("\tblx\tr1\n");
				}
				writer.write(String.format("\tldr\tr0, BasePC_T%d\n", context.currentTraceIdx));
				writer.write(String.format("\tadd\tr0, r0, #%d\n", exceptionCodeAddress*2));
				writer.write(String.format("\tb\tMainExceptionHandler_T%d\n", context.currentTraceIdx));
				
				
			}
			writer.write(String.format("MainExceptionHandler_T%d:\n", context.currentTraceIdx));
			writer.write("\tldr\tr1, [r6, #108]\n");
			writer.write("\tblx\tr1\n");
			writer.write("\n");
		}
	
		
		for (ChainingCell cc : curTrace.meta.chainingCells.values()) {

			if (cc.type == ChainingCell.Type.INVOKE_PREDICTED) {
				writer.write("\t.align 4\n");
				writer.write(String.format("ChainingCell_T%d_A%#x:\n", context.currentTraceIdx, cc.codeAddress));
				writer.write("\t.2byte 0xe7fe\n");
				for (int i = 0; i < 7; i++) {
					writer.write("\t.2byte 0x0000\n");
				}
			} else {
				writer.write("\t.align 4\n");
				writer.write(String.format("ChainingCell_T%d_A%#x:\n", context.currentTraceIdx, cc.codeAddress));
				writer.write(String.format("\tb\tChainingCellNext_T%d_A%#x\n", context.currentTraceIdx, cc.codeAddress));
				writer.write("\torrs\tr0, r0\n");
				writer.write(String.format("ChainingCellNext_T%d_A%#x:\n", context.currentTraceIdx, cc.codeAddress));
				if (cc.type == ChainingCell.Type.HOT) {
					writer.write("\tldr\tr0, [r6, #116]\n");
				} else if (cc.type == ChainingCell.Type.BACKWARD_BRANCH) {
					writer.write("\tldr\tr0, [r6, #100]\n"); // I thought this was 120, not 100?
				} else {
					writer.write("\tldr\tr0, [r6, #100]\n");
				}
				writer.write("\tblx\tr0\n");
				writer.write(String.format("ChainingCellValue_T%d_A%#x:\n", context.currentTraceIdx, cc.codeAddress));
				writer.write("\t.word 0x00000000\n");
			}
		}
		writer.write("\n");
		
		// end of the trace
		writer.write(".align 4\n");
		writer.write(String.format("End_T%d:\n", context.currentTraceIdx));
		writer.write("\t.word 0x00000000\n");
	}
}
