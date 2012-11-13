package eu.whrl.aottracegen.converters;

import java.util.Iterator;

import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.LiteralInstruction;
import org.jf.dexlib.Code.OdexedFieldAccess;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction.PackedSwitchTarget;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.LiteralPoolType;
import eu.whrl.aottracegen.Trace;
import eu.whrl.aottracegen.exceptions.UnimplementedInstructionException;

public class BytecodeToCConverter {
	
	private static final int offsetThreadRetValue = 16; 
	
	/*
	 * Return a string representing the instruction at codeAddress,
	 * as a C implementation. ALSO HAS SIDE EFFECTS OF UPDATING
	 * THE CURRENT TRACE'S METADATA.
	 */
	public String convert(CodeGenContext context, int codeAddress) throws UnimplementedInstructionException {
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		String result = "";
		
		Trace curTrace = context.getCurrentTrace();
		
		switch (instruction.opcode) {
		
		case MOVE_RESULT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  v[%d] = *((int*) (self+%d));", vA, offsetThreadRetValue);
			break;
		}
		
		case RETURN_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  *((int*) (self+%d)) = v[%d];\n", offsetThreadRetValue, vA) +
					String.format("  goto __return_L%#x;", codeAddress);
			break;
		}
		
		case CONST_4:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  v[%d] = %d;", vA, lit);
			break;
		}
		
		case CONST_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  v[%d] = %d;", vA, lit);
			break;
		}
		
		case CONST_WIDE_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			long high = 0;
			if (lit < 0) {
				high = -1;
			}
			
			result = String.format("  v[%1$d] = %3$d;\n" + 
					               "  v[%2$d] = %4$d;", vA, vA+1, high, lit);
			break;
		}
		
		case NEW_INSTANCE:
		{			
			result = String.format("  // placeholder for new-instance");
			break;
		}
		
		case GOTO:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = "  " + getGotoLabel(curTrace, targetAddress) + ";\n";
			break;
		}
		
		case GOTO_16:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = "  " + getGotoLabel(curTrace, targetAddress) + ";\n";
			break;
		}
		
		case GOTO_32:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = "  " + getGotoLabel(curTrace, targetAddress) + ";\n";
			break;
		}
		
		case PACKED_SWITCH: 
		{
			curTrace.meta.containsSwitch = true;
			
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int dataOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			
			PackedSwitchDataPseudoInstruction dataInstruction = (PackedSwitchDataPseudoInstruction) context.getInstructionAtCodeAddress(codeAddress + dataOffset);
			
			result = String.format("  switch (v[%d]) {\n", vA);
			
			Iterator<PackedSwitchTarget> targetIterator = dataInstruction.iterateKeysAndTargets();
			while (targetIterator.hasNext()) {
				PackedSwitchTarget target = targetIterator.next();
				
				int targetAddress = codeAddress + target.targetAddressOffset;
				
				result += String.format("    case %d: %s;\n", target.value, getGotoLabel(curTrace, targetAddress));
			}
			
			int fallthroughAddress = codeAddress + instruction.getSize(codeAddress);
			result += String.format("    default: %s;\n", getGotoLabel(curTrace, fallthroughAddress));
			
			result += "  }";
			break;
		}
		
		case IF_EQ:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] == v[%d]) { %s; }",
					vA, vB, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_NE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] != v[%d]) { %s; }",
					vA, vB, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_LT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] < v[%d]) { %s; }",
					vA, vB, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_GE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] >= v[%d]) { %s; }",
					vA, vB, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_GT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] > v[%d]) { %s; }",
					vA, vB, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_LE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] <= v[%d]) { %s; }",
					vA, vB, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_EQZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] == 0) { %s; }",
					vA, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_NEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] != 0) { %s; }",
					vA, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_LTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] < 0) { %s; }",
					vA, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_GEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] >= 0) { %s; }",
					vA, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_GTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] > 0) { %s; }",
					vA, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_LEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (v[%d] <= 0) { %s; }",
					vA, getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case AGET:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  {\n" +
					 "  char *array = (char*) v[%2$d];\n" + 
					 "  int array_size = *((int*) (array + 8));\n" +
					 "  if (array == 0) goto __exception_L%4$#x;\n" +
					 "  if (((unsigned int) v[%3$d]) >= array_size) goto __exception_L%4$#x;\n" +
					 "  int *array_contents = array + 16;\n" +
					 "  v[%1$d] = array_contents[v[%3$d]];\n" +
					 "  }",
					 vA, vB, vC, codeAddress);
			break;
		}
		
		case AGET_BYTE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  {\n" +
					 "  char *array = (char*) v[%2$d];\n" + 
					 "  int array_size = *((int*) (array + 8));\n" +
					 "  if (array == 0) goto __exception_L%4$#x;\n" +
					 "  if (((unsigned int) v[%3$d]) >= array_size) goto __exception_L%4$#x;\n" +
					 "  char *array_contents = (char*) (array + 16);\n" +
					 "  v[%1$d] = (char) array_contents[v[%3$d]];\n" +
					 "  }",
					 vA, vB, vC, codeAddress);
			break;
		}
		
		case APUT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  {\n" +
					 "  char *array = (char*) v[%2$d];\n" + 
					 "  int array_size = *((int*) (array + 8));\n" +
					 "  if (array == 0) goto __exception_L%4$#x;\n" +
					 "  if (v[%3$d] >= array_size || v[%3$d] < 0) goto __exception_L%4$#x;\n" +
					 "  int *array_contents = array + 16;\n" +
					 "  array_contents[v[%3$d]] = v[%1$d];\n" +
					 "  }",
					 vA, vB, vC, codeAddress);
			break;
		}
		
		case SGET_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(LiteralPoolType.STATIC_FIELD, field);
			
			result = String.format("  v[%d] = *((int*) lit[%d]);", vA, literalPoolLoc);
			break;
		}
		
		case INVOKE_DIRECT:
		{			
			result = String.format("  // placeholder for invoke-direct");
			break;
		}
		
		case INT_TO_LONG:
		{			
			result = String.format("  // placeholder for int-to-long");
			break;
		}
		
		case ADD_INT_LIT8:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			long constant = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  v[%d] = v[%d] + %d;", vA, vB, constant);
			break;
		}
		
		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  if (v[%2$d] == 0) goto __exception_L%4$#x;\n" +
		               			   "  v[%1$d] = *((int*) (((char*)v[%2$d]) + %3$#x));", vA, vB, offset, codeAddress);
			break;
		}
		
		case IPUT_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  if (v[%2$d] == 0) goto __exception_L%4$#x;\n" +
					               "  *((int*) (((char*)v[%2$d]) + %3$#x)) = v[%1$d];", vA, vB, offset, codeAddress);
			break;
		}
		
		case INVOKE_VIRTUAL_QUICK:
		{
			result = String.format("  __asm__(\"# invoke_virtual_quick_L%#x\" : : : \"r0\", \"r1\", \"r2\", \"r3\", \"r4\", \"r7\", \"r8\", \"r9\", \"r10\", \"r11\", \"r12\" );", codeAddress);
			//result = String.format("  __asm__(\"# invoke_virtual_quick_L%#x\");", codeAddress);
			/*
			int reg = ((FiveRegisterInstruction)instruction).getRegisterE();
			
			result = String.format("  if (v[%1$d] >= '0' && v[%1$d] <= '9') {\n" +
									"   *((int*) (self+%2$d)) = 1;\n" +
									"  } else {\n" + 
									"   *((int*) (self+%2$d)) = 0;\n" +
									"  }", reg, offsetThreadRetValue);
		*/
			break;
		}
		
		default:
		{
			result = "  //\n  // Not implemented!!\n  //";
			throw new UnimplementedInstructionException(instruction.opcode.name, codeAddress);
		}
		
		}
		
		return result + "\n\n";
	}
	
	public String getGotoLabel(Trace trace, int codeAddress) {
		String labelPrefix = "__";
		if (!trace.containsCodeAddress(codeAddress)) {
			labelPrefix = "__exit_";
		}
		return String.format("goto %sL%#x", labelPrefix, codeAddress);
	}
}