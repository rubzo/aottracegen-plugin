package eu.whrl.aottracegen;

import java.util.List;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Util.SparseIntArray;

/*
 * NB: A CodeGenContext only works within one method!
 */
public class CodeGenContext {
	public DexFile dexFile;
	public List<Region> regions;
	public Config config;
	
	public Region currentRegion = null;
	public int currentRegionIndex = 0;
	
	public CodeGenContext(DexFile df, Config c) {
		dexFile = df;
		regions = c.regions;
		config = c;
		
		currentRegion = regions.get(0);

		for (Region region : regions) {
			// Store all the instructions the method around this region contains.
			region.instructions = region.encodedMethod.codeItem.getInstructions();

			// Create the packed switch, sparse switch and instruction maps.
			region.packedSwitchMap = new SparseIntArray(1);
			region.sparseSwitchMap = new SparseIntArray(1);
			region.instructionMap = new SparseIntArray(region.instructions.length);
			
			int currentCodeAddress = 0;
	        for (int i=0; i<region.instructions.length; i++) {
	            Instruction instruction = region.instructions[i];
	            if (instruction.opcode == Opcode.PACKED_SWITCH) {
	            	region.packedSwitchMap.append(
	                        currentCodeAddress +
	                                ((OffsetInstruction)instruction).getTargetAddressOffset(),
	                        currentCodeAddress);
	            } else if (instruction.opcode == Opcode.SPARSE_SWITCH) {
	            	region.sparseSwitchMap.append(
	                        currentCodeAddress +
	                                ((OffsetInstruction)instruction).getTargetAddressOffset(),
	                        currentCodeAddress);
	            }
	            region.instructionMap.append(currentCodeAddress, i);
	            currentCodeAddress += instruction.getSize(currentCodeAddress);
	        }
		}
	}
	
	/*
	 * This method must be called before calling generateC() or compileC()!
	 */
	public void selectCurrentRegion(int idx) {
		currentRegion = regions.get(idx);
		currentRegionIndex = idx;
	}
	
	
}
