package eu.whrl.aottracegen;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.LiteralInstruction;
import org.jf.dexlib.Code.OdexedFieldAccess;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;

public class BytecodeToCConverter {

	public String convert(CodeGenContext context, int codeAddress) {
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		String result = "";
		
		switch (instruction.opcode) {
		
		case CONST_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  v[%d] = %d;", vA, lit);
			break;
		}
		
		case IF_EQ:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] == v[%d]) { goto %sL0x%x; }",
					vA, vB, labelPrefix, targetAddress);
			break;
		}
		
		case IF_NE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] != v[%d]) { goto %sL0x%x; }",
					vA, vB, labelPrefix, targetAddress);
			break;
		}
		
		case IF_LT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] < v[%d]) { goto %sL0x%x; }",
					vA, vB, labelPrefix, targetAddress);
			break;
		}
		
		case IF_GE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] >= v[%d]) { goto %sL0x%x; }",
					vA, vB, labelPrefix, targetAddress);
			break;
		}
		
		case IF_GT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] > v[%d]) { goto %sL0x%x; }",
					vA, vB, labelPrefix, targetAddress);
			break;
		}
		
		case IF_LE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] <= v[%d]) { goto %sL0x%x; }",
					vA, vB, labelPrefix, targetAddress);
			break;
		}
		
		case IF_EQZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] == 0) { goto %sL0x%x; }",
					vA, labelPrefix, targetAddress);
			break;
		}
		
		case IF_NEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] != 0) { goto %sL0x%x; }",
					vA, labelPrefix, targetAddress);
			break;
		}
		
		case IF_LTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] < 0) { goto %sL0x%x; }",
					vA, labelPrefix, targetAddress);
			break;
		}
		
		case IF_GEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] >= 0) { goto %sL0x%x; }",
					vA, labelPrefix, targetAddress);
			break;
		}
		
		case IF_GTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] > 0) { goto %sL0x%x; }",
					vA, labelPrefix, targetAddress);
			break;
		}
		
		case IF_LEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			String labelPrefix = "__";
			if (!context.trace.containsCodeAddress(targetAddress)) {
				labelPrefix = "__exit_";
			}
			
			result = String.format("  if (v[%d] <= 0) { goto %sL0x%x; }",
					vA, labelPrefix, targetAddress);
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
		
		case AGET_BYTE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  char *array = (char*) v[%2$d];\n" + 
					 "  int array_size = *((int*) (array + 8));\n" +
					 "  if (array == 0) goto __exception_0x%4$x;\n" +
					 "  if (v[%3$d] >= array_size || v[3] < 0) goto __exception_0x%4$x;\n" +
					 "  char *array_contents = (char*) (array + 16);\n" +
					 "  v[%1$d] = (char) array_contents[v[%3$d]];",
					 vA, vB, vC, codeAddress);
			break;
		}
		
		case SGET_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = context.literalPoolSize;
			context.literalPoolIndices.add(new Integer(field));
			context.literalPoolOpcodes.add(instruction.opcode);
			context.literalPoolSize++;
			
			result = String.format("  int *obj = (int*) lit[%d];\n" +
					 "  v[%d] = *obj;",
					 literalPoolLoc, vA);
			break;
		}
		
		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  v[%d] = iget_quick(v[%d], 0x%x);", vA, vB, offset);
			break;
		}
		
		default:
		{
			result = "  //\n  // Not implemented!!\n  //";
			break;
		}
		
		}
		
		return result + "\n\n";
	}
}
