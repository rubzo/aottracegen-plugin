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
	
	static Map<LiteralPoolType,String> literalPoolTypeMap;
	static {
		literalPoolTypeMap = new HashMap<LiteralPoolType,String>();
		literalPoolTypeMap.put(LiteralPoolType.STATIC_FIELD, "static_field");
		literalPoolTypeMap.put(LiteralPoolType.RETURN_HANDLER, "return_handler");
		literalPoolTypeMap.put(LiteralPoolType.DPC_OFFSET, "dpc_offset");
		literalPoolTypeMap.put(LiteralPoolType.METHOD_PREDICTED_CHAIN_HANDLER, "method_predicted_chain_handler");
	}

	public void prepare(String name) {
		try {
			File file = new File(name);
			writer = new FileWriter(file);			
			prepared = true;	
		} catch (IOException e) {
			System.err.println("Couldn't open injectable trace file for writing!");
		}
	}
	
	/*
	 * Closes the file we've been writing an injectable trace description to.
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
	
	public void generate(CodeGenContext context) throws ITraceDescGeneratorFaultException {
		if (!prepared) {
			System.err.println("Injectable trace generator wasn't prepared?");
			throw new ITraceDescGeneratorFaultException();
		}
		
		// Reset the context.
		context.setCurrentTraceIndex(0);
		
		try {
			emitHeader(context);
			writer.write("\n");
			
			for (int i = 0; i < context.traces.size(); i++) {
				context.setCurrentTraceIndex(i);
			
				int curTraceEntryAddress = context.getCurrentTrace().entry;
				
				writer.write(String.format("trace_desc %d\n", i+1));
				writer.write(String.format("trace_entry %#x\n", curTraceEntryAddress));
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
			
			int value = curTrace.meta.literalPoolIndices.get(i);
			
			LiteralPoolType type = curTrace.meta.literalPoolTypes.get(i);
			String typeName = literalPoolTypeMap.get(type);
			
			writer.write(String.format("literal_pool %d %s %#x\n", i, typeName, value));
		}
	}
	
	private void emitChainingCellInfo(FileWriter writer2, CodeGenContext context) throws IOException {
		Trace curTrace = context.getCurrentTrace();
		int i = 0;
		for (int successor : curTrace.successors) {
			writer.write(String.format("chaining_cell %d %#x\n", i, successor));
			i++;
		}
	}

	private void emitHeader(CodeGenContext context) throws IOException {
		writer.write("application_name " + context.config.app + "\n");
		writer.write(String.format("method_index %#x\n", context.methodIndex));
		writer.write(String.format("num_traces %d\n", context.traces.size()));
	}
}
