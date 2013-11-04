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
	
	public void generateCodeFromContext(CodeGenContext context) {
		String[] asmTraceFileNames = generateASMTraces(context);
		
		try {
			// Take the list of generated asm files, and produce a 'injectable trace' asm file.
			emitITrace(context, asmTraceFileNames, "IRegions.S");
			
			// Produce the trace description file that the VM will read to know when to inject traces.
			emitITraceDesc(context, "IRegions.cfg");
			
			boolean success = false;
			
			while (!success) {
				emitSharedObjectITrace(context, "IRegions.S", "IRegions.o", "IRegions.so");
				success = true;
			}
			
		} catch (ITraceGeneratorFaultException e) {
			
			System.err.println("Fault in the injectable trace generator. Cannot continue.");
			
		} catch (ITraceDescGeneratorFaultException e) {
			
			System.err.println("Fault in the injectable trace description generator. Cannot continue.");
			
		} catch (CommandException e) {
			
			System.err.println("Command exception when emitting shared object. Cannot continue.");
		}
	}
	
	public String[] generateASMTraces(CodeGenContext context) {
		int numRegions = context.regions.size();
		String[] asmTraceFileNames = new String[numRegions];
		
		String[] cTraceFileNames = new String[numRegions];

		// Generate all the file names.
		for (int i = 0; i < numRegions; i++) {
			cTraceFileNames[i] = String.format("region_%03d.c", i);
			asmTraceFileNames[i] = String.format("region_%03d.S", i);
		}

		try {

			for (int i = 0; i < numRegions; i++) {
				// Makes sure the generateC() and compileC() functions use the correct trace!
				context.selectCurrentRegion(i);
				System.out.println(String.format("Handling region at %s + %#x", context.currentRegion.method, context.currentRegion.entryOffset));

				// Do the generation and compilation.
				generateC(context, cTraceFileNames[i]);
				compileC(context, cTraceFileNames[i], asmTraceFileNames[i]);

			}
		}catch (UnimplementedInstructionException e) {
			System.err.println(String.format("Unimplemented instruction: '%s' at %#x. Cannot generate code.", 
					e.getUnimplementedInstructionName(), e.getUnimplementedInstructionCodeAddress()));
		} catch (CompilationException e) {
			System.err.println("Couldn't compile generated C. Cannot continue.");

		} catch (CGeneratorFaultException e) {
			System.err.println("Fault in the C generator. Cannot continue.");
		}

		return asmTraceFileNames;
	}
	
	private void emitSharedObjectITrace(CodeGenContext context, String iTraceSName, String iTraceOName, String iTraceSOName) throws CommandException {
		String assembleCommand = String.format("arm-linux-androideabi-gcc -march=armv7-a -mfloat-abi=hard -mfpu=neon -c -fno-rtti -fPIC -o %s %s", iTraceOName, iTraceSName);
		String sharedObjectCommand = String.format("arm-linux-androideabi-gcc -shared -L/Volumes/Android/4.0.4/out/target/product/maguro/system/lib -o %s %s", iTraceSOName, iTraceOName);
		try {
			System.out.println("Assembling O file...");
			System.out.println("  (cmd: " + assembleCommand + ")");
			runCommand(assembleCommand);
			
			System.out.println("Creating SO file...");
			System.out.println("  (cmd: " + sharedObjectCommand + ")");
			runCommand(sharedObjectCommand);
		} catch (CommandException exception) {
			
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
	
	/*
	 * Compile the C file called cTraceFileName into the .S file called asmTraceFileName using GCC.
	 * 
	 * Will throw a CompilationException if it encounters any errors during compilation.
	 */
	public void compileC(CodeGenContext context, String cTraceFileName, String asmTraceFileName) throws CompilationException {
		String useThumb = "-mthumb";
		if (context.config.armMode) {
			useThumb = "";
		}
		String command = String.format("arm-linux-androideabi-gcc -march=armv7-a -mfloat-abi=hard -mfpu=neon %s %s -S -o %s %s", context.config.cflags, useThumb, asmTraceFileName, cTraceFileName);
		System.out.println("Compiling C...");
		System.out.println("  (cmd: " + command + ")");
		try {
			runCommand(command);
		} catch (CommandException e) {
			throw new CompilationException();
		}
	}
	
	public void optimiseLLVM(CodeGenContext context, String llvmTraceFileName, String llvmOptTraceFileName) throws CompilationException {
		System.out.println("Optimising LLVM bytecode...");
		String command = String.format("opt -O3 -S %s -o %s", llvmTraceFileName, llvmOptTraceFileName);
		System.out.println("  (cmd: " + command + ")");
		try {
			runCommand(command);
		} catch (CommandException e) {
			throw new CompilationException();
		}
	}
	
	public void compileLLVM(CodeGenContext context, String llvmOptTraceFileName, String asmTraceFileName) throws CompilationException {
		System.out.println("Compiling LLVM bytecode...");
		String command = String.format("llc -O3 -mcpu=cortex-a9 --enable-unsafe-fp-math %s -o %s", llvmOptTraceFileName, asmTraceFileName);
		System.out.println("  (cmd: " + command + ")");
		try {
			runCommand(command);
		} catch (CommandException e) {
			throw new CompilationException();
		}
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
					
					// Error is not recoverable
					System.err.println(line);
					if (!error) {
						error = true;
					}
				}
			}
			
			// We had non-recoverable errors
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
