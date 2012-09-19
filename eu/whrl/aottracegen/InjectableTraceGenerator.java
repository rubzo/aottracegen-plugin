package eu.whrl.aottracegen;

public class InjectableTraceGenerator {

	public InjectableTraceGenerator() {
		
	}
	
	public void loadAsmFiles(CodeGenContext context, String[] asmFileNames) {
		for (String asmFileName : asmFileNames) {
			loadAsmFile(context, asmFileName);
		}
		
	}

	private void loadAsmFile(CodeGenContext context, String asmFileName) {
		String traceCode = extractTraceFunction(context, asmFileName);
		
		// ...
		
	}

	private String extractTraceFunction(CodeGenContext context, String asmFileName) {
		
		return "noll";
	}

	public void createInjectableTrace(CodeGenContext context,
			String injectableTraceName) {
		// TODO Auto-generated method stub
		
	}

}
