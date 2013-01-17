package eu.whrl.aottracegen.converters;

import java.io.IOException;
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
	
	private CodeGenContext context;
	
	public BytecodeToCConverter(CodeGenContext context) {
		this.context = context;
	}
	
	// vrs = get the Virtual Register String
	private String vrs(int reg) {
		if (context.config.avoidVirtualRegs) {
			return String.format("v%d", reg);
		}
		return String.format("v[%d]", reg);
	}
	
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
		
		case MOVE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %s = %s;", vrs(vA), vrs(vB));
			break;
		}
		
		case MOVE_WIDE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %s = %s;\n" +
			                       "  %s = %s;", vrs(vA), vrs(vB), vrs(vA+1), vrs(vB+1));
			break;
		}
		
		case MOVE_RESULT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  %s = *((int*) (self+%d));", vrs(vA), offsetThreadRetValue);
			break;
		}
		
		case RETURN:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  *((int*) (self+%d)) = %s;\n", offsetThreadRetValue, vrs(vA)) +
					String.format("  goto __return_L%#x;", codeAddress);
			break;
		}
		
		case RETURN_VOID:
		{	
			result = String.format("  goto __return_L%#x;", codeAddress);
			break;
		}
		
		case RETURN_WIDE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  *((int*) (self+%d)) = %s;\n", offsetThreadRetValue, vrs(vA)) +
					String.format("  *((int*) (self+%d)) = %s;\n", offsetThreadRetValue+4, vrs(vA+1)) +
					String.format("  goto __return_L%#x;", codeAddress);
			break;
		}
		
		case RETURN_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  *((int*) (self+%d)) = %s;\n", offsetThreadRetValue, vrs(vA)) +
					String.format("  goto __return_L%#x;", codeAddress);
			break;
		}
		
		case CONST_4:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  %s = %d;", vrs(vA), lit);
			break;
		}
		
		case CONST_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  %s = %d;", vrs(vA), lit);
			break;
		}
		
		case CONST:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  %s = %d;", vrs(vA), lit);
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
			
			result = String.format("  %1$s = %3$d;\n" + 
					               "  %2$s = %4$d;", vrs(vA), vrs(vA+1), lit, high);
			break;
		}
		
		case CONST_WIDE_HIGH16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			lit = lit << 16;
			
			long low = 0;
			
			
			result = String.format("  %1$s = %3$d;\n" + 
					               "  %2$s = %4$d;", vrs(vA), vrs(vA+1), low, lit);
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
			
			result = String.format("  switch (%s) {\n", vrs(vA));
			
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
		
		case CMP_LONG:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
			
			result = String.format(
			"  {\n" +
			"    long long value1 = *((long long*) (((char*)v) + (4 * %1$d)));\n" +
			"    long long value2 = *((long long*) (((char*)v) + (4 * %2$d)));\n" +
			"    if (value1 == value2) { %3$s = 0; }\n" +
			"    else if (value1 > value2) { %3$s = 1; }\n" +
			"    else { %3$s = -1; }\n" +
			"  }", vB, vC, vrs(vA));
					
			break;
		}
		
		case IF_EQ:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s == %s) { %s; }",
					vrs(vA), vrs(vB), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_NE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s != %s) { %s; }",
					vrs(vA), vrs(vB), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_LT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s < %s) { %s; }",
					vrs(vA), vrs(vB), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_GE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s >= %s) { %s; }",
					vrs(vA), vrs(vB), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_GT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s > %s) { %s; }",
					vrs(vA), vrs(vB), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_LE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s <= %s) { %s; }",
					vrs(vA), vrs(vB), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_EQZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s == 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_NEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s != 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_LTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s < 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_GEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s >= 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_GTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s > 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case IF_LEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s <= 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		case AGET:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  {\n" +
					 "    char *array = (char*) %2$s;\n" + 
					 "    int array_size = *((int*) (array + 8));\n" +
					 "    if (array == 0) goto __exception_L%4$#x;\n" +
					 "    if (((unsigned int) %3$s) >= array_size) goto __exception_L%4$#x;\n" +
					 "    int *array_contents = array + 16;\n" +
					 "    %1$s = array_contents[%3$s];\n" +
					 "  }",
					 vrs(vA), vrs(vB), vrs(vC), codeAddress);
			break;
		}
		
		case AGET_BYTE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  {\n" +
					 "    char *array = (char*) %2$s;\n" + 
					 "    int array_size = *((int*) (array + 8));\n" +
					 "    if (array == 0) goto __exception_L%4$#x;\n" +
					 "    if (((unsigned int) %3$s) >= array_size) goto __exception_L%4$#x;\n" +
					 "    char *array_contents = (char*) (array + 16);\n" +
					 "    %1$s = (char) array_contents[%3$s];\n" +
					 "  }",
					 vrs(vA), vrs(vB), vrs(vC), codeAddress);
			break;
		}
		
		case APUT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  {\n" +
					 "    char *array = (char*) %2$s;\n" + 
					 "    int array_size = *((int*) (array + 8));\n" +
					 "    if (array == 0) goto __exception_L%4$#x;\n" +
					 "    if (((unsigned int) %3$s) >= array_size) goto __exception_L%4$#x;\n" +
					 "    int *array_contents = array + 16;\n" +
					 "    array_contents[%3$s] = %1$s;\n" +
					 "  }",
					 vrs(vA), vrs(vB), vrs(vC), codeAddress);
			break;
		}
		
		case SGET_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(LiteralPoolType.STATIC_FIELD, field);
			
			result = String.format("  %s = *((int*) lit[%d]);", vrs(vA), literalPoolLoc);
			break;
		}
		
		case ADD_INT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %1$s = %1$s + %2$s;", vrs(vA), vrs(vB));
			break;
		}
		
		case ADD_LONG_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format(
			"  {\n" +
			"    unsigned int a = ((unsigned int) %1$s) + ((unsigned int) %3$s);\n" +
			"    unsigned int b = ((unsigned int) %2$s) + ((unsigned int) %4$s);\n" +
			"    if (a < ((unsigned int) %1$s)) { b++; }\n" +
			"    %1$s = a;\n" +
			"    %2$s = b;\n" +
			"  }", vrs(vA), vrs(vA+1), vrs(vB), vrs(vB+1));
			break;
		}
		
		case SHL_LONG_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format(" // --- TBC --- //");
			break;
		}
		
		case DIV_DOUBLE_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format(
			"  {\n" +
			"    double a = *((double*) (((char*)v) + (4 * %1$d)));\n" +
			"    double b = *((double*) (((char*)v) + (4 * %2$d)));\n" +
			"    *(((double*) (((char*)v) + (4 * %1$d)))) = a/b;\n" +
			"  }", vA, vB);
			break;
		}
		
		case ADD_INT_LIT8:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			long constant = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  %s = %s + %d;", vrs(vA), vrs(vB), constant);
			break;
		}
		
		case INT_TO_LONG:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %1$s = %3$s;\n" +
								   "  %2$s = (%3$s < 0) ? -1 : 0;", vrs(vA), vrs(vA+1), vrs(vB));
			break;
		}
		
		case INT_TO_DOUBLE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %2$s = %3$s;\n" +
			                       "  %1$s = 0;\n", vrs(vA), vrs(vA+1), vrs(vB));
			break;
		}
		
		case LONG_TO_DOUBLE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = "  {\n" + 
					String.format("  long long value = *((long long*) (((char*)v) + (4 * %2$d)));\n" +
			                      "  *(((double*) (((char*)v) + (4 * %1$d)))) = (double) value;\n", vA, vB)
			         + "  }";
			break;
		}
		
		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  if (%2$s == 0) goto __exception_L%4$#x;\n" +
		               			   "  %1$s = *((int*) (((char*)%2$s) + %3$#x));", vrs(vA), vrs(vB), offset, codeAddress);
			break;
		}
		
		case IGET_WIDE_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  if (%3$s == 0) goto __exception_L%6$#x;\n" +
		               			   "  %1$s = *((int*) (((char*)%3$s) + %4$#x));\n" +
		               			   "  %2$s = *((int*) (((char*)%3$s) + %5$#x));", vrs(vA), vrs(vA+1), vrs(vB), offset, offset+4, codeAddress);
			break;
		}
		
		case IPUT_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  if (%2$s == 0) goto __exception_L%4$#x;\n" +
					               "  *((int*) (((char*)%2$s) + %3$#x)) = %1$s;", vrs(vA), vrs(vB), offset, codeAddress);
			break;
		}
		
		case IPUT_WIDE_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  if (%3$s == 0) goto __exception_L%6$#x;\n" +
					               "  *((int*) (((char*)%3$s) + %4$#x)) = %1$s;\n" +
					               "  *((int*) (((char*)%3$s) + %5$#x)) = %2$s;", vrs(vA), vrs(vA+1), vrs(vB), offset, offset+4, codeAddress);
			break;
		}
		
		case INVOKE_VIRTUAL_QUICK:
		{
			String saveVRegsString = "";
			if (context.config.avoidVirtualRegs) {
				saveVRegsString = "SAVE_VREGS";
			}
			result = String.format("  %s __asm__(\"# invoke_virtual_quick_L%#x\" : : : \"r0\", \"r1\", \"r2\", \"r3\", \"r4\", \"r7\", \"r8\", \"r9\", \"r10\", \"r11\", \"r12\");", 
					saveVRegsString, codeAddress);
			break;
		}
		
		case INVOKE_VIRTUAL:
		{
			
			result = "  // --- TBC --- //";
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