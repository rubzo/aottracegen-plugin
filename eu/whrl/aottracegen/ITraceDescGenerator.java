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
		literalPoolTypeMap.put(LiteralPoolType.CLASS_POINTER, "class_pointer");
		literalPoolTypeMap.put(LiteralPoolType.STRING_POINTER, "string_pointer");
		literalPoolTypeMap.put(LiteralPoolType.STATIC_FIELD, "static_field");
		literalPoolTypeMap.put(LiteralPoolType.STATIC_METHOD, "static_method");
		literalPoolTypeMap.put(LiteralPoolType.DIRECT_METHOD, "direct_method");
		literalPoolTypeMap.put(LiteralPoolType.SUPERQUICK_METHOD, "superquick_method");
		literalPoolTypeMap.put(LiteralPoolType.RETURN_HANDLER, "return_handler");
		literalPoolTypeMap.put(LiteralPoolType.DPC_OFFSET, "dpc_offset");
		literalPoolTypeMap.put(LiteralPoolType.INVOKE_METHOD_PREDICTED_CHAIN_HANDLER, "invoke_method_predicted_chain_handler");
		literalPoolTypeMap.put(LiteralPoolType.JIT_TO_PATCH_PREDICTED_CHAIN_HANDLER, "jit_to_patch_predicted_chain_handler");
		literalPoolTypeMap.put(LiteralPoolType.INVOKE_METHOD_NO_OPT_HANDLER, "invoke_method_no_opt_handler");
		literalPoolTypeMap.put(LiteralPoolType.INVOKE_METHOD_CHAIN_HANDLER, "invoke_method_chain_handler");
		literalPoolTypeMap.put(LiteralPoolType.INVOKE_METHOD_NATIVE_HANDLER, "invoke_method_native_handler");
		literalPoolTypeMap.put(LiteralPoolType.AOT_DEBUG_COUNTER_FUNCTION, "aot_debug_counter_function");
		literalPoolTypeMap.put(LiteralPoolType.AOT_DEBUG_LOG_MESSAGE_FUNCTION, "aot_debug_log_message_function");
		literalPoolTypeMap.put(LiteralPoolType.CALL_AOT_INVOKE_SINGLETON_NATIVE, "call_aot_invoke_singleton_native");
		literalPoolTypeMap.put(LiteralPoolType.CALL_AOT_INVOKE_SINGLETON_JAVA, "call_aot_invoke_singleton_java");
		literalPoolTypeMap.put(LiteralPoolType.CALL_AOT_INVOKE_VIRTUAL_QUICK, "call_aot_invoke_virtual_quick");
		literalPoolTypeMap.put(LiteralPoolType.CALL_AOT_INVOKE_INTERFACE, "call_aot_invoke_interface");
		literalPoolTypeMap.put(LiteralPoolType.CALL_INSTANCEOF_NON_TRIVIAL, "call_instanceof_non_trivial");
		literalPoolTypeMap.put(LiteralPoolType.CALL_ALLOC_OBJECT, "call_alloc_object");
		literalPoolTypeMap.put(LiteralPoolType.EXECUTE_INLINE, "execute_inline");
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
		context.selectCurrentRegion(0);
		
		try {
			emitHeader(context);
			writer.write("\n");
			
			for (int i = 0; i < context.regions.size(); i++) {
				context.selectCurrentRegion(i);
			
				int curTraceEntryAddress = context.currentRegion.entryOffset;
				
				writer.write("region\n");
				
				int literalPoolSize = context.currentRegion.trace.meta.literalPoolSize;
				for (ChainingCell cc : context.currentRegion.trace.meta.chainingCells.values()) {
					if (cc.type != ChainingCell.Type.INVOKE_PREDICTED) {
						literalPoolSize++;
					}
				}
				
				writer.write(String.format("class %s;\n", context.currentRegion.clazz));
				writer.write(String.format("method %s\n", context.currentRegion.method));
				writer.write(String.format("signature %s\n", context.currentRegion.signature));
				writer.write(String.format("pc_offset %#x\n", curTraceEntryAddress));
				writer.write(String.format("literal_pool_size %d\n", literalPoolSize));
				
				emitLiteralPoolInfo(context);
				emitChainingCellInfo(context);
				writer.write("end_region\n");
			}
			
		} catch (IOException e) {
			System.err.println("Couldn't write to injectable trace desc file!");
			throw new ITraceDescGeneratorFaultException();
		}
	}

	private void emitLiteralPoolInfo(CodeGenContext context) throws IOException {
		Trace curTrace = context.currentRegion.trace;
		for (int i = 0; i < curTrace.meta.literalPoolSize; i++) {
			
			int value = curTrace.meta.literalPoolIndices.get(i);
			
			LiteralPoolType type = curTrace.meta.literalPoolTypes.get(i);
			String typeName = literalPoolTypeMap.get(type);
			
			writer.write(String.format("literal_pool %d %s %#x\n", i, typeName, value));
		}
	}
	
	private void emitChainingCellInfo(CodeGenContext context) throws IOException {
		Trace curTrace = context.currentRegion.trace;
		int i = 0;
		for (ChainingCell cc : curTrace.meta.chainingCells.values()) {
			if (cc.type == ChainingCell.Type.INVOKE_SINGLETON) {
				writer.write(String.format("chaining_cell_singleton_method %d %#x\n", i, cc.codeAddress));
				i++;
			} else if (cc.type != ChainingCell.Type.INVOKE_PREDICTED) {
				writer.write(String.format("chaining_cell %d %#x\n", i, cc.codeAddress));
				i++;
			}
			
			
		}
	}

	private void emitHeader(CodeGenContext context) throws IOException {
		writer.write("application_name " + context.config.app + "\n");
		writer.write("region_count " + context.regions.size() + "\n");
		writer.write("#method_jit 5\n");
		writer.write("load_immediate\n");
	}
}
