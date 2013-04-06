package eu.whrl.aottracegen;

import java.util.Map;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InvokeInstruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.Code.Format.SparseSwitchDataPseudoInstruction;

public class TraceFinder {
	
	/*
	 * Checks if a given instruction is an invoke instruction.
	 */
	private boolean isInvokeInstruction(Instruction i) {
		if (i instanceof InvokeInstruction) {
			return true;
		}
		return false;
	}
	
	private boolean isMoveResultInstruction(Instruction i) {
		if (i.opcode == Opcode.MOVE_RESULT ||
			i.opcode == Opcode.MOVE_RESULT_WIDE ||
			i.opcode == Opcode.MOVE_RESULT_OBJECT) {
			return true;
		}
		return false;
	}
	
	/*
	 * Checks if a given instruction should cause the tracer to stop.
	 */
	private boolean isTraceEndingInstruction(Region region, int codeAddress, Instruction instruction) {
		// Conditional branch? 
		if (instruction.opcode == Opcode.IF_EQ ||
				instruction.opcode == Opcode.IF_EQZ ||
				instruction.opcode == Opcode.IF_GE ||
				instruction.opcode == Opcode.IF_GEZ ||
				instruction.opcode == Opcode.IF_GT ||
				instruction.opcode == Opcode.IF_GTZ ||
				instruction.opcode == Opcode.IF_LE ||
				instruction.opcode == Opcode.IF_LEZ ||
				instruction.opcode == Opcode.IF_LT ||
				instruction.opcode == Opcode.IF_LTZ ||
				instruction.opcode == Opcode.IF_NE ||
				instruction.opcode == Opcode.IF_NEZ) {
			return true;
		}
		
		// Invoke instruction?
		if (isInvokeInstruction(instruction)) {
			return true;
		}
		
		// Switch statement?
		if (instruction.opcode == Opcode.PACKED_SWITCH || instruction.opcode == Opcode.SPARSE_SWITCH) {
			return true;
		}
		
		// Return statement?
		if (instruction.opcode == Opcode.RETURN ||
				instruction.opcode == Opcode.RETURN_OBJECT ||
				instruction.opcode == Opcode.RETURN_VOID ||
				instruction.opcode == Opcode.RETURN_VOID_BARRIER ||
				instruction.opcode == Opcode.RETURN_WIDE) {
			return true;
		}
		
		// Throwing an exception on purpose?
		if (instruction.opcode == Opcode.THROW) {
			return true;
		}
		
		// The instruction after you is going to be a switch statement?
		int nextCodeAddress = region.getNextCodeAddress(codeAddress, instruction);
		Instruction nextInstruction = region.getInstructionAtCodeAddress(nextCodeAddress);
		if (nextInstruction.opcode == Opcode.PACKED_SWITCH ||
				nextInstruction.opcode == Opcode.SPARSE_SWITCH) {
			return true;
		}
		
		// Carry on.
		return false;
	}
	
	/*
	 * Allocates the correct space for successors in the trace, and adds all successor code addresses to that array.
	 */
	private void handleSuccessors(Region region, Trace trace, int currentCodeAddress, Instruction currentInstruction) {
		// Get the address where we'll be going next.
		int fallthruCodeAddress = region.getNextCodeAddress(currentCodeAddress, currentInstruction);
		
		if (currentInstruction.opcode == Opcode.RETURN ||
				currentInstruction.opcode == Opcode.RETURN_VOID ||
				currentInstruction.opcode == Opcode.RETURN_OBJECT ||
				currentInstruction.opcode == Opcode.RETURN_VOID_BARRIER ||
				currentInstruction.opcode == Opcode.RETURN_WIDE) {
			return;
		}
		
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
			
			trace.addSuccessor(fallthruCodeAddress);
			trace.addSuccessor(currentCodeAddress + ((OffsetInstruction)currentInstruction).getTargetAddressOffset());
			return;
		}
		
		if (currentInstruction.opcode == Opcode.PACKED_SWITCH) {
			int switchDataAddress = currentCodeAddress + 
					((OffsetInstruction)currentInstruction).getTargetAddressOffset();
			
			PackedSwitchDataPseudoInstruction switchData = (PackedSwitchDataPseudoInstruction) region.getInstructionAtCodeAddress(switchDataAddress);
			
			trace.addSuccessor(fallthruCodeAddress);
			for (int target : switchData.getTargets()) {
				if (currentCodeAddress + target != fallthruCodeAddress) {
					trace.addSuccessor(currentCodeAddress + target);
				}
			}
			return;
		}
		
		if (currentInstruction.opcode == Opcode.SPARSE_SWITCH) {
			int switchDataAddress = currentCodeAddress + 
					((OffsetInstruction)currentInstruction).getTargetAddressOffset();
			
			SparseSwitchDataPseudoInstruction switchData = (SparseSwitchDataPseudoInstruction) region.getInstructionAtCodeAddress(switchDataAddress);
		
			trace.addSuccessor(fallthruCodeAddress);
			for (int target : switchData.getTargets()) {
				if (currentCodeAddress + target != fallthruCodeAddress) {
					trace.addSuccessor(currentCodeAddress + target);
				}
			}
			return;
		}
		
		// If we get here, there's only the fallthrough successor.
		trace.addSuccessor(fallthruCodeAddress);
		return;
	}
	
	/*
	 * Starts trace generation from a given code address.
	 */
	public void generateTracesFromCodeAddress(Region region, Map<Integer,Trace> traceMap, int codeAddress) {
		// First check, have we already generated the traces starting from this point?
		if (traceMap.containsKey(codeAddress)) {
			return;
		}
		
		int currentCodeAddress = codeAddress;
		Instruction currentInstruction = null;
		
		// We haven't, so create a new trace.
		Trace trace = new Trace();
		
		// Initialise the trace
		trace.extend(currentCodeAddress);
		currentInstruction = region.getInstructionAtCodeAddress(currentCodeAddress);
		
		// Trace!
		while (!isTraceEndingInstruction(region, currentCodeAddress, currentInstruction)) {
			currentCodeAddress = region.getNextCodeAddress(currentCodeAddress, currentInstruction);
			if (currentCodeAddress == -1) {
				// We have a loop, bailout
				return;
			}
			
			trace.extend(currentCodeAddress);
			currentInstruction = region.getInstructionAtCodeAddress(currentCodeAddress);
		}
		
		// If the last instruction in the trace is an invoke, extend it to include the
		// move-return-* instruction.
		if (isInvokeInstruction(currentInstruction)) {
			currentCodeAddress = region.getNextCodeAddress(currentCodeAddress, currentInstruction);
			if (currentCodeAddress == -1) {
				// We have a loop, bailout
				return;
			}
			if (isMoveResultInstruction(region.getInstructionAtCodeAddress(currentCodeAddress))) {
				trace.extend(currentCodeAddress);
				currentInstruction = region.getInstructionAtCodeAddress(currentCodeAddress);
			}
		}
		
		// Deal with successors
		handleSuccessors(region, trace, currentCodeAddress, currentInstruction);
		
		trace.entry = trace.addresses.get(0);
		
		// Add to the map
		traceMap.put(codeAddress, trace);
		
		// Recurse into the successors to get their traces
		for (int successor : trace.successors) {
			generateTracesFromCodeAddress(region, traceMap, successor);
		}
	}
}
