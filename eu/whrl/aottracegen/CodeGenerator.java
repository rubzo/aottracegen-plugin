package eu.whrl.aottracegen;

public class CodeGenerator {
	public CodeGenerator() { }
	
	public void generateCodeFromContext(CodeGenContext context) {
		String name = String.format("trace_0x%x", context.traceEntryAddress);
		
		generateC(context, name + ".c");	
		compileAndExtractAssembly(context, name + ".c", name + ".S");
		emitInjectableTraceAssembly(context, name + ".S", "InjectableTrace.S");
		emitInjectableTraceDescriptionFile(context, "trace_inject_desc.cfg");
	}
	
	public void generateC(CodeGenContext context, String name) {
		CTraceGenerator generator = new CTraceGenerator(context);
		generator.prepare(name);
		generator.generate();
		generator.finish();
	}
	
	public void compileAndExtractAssembly(CodeGenContext context, String cName, String asmName) {
		
	}
	
	public void emitInjectableTraceAssembly(CodeGenContext context, String asmName, String injectableTraceName) {
		
	}
	
	public void emitInjectableTraceDescriptionFile(CodeGenContext context, String name) {
		
	}
}
