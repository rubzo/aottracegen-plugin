package eu.whrl.aottracegen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import eu.whrl.aottracegen.exceptions.CGeneratorFaultException;
import eu.whrl.aottracegen.exceptions.CommandException;
import eu.whrl.aottracegen.exceptions.CompilationException;
import eu.whrl.aottracegen.exceptions.ITraceDescGeneratorFaultException;
import eu.whrl.aottracegen.exceptions.ITraceGeneratorFaultException;
import eu.whrl.aottracegen.exceptions.UnimplementedInstructionException;

public class CodeGenerator {
	public CodeGenerator() { }
	
	/*
	 * This method is called to generate the C, ASM, and then injectable ASM and trace description files.
	 * Everything in the CodeGenContext will have been already set up at this point.
	 */
	public void generateCodeFromContext(CodeGenContext context) {
		int numTraces = context.traces.size();
		String[] cTraceFileNames = new String[numTraces];
		String[] asmTraceFileNames = new String[numTraces];
		
		// Generate all the file names.
		for (int i = 0; i < numTraces; i++) {
			cTraceFileNames[i] = String.format("trace_%#x.c", context.traces.get(i).entry);
			asmTraceFileNames[i] = String.format("trace_%#x.S", context.traces.get(i).entry);
		}
		
		try {
			
			for (int i = 0; i < numTraces; i++) {
				
				// Makes sure the generateC() and compileC() functions use the correct trace!
				context.setCurrentTraceIndex(i);
				System.out.println(String.format("Handling trace at %#x", context.getCurrentTrace().entry));
				
				// Do the generation and compilation.
				generateC(context, cTraceFileNames[i]);
				compileC(cTraceFileNames[i], asmTraceFileNames[i]);
				
			}
			
			// Take the list of generated asm files, and produce a 'injectable trace' asm file.
			emitITrace(context, asmTraceFileNames, "ITraces.S");
			
			// Produce the trace description file that the VM will read to know when to inject traces.
			emitITraceDesc(context, "ITracesDesc.cfg");
			
			emitSharedObjectITrace(context, "ITraces.S", "ITraces.o", "ITraces.so");
			
		} catch (UnimplementedInstructionException e) {
			
			System.err.println(String.format("Unimplemented instruction: '%s' at %#x. Cannot generate code.", 
					e.getUnimplementedInstructionName(), e.getUnimplementedInstructionCodeAddress()));
			
		} catch (CompilationException e) {
			
			System.err.println("Couldn't compile generated C. Cannot continue.");
			
		} catch (CGeneratorFaultException e) {
			
			System.err.println("Fault in the C generator. Cannot continue.");
			
		} catch (ITraceGeneratorFaultException e) {
			
			System.err.println("Fault in the injectable trace generator. Cannot continue.");
			
		} catch (ITraceDescGeneratorFaultException e) {
			
			System.err.println("Fault in the injectable trace description generator. Cannot continue.");
			
		}
		
	}
	
	private void emitSharedObjectITrace(CodeGenContext context, String iTraceSName, String iTraceOName, String iTraceSOName) {
		String assembleCommand = String.format("arm-linux-androideabi-gcc -march=armv7-a -mfloat-abi=softfp -mthumb -c -Os -fno-rtti -fPIC -o %s %s", iTraceOName, iTraceSName);
		String sharedObjectCommand = String.format("arm-linux-androideabi-gcc -shared -L/Volumes/Android/4.0.4/out/target/product/maguro/system/lib -o %s %s", iTraceSOName, iTraceOName);
		try {
			System.out.println("Assembling O file...");
			System.out.println("  (cmd: " + assembleCommand + ")");
			runCommand(assembleCommand);
			
			System.out.println("Creating SO file...");
			System.out.println("  (cmd: " + sharedObjectCommand + ")");
			runCommand(sharedObjectCommand);
		} catch (CommandException e) {
			System.err.println("Couldn't create shared object?");
		}
	}

	/*
	 * Takes the current context (a trace must have been selected using context.setCurrentTraceIndex(int)!)
	 * and generates a C file called called cTraceFileName that represents the currently selected trace.
	 * This will also update the trace's metadata with literal pool, exception and helper functon info.
	 * 
	 * Will throw an UnimplementedInstructionException if any of the bytecodes in the trace have not yet
	 * been implemented.
	 */
	public void generateC(CodeGenContext context, String cTraceFileName) throws UnimplementedInstructionException, CGeneratorFaultException {
		System.out.println("Generating C...");
		CTraceGenerator generator = new CTraceGenerator(context);
		generator.prepare(cTraceFileName);
		generator.generate();
		generator.finish();
	}
	
	private void runCommand(String command) throws CommandException {
		try {
			Process process = Runtime.getRuntime().exec(command);
			BufferedReader errorStreamReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			
			process.waitFor();
			
			boolean error = false;
			String line;
			
			while (errorStreamReader.ready()) {
				line = errorStreamReader.readLine();
				if (line.contains("error") || line.contains("Error")) {
					System.err.println(line);
					if (!error) {
						error = true;
					}
				}
			}
			if (error) {
				throw new CommandException();
			}
		} catch (IOException e) {
			System.err.println("IO Exception encountered when executing command?");
			throw new CommandException();
		} catch (InterruptedException e) {
			System.err.println("InterruptedException encountered when executing command?");
			throw new CommandException();
		}
	}
	
	/*
	 * Compile the C file called cTraceFileName into the .S file called asmTraceFileName using GCC.
	 * 
	 * Will throw a CompilationException if it encounters any errors during compilation.
	 */
	public void compileC(String cTraceFileName, String asmTraceFileName) throws CompilationException {
		String command = String.format("arm-linux-androideabi-gcc -march=armv7-a -mfloat-abi=softfp -mthumb -Os -S -o %s %s", asmTraceFileName, cTraceFileName);
		System.out.println("Compiling C...");
		System.out.println("  (cmd: " + command + ")");
		try {
			runCommand(command);
		} catch (CommandException e) {
			throw new CompilationException();
		}
	}
	
	/*
	 * Convert all the provided asmFileNames into one asm file in the format that can link with the DVM.
	 */
	public void emitITrace(CodeGenContext context, String[] asmFileNames, String iTraceName) throws ITraceGeneratorFaultException {
		System.out.println("Producing linkable assembly file...");
		ITraceGenerator iTraceGen = new ITraceGenerator();
		iTraceGen.loadAsmFiles(context, asmFileNames);
		iTraceGen.prepare(iTraceName);
		iTraceGen.generate(context);
		iTraceGen.finish();
	}
	
	/*
	 * Produce the injectable trace description file that the DVM looks for to know which traces can be injected.
	 */
	public void emitITraceDesc(CodeGenContext context, String fileName) throws ITraceDescGeneratorFaultException {
		System.out.println("Producing trace description file...");
		ITraceDescGenerator iTraceDescGen = new ITraceDescGenerator();
		iTraceDescGen.prepare(fileName);
		iTraceDescGen.generate(context);
		iTraceDescGen.finish();
	}
}
