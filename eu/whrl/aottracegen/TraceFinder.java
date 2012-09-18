package eu.whrl.aottracegen;

import java.util.HashMap;

import org.jf.dexlib.ClassDataItem.EncodedMethod;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InvokeInstruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.Code.Format.SparseSwitchDataPseudoInstruction;
import org.jf.dexlib.Util.SparseIntArray;

public class TraceFinder {
	public HashMap<Integer,Trace> generateTracesFromMethod(CodeGenContext context, EncodedMethod method) {
		
		context.instructions = method.codeItem.getInstructions();
		
		// MAP <switch data location> -> <switch inst code address>
		context.packedSwitchMap = new SparseIntArray(1);
		// MAP <switch data location> -> <switch inst code address>
		context.sparseSwitchMap = new SparseIntArray(1);
		// MAP <inst code address> -> <index in instructions list>
		context.instructionMap = new SparseIntArray(context.instructions.length);

		// Create the packed switch, sparse switch and instruction maps.
        int currentCodeAddress = 0;
        for (int i=0; i<context.instructions.length; i++) {
            Instruction instruction = context.instructions[i];
            if (instruction.opcode == Opcode.PACKED_SWITCH) {
            	context.packedSwitchMap.append(
                        currentCodeAddress +
                                ((OffsetInstruction)instruction).getTargetAddressOffset(),
                        currentCodeAddress);
            } else if (instruction.opcode == Opcode.SPARSE_SWITCH) {
            	context.sparseSwitchMap.append(
                        currentCodeAddress +
                                ((OffsetInstruction)instruction).getTargetAddressOffset(),
                        currentCodeAddress);
            }
            context.instructionMap.append(currentCodeAddress, i);
            currentCodeAddress += instruction.getSize(currentCodeAddress);
        }
        
        // Now generate all the traces.
        HashMap<Integer,Trace> traceMap = new HashMap<Integer,Trace>();
        generateAllTracesInMethod(context, traceMap);
        
        return traceMap;
	}
	
	private boolean isInvokeInstruction(Instruction i) {
		if (i instanceof InvokeInstruction) {
			return true;
		}
		return false;
	}
	
	private boolean isTraceEndingInstruction(CodeGenContext context, int codeAddress, Instruction i) {
		if (i.opcode == Opcode.IF_EQ ||
				i.opcode == Opcode.IF_EQZ ||
				i.opcode == Opcode.IF_GE ||
				i.opcode == Opcode.IF_GEZ ||
				i.opcode == Opcode.IF_GT ||
				i.opcode == Opcode.IF_GTZ ||
				i.opcode == Opcode.IF_LE ||
				i.opcode == Opcode.IF_LEZ ||
				i.opcode == Opcode.IF_LT ||
				i.opcode == Opcode.IF_LTZ ||
				i.opcode == Opcode.IF_NE ||
				i.opcode == Opcode.IF_NEZ) {
			return true;
		}
		
		if (i instanceof InvokeInstruction) {
			return true;
		}
		
		if (i.opcode == Opcode.PACKED_SWITCH || i.opcode == Opcode.SPARSE_SWITCH) {
			return true;
		}
		
		if (i.opcode == Opcode.RETURN ||
				i.opcode == Opcode.RETURN_OBJECT ||
				i.opcode == Opcode.RETURN_VOID ||
				i.opcode == Opcode.RETURN_VOID_BARRIER ||
				i.opcode == Opcode.RETURN_WIDE) {
			return true;
		}
		
		if (i.opcode == Opcode.THROW) {
			return true;
		}
		
		int nextCodeAddress = getNextCodeAddress(codeAddress, i);
		Instruction nextInstruction = context.getInstructionAtCodeAddress(nextCodeAddress);
		if (nextInstruction.opcode == Opcode.PACKED_SWITCH ||
				nextInstruction.opcode == Opcode.SPARSE_SWITCH) {
			return true;
		}
		
		return false;
	}
	
	private int getNextCodeAddress(int currentCodeAddress, Instruction instruction) {
		int nextCodeAddress = currentCodeAddress + instruction.getSize(currentCodeAddress);
		if (instruction.opcode == Opcode.GOTO || 
				instruction.opcode == Opcode.GOTO_16 || 
				instruction.opcode == Opcode.GOTO_32) {
			nextCodeAddress = currentCodeAddress + ((OffsetInstruction) instruction).getTargetAddressOffset();
		} 
		if (nextCodeAddress == currentCodeAddress) {
			// We have a loop
			return -1;
		}
		return nextCodeAddress;
	}
	
	private void handleSuccessors(CodeGenContext context, Trace trace, int currentCodeAddress, Instruction currentInstruction) {
		// Get the address where we'll be going next.
		int fallthruCodeAddress = getNextCodeAddress(currentCodeAddress, currentInstruction);
		
		if (currentInstruction.opcode == Opcode.IF_EQ ||
				currentInstruction.opcode == Opcode.IF_EQZ ||
				currentInstruction.opcode == Opcode.IF_GE ||
				currentInstruction.opcode == Opcode.IF_GEZ ||
				currentInstruction.opcode == Opcode.IF_GT ||
				currentInstruction.opcode == Opcode.IF_GTZ ||
				currentInstruction.opcode == Opcode.IF_LE ||
				currentInstruction.opcode == Opcode.IF_LEZ ||
				currentInstruction.opcode == Opcode.IF_LT ||
				currentInstruction.opcode == Opcode.IF_LTZ ||
				currentInstruction.opcode == Opcode.IF_NE ||
				currentInstruction.opcode == Opcode.IF_NEZ) {
			
			trace.allocSuccessors(2);
			trace.addSuccessor(fallthruCodeAddress);
			trace.addSuccessor(currentCodeAddress + ((OffsetInstruction)currentInstruction).getTargetAddressOffset());
			return;
		}
		
		if (currentInstruction.opcode == Opcode.PACKED_SWITCH) {
			int switchDataAddress = currentCodeAddress + 
					((OffsetInstruction)currentInstruction).getTargetAddressOffset();
			PackedSwitchDataPseudoInstruction switchData = (PackedSwitchDataPseudoInstruction) context.getInstructionAtCodeAddress(switchDataAddress);
			
			trace.allocSuccessors(switchData.getTargetCount() + 1);
			trace.addSuccessor(fallthruCodeAddress);
			for (int target : switchData.getTargets()) {
				trace.addSuccessor(currentCodeAddress + target);
			}
			return;
		}
		
		if (currentInstruction.opcode == Opcode.SPARSE_SWITCH) {
			int switchDataAddress = currentCodeAddress + 
					((OffsetInstruction)currentInstruction).getTargetAddressOffset();
			SparseSwitchDataPseudoInstruction switchData = (SparseSwitchDataPseudoInstruction) context.getInstructionAtCodeAddress(switchDataAddress);
			
			trace.allocSuccessors(switchData.getTargetCount() + 1);
			trace.addSuccessor(fallthruCodeAddress);
			for (int target : switchData.getTargets()) {
				trace.addSuccessor(currentCodeAddress + target);
			}
			return;
		}
		
		// If we get here, there's only the fallthru successor.
		trace.allocSuccessors(1);
		trace.addSuccessor(fallthruCodeAddress);
		return;
	}
	
	private void generateAllTracesInMethod(CodeGenContext context, HashMap<Integer,Trace> traceMap) {
		generateTracesFromCodeAddress(context, traceMap, 0x0);
	}
	
	private void generateTracesFromCodeAddress(CodeGenContext context, HashMap<Integer,Trace> traceMap, int codeAddress) {
		// First check, have we already generated the traces starting from this point?
		if (traceMap.containsKey(new Integer(codeAddress))) {
			return;
		}
		
		int currentCodeAddress = codeAddress;
		Instruction currentInstruction = null;
		
		// We haven't, so create a new trace.
		Trace trace = new Trace();
		
		// Initialise the trace
		trace.extend(currentCodeAddress);
		currentInstruction = context.getInstructionAtCodeAddress(currentCodeAddress);
		
		// Trace!
		while (!isTraceEndingInstruction(context, currentCodeAddress, currentInstruction)) {
			currentCodeAddress = getNextCodeAddress(currentCodeAddress, currentInstruction);
			if (currentCodeAddress == -1) {
				// We have a loop, bailout
				return;
			}
			
			trace.extend(currentCodeAddress);
			currentInstruction = context.getInstructionAtCodeAddress(currentCodeAddress);
		}
		
		// If the last instruction in the trace is an invoke, extend it to include the
		// return-* instruction.
		if (isInvokeInstruction(currentInstruction)) {
			currentCodeAddress = getNextCodeAddress(currentCodeAddress, currentInstruction);
			if (currentCodeAddress == -1) {
				// We have a loop, bailout
				return;
			}
			trace.extend(currentCodeAddress);
			currentInstruction = context.getInstructionAtCodeAddress(currentCodeAddress);
		}
		
		// Deal with successors
		handleSuccessors(context, trace, currentCodeAddress, currentInstruction);
		
		// Add to the map
		traceMap.put(new Integer(codeAddress), trace);
		
		// Recurse into the successors to get their traces
		for (int successor : trace.successors) {
			generateTracesFromCodeAddress(context, traceMap, successor);
		}
	}
}
