package eu.whrl.aottracegen;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.LiteralInstruction;
import org.jf.dexlib.Code.OdexedFieldAccess;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;

public class BytecodeToStringConverter {
	public String convert(CodeGenContext context, int codeAddress) {
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		String result = String.format("  // BYTECODE AT 0x%x: ", codeAddress);
		
		switch (instruction.opcode) {
		case CONST_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			result += String.format("const/16 v%d, #%d", vA, lit);
			break;
		}

		case IF_EQ:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-eq v%d, v%d, +%x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_NE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-ne v%d, v%d, +%x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_LT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-lt v%d, v%d, +%x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_GE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-ge v%d, v%d, +%x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_GT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-gt v%d, v%d, +%x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_LE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-le v%d, v%d, +%x",
					vA, vB, targetAddressOffset);
			break;
		}

		case IF_EQZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-eqz v%d, +%x",
					vA, targetAddressOffset);
			break;
		}

		case IF_NEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-nez v%d, +%x",
					vA, targetAddressOffset);
			break;
		}

		case IF_LTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-ltz v%d, +%x",
					vA, targetAddressOffset);
			break;
		}

		case IF_GEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-gez v%d, +%x",
					vA, targetAddressOffset);
			break;
		}

		case IF_GTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-gtz v%d, +%x",
					vA, targetAddressOffset);
			break;
		}

		case IF_LEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-lez v%d, +%x",
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

		case SGET_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = context.literalPoolSize;
			
			result += String.format("sget-object v%d, field@0x%x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();

			result += String.format("+iget-quick v%d, v%d, [obj+v%d]",
					vA, vB, offset);
			break;
		}

		default:
		{
			result += String.format("%s", instruction.opcode.name);
			break;
		}

		}

		return result + "\n";
	}
}
