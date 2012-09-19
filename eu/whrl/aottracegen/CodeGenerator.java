package eu.whrl.aottracegen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import eu.whrl.aottracegen.exceptions.CompilationException;
import eu.whrl.aottracegen.exceptions.UnimplementedInstructionException;

public class CodeGenerator {
	public CodeGenerator() { }
	
	public void generateCodeFromContext(CodeGenContext context) {
		int numTraces = context.traces.size();
		String[] cTraceFileNames = new String[numTraces];
		String[] asmTraceFileNames = new String[numTraces];
		
		
		for (int i = 0; i < numTraces; i++) {
			cTraceFileNames[i] = String.format("trace_0x%x.c", context.traceEntryAddresses.get(i));
			asmTraceFileNames[i] = String.format("trace_0x%x.S", context.traceEntryAddresses.get(i));
		}
		
		try {
			
			for (int i = 0; i < numTraces; i++) {
				
				context.setCurrentTraceIndex(i);
				System.out.println(String.format("Handling trace at 0x%x", context.getCurrentTraceEntryAddress()));
				
				generateC(context, cTraceFileNames[i]);
				compileC(context, cTraceFileNames[i], asmTraceFileNames[i]);
				
			}
			
			emitInjectableTraceAssembly(context, asmTraceFileNames, "InjectableTrace.S");
			emitInjectableTraceDescriptionFile(context, "trace_inject_desc.cfg");
			
		} catch (UnimplementedInstructionException e) {
			
			System.err.println(String.format("Unimplemented instruction: '%s' at 0x%x. Cannot generate code.", 
					e.getUnimplementedInstructionName(), e.getUnimplementedInstructionCodeAddress()));
			
		} catch (CompilationException e) {
			
			System.err.println("Couldn't compile generated C. Cannot continue.");
			
		}
		
	}
	
	public void generateC(CodeGenContext context, String cTraceFileName) throws UnimplementedInstructionException {
		System.out.println("Generating C...");
		CTraceGenerator generator = new CTraceGenerator(context);
		generator.prepare(cTraceFileName);
		generator.generate();
		generator.finish();
	}
	
	public void compileC(CodeGenContext context, String cTraceFileName, String asmTraceFileName) throws CompilationException {
		String command = String.format("arm-linux-androideabi-gcc -march=armv7-a -mfloat-abi=softfp -mthumb -O3 -S -o %s %s", asmTraceFileName, cTraceFileName);
		System.out.println("Compiling C...");
		System.out.println("  (cmd: " + command + ")");
		try {
			// Compile the C
			Process process = Runtime.getRuntime().exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			
			process.waitFor();
			
			boolean error = false;
			String line;
			
			while (reader.ready()) {
				line = reader.readLine();
				if (line.contains("error")) {
					System.err.println(line);
					if (!error) {
						error = true;
					}
				}
			}
			if (error) {
				throw new CompilationException();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void emitInjectableTraceAssembly(CodeGenContext context, String[] asmFileNames, String injectableTraceName) {
		System.out.println("Producing linkable assembly file...");
	}
	
	public void emitInjectableTraceDescriptionFile(CodeGenContext context, String name) {
		System.out.println("Producing trace description file...");
	}
}
