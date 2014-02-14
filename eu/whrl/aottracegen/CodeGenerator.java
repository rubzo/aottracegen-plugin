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
			System.exit(1);
		} catch (CompilationException e) {
			System.err.println("Couldn't compile generated C. Cannot continue.");
			System.exit(1);
		} catch (CGeneratorFaultException e) {
			System.err.println("Fault in the C generator. Cannot continue.");
			System.exit(1);
		}

		return asmTraceFileNames;
	}
	
	private void emitSharedObjectITrace(CodeGenContext context, String iTraceSName, String iTraceOName, String iTraceSOName) throws CommandException {
		String assembleCommand = String.format("arm-linux-androideabi-gcc -march=armv7-a -mfloat-abi=hard -mfpu=neon -c -fno-rtti -fPIC -o %s %s", iTraceOName, iTraceSName);
		String sharedObjectCommand = String.format("arm-linux-androideabi-gcc -march=armv7-a -shared -Xlinker -fPIC -o %s %s", iTraceSOName, iTraceOName);
		try {
			System.out.println("Assembling O file...");
			System.out.println("  (cmd: " + assembleCommand + ")");
			runCommandLocally(assembleCommand);
			
			System.out.println("Creating SO file...");
			System.out.println("  (cmd: " + sharedObjectCommand + ")");
			runCommandLocally(sharedObjectCommand);
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
		if (context.currentRegion.useAlternative) {
			cTraceFileName = "alternative_" + cTraceFileName;
		}
		String pushCommand = String.format("adb push %s /sdcard/", cTraceFileName);
		String pullCommand = String.format("adb pull /sdcard/%s .", asmTraceFileName);
		
		String clangBinary = "arm-linux-androideabi-clang";
		String optBinary = "arm-linux-androideabi-opt";
		String llcBinary = "arm-linux-androideabi-llc";
		
		// used in both modes
		String clangCommand = "";
		
		// used in JIT emulation mode only
		String optCommand = "";
		String llcCommand = "";
		
		if (context.config.emulateJitMode) {
			
			String clangOptions = "-target armv7a -O0";
						
			// O2 options
			String optOptions = "-march=armv7-a -tbaa -basicaa -globalopt -ipsccp -deadargelim -instcombine -simplifycfg -prune-eh -inline -functionattrs " +
					"-scalarrepl -early-cse -simplify-libcalls -jump-threading -correlated-propagation -simplifycfg -instcombine " +
					"-tailcallelim -simplifycfg -reassociate -loop-rotate -licm -loop-unswitch -instcombine -indvars -loop-idiom " +
					"-loop-deletion -loop-unroll -gvn -memcpyopt -sccp -instcombine -jump-threading -correlated-propagation -dse " +
					"-slp-vectorizer -bb-vectorize -instcombine -gvn -loop-unroll -adce -simplifycfg -instcombine " +
					"-strip-dead-prototypes -globaldce -constmerge";
			
			String llcOptions = "-mcpu=cortex-a15 -march=thumb -O2 -regalloc=basic -code-model=default -float-abi=hard -pre-RA-sched=fast -relocation-model=static";
			
			clangCommand = String.format("%s -emit-llvm -S %s -o /sdcard/bitcode.bc /sdcard/%s", clangBinary, clangOptions, cTraceFileName);
			optCommand = String.format("%s -S %s -o /sdcard/bitcode_opt.bc /sdcard/bitcode.bc", optBinary, optOptions);
			llcCommand = String.format("%s %s -o /sdcard/%s /sdcard/bitcode_opt.bc", llcBinary, llcOptions, asmTraceFileName);
			
		} else {
			
			String clangOptions = "-target armv7a -mcpu=cortex-a15 -mtune=cortex-a15 -O3 -funsafe-math -funroll-loops -mfpu=neon -mfloat-abi=hard -mthumb";
			
			clangCommand = String.format("%s %s -S -o /sdcard/%s /sdcard/%s", clangBinary, clangOptions, asmTraceFileName, cTraceFileName);
		}
		
		try {
			System.out.println("Compiling C...");
			
			runCommandOnDevice("rm /sdcard/*.S /sdcard/*.c");
			System.out.println("  PUSH: " + pushCommand);
			runCommandLocally(pushCommand);
			if (context.config.emulateJitMode) {
				System.out.println("  --- EMULATING JIT CODE ---");
				runCommandOnDevice("rm /sdcard/bitcode*");
				System.out.println("  (device) CLANG: " + clangCommand);
				runCommandOnDevice(clangCommand);
				System.out.println("  (device) OPT: " + optCommand);
				runCommandOnDevice(optCommand);
				System.out.println("  (device) LLC: " + llcCommand);
				runCommandOnDevice(llcCommand);
			} else {
				System.out.println("  --- STANDALONE COMPILER CODE ---");
				System.out.println("  (device) CLANG: " + clangCommand);
				runCommandOnDevice(clangCommand);
			}
			System.out.println("  PULL: " + pullCommand);
			runCommandLocally(pullCommand);
		} catch (CommandException e) {
			throw new CompilationException();
		}
	}
	
	private void runCommandOnDevice(String command) throws CommandException {
		runCommand(command, true);
	}
	
	private void runCommandLocally(String command) throws CommandException {
		runCommand(command, false);
	}
	
	private void runCommand(String command, boolean onDevice) throws CommandException {
		try {
			
			if (onDevice) {
				command = "adb shell " + command; 
			}
			
			Process process = Runtime.getRuntime().exec(command);
			BufferedReader errorStreamReader;
			if (onDevice) {
				errorStreamReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			} else {
				errorStreamReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			}
			
			process.waitFor();
			if (onDevice) {
				Thread.sleep(500);
			}
			
			
			boolean error = false;
			String line;
			
			while (errorStreamReader.ready()) {
				line = errorStreamReader.readLine();
				
				if (line.toLowerCase().contains("error")) {
					
					// Error is not recoverable
					//System.err.println("Offending command: "+ command);
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
