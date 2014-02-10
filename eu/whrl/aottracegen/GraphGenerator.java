package eu.whrl.aottracegen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.Code.Format.SparseSwitchDataPseudoInstruction;

public class GraphGenerator {
	
	// BasicBlock!
	class BasicBlock {
		public int head;
		public List<Integer> contents;
		public List<BasicBlock> successors;
		
		public BasicBlock() {
			head = -1;
			contents = new LinkedList<Integer>();
			successors = new LinkedList<BasicBlock>();
		}
		
		public boolean contains(int codeAddress) {
			return (codeAddress != head && contents.contains(codeAddress));
		}
		
		public BasicBlock split(int codeAddress) {
			BasicBlock newBasicBlock = new BasicBlock();
			
			newBasicBlock.head = codeAddress;
			newBasicBlock.successors = successors;
			
			successors = new LinkedList<BasicBlock>();
			successors.add(newBasicBlock);
			
			int copyIndex = contents.indexOf(codeAddress);
			int newContentsSize = contents.size() - copyIndex;
			
			for (int i = 0; i < newContentsSize; i++) {
				
				newBasicBlock.contents.add(contents.remove(copyIndex));
			}
			
			return newBasicBlock;
		}
	}
	
	private BasicBlock currentBasicBlock = null;
	private Map<Integer,BasicBlock> basicBlocks;
	
	public GraphGenerator() {
		basicBlocks = new HashMap<Integer,BasicBlock>();
	}
	
	public void visit(Region region, Instruction inst, int codeAddress, int nextCodeAddress) {
		
		// Has a basic block for this address already been created?
		if (basicBlocks.containsKey(codeAddress)) {
			
			// Were we in the middle of a basic block already? Time to extend!
			if (currentBasicBlock != null) {
				currentBasicBlock.successors.add(basicBlocks.get(codeAddress));
			}
			
			// We now have our new basic block
			currentBasicBlock = basicBlocks.get(codeAddress);
		} 

		// Don't have a basic block for this instruction? Make one!
		if (currentBasicBlock == null){
			currentBasicBlock = new BasicBlock();
			currentBasicBlock.head = codeAddress;
			basicBlocks.put(codeAddress, currentBasicBlock);
		}
		
		// Record this address as part of the current basic block
		currentBasicBlock.contents.add(codeAddress);
		
		// Are we finishing this basic block?
		if (isBasicBlockEnded(inst)) {
			// Get every successor address of this basic block
			List<Integer> successors = getSuccessors(region, inst, codeAddress, nextCodeAddress);
			
			for (int successor : successors) {
				
				BasicBlock successorBasicBlock = null;
				
				// Have we seen this successor already?
				if (basicBlocks.containsKey(successor)) {
					
					successorBasicBlock = basicBlocks.get(successor);
					
				} else {

					// Is this successor part of some other basic block (requiring a split)
					for (BasicBlock otherBasicBlock : basicBlocks.values()) {
						if (otherBasicBlock.contains(successor)) {
							successorBasicBlock = otherBasicBlock.split(successor);
						}
					}
					
					// We didn't split a basic block, this successor must be new?
					if (successorBasicBlock == null) {
						successorBasicBlock = new BasicBlock();
						successorBasicBlock.head = successor;
					}
					
					// Remember our new basic block
					basicBlocks.put(successor, successorBasicBlock);	
				
				}
				
				// Mark this successor basic block as a successor of the current one
				currentBasicBlock.successors.add(successorBasicBlock);
				
			}
			
			// We've checked all the successors, not currently looking at a basic block
			currentBasicBlock = null;
		}
	}
	
	private boolean isBasicBlockEnded(Instruction inst) {
		if (inst.opcode == Opcode.IF_EQ ||
				inst.opcode == Opcode.IF_EQZ ||
				inst.opcode == Opcode.IF_GE ||
				inst.opcode == Opcode.IF_GEZ ||
				inst.opcode == Opcode.IF_LE ||
				inst.opcode == Opcode.IF_LEZ ||
				inst.opcode == Opcode.IF_NE ||
				inst.opcode == Opcode.IF_NEZ ||
				inst.opcode == Opcode.IF_GT ||
				inst.opcode == Opcode.IF_GTZ ||
				inst.opcode == Opcode.IF_LT ||
				inst.opcode == Opcode.IF_LTZ ||
				inst.opcode == Opcode.PACKED_SWITCH ||
				inst.opcode == Opcode.SPARSE_SWITCH ||
				inst.opcode == Opcode.GOTO ||
				inst.opcode == Opcode.GOTO_16 ||
				inst.opcode == Opcode.GOTO_32 ||
				inst.opcode == Opcode.RETURN ||
				inst.opcode == Opcode.RETURN_OBJECT ||
				inst.opcode == Opcode.RETURN_VOID ||
				inst.opcode == Opcode.RETURN_VOID_BARRIER ||
				inst.opcode == Opcode.RETURN_WIDE) {
			return true;
		}
		return false;
	}
	
	private List<Integer> getSuccessors(Region region, Instruction inst, int codeAddress, int nextCodeAddress) {
		List<Integer> theList = new LinkedList<Integer>();
		
		// do fallthru
		if (inst.opcode != Opcode.RETURN &&
				inst.opcode != Opcode.RETURN_OBJECT &&
				inst.opcode != Opcode.RETURN_VOID &&
				inst.opcode != Opcode.RETURN_VOID_BARRIER &&
				inst.opcode != Opcode.RETURN_WIDE &&
				inst.opcode != Opcode.GOTO &&
				inst.opcode != Opcode.GOTO_16 &&
				inst.opcode != Opcode.GOTO_32) {
			theList.add(nextCodeAddress);
		}
		
		// do target
		if (inst.opcode == Opcode.IF_EQ ||		
				inst.opcode == Opcode.IF_EQZ ||
				inst.opcode == Opcode.IF_GE ||
				inst.opcode == Opcode.IF_GEZ ||
				inst.opcode == Opcode.IF_LE ||
				inst.opcode == Opcode.IF_LEZ ||
				inst.opcode == Opcode.IF_NE ||
				inst.opcode == Opcode.IF_NEZ ||
				inst.opcode == Opcode.IF_GT ||
				inst.opcode == Opcode.IF_GTZ ||
				inst.opcode == Opcode.IF_LT ||
				inst.opcode == Opcode.IF_LTZ ||
				inst.opcode == Opcode.GOTO ||
				inst.opcode == Opcode.GOTO_16 ||
				inst.opcode == Opcode.GOTO_32) {
			int offset = ((OffsetInstruction)inst).getTargetAddressOffset();
			theList.add(codeAddress + offset);
			
		// do switch
		} else {
			if (inst.opcode == Opcode.PACKED_SWITCH || inst.opcode == Opcode.SPARSE_SWITCH) {
				int[] targets = null;
				int dataOffset = ((OffsetInstruction) inst).getTargetAddressOffset();
				if (inst.opcode == Opcode.PACKED_SWITCH) {
					PackedSwitchDataPseudoInstruction dataInstruction = (PackedSwitchDataPseudoInstruction) region
							.getInstructionAtCodeAddress(codeAddress + dataOffset);
					
					targets = dataInstruction.getTargets();
				} else if (inst.opcode == Opcode.SPARSE_SWITCH) {
					SparseSwitchDataPseudoInstruction dataInstruction = (SparseSwitchDataPseudoInstruction) region
							.getInstructionAtCodeAddress(codeAddress + dataOffset);
					
					targets = dataInstruction.getTargets();
				}
				
				for (int offset : targets ) {
					theList.add(codeAddress + offset);
				}
			}
		}
		return theList;
	}
	
	public void printDotFile(Region region) {
		Writer writer = null;
		String name = String.format("region_%03d.dot", region.id);
		try {
			File cFile = new File(name);
			writer = new FileWriter(cFile);
			
			writer.write("digraph Method {\n");
		
			boolean ranked = false;
			
			for (BasicBlock basicBlock : basicBlocks.values()) {
				String basicBlockName = String.format("BB_0x%x_%d", basicBlock.head, basicBlock.contents.size());
				
				String insts = String.format("0x%x (%d)\\l", basicBlock.head, basicBlock.contents.size());
				for (int codeAddress : basicBlock.contents) {
					Instruction inst = region.getInstructionAtCodeAddress(codeAddress);
					insts += inst.opcode.toString().toLowerCase() + "\\l";
				}
				
				
				String basicBlockLabel = String.format("label=\"%s\"", insts);
				
				if (ranked) {
					writer.write(basicBlockName + " [shape=box," + basicBlockLabel + "];\n");
				} else {
					writer.write(basicBlockName + " [shape=box," + basicBlockLabel + ",rank=\"min\"];\n");
					ranked = true;
				}
			}
			for (BasicBlock basicBlock : basicBlocks.values()) {
				String basicBlockName = String.format("BB_0x%x_%d", basicBlock.head, basicBlock.contents.size());
				for (BasicBlock successor : basicBlock.successors) {
					String successorName = String.format("BB_0x%x_%d", successor.head, successor.contents.size());
					writer.write(basicBlockName + " -> " + successorName + ";\n");
				}
			}
			writer.write("}");
			
			writer.close();
		} catch (IOException e) {
			System.err.println("Couldn't open dot file for writing!");
			return;
		}
	}
	
}
