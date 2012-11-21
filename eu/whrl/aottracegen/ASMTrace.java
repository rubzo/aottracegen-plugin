package eu.whrl.aottracegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OdexedInvokeVirtual;

public class ASMTrace {
	private List<String> traceBody;
	
	public String getFullStringTraceBody() {
		String result = "";
		for (String line : traceBody) {
			result += line + "\n";
		}
		return result;
	}
	
	public void setTraceBody(List<String> traceBody) {
		this.traceBody = traceBody;
	}
	
	private String cleanupLine(String line) {
		return line.replaceFirst("\\s+","").replaceAll("\\s+"," ");
	}
	
	public void fixBadBranch(Set<String> badLines) {
		for (int cl = 0; cl < traceBody.size(); cl++) {
			
			String line = traceBody.get(cl);
			String cleanedLine = cleanupLine(line);
			
			for (String badLine : badLines) {
				if (cleanedLine.equals(badLine)) {
					if (cleanedLine.startsWith("cbz")) {
						Pattern p = Pattern.compile("cbz (r\\d+), (.*)");
						Matcher m = p.matcher(cleanedLine);

						if (m.find()) {
							String reg = m.group(1);
							String targetLabel = m.group(2);

							cl = removeLine(cl);

							cl = addLine(cl, String.format("\tcmp\t%s, #0", reg));
							cl = addLine(cl, String.format("\tbeq\t%s", targetLabel));
						}
					} else if (cleanedLine.startsWith("cbnz")) { 
						Pattern p = Pattern.compile("cbnz (r\\d+), (.*)");
						Matcher m = p.matcher(cleanedLine);
						if (m.find()) {
							String reg = m.group(1);
							String targetLabel = m.group(2);

							cl = removeLine(cl);

							cl = addLine(cl, String.format("\tcmp\t%s, #0", reg));
							cl = addLine(cl, String.format("\tbne\t%s", targetLabel));
						}
					} else {
						System.err.println("Don't know what to do with: " + badLine);
					}
				}
			}
		}
	}
	
	/*
	 * Modifies the loaded traceBody, doing the following:
	 * 
	 * - remove push/pop instructions
	 * - remove any branches to the original exit label
	 * - rename all line labels to be unique when output with other traces
	 * - replace all bl's to exit/exception functions with b's to our exit/exception labels
	 * - remove any redundant labels
	 */
	public void cleanupTrace(CodeGenContext context) {
		optRemovePushPopInsts(context);		
		removeStackManipulation(context);
		renameLabels(context);
		generalCleanup(context);
		emitHandlers(context);
		removeRedundantLabels(context);	
		addLiteralPoolPointer(context);
		//removeStackReferences(context);
	}
	
	private void optRemovePushPopInsts(CodeGenContext context) {
		// Find the push/pop instructions,
		//
		int pushIdx = 0;
		List<Integer> popIdxs = new LinkedList<Integer>();

		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);
			
			if (line.startsWith("\tldr\tlr, [sp]") || line.startsWith("\tldmfd\t") || line.startsWith("\tpop\t{")) {				
				popIdxs.add(cl);
			} else if (line.startsWith("\tpush\t{") || line.startsWith("\tstmfd\t")) {
				pushIdx = cl;
			}
		}
		
		// Remove push/pops
		//
		traceBody.remove(pushIdx);
		int i = 1;
		for (int idx : popIdxs) {
			traceBody.remove(idx-i);
			i++;
		}
	}
	
	private void removeStackManipulation(CodeGenContext context) {
		// Remove any instructions that add or subtract the stack pointer, as this breaks the interpreter.
		//
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = cleanupLine(traceBody.get(cl));

			if (line.contains("add sp, sp") || line.contains("sub sp, sp")) {
				System.out.println("Removing " + line);
				traceBody.remove(cl);
				cl--;
			}
		}
	}
	
	private void renameLabels(CodeGenContext context) {
		Trace curTrace = context.getCurrentTrace();
		
		// Rename all the .L labels, so they don't clash if we're pasting them together
		// with other asm traces to form an injectable trace.
		//
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.contains(".L")) {
				line = line.replaceAll(".L(\\d+)", String.format(".LT%#x_$1", curTrace.entry));
			}

			traceBody.remove(cl);
			traceBody.add(cl, line);
		}
	}
	
	private void generalCleanup(CodeGenContext context) {
		// Remove #APP and @ signs - general cleanup, basically.
		// 
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.startsWith("#APP")) {
				traceBody.remove(cl);
				cl--;
			} else if (line.startsWith("@")) {
				traceBody.remove(cl);
				cl--;
			} else if (line.contains(".thumb")) {
				traceBody.remove(cl);
				cl--;
			}
		}		
	}
	
	private void emitHandlers(CodeGenContext context) {
		// Replace all the bl's to exit/exception functions with b's to labels we'll generate
		// in the injectable trace.
		//
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			if (line.contains("b\texception")) {
				cl = handleException(context, cl);
			} else if (line.contains("b\texit")) {
				cl = handleExit(context, cl);
			} else if (line.contains("b\treturn")) {
				cl = handleReturn(context, cl);
			} else if (line.contains("# invoke_virtual_quick")) {
				cl = handleInvokeVirtualQuick(context, cl);
			}
		}
	}
	
	private void removeRedundantLabels(CodeGenContext context) {
		Trace curTrace = context.getCurrentTrace();
		
		// Remove redundant labels
		//
		// NB: don't do this for traces containing switches just now - some teething troubles need ironed out.
		if (!curTrace.meta.containsSwitch) {
			Set<String> referencedLabels = new HashSet<String>();
			for (int cl = 0; cl < traceBody.size(); cl++) {
				String line = traceBody.get(cl);

				if (line.matches(".*\\.L.*[^:]$")) {
					Pattern p = Pattern.compile(".*(\\.L.*)$");
					Matcher m = p.matcher(line);
					if (m.find()) {
						referencedLabels.add(m.group(1));
					}
				}
			}
			for (int cl = 0; cl < traceBody.size(); cl++) {
				String line = traceBody.get(cl);

				if (line.matches("^\\.L.*:$") && !referencedLabels.contains(line.substring(0, line.length() - 1))) {
					traceBody.remove(cl);
					cl--;
				}
			}
		}
	}
	
	private void addLiteralPoolPointer(CodeGenContext context) {
		Trace curTrace = context.getCurrentTrace();
		
		if (context.getCurrentTrace().meta.literalPoolSize > 0) {
			addLine(0, String.format("\tadr.w\tr0, ITrace_%#x_LiteralPool", curTrace.entry));
		}
	}
	
	private void addStackReference(CodeGenContext context, Map<Integer, StackSpill> stackSpills, int loc, String line) {
		Pattern stackStorePattern = Pattern.compile("\tstr\tr(\\d+), \\[sp, #(\\d+)\\]$");
		Pattern addPattern = Pattern.compile("\tadds?\tr\\d+, r(\\d+), #(.+)$");
		Pattern postIndexPattern = Pattern.compile("\t.+\tr\\d+, \\[r(\\d+)\\], #(\\d+)");
		
		StackSpill spill = new StackSpill();
		spill.reg = 0;
		spill.offset = 0;
		if (line.contains("LiteralPool")) {
			spill.type = StackSpill.Type.LITPOOL_POINTER;
		} else if (line.contains("sp")) {
			spill.type = StackSpill.Type.SUM;
			
			Matcher stackStoreMatcher = stackStorePattern.matcher(line);
			if (stackStoreMatcher.find()) {
				spill.reg = Integer.parseInt(stackStoreMatcher.group(1)); 
			} else {
				System.err.println("Shouldn't happen");
			}
		} else if (line.contains("add")) {
			spill.type = StackSpill.Type.SUM;
			
			Matcher addMatcher = addPattern.matcher(line);
			if (addMatcher.find()) {
				spill.reg = Integer.parseInt(addMatcher.group(1));
				spill.offset = Integer.parseInt(addMatcher.group(2));
			} else {
				System.err.println("Shouldn't happen");
			}
		} else {
			System.err.println("I DON'T GET THIS! wink");
			
			spill.type = StackSpill.Type.LITPOOL_POINTER_SUM;
			
			Matcher addMatcher = postIndexPattern.matcher(line);
			if (addMatcher.find()) {
				spill.offset = Integer.parseInt(addMatcher.group(2));
			} else {
				System.err.println("Shouldn't happen");
			}
		}
		
		stackSpills.put(loc, spill);
	}
	
	private void removeStackReferences(CodeGenContext context) {
		Trace curTrace = context.getCurrentTrace();
		
		Pattern stackStorePattern = Pattern.compile("\tstr\tr(\\d+), \\[sp, #(\\d+)\\]$");
		Pattern registerWritePattern = Pattern.compile("\t.+\tr(\\d+)");
		Pattern registerWritePostIndexPattern = Pattern.compile("\t.+\tr\\d+, \\[r(\\d+)\\], #\\d+");
		
		Map<Integer, StackSpill> stackSpills = new HashMap<Integer, StackSpill>();
		
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			Matcher stackStoreMatcher = stackStorePattern.matcher(line);
			if (stackStoreMatcher.find()) {
				System.out.println("Found store of r" + stackStoreMatcher.group(1) + " to stack loc " + stackStoreMatcher.group(2));
				
				int previousRegister = Integer.parseInt(stackStoreMatcher.group(1));
				int stackLocation = Integer.parseInt(stackStoreMatcher.group(2));
				
				int savedCl = cl;
				
				boolean foundPrevious = false;
				
				if (previousRegister == 5 || previousRegister == 6) {
					System.out.println("Found previous write: " + line);
					addStackReference(context, stackSpills, stackLocation, line);
					foundPrevious = true;
				}
				
				while (!foundPrevious) {
					cl--;
					line = traceBody.get(cl);
					
					//System.out.println(line);
					
					Matcher registerWriteMatcher = registerWritePattern.matcher(line);
					if (registerWriteMatcher.find()) {
						int reg = Integer.parseInt(registerWriteMatcher.group(1));
						
						if (reg == previousRegister && !line.contains("sp")) {
							System.out.println("Found previous write: " + line);
							foundPrevious = true;
							addStackReference(context, stackSpills, stackLocation, line);
						} else {
							Matcher registerWritePostIndexMatcher = registerWritePostIndexPattern.matcher(line);
							if (registerWritePostIndexMatcher.find()) {
								reg = Integer.parseInt(registerWritePostIndexMatcher.group(1));
								if (reg == previousRegister) {
									System.out.println("Found previous write: " + line);
									foundPrevious = true;
									addStackReference(context, stackSpills, stackLocation, line);
								}
							}
						}
					}
					
					// Run out of lines?
					if (cl == 0 && !foundPrevious) {
						System.err.println("Couldn't find previous write! :(");
						foundPrevious = true;
					}
				}
				
				cl = savedCl;
				line = traceBody.get(cl);
				cl = replaceLine(cl, "# Removed: " + line);
			}
		}
		
		// Half way there!
		Pattern stackLoadPattern = Pattern.compile("\tldr\tr(\\d+), \\[sp, #(\\d+)\\]$");
		
		for (int cl = 0; cl < traceBody.size(); cl++) {
			String line = traceBody.get(cl);

			Matcher stackLoadMatcher = stackLoadPattern.matcher(line);
			if (stackLoadMatcher.find()) {
				System.out.println("Found load of r" + stackLoadMatcher.group(1) + " from stack loc " + stackLoadMatcher.group(2));
				
				int register = Integer.parseInt(stackLoadMatcher.group(1));
				int stackLocation = Integer.parseInt(stackLoadMatcher.group(2));
				
				StackSpill spill = stackSpills.get(stackLocation);
				
				cl = replaceLine(cl, "# " + line);
				cl++;
				
				switch (spill.type) {
				case LITPOOL_POINTER:
					cl = addLine(cl, String.format("\tadr.w\tr%d, ITrace_%#x_LiteralPool", register, curTrace.entry));
					break;
				case SUM:
					cl = addLine(cl, String.format("\tadd\tr%d, r%d, #%d", register, spill.reg, spill.offset));
					break;
				case LITPOOL_POINTER_SUM:
					cl = addLine(cl, String.format("\tadr.w\tr%d, ITrace_%#x_LiteralPool", register, curTrace.entry));
					cl = addLine(cl, String.format("\tadd\tr%d, r%d, #%d", register, register, spill.offset));
					break;
				}
				
				cl = addLine(cl, "# replacement done");
				cl--;
			}
		}
	}
	
	private int handleExit(CodeGenContext context, int cl) {
		Trace curTrace = context.getCurrentTrace();
		
		cl = replaceLine(cl, traceBody.get(cl).replaceFirst("\tb\texit_L(0x[0-9a-f]+)\\(PLT\\)", String.format("\tb\tLT%#x_CC_$1", curTrace.entry)));
		
		return cl;
	}
	
	private int handleException(CodeGenContext context, int cl) {
		Trace curTrace = context.getCurrentTrace();
		
		cl = replaceLine(cl, traceBody.get(cl).replaceFirst("\tb\texception_L(0x[0-9a-f]+)\\(PLT\\)", String.format("\tb\tLT%#x_EH_$1", curTrace.entry)));
		
		return cl;
	}
	
	private int handleReturn(CodeGenContext context, int cl) {
		Trace curTrace = context.getCurrentTrace();
		
		// If we add an entry to the literal pool, then the return 
		// handler will be loaded into this literal pool location.
		//
		int literalPoolLoc = curTrace.meta.addLiteralPoolType(LiteralPoolType.RETURN_HANDLER);
		
		// Create the jump to the return handler
		cl = addLine(cl, String.format("\tadr.w\tr2, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr0, [r2, #%d]", literalPoolLoc*4));
		cl = addLine(cl, "\tblx\tr0");
		
		
		cl = replaceLine(cl, traceBody.get(cl).replaceFirst("\tb\treturn_L(0x[0-9a-f]+)\\(PLT\\)", String.format("\tb\tLT%#x_EH_$1", curTrace.entry)));
		
		return cl;
	}
	
	private int handleInvokeVirtualQuick(CodeGenContext context, int cl) {
		Trace curTrace = context.getCurrentTrace();
		
		String line = traceBody.get(cl);
		
		int codeAddress = 0;
		Pattern p = Pattern.compile("# invoke_virtual_quick_L0x(.*)$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			codeAddress = Integer.parseInt(m.group(1), 16);
		}
		
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		// Remove the bl placeholder instruction
		//
		cl = removeLine(cl);
		
		// Handle arguments
		//
		cl = handleArgumentLoading(context, cl, codeAddress);
		
		// Load DPC from literal pool into r4
		//
		int literalPoolLoc = 0;
		if (!context.reassembling) {
			literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(LiteralPoolType.DPC_OFFSET, codeAddress);
		} else {
			literalPoolLoc = curTrace.meta.getLiteralPoolLocationForType(LiteralPoolType.DPC_OFFSET);
		}
		
		cl = addLine(cl, String.format("\tadr.w\tr2, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr4, [r2, #%d]", literalPoolLoc*4));
		
		// Load the address of the beginning of the next instruction into r1
		//
		cl = addLine(cl, String.format("\tadr.w\tr1, JumpAfter_%#x", codeAddress));
		
		// Load the address of the predicted chaining cell into r2
		//
		cl = addLine(cl, String.format("\tadr.w\tr2, LT%#x_CC_%#x", curTrace.entry, codeAddress));
		
		// Load the method predicted chain handler's address, blx to it
		//
		literalPoolLoc = curTrace.meta.addLiteralPoolType(LiteralPoolType.INVOKE_METHOD_PREDICTED_CHAIN_HANDLER);
		cl = addLine(cl, String.format("\tadr.w\tr3, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr3, [r3, #%d]", literalPoolLoc*4));
		cl = addLine(cl, "\tblx\tr3");
		
		// Jump to the predicted chaining cell
		//
		cl = addLine(cl, String.format("\tb\tLT%#x_CCLAUNCHPAD_%#x", curTrace.entry, codeAddress));
		
		// Jump to the exception handler
		//
		cl = addLine(cl, String.format("\tb\tLT%#x_EHLAUNCHPAD_%#x", curTrace.entry, codeAddress));
		
		// Load vtable[methodIdx] into r0
		//
		cl = addLine(cl, String.format("\tldr\tr0, [r7, #%d]", ((OdexedInvokeVirtual)instruction).getVtableIndex() * 4));
		
		cl = addLine(cl, String.format("\tb\tLT%#x_AFTERLAUNCHPADS_%#x", curTrace.entry, codeAddress));
		
		cl = addLine(cl, String.format("LT%#x_CCLAUNCHPAD_%#x:", curTrace.entry, codeAddress));
				
		// Jump to the predicted chaining cell
		//
		cl = addLine(cl, String.format("\tb\tLT%#x_CC_%#x", curTrace.entry, codeAddress));
		
		cl = addLine(cl, String.format("LT%#x_EHLAUNCHPAD_%#x:", curTrace.entry, codeAddress));
		
		// Jump to the exception handler
		//
		cl = addLine(cl, String.format("\tb\tLT%#x_EH_%#x", curTrace.entry, codeAddress));
		
		cl = addLine(cl, String.format("LT%#x_AFTERLAUNCHPADS_%#x:", curTrace.entry, codeAddress));
		
		
		// Compare r1 to 0
		//
		cl = addLine(cl, "\tcmp\tr1, #0");
		
		// If gt, jump past this next function call
		//
		cl = addLine(cl, String.format("\tbgt\tJumpAfterChainCall_%#x", codeAddress));
		
		// load dvmJitToPatchPredictedChain pointer
		//
		literalPoolLoc = curTrace.meta.addLiteralPoolType(LiteralPoolType.JIT_TO_PATCH_PREDICTED_CHAIN_HANDLER);
		cl = addLine(cl, String.format("\tadr.w\tr7, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr7, [r7, #%d]", literalPoolLoc*4));
		
		// Load self pointer into r1
		cl = addLine(cl, "\tmovs\tr1, r6");
		
		// blx to method
		//
		cl = addLine(cl, "\tblx\tr7");
		
		// Emit this label that we use to avoid the dvmJitToPatchPredictedChain call
		//
		cl = addLine(cl, String.format("JumpAfterChainCall_%#x:", codeAddress));
		
		// load address of next instruction into r1
		//
		cl = addLine(cl, String.format("\tadr.w\tr1, JumpAfter_%#x", codeAddress));
		
		// load the invoke method no opt handler, blx to it
		//
		literalPoolLoc = curTrace.meta.addLiteralPoolType(LiteralPoolType.INVOKE_METHOD_NO_OPT_HANDLER);		
		cl = addLine(cl, String.format("\tadr.w\tr7, ITrace_%#x_LiteralPool", curTrace.entry));
		cl = addLine(cl, String.format("\tldr\tr7, [r7, #%d]", literalPoolLoc*4));
		cl = addLine(cl, "\tblx\tr7");
		
		// If there's an exception in the handler, we'll return here, so jump to our exception handler.
		//
		cl = addLine(cl, String.format("\tb\tLT%#x_EH_%#x", curTrace.entry, codeAddress));
		
		cl = addLine(cl, "\t.align 4");
		
		// Emit this label that we use as the return point after function invocation.
		//
		cl = addLine(cl, String.format("JumpAfter_%#x:", codeAddress));
		
		// Restore the literal pool pointer, if needed
		// NB: this assumes that GCC left the literal pointer in r0 at this point, obviously this may not be the case
		if (curTrace.meta.literalPoolSize > 0) {
			cl = addLine(cl, String.format("\tadr.w\tr0, ITrace_%#x_LiteralPool", curTrace.entry));
		}
		
		return cl;
	}
	
	private int handleArgumentLoading(CodeGenContext context, int cl, int codeAddress) {
		Trace curTrace = context.getCurrentTrace();
		
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		cl = addLine(cl, "  # begin arg loading");
		
		int regCount = ((FiveRegisterInstruction)instruction).getRegCount();
		
		if (regCount > 0) {
			cl = addLine(cl, String.format("\tldr\tr0, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterD()*4 ) );
		}
		if (regCount > 1) {
			cl = addLine(cl, String.format("\tldr\tr1, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterE()*4 ) );
		}
		if (regCount > 2) {
			cl = addLine(cl, String.format("\tldr\tr2, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterF()*4 ) );
		}
		if (regCount > 3) {
			cl = addLine(cl, String.format("\tldr\tr3, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterG()*4 ) );
		}
		if (regCount > 4) {
			cl = addLine(cl, String.format("\tldr\tr4, [r5, #%d]", ((FiveRegisterInstruction)instruction).getRegisterA()*4 ) );
		}
		
		cl = addLine(cl, String.format("\tsub\tr7, r5, #%d", 20 /*size of StackSaveArea*/ + ((FiveRegisterInstruction)instruction).getRegCount()*4));
		
		if (regCount > 0) {
			cl = addLine(cl, "\tcmp\tr0, #0");
			
			cl = addLine(cl, String.format("\tbeq\tLT%#x_EH_%#x", curTrace.entry, codeAddress));
			
			String argsToPush = "";
			for (int i = 0; i < regCount; i++) {
				argsToPush += String.format("r%d",i);
				if (i != (regCount-1)) {
					argsToPush += ",";
				}
			}
			
			cl = addLine(cl, String.format("\tstmia\tr7, {%s}", argsToPush));
		}
		cl = addLine(cl, "  # end arg loading");
		
		return cl;
	}
	
	private int removeLine(int cl) {
		traceBody.remove(cl);
		
		return cl;
	}
	
	private int replaceLine(int cl, String line) {
		traceBody.remove(cl);
		traceBody.add(cl, line);
		
		return cl;
	}
	
	private int addLine(int cl, String line) {
		traceBody.add(cl, line);
		
		return cl + 1;
	}
}
