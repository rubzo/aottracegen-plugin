package eu.whrl.aottracegen.converters;

import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.LiteralInstruction;
import org.jf.dexlib.Code.OdexedFieldAccess;
import org.jf.dexlib.Code.OdexedInvokeVirtual;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.Trace;

public class BytecodeToPrettyConverter {
	
	/*
	 * Return a string representing the instruction at codeAddress,
	 * as a human readable representation. MUST BE CALLED BEFORE THE
	 * BYTECODE TO C CONVERTER, AS THAT HAS SIDE EFFECTS.
	 */
	public String convert(CodeGenContext context, int codeAddress) {
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		Trace curTrace = context.getCurrentTrace();
		
		String result = String.format("  // BYTECODE AT %#x: ", codeAddress);
		
		switch (instruction.opcode) {
		
		case MOVE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move v%d, v%d", vA, vB);
			break;
		}
		
		case MOVE_WIDE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move-wide v%d, v%d", vA, vB);
			break;
		}
		
		case MOVE_RESULT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("move-result v%d", vA);
			break;
		}
		
		case RETURN:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("return v%d", vA);
			break;
		}
		
		case RETURN_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("return-object v%d", vA);
			break;
		}
		
		case RETURN_VOID:
		{
			result += "return-void";
			break;
		}
		
		case CONST_4:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			result += String.format("const/4 v%d, #%d", vA, lit);
			break;
		}
		
		case CONST_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			result += String.format("const/16 v%d, #%d", vA, lit);
			break;
		}
		
		case CONST:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			result += String.format("const v%d, #%d", vA, lit);
			break;
		}
		
		case CONST_WIDE_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			result += String.format("const-wide/16 v%d, #%d", vA, lit);
			break;
		}
		
		case CONST_WIDE_HIGH16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			lit = lit << 48;
			
			result += String.format("const-wide/high16 v%d, #%d", vA, lit);
			break;
		}
		
		case NEW_INSTANCE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();

			result += String.format("new-instance v%d, type@%#x", vA, field);
			break;
		}
		
		case GOTO:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result += String.format("goto +%#x;", targetAddress);
			break;
		}
		
		case GOTO_16:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result += String.format("goto/16 +%#x;", targetAddress);
			break;
		}
		
		case GOTO_32:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result += String.format("goto/32 +%#x;", targetAddress);
			break;
		}
		
		case PACKED_SWITCH:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("packed-switch v%d", vA);
			break;
		}

		case IF_EQ:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-eq v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_NE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-ne v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_LT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-lt v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_GE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-ge v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_GT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-gt v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_LE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-le v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_EQZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-eqz v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		case IF_NEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-nez v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		case IF_LTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-ltz v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		case IF_GEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-gez v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		case IF_GTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-gtz v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		case IF_LEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-lez v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		case AGET:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aget v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}

		case AGET_BYTE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aget-byte v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		case APUT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aput v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}

		case SGET_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.literalPoolSize;
			
			result += String.format("sget-object v%d, field@%#x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		case INVOKE_DIRECT:
		{
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			String argsString = getArgsString(instruction);
			
			result += String.format("invoke-direct %s, meth@%#x", argsString, field);
			break;
		}
		
		case INT_TO_LONG:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("int-to-long v%d, v%d", vA, vB);
			break;
		}
		
		case ADD_INT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("add-int/2addr v%d, v%d", vA, vB);
			break;
		}
		
		case ADD_INT_LIT8:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			long constant = ((LiteralInstruction)instruction).getLiteral();
			
			result += String.format("add-int v%d, v%d, #%d", vA, vB, constant);
			break;
		}
		
		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();

			result += String.format("+iget-quick v%d, v%d, [obj+%d]",
					vA, vB, offset);
			break;
		}
		
		case IPUT_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();

			result += String.format("+iput-quick v%d, v%d, [obj+%d]",
					vA, vB, offset);
			break;
		}
		
		case INVOKE_VIRTUAL_QUICK:
		{
			
			int vtableIndex = ((OdexedInvokeVirtual)instruction).getVtableIndex();
			
			String argsString = getArgsString(instruction);
			
			result += String.format("+invoke-virtual-quick %s, [%#x]", argsString, vtableIndex);
			break;
		}

		default:
		{
			result += String.format("%s", instruction.opcode.name);
			System.err.println("Reminder: add case for " + instruction.opcode.name + " to BytecodeToPrettyConverter");
			break;
		}

		}

		return result + "\n";
	}
	
	private String getArgsString(Instruction instruction) {
		String argsString = "{";
		
		FiveRegisterInstruction regRef = ((FiveRegisterInstruction)instruction); 
		int regCount = regRef.getRegCount();
		
		if (regCount > 0) {
			argsString += "v" + regRef.getRegisterD();
			if (regCount != 1) {
				argsString += ", ";
			}
		}
		if (regCount > 1) {
			argsString += "v" + regRef.getRegisterE();
			if (regCount != 2) {
				argsString += ", ";
			}
		}
		if (regCount > 2) {
			argsString += "v" + regRef.getRegisterF();
			if (regCount != 3) {
				argsString += ", ";
			}
		}
		if (regCount > 3) {
			argsString += "v" + regRef.getRegisterG();
			if (regCount != 4) {
				argsString += ", ";
			}
		}
		if (regCount > 4) {
			argsString += "v" + regRef.getRegisterA();
		}

		argsString += "}";
		
		return argsString;
	}
}
