package eu.whrl.aottracegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jf.dexlib.Code.Opcode;

import eu.whrl.aottracegen.exceptions.ITraceDescGeneratorFaultException;

public class ITraceDescGenerator {

	private FileWriter writer;
	private boolean prepared;
	
	static Map<Opcode,String> opcodeTypeMap;
	static {
		opcodeTypeMap = new HashMap<Opcode,String>();
		opcodeTypeMap.put(Opcode.SGET_OBJECT, "static_field");
	}

	public void createInjectableTraceDesc(CodeGenContext context, String fileName) throws ITraceDescGeneratorFaultException {
		prepare(fileName);
		generate(context);
		finish();
	}

	public void prepare(String name) {
		try {
			File cFile = new File(name);
			writer = new FileWriter(cFile);			
			prepared = true;	
		} catch (IOException e) {
			System.err.println("Couldn't open injectable trace file for writing!");
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
			System.err.println("Couldn't close injectable trace file?");
		}
	}
	
	private void generate(CodeGenContext context) throws ITraceDescGeneratorFaultException {
		if (!prepared) {
			System.err.println("Injectable trace generator wasn't prepared?");
			throw new ITraceDescGeneratorFaultException();
		}
		
		// Reset the context, just in case.
		context.setCurrentTraceIndex(0);
		
		try {
			emitHeader(context);
			writer.write("\n");
			
			for (int i = 0; i < context.traces.size(); i++) {
				context.setCurrentTraceIndex(i);
			
				int curTraceEntryAddress = context.getCurrentTrace().getPrimaryEntry();
				
				writer.write(String.format("trace_desc %d\n", i+1));
				writer.write(String.format("trace_entry 0x%x\n", curTraceEntryAddress));
				emitLiteralPoolInfo(writer, context);
				emitChainingCellInfo(writer, context);
				writer.write(String.format("end_trace_desc %d\n", i+1));
			}
			
		} catch (IOException e) {
			System.err.println("Couldn't write to injectable trace desc file!");
			throw new ITraceDescGeneratorFaultException();
		}
	}

	private void emitLiteralPoolInfo(FileWriter writer2, CodeGenContext context) throws IOException {
		Trace curTrace = context.getCurrentTrace();
		for (int i = 0; i < curTrace.meta.literalPoolSize; i++) {
			Opcode opcode = curTrace.meta.literalPoolOpcodes.get(i);
			int value = curTrace.meta.literalPoolIndices.get(i).intValue();
			String type = opcodeTypeMap.get(opcode);
			
			writer.write(String.format("literal_pool %d %s 0x%x\n", i, type, value));
		}
	}
	
	private void emitChainingCellInfo(FileWriter writer2, CodeGenContext context) throws IOException {
		Trace curTrace = context.getCurrentTrace();
		int i = 0;
		for (int successor : curTrace.successors) {
			writer.write(String.format("chaining_cell %d 0x%x\n", i, successor));
			i++;
		}
	}

	private void emitHeader(CodeGenContext context) throws IOException {
		writer.write("application_name " + context.config.app + "\n");
		writer.write(String.format("method_index 0x%x\n", context.methodIndex));
		writer.write(String.format("num_traces %d\n", context.traces.size()));
	}
}
