package eu.whrl.aottracegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.whrl.aottracegen.armgen.AssemblyBlob;
import eu.whrl.aottracegen.exceptions.ITraceGeneratorFaultException;

public class ITraceGenerator {
	private List<AssemblyBlob> assemblyBlobs;
	
	private boolean prepared;
	private FileWriter writer;

	public ITraceGenerator() {
		assemblyBlobs = new ArrayList<AssemblyBlob>();
	}
	
	public void loadAsmFiles(CodeGenContext context, String[] asmFileNames) throws ITraceGeneratorFaultException {
		int i = 0;
		for (String asmFileName : asmFileNames) {
			context.selectCurrentRegion(i);
			loadAsmFile(context, asmFileName);
			i++;
		}
		
	}

	private void loadAsmFile(CodeGenContext context, String asmFileName) throws ITraceGeneratorFaultException {
		AssemblyBlob assemblyBlob = new AssemblyBlob(extractInsts(context, asmFileName));
		assemblyBlob.cleanup(context);
		assemblyBlobs.add(assemblyBlob);
	}

	/*
	 * Given a filename of a generated .S file, extracts solely the body of the 'trace' function,
	 * and returns this body as an array of strings, one string for each line.
	 */
	private List<String> extractInsts(CodeGenContext context, String asmFileName) throws ITraceGeneratorFaultException {
		final String startMarker = "trace:";
		final String endMarker = "\t.size\ttrace, .-trace";
		
		List<String> instsList = new ArrayList<String>();
		
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
					String trimmedLine = line.trim().replaceAll("\\s+", " ");
					instsList.add(trimmedLine);
				}
				
				if (line.equals(startMarker)) {
					inTraceBody = true;
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
		
		return instsList;
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
		context.selectCurrentRegion(0);
	
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
		writer.write("\t.global dvmIRegionsStartTable\n");
		writer.write("\t.global dvmIRegionsEndTable\n");
		writer.write("\t.global dvmIRegionsBasePCTable\n");
		writer.write("\t.global dvmIRegionsChainingCellsTable\n");
		writer.write("\t.global dvmIRegionsLiteralPoolTable\n");
		writer.write("\n");
		writer.write("\t.syntax unified\n");
		writer.write("\t.thumb\n");
		writer.write("\t# Magic number used to make sure the loader has loaded the right code:\n");
		writer.write("\t.word 0xDEADBEEF\n");
		
	}
	
	private void emitTables(CodeGenContext context) throws IOException {
		int numTraces = context.regions.size();
		
		writer.write("dvmIRegionsStartTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word Start_T%d\n", i));
		}
		writer.write("dvmIRegionsEndTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word End_T%d\n", i));
		}
		writer.write("dvmIRegionsBasePCTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word BasePC_T%d\n", i));
		}
		writer.write("dvmIRegionsChainingCellsTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word ChainingCells_T%d\n", i));
		}
		writer.write("dvmIRegionsLiteralPoolTable:\n");
		for (int i = 0; i < numTraces; i++) {
			writer.write(String.format("\t.word LiteralPool_T%d\n", i));
		}
		
	}
	
	private void emitChainingCellsTable(CodeGenContext context) throws IOException {
		for (int traceIdx = 0; traceIdx < context.regions.size(); traceIdx++) {
			context.selectCurrentRegion(traceIdx);
			Trace curTrace = context.currentRegion.trace;
			
			writer.write(String.format("ChainingCells_T%d:\n", traceIdx));
			
			for (ChainingCell cc : curTrace.meta.chainingCells.values()) {
				if (cc.type == ChainingCell.Type.INVOKE_SINGLETON) {
					writer.write(String.format("\t.word ChainingCellValue_T%d_M%#x\n", context.currentRegionIndex, cc.codeAddress));
				} else if (cc.type == ChainingCell.Type.INVOKE_SUPER_SINGLETON) {
					writer.write(String.format("\t.word ChainingCellValue_T%d_MV%#x\n", context.currentRegionIndex, cc.codeAddress));
				} else if (cc.type != ChainingCell.Type.INVOKE_PREDICTED) {
					writer.write(String.format("\t.word ChainingCellValue_T%d_A%#x\n", context.currentRegionIndex, cc.codeAddress));
				}
			}
		}
	}
	
	private void emitTraces(CodeGenContext context) throws IOException {
		for (int traceIdx = 0; traceIdx < context.regions.size(); traceIdx++) {
			context.selectCurrentRegion(traceIdx);
			emitTrace(context);
			writer.write("\n");
		}
	}
	
	private void emitTrace(CodeGenContext context) throws IOException {
		Trace curTrace = context.currentRegion.trace;
		AssemblyBlob assemblyBlob = assemblyBlobs.get(context.currentRegionIndex);
		
		// start of the trace
		writer.write(".align 4\n");
		writer.write(String.format("Start_T%d:\n", context.currentRegionIndex));
		
		if (context.config.armMode) {
			// Code that enters ARM execution mode
			writer.write("# Code that enters ARM exceution mode.\n");
			writer.write(String.format("\tadr.w\tr0, Start_ARMCode_T%d\n", context.currentRegionIndex));
			writer.write("\tbx\tr0\n");
			writer.write(String.format("Start_ARMCode_T%d:\n", context.currentRegionIndex));
			writer.write("\t.arm\n");
			writer.write("# Load inputs to the trace code.\n");
		}
		writer.write(String.format("\tadr\tr0, LiteralPool_T%d\n", context.currentRegionIndex));
		writer.write("\tmov\tr1, r5\n");
		writer.write("\tmov\tr2, r6\n");
		
		// and the actual trace body now...
		writer.write("# Actual trace code begins now:\n");
		assemblyBlob.writeOut(writer);
		writer.write("\n");
		
		// Exit code! r0 = {1 = exit, 2 = exception, 3 = return}, r1 = code address
		writer.write("# End of actual trace code, now for code to determine how we leave the trace:\n");
		writer.write(String.format("Leave_T%d:\n", context.currentRegionIndex));
		
		if (context.config.armMode) {
			writer.write("# First thing we need to do is enter Thumb2 code again\n");
			writer.write(String.format("\tadr\tr2, End_ThumbCode_T%d\n", context.currentRegionIndex));
			writer.write("\tadd\tr2, r2, #1\n");
			writer.write("\tbx\tr2\n");
			writer.write(String.format("End_ThumbCode_T%d:\n", context.currentRegionIndex));
			writer.write(".thumb\n");
		}
		
		writer.write("\tcmp\tr0, #1\n");
		writer.write(String.format("\tbeq\tExits_T%d\n", context.currentRegionIndex));
		if (curTrace.meta.codeAddressesThatThrowExceptions.size() > 0) {
			writer.write("\tcmp\tr0, #2\n");
			writer.write(String.format("\tbeq\tExceptions_T%d\n", context.currentRegionIndex));
		}
		if (curTrace.meta.containsReturn) {
			writer.write("\tcmp\tr0, #3\n");
			writer.write(String.format("\tbeq\tReturns_T%d\n", context.currentRegionIndex));
		}
		
		writer.write(String.format("Exits_T%d:\n", context.currentRegionIndex));
		int jumpsProduced = 0;
		int previousCodeAddress = 0;
		for (ChainingCell cc : curTrace.meta.chainingCells.values()) {
			if (cc.type == ChainingCell.Type.INVOKE_SINGLETON || cc.type == ChainingCell.Type.INVOKE_SUPER_SINGLETON ||
					cc.type == ChainingCell.Type.INVOKE_PREDICTED) {
				continue;
			}
			
			writer.write(String.format("\t# Jump to offset %#x\n", cc.codeAddress));
			if (jumpsProduced == 0) {
				writer.write(String.format("\tmov\tr2, #%d\n", cc.codeAddress));
				writer.write("\tcmp\tr1, r2\n");
				previousCodeAddress = cc.codeAddress;
			} else {
				writer.write(String.format("\tadd\tr2, r2, #%d\n", cc.codeAddress - previousCodeAddress));
				writer.write("\tcmp\tr1, r2\n");
				previousCodeAddress = cc.codeAddress;
			}
			jumpsProduced++;
			
			writer.write(String.format("\tbeq\tChainingCell_T%d_A%#x\n", context.currentRegionIndex, cc.codeAddress));
		}
		
		if (curTrace.meta.codeAddressesThatThrowExceptions.size() > 0) {
			writer.write(String.format("Exceptions_T%d:\n", context.currentRegionIndex));
			if (context.config.emitEHCounter) {
				writer.write("# Calling the AOTDebugCounter...\n");
				writer.write("\tmov\tr0, r1\n");
				writer.write("\tmov\tr4, r1\n");
				writer.write(String.format("\tadr.w\tr2, AOTDebugCounter_T%d\n", context.currentRegionIndex));
				writer.write("\tldr\tr2, [r2]\n");
				writer.write("\tblx\tr2\n");
				writer.write("\tmov\tr1, r4\n");
				writer.write("# ...done.\n");
			}
			writer.write(String.format("\tldr\tr0, BasePC_T%d\n", context.currentRegionIndex));
			writer.write("\tlsl\tr1, r1, #1\n");
			writer.write("\tadd\tr0, r0, r1\n");
			writer.write(String.format("\tb\tMainExceptionHandler_T%d\n", context.currentRegionIndex));
			
			/*
			for (int exceptionCodeAddress : curTrace.meta.codeAddressesThatThrowExceptions) {
				writer.write(String.format("\tcmp\tr1, #%d\n", exceptionCodeAddress));
				writer.write(String.format("\tbeq\tExceptionHandler_T%d_A%#x\n", context.currentRegionIndex, exceptionCodeAddress));
			}
			 */
		}
		
		if (curTrace.meta.containsReturn) {
			writer.write(String.format("Returns_T%d:\n", context.currentRegionIndex));
			
			int literalPoolLoc = curTrace.meta.addLiteralPoolType(LiteralPoolType.RETURN_HANDLER);

			// Create the jump to the return handler
			writer.write(String.format("\tadr.w\tr2, LiteralPool_T%d\n", context.currentRegionIndex));
			writer.write(String.format("\tldr\tr0, [r2, #%d]\n", literalPoolLoc*4));
			writer.write("\tblx\tr0\n");
		}
		
		writer.write(".align 4\n");
		
		writer.write("\t.word 0x00000000\n");
		writer.write("\t.word 0x00000000\n");
		writer.write("\t.word 0x00000000\n");
		// base pc location
		// NB: this MUST come just before the literal pool!
		writer.write(String.format("BasePC_T%d:\n", context.currentRegionIndex));
		writer.write("\t.word 0x00000000\n");
		writer.write("\n");
		
		// Add an entry to the literal pool for our AOTDebug function, if we ever need to use it
		if (context.config.emitDebugFunctions) {
			curTrace.meta.addLiteralPoolType(LiteralPoolType.AOT_DEBUG_COUNTER_FUNCTION);
			curTrace.meta.addLiteralPoolType(LiteralPoolType.AOT_DEBUG_LOG_MESSAGE_FUNCTION);
		}
		
		// its literal pool
		writer.write(String.format("LiteralPool_T%d:\n", context.currentRegionIndex));
		for (int litPoolIdx = 0; litPoolIdx < curTrace.meta.literalPoolSize; litPoolIdx++) {
			if (context.config.emitDebugFunctions && curTrace.meta.literalPoolTypes.get(litPoolIdx) == LiteralPoolType.AOT_DEBUG_COUNTER_FUNCTION) {
				writer.write(String.format("AOTDebugCounter_T%d:\n", context.currentRegionIndex));
			} else if (context.config.emitDebugFunctions && curTrace.meta.literalPoolTypes.get(litPoolIdx) == LiteralPoolType.AOT_DEBUG_LOG_MESSAGE_FUNCTION) {
				writer.write(String.format("AOTDebugLogMessage_T%d:\n", context.currentRegionIndex));
			}
			writer.write("\t.word 0x00000000\n");
		}
		writer.write("\n");
		
		// exception handlers
		if (curTrace.meta.codeAddressesThatThrowExceptions.size() > 0) {
			/*
			for (int exceptionCodeAddress : curTrace.meta.codeAddressesThatThrowExceptions) {
				writer.write(String.format("ExceptionHandler_T%d_A%#x:\n", context.currentRegionIndex, exceptionCodeAddress));
				
				if (context.config.emitEHCounter) {
					writer.write(String.format("\tadr.w\tr1, AOTDebugCounter_T%d\n", context.currentRegionIndex));
					writer.write("\tldr\tr1, [r1]\n");
					writer.write(String.format("\tmovw\tr0, #%d\n", exceptionCodeAddress));
					writer.write("\tblx\tr1\n");
				}
				writer.write(String.format("\tldr\tr0, BasePC_T%d\n", context.currentRegionIndex));
				writer.write(String.format("\tadd\tr0, r0, #%d\n", exceptionCodeAddress*2));
				writer.write(String.format("\tb\tMainExceptionHandler_T%d\n", context.currentRegionIndex));
				
				
			}
			*/
			writer.write(String.format("MainExceptionHandler_T%d:\n", context.currentRegionIndex));
			writer.write("\tldr\tr1, [r6, #108]\n");
			writer.write("\tblx\tr1\n");
			writer.write("\n");
		}
	
		
		for (ChainingCell cc : curTrace.meta.chainingCells.values()) {

			if (cc.type == ChainingCell.Type.INVOKE_PREDICTED) {
				writer.write("\t.align 4\n");
				writer.write(String.format("ChainingCell_T%d_M%#x:\n", context.currentRegionIndex, cc.codeAddress));
				writer.write("\t.2byte 0xe7fe\n");
				for (int i = 0; i < 7; i++) {
					writer.write("\t.2byte 0x0000\n");
				}
			} else {
				String id = String.format("T%d_A%#x", context.currentRegionIndex, cc.codeAddress);
				if (cc.type == ChainingCell.Type.INVOKE_SINGLETON) {
					id = String.format("T%d_M%#x", context.currentRegionIndex, cc.codeAddress);
				} else if (cc.type == ChainingCell.Type.INVOKE_SUPER_SINGLETON) {
					id = String.format("T%d_MV%#x", context.currentRegionIndex, cc.codeAddress);
				}
				writer.write("\t.align 4\n");
				writer.write(String.format("ChainingCell_%s:\n", id));
				writer.write(String.format("\tb.n\tChainingCellNext_%s\n", id));
				writer.write("\torrs\tr0, r0\n");
				writer.write(String.format("ChainingCellNext_%s:\n", id));
				if (cc.type == ChainingCell.Type.HOT || cc.type == ChainingCell.Type.INVOKE_SINGLETON || 
						cc.type == ChainingCell.Type.INVOKE_SUPER_SINGLETON) {
					writer.write("\tldr\tr0, [r6, #116]\n");
				} else if (cc.type == ChainingCell.Type.BACKWARD_BRANCH) {
					writer.write("\tldr\tr0, [r6, #100]\n"); // I thought this was 120, not 100?
				} else {
					writer.write("\tldr\tr0, [r6, #100]\n");
				}
				writer.write("\tblx\tr0\n");
				writer.write(String.format("ChainingCellValue_%s:\n", id));
				writer.write("\t.word 0x00000000\n");
			}
		}
		writer.write("\n");
		
		// end of the trace
		writer.write(".align 4\n");
		writer.write(String.format("End_T%d:\n", context.currentRegionIndex));
		writer.write("\t.word 0x00000000\n");
	}
}
