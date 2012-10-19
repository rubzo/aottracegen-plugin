package eu.whrl.aottracegen.converters;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OdexedFieldAccess;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.exceptions.UnimplementedInstructionException;

public class BytecodeToUnsafeCConverter extends BytecodeToCConverter {
	public String convert(CodeGenContext context, int codeAddress) throws UnimplementedInstructionException {
		
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		String result = "";
		
		switch (instruction.opcode) {

		case AGET:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result = String.format("  {\n" +
					"  char *array = (char*) v[%2$d];\n" + 
					"  int *array_contents = array + 16;\n" +
					"  v[%1$d] = array_contents[v[%3$d]];\n" +
					"  }",
					vA, vB, vC);
			break;
		}

		case AGET_BYTE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result = String.format("  {\n" +
					"  char *array = (char*) v[%2$d];\n" + 
					"  char *array_contents = (char*) (array + 16);\n" +
					"  v[%1$d] = (char) array_contents[v[%3$d]];\n" +
					"  }",
					vA, vB, vC);
			break;
		}

		case APUT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result = String.format("  {\n" +
					"  char *array = (char*) v[%2$d];\n" + 
					"  int *array_contents = array + 16;\n" +
					"  array_contents[v[%3$d]] = v[%1$d];\n" +
					"  }",
					vA, vB, vC);
			break;
		}

		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();

			result = String.format("  v[%1$d] = *((int*) (((char*)v[%2$d]) + %3$#x));", vA, vB, offset);
			break;
		}

		case IPUT_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();

			result = String.format("  *((int*) (((char*)v[%2$d]) + %3$#x)) = v[%1$d];", vA, vB, offset);
			break;
		}
		
		default:
		{
			return super.convert(context, codeAddress);
		}
		
		}
		
		return result + "\n\n";
	}
}
