package eu.whrl.aottracegen;

import java.util.TreeMap;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.Analysis.ClassPath;
import org.jf.dexlib.Code.Format.Instruction35ms;

public class MethodLookup {
	/*
	 * Make it a singleton.
	 */
	private MethodLookup() { }
	private static MethodLookup singleton = new MethodLookup();
	public static MethodLookup getMethodLookup() {
		return singleton;
	}
	
	private static TreeMap<String,EncodedMethod> methodMap = new TreeMap<String,EncodedMethod>();
	
	public EncodedMethod getSuperQuickMethodFromInstruction(Instruction instruction, CodeGenContext context) {
		/* get vtable index */
		int vtableIndex = ((Instruction35ms)instruction).getVtableIndex();
		
		/* get super class */
		TypeIdItem clazz = context.currentRegion.encodedMethod.method.getContainingClass();
		ClassPath.ClassDef superClassDef = ClassPath.getClassDef(clazz);
		
		String methodName = clazz.getTypeDescriptor() + "->" + superClassDef.getVirtualMethod(vtableIndex);
		EncodedMethod method = methodMap.get(methodName);
		if (method == null) {
			System.err.println("Unable to find super quick callee method referenced from invoke in our mapping: " + methodName);
		}
		return method;
	}
	
	public EncodedMethod getCalleeMethodFromInstruction(Instruction instruction, CodeGenContext context) {
		MethodIdItem methodId = (MethodIdItem) ((InstructionWithReference)instruction).getReferencedItem();
		String methodName = methodId.getMethodString();
		EncodedMethod method = methodMap.get(methodName);
		if (method == null) {
			System.err.println("Unable to find callee method referenced from invoke in our mapping: " + methodName);
		}
		return method;
	}
	
	public EncodedMethod getMethodByName(String methodName) {
		EncodedMethod method = methodMap.get(methodName);
		if (method == null) {
			System.err.println("Unable to find method in our mapping: " + methodName);
		}
		return method;
	}
	
	public void addMethod(String name, EncodedMethod method) {
		methodMap.put(name, method);
	}
	
	public void initClassPath(DexFile dexFile) {
		String[] classPathDirs = {"."};
		String[] extraDirs = {};
		ClassPath.InitializeClassPathFromOdex(classPathDirs, extraDirs, "my odex", dexFile, false);
	}
}
