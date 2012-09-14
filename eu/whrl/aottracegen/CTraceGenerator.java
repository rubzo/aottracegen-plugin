package eu.whrl.aottracegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class CTraceGenerator {
	private CodeGenContext context;
	private FileWriter writer;
	private boolean prepared;
	
	public CTraceGenerator(CodeGenContext context) {
		this.context = context;
		writer = null;
		prepared = false;
	}
	
	public void prepare() {
		try {
			File cFile = new File("aottrace_output.c");
			writer = new FileWriter(cFile);			
			prepared = true;	
		} catch (IOException e) {
			System.err.println("Couldn't open C file for writing!");
		}
	}
	
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
	
	public void generate() {
		if (!prepared) {
			return;
		}
		
		try {
			// Everything that this calls MUST throw the IOException back up here!
			emitFunctions(writer);
			emitExitFunctionPrototypes(writer);
			emitFunctionStart(writer);
			Trace trace = context.trace;
			for (int i = 0; i < trace.length; i++) {
				emitForCodeAddress(writer, trace.addresses[i]);
			}
			emitExitLabels(writer);
			emitFunctionEnd(writer);
			
			
		} catch (IOException e) {
			System.err.println("Couldn't write to C file!");
		}
	}
	
	private void emitFunctions(Writer writer) throws IOException {
		writer.write("// FUNCTIONS\n");
		
		writer.write("\n");
	}
	
	private void emitExitFunctionPrototypes(Writer writer) throws IOException {
		writer.write("// EXIT FUNCTION PROTOTYPES\n");
		
		writer.write("\n");
	}
	
	private void emitFunctionStart(Writer writer) throws IOException {
		writer.write("void trace(int* v, char* self, int *lit) {\n");
	}
	
	private void emitForCodeAddress(Writer writer, int codeAddress) throws IOException {
		writer.write(String.format("  // BYTECODE AT 0x%x\n", codeAddress));
		
		writer.write("\n");
	}
	
	private void emitExitLabels(Writer writer) throws IOException {
		writer.write("  // EXIT LABELS\n");
		
		writer.write("\n");
	}
	
	private void emitFunctionEnd(Writer writer) throws IOException {
		writer.write("}\n");
	}
}
