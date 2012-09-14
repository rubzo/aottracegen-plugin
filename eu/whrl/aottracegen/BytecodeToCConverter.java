package eu.whrl.aottracegen;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OdexedFieldAccess;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;

public class BytecodeToCConverter {

	public String convert(CodeGenContext context, int codeAddress) {
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		String result = "";
		
		switch (instruction.opcode) {
		
		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  v[%d] = iget_quick(v[%d], 0x%x);", vA, vB, offset);
			break;
		}
		
		case AGET:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  char *array = (char*) v[%2$d];\n" + 
					 "  int array_size = *((int*) (array + 8));\n" +
					 "  if (array == 0) goto __exception_0x%4$x;\n" +
					 "  if (v[%3$d] >= array_size || v[3] < 0) goto __exception_0x%4$x;\n" +
					 "  int *array_contents = array + 16;\n" +
					 "  v[%1$d] = array_contents[v[%3$d]];",
					 vA, vB, vC, codeAddress);
			break;
		}
		
		default:
		{
			result = "// Not implemented!!";
			break;
		}
		
		}
		
		return result + "\n\n";
	}
}
