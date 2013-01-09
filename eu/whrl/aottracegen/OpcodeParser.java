package eu.whrl.aottracegen;

import java.util.Set;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.LiteralInstruction;
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;

public class OpcodeParser {
	public static void calculateRegisterInteraction(Instruction instruction, Set<Integer> reads, Set<Integer> writes) {
		
		switch (instruction.opcode) {
		case MOVE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			writes.add(vA);
			reads.add(vB);
			break;
		}

		case MOVE_WIDE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			writes.add(vA);
			writes.add(vA+1);
			reads.add(vB);
			reads.add(vB+1);
			break;
		}

		case MOVE_RESULT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			writes.add(vA);
			break;
		}

		case RETURN:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			reads.add(vA);
			break;
		}

		case RETURN_VOID:
		{		
			break;
		}

		case RETURN_WIDE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			reads.add(vA);
			reads.add(vA+1);
			break;
		}

		case RETURN_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			reads.add(vA);
			break;
		}

		case CONST_4:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			writes.add(vA);
			break;
		}

		case CONST_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			writes.add(vA);
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

			writes.add(vA);
			writes.add(vA+1);
			break;
		}

		case GOTO:
		{
			break;
		}

		case GOTO_16:
		{
			break;
		}

		case GOTO_32:
		{
			break;
		}

		case PACKED_SWITCH: 
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			reads.add(vA);
			break;
		}

		case CMP_LONG:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			writes.add(vA);
			reads.add(vB);
			reads.add(vB+1);
			reads.add(vC);
			reads.add(vC+1);
			break;
		}

		case IF_EQ:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			reads.add(vA);
			reads.add(vB);
			break;
		}

		case IF_NE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			reads.add(vA);
			reads.add(vB);
			break;
		}

		case IF_LT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			reads.add(vA);
			reads.add(vB);
			break;
		}

		case IF_GE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			reads.add(vA);
			reads.add(vB);
			break;
		}

		case IF_GT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			reads.add(vA);
			reads.add(vB);
			break;
		}

		case IF_LE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			reads.add(vA);
			reads.add(vB);
			break;
		}

		case IF_EQZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			reads.add(vA);
			break;
		}

		case IF_NEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			reads.add(vA);
			break;
		}

		case IF_LTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			reads.add(vA);
			break;
		}

		case IF_GEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			reads.add(vA);
			break;
		}

		case IF_GTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			reads.add(vA);
			break;
		}

		case IF_LEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			reads.add(vA);
			break;
		}

		case AGET:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			writes.add(vA);
			reads.add(vB);
			reads.add(vC);
			break;
		}

		case AGET_BYTE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			writes.add(vA);
			reads.add(vB);
			reads.add(vC);
			break;
		}

		case APUT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			reads.add(vA);
			reads.add(vB);
			reads.add(vC);
			break;
		}

		case SGET_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			writes.add(vA);
			break;
		}

		case ADD_INT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			writes.add(vA);
			reads.add(vA);
			reads.add(vB);
			break;
		}

		case ADD_LONG_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			writes.add(vA);
			writes.add(vA+1);
			reads.add(vA);
			reads.add(vA+1);
			reads.add(vB);
			reads.add(vB+1);
			break;
		}

		case ADD_INT_LIT8:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			writes.add(vA);
			reads.add(vB);
			break;
		}

		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			writes.add(vA);
			reads.add(vB);
			break;
		}

		case IGET_WIDE_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			writes.add(vA);
			writes.add(vA+1);
			reads.add(vB);
			reads.add(vB+1);
			break;
		}

		case IPUT_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			reads.add(vA);
			reads.add(vB);
			break;
		}

		case IPUT_WIDE_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			reads.add(vA);
			reads.add(vA+1);
			reads.add(vB);
			reads.add(vB+1);
			break;
		}

		case INVOKE_VIRTUAL_QUICK:
		{
			break;
		}

		default:
		{
			System.err.println("Missing a definition for OpcodeParser.java! : " + instruction.opcode.name);
		}
		
		}	
	}
}
