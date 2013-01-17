package eu.whrl.aottracegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
		literalPoolTypeMap.put(LiteralPoolType.INVOKE_METHOD_PREDICTED_CHAIN_HANDLER, "invoke_method_predicted_chain_handler");
		literalPoolTypeMap.put(LiteralPoolType.JIT_TO_PATCH_PREDICTED_CHAIN_HANDLER, "jit_to_patch_predicted_chain_handler");
		literalPoolTypeMap.put(LiteralPoolType.INVOKE_METHOD_NO_OPT_HANDLER, "invoke_method_no_opt_handler");
		literalPoolTypeMap.put(LiteralPoolType.AOT_DEBUG_COUNTER_FUNCTION, "aot_debug_counter_function");
		literalPoolTypeMap.put(LiteralPoolType.AOT_DEBUG_LOG_MESSAGE_FUNCTION, "aot_debug_log_message_function");
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
				
				writer.write("trace\n");
				
				writer.write(String.format("class %s\n", context.config.clazz));
				writer.write(String.format("method %s\n", context.config.method));
				writer.write(String.format("pc_offset %#x\n", curTraceEntryAddress));
				
				emitLiteralPoolInfo(context);
				emitChainingCellInfo(context);
				writer.write("end_trace\n");
			}
			
		} catch (IOException e) {
			System.err.println("Couldn't write to injectable trace desc file!");
			throw new ITraceDescGeneratorFaultException();
		}
	}

	private void emitLiteralPoolInfo(CodeGenContext context) throws IOException {
		Trace curTrace = context.getCurrentTrace();
		for (int i = 0; i < curTrace.meta.literalPoolSize; i++) {
			
			int value = curTrace.meta.literalPoolIndices.get(i);
			
			LiteralPoolType type = curTrace.meta.literalPoolTypes.get(i);
			String typeName = literalPoolTypeMap.get(type);
			
			writer.write(String.format("literal_pool %d %s %#x\n", i, typeName, value));
		}
	}
	
	private void emitChainingCellInfo(CodeGenContext context) throws IOException {
		Trace curTrace = context.getCurrentTrace();
		int i = 0;
		for (ChainingCell cc : curTrace.meta.chainingCells.values()) {
			if (cc.type != ChainingCell.Type.INVOKE_PREDICTED) {
				writer.write(String.format("chaining_cell %d %#x\n", i, cc.codeAddress));
				i++;
			}
		}
	}

	private void emitHeader(CodeGenContext context) throws IOException {
		writer.write("application_name " + context.config.app + "\n");
	}
}
