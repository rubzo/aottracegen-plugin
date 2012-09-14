package eu.whrl.aottracegen;

public class CodeGenerator {
	public CodeGenerator() { }
	
	public void generateCodeFromContext(CodeGenContext context) {				
		generateC(context);	
		compileAndExtractAssembly(context);
		emitInjectableTraceAssembly(context);
		emitInjectableTraceDescriptionFile(context);
	}
	
	public void generateC(CodeGenContext context) {
		CTraceGenerator generator = new CTraceGenerator(context);
		generator.prepare();
		generator.generate();
		generator.finish();
	}
	
	public void compileAndExtractAssembly(CodeGenContext context) {
		
	}
	
	public void emitInjectableTraceAssembly(CodeGenContext context) {
		
	}
	
	public void emitInjectableTraceDescriptionFile(CodeGenContext context) {
		
	}
}
