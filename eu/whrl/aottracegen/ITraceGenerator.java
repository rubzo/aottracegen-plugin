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
	
	private Set<String> badLines = new HashSet<String>();
	private boolean badLinesSet = false;
	
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
	
	public void markBranchOutOfRangeError(int offendingLineNumber, String name) {
		try {
			File file = new File("ITraces.S");
			BufferedReader buff = new BufferedReader(new FileReader(file));
			
			int lineNumber = 1;
			String line = buff.readLine();
			while (lineNumber != offendingLineNumber) {
				line = buff.readLine();
				lineNumber++;
			}
			
			badLines.add(line.replaceFirst("\\s+", "").replaceAll("\\s+", " "));
			
			badLinesSet = true;
			
			buff.close();
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	private void loadAsmFile(CodeGenContext context, String asmFileName) throws ITraceGeneratorFaultException {
		ASMTrace asmTrace = new ASMTrace();
	
		asmTrace.setTraceBody(extractTraceBody(context, asmFileName));
		asmTrace.cleanupTrace(context);
		if (badLinesSet) {
			asmTrace.fixBadBranch(badLines);
		}
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
		writer.write("\t.word 0xDEADBEEF\n");
		
	}
	
	private void emitTables(CodeGenContext context) throws IOException {
		int[] traceEntries = new int[context.traces.size()];
		int idx = 0;
		for (Trace trace : context.traces) {
			traceEntries[idx] = trace.entry;
			idx++;
		}
		
		writer.write("dvmITraceStartTable:\n");
		for (int traceEntry : traceEntries) {
			writer.write(String.format("\t.word ITrace_%#x_Start\n", traceEntry));
		}
		writer.write("dvmITraceEndTable:\n");
		for (int traceEntry : traceEntries) {
			writer.write(String.format("\t.word ITrace_%#x_End\n", traceEntry));
		}
		writer.write("dvmITraceBasePCTable:\n");
		for (int traceEntry : traceEntries) {
			writer.write(String.format("\t.word ITrace_%#x_BasePC\n", traceEntry));
		}
		writer.write("dvmITraceChainingCellsTable:\n");
		for (int traceEntry : traceEntries) {
			writer.write(String.format("\t.word ITrace_%#x_ChainingCells\n", traceEntry));
		}
		writer.write("dvmITraceLiteralPoolTable:\n");
		for (int traceEntry : traceEntries) {
			writer.write(String.format("\t.word ITrace_%#x_LiteralPool\n", traceEntry));
		}
		
	}
	
	private void emitChainingCellsTable(CodeGenContext context) throws IOException {
		for (int traceIdx = 0; traceIdx < context.traces.size(); traceIdx++) {
			context.setCurrentTraceIndex(traceIdx);
			Trace curTrace = context.getCurrentTrace();
			
			writer.write(String.format("ITrace_%#x_ChainingCells:\n", curTrace.entry));
			
			for (ChainingCell cc : curTrace.meta.chainingCells.values()) {
				if (cc.type != ChainingCell.Type.INVOKE_PREDICTED) {
					writer.write(String.format("\t.word LT%#x_CC_%#x_value\n", curTrace.entry, cc.codeAddress));
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
		writer.write(String.format("ITrace_%#x_Start:\n", curTrace.entry));
		writer.write("\n");
		
		// and the actual trace body now...
		writer.write(curAsmTrace.getFullStringTraceBody());
		writer.write("\n");
		
		writer.write(".align 4\n");
		
		// base pc location
		// NB: this MUST come just before the literal pool!
		writer.write(String.format("ITrace_%#x_BasePC:\n", curTrace.entry));
		writer.write("\t.word 0x00000000\n");
		writer.write("\n");
		
		// Add an entry to the literal pool for our AOTDebug function, if we ever need to use it
		if (context.config.emitDebugFunctions) {
			curTrace.meta.addLiteralPoolType(LiteralPoolType.AOT_DEBUG_COUNTER_FUNCTION);
			curTrace.meta.addLiteralPoolType(LiteralPoolType.AOT_DEBUG_LOG_MESSAGE_FUNCTION);
		}
		
		// its literal pool
		writer.write(String.format("ITrace_%#x_LiteralPool:\n", curTrace.entry));
		for (int litPoolIdx = 0; litPoolIdx < curTrace.meta.literalPoolSize; litPoolIdx++) {
			if (context.config.emitDebugFunctions && curTrace.meta.literalPoolTypes.get(litPoolIdx) == LiteralPoolType.AOT_DEBUG_COUNTER_FUNCTION) {
				writer.write(String.format("AOTDebugCounter_%#x:\n", curTrace.entry));
			} else if (context.config.emitDebugFunctions && curTrace.meta.literalPoolTypes.get(litPoolIdx) == LiteralPoolType.AOT_DEBUG_LOG_MESSAGE_FUNCTION) {
				writer.write(String.format("AOTDebugLogMessage_%#x:\n", curTrace.entry));
			}
			writer.write("\t.word 0x00000000\n");
		}
		writer.write("\n");
		
		// exception handlers
		if (curTrace.meta.codeAddressesThatThrowExceptions.size() > 0) {
			for (int exceptionCodeAddress : curTrace.meta.codeAddressesThatThrowExceptions) {
				writer.write(String.format("LT%#x_EH_%#x:\n", curTrace.entry, exceptionCodeAddress));
				
				if (context.config.emitEHCounter) {
					writer.write(String.format("\tadr.w\tr1, AOTDebug_%#x\n", curTrace.entry));
					writer.write("\tldr\tr1, [r1]\n");
					writer.write(String.format("\tmovw\tr0, #%d\n", exceptionCodeAddress));
					writer.write("\tblx\tr1\n");
				}
				writer.write(String.format("\tldr\tr0, ITrace_%#x_BasePC\n", curTrace.entry));
				writer.write(String.format("\tadd\tr0, r0, #%d\n", exceptionCodeAddress*2));
				writer.write(String.format("\tb\tLT%#x_EH_MAIN\n", curTrace.entry));
				
				
			}
			writer.write(String.format("LT%#x_EH_MAIN:\n", curTrace.entry));
			writer.write("\tldr\tr1, [r6, #108]\n");
			writer.write("\tblx\tr1\n");
			writer.write("\n");
		}
	
		
		for (ChainingCell cc : curTrace.meta.chainingCells.values()) {

			if (cc.type == ChainingCell.Type.INVOKE_PREDICTED) {
				writer.write("\t.align 4\n");
				writer.write(String.format("LT%#x_CC_%#x:\n", curTrace.entry, cc.codeAddress));
				writer.write("\t.2byte 0xe7fe\n");
				for (int i = 0; i < 7; i++) {
					writer.write("\t.2byte 0x0000\n");
				}
			} else {
				writer.write("\t.align 4\n");
				writer.write(String.format("LT%#x_CC_%#x:\n", curTrace.entry, cc.codeAddress));
				writer.write(String.format("\tb\tLT%#x_CC_%#x_next\n", curTrace.entry, cc.codeAddress));
				writer.write("\torrs\tr0, r0\n");
				writer.write(String.format("LT%#x_CC_%#x_next:\n", curTrace.entry, cc.codeAddress));
				if (cc.type == ChainingCell.Type.HOT) {
					writer.write("\tldr\tr0, [r6, #116]\n");
				} else {
					writer.write("\tldr\tr0, [r6, #100]\n");
				}
				writer.write("\tblx\tr0\n");
				writer.write(String.format("LT%#x_CC_%#x_value:\n", curTrace.entry, cc.codeAddress));
				writer.write("\t.word 0x00000000\n");
			}
		}
		writer.write("\n");
		
		// end of the trace
		writer.write(".align 4\n");
		writer.write(String.format("ITrace_%#x_End:\n", curTrace.entry));
		writer.write("\t.word 0x00000000\n");
	}
}
