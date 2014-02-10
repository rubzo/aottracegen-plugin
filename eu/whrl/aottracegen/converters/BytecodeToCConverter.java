package eu.whrl.aottracegen.converters;

import java.util.Iterator;

import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.InvokeInstruction;
import org.jf.dexlib.Code.LiteralInstruction;
import org.jf.dexlib.Code.OdexedFieldAccess;
import org.jf.dexlib.Code.OdexedInvokeInline;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.RegisterRangeInstruction;
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction.PackedSwitchTarget;
import org.jf.dexlib.Code.Format.SparseSwitchDataPseudoInstruction;
import org.jf.dexlib.Code.Format.SparseSwitchDataPseudoInstruction.SparseSwitchTarget;

import eu.whrl.aottracegen.CTraceGenerator;
import eu.whrl.aottracegen.ChainingCell;
import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.LiteralPoolType;
import eu.whrl.aottracegen.Region;
import eu.whrl.aottracegen.Trace;
import eu.whrl.aottracegen.armgen.ArmRegister;
import eu.whrl.aottracegen.exceptions.UnimplementedInstructionException;

public class BytecodeToCConverter {

	private static final int offsetThreadReturn = 16;
	private static final int offsetThreadException = 68;
	private static final int offsetArrayObjectLength = 8;

	private static final int INLINE_EMPTYINLINEMETHOD = 0;
	private static final int INLINE_STRING_CHARAT = 1;
	private static final int INLINE_STRING_COMPARETO = 2;
	private static final int INLINE_STRING_EQUALS = 3;
	private static final int INLINE_STRING_FASTINDEXOF_II = 4;
	private static final int INLINE_STRING_IS_EMPTY = 5;
	private static final int INLINE_STRING_LENGTH = 6;
	private static final int INLINE_MATH_ABS_INT = 7;
	private static final int INLINE_MATH_ABS_LONG = 8;
	private static final int INLINE_MATH_ABS_FLOAT = 9;
	private static final int INLINE_MATH_ABS_DOUBLE = 10;
	private static final int INLINE_MATH_MIN_INT = 11;
	private static final int INLINE_MATH_MAX_INT = 12;
	private static final int INLINE_MATH_SQRT = 13;
	private static final int INLINE_MATH_COS = 14;
	private static final int INLINE_MATH_SIN = 15;
	private static final int INLINE_FLOAT_TO_INT_BITS = 16;
	private static final int INLINE_FLOAT_TO_RAW_INT_BITS = 17;
	private static final int INLINE_INT_BITS_TO_FLOAT = 18;
	private static final int INLINE_DOUBLE_TO_LONG_BITS = 19;
	private static final int INLINE_DOUBLE_TO_RAW_LONG_BITS = 20;
	private static final int INLINE_LONG_BITS_TO_DOUBLE = 21;
	private static final int INLINE_STRICT_MATH_ABS_INT = 22;
	private static final int INLINE_STRICT_MATH_ABS_LONG = 23;
	private static final int INLINE_STRICT_MATH_ABS_FLOAT = 24;
	private static final int INLINE_STRICT_MATH_ABS_DOUBLE = 25;
	private static final int INLINE_STRICT_MATH_MIN_INT = 26;
	private static final int INLINE_STRICT_MATH_MAX_INT = 27;
	private static final int INLINE_STRICT_MATH_SQRT = 28;

	/*
	 * Return a string representing the instruction at codeAddress, as a C
	 * implementation. ALSO HAS SIDE EFFECTS OF UPDATING THE CURRENT TRACE'S
	 * METADATA.
	 */
	public String convert(CodeGenContext context, int codeAddress)
			throws UnimplementedInstructionException {
		Instruction instruction = context.currentRegion
				.getInstructionAtCodeAddress(codeAddress);

		String result = "";

		Trace curTrace = context.currentRegion.trace;

		// Potentially print vregs?
		if (context.config.printVregsMode
				&& !CTraceGenerator.opcodesThatCanReturn
						.contains(instruction.opcode)
				&& !CTraceGenerator.opcodesThatCanBranch
						.contains(instruction.opcode)) {
			result += emitPrintVregs(codeAddress) + "\n";
		}
		
		// potentially do trail mode
		if (context.config.trailMode) {
			result += emitTrail(codeAddress) + "\n";
		}

		// Potentially use single stepping?
		if (context.currentRegion.singleStepOnly
				&& !CTraceGenerator.opcodesThatCanReturn
						.contains(instruction.opcode)
				&& !CTraceGenerator.opcodesThatCanBranch
						.contains(instruction.opcode)) {
			result += emitSingleStep(codeAddress, curTrace, instruction);
			return result;
		}

		switch (instruction.opcode) {

		// opcode: 00 nop
		case NOP: {
			result += "  // NOP";
			break;
		}

		// opcode: 01 move
		case MOVE: {
			result += emitMove(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 02 move/from16
		case MOVE_FROM16: {
			result += emitMove(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 03 move/16
		case MOVE_16: {
			result += emitMove(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 04 move-wide
		case MOVE_WIDE: {
			result += emitMoveWide(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 05 move-wide/from16
		case MOVE_WIDE_FROM16: {
			result += emitMoveWide(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 06 move-wide/16
		case MOVE_WIDE_16: {
			result += emitMoveWide(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 07 move-object
		case MOVE_OBJECT: {
			result += emitMove(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 08 move-object/from16
		case MOVE_OBJECT_FROM16: {
			result += emitMove(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 09 move-object/16
		case MOVE_OBJECT_16: {
			result += emitMove(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 0a move-result
		case MOVE_RESULT: {
			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();

			result += String.format("  v[%d] = *((int*) (self+%d));", vA,
					offsetThreadReturn);
			break;
		}

		// opcode: 0b move-result-wide
		case MOVE_RESULT_WIDE: {
			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();

			result += String.format(
					"  *((long long*)(v + %d)) = *((long long*) (self+%d));",
					vA, offsetThreadReturn);
			break;
		}

		// opcode: 0c move-result-object
		case MOVE_RESULT_OBJECT: {
			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();

			result += String.format("  v[%d] = *((int*) (self+%d));", vA,
					offsetThreadReturn);
			break;
		}

		// opcode: 0d move-exception
		case MOVE_EXCEPTION: {
			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();

			result += String.format("  v[%d] = *((int*) (self+%d));", vA,
					offsetThreadException);
			break;
		}

		// opcode: 0e return-void
		case RETURN_VOID: {
			curTrace.meta.containsReturn = true;

			result += String.format("  TRACE_RETURN(%#x)", codeAddress);
			break;
		}

		// opcode: 0f return
		case RETURN: {
			curTrace.meta.containsReturn = true;

			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();

			result += String.format("  *((int*) (self+%d)) = v[%d];\n",
					offsetThreadReturn, vA)
					+ String.format("  TRACE_RETURN(%#x)", codeAddress);
			break;
		}

		// opcode: 10 return-wide
		case RETURN_WIDE: {
			curTrace.meta.containsReturn = true;

			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();

			result += String.format(
					"  *((long long*) (self+%d)) = *((long long*)(v + %d));",
					offsetThreadReturn, vA)
					+ String.format("  TRACE_RETURN(%#x)", codeAddress);
			break;
		}

		// opcode: 11 return-object
		case RETURN_OBJECT: {
			curTrace.meta.containsReturn = true;

			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();

			result += String.format("  *((int*) (self+%d)) = v[%d];\n",
					offsetThreadReturn, vA)
					+ String.format("  TRACE_RETURN(%#x)", codeAddress);
			break;
		}

		// opcode: 12 const/4
		case CONST_4: {
			result += emitConst(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 13 const/16
		case CONST_16: {
			result += emitConst(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 14 const
		case CONST: {
			result += emitConst(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 15 const/high16
		case CONST_HIGH16: {
			result += emitConstHigh(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 16 const-wide/16
		case CONST_WIDE_16: {
			result += emitConstWide(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 17 const-wide/32
		case CONST_WIDE_32: {
			result += emitConstWide(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 18 const-wide
		case CONST_WIDE: {
			result += emitConstWide(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 19 const-wide/high16
		case CONST_WIDE_HIGH16: {
			result += emitConstWideHigh(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 1a const-string
		case CONST_STRING: {
			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
			int stringIndex = ((InstructionWithReference) instruction)
					.getReferencedItem().getIndex();

			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
					LiteralPoolType.STRING_POINTER, stringIndex);

			result += String.format("  v[%d] = lit[%d];", vA, literalPoolLoc);
			break;
		}

		// opcode: 1b const-string/jumbo
		case CONST_STRING_JUMBO: {
			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
			int stringIndex = ((InstructionWithReference) instruction)
					.getReferencedItem().getIndex();

			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
					LiteralPoolType.STRING_POINTER, stringIndex);

			result += String.format("  v[%d] = lit[%d];", vA, literalPoolLoc);
			break;
		}

		// opcode: 1c const-class
		case CONST_CLASS: {
			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
			int classIndex = ((InstructionWithReference) instruction)
					.getReferencedItem().getIndex();

			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
					LiteralPoolType.CLASS_POINTER, classIndex);

			result += String.format("  v[%d] = lit[%d];", vA, literalPoolLoc);
			break;
		}

		// opcode: 1d monitor-enter
		case MONITOR_ENTER: {
			result += emitLeaveRegion(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 1e monitor-exit
		case MONITOR_EXIT: {
			result += emitLeaveRegion(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 1f check-cast
		case CHECK_CAST: {
			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();

			int classIndex = ((InstructionWithReference) instruction)
					.getReferencedItem().getIndex();

			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
					LiteralPoolType.CLASS_POINTER, classIndex);

			result += "  {\n";
			result += String
					.format("    if (!instanceof_%2$#x(v[%1$d], lit[%3$d], lit)) TRACE_EXCEPTION(%2$#x);\n",
							vA, codeAddress, literalPoolLoc);
			result += "  }";

			break;
		}

		// opcode: 20 instance-of
		case INSTANCE_OF: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

			int classIndex = ((InstructionWithReference) instruction)
					.getReferencedItem().getIndex();

			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
					LiteralPoolType.CLASS_POINTER, classIndex);

			result += "  {\n";
			result += String.format(
					"    if (v[%d] == 0) TRACE_EXCEPTION(%#x);\n", vB,
					codeAddress);
			result += String.format(
					"    v[%d] = instanceof_%#x(v[%d], lit[%d], lit);\n", vA,
					codeAddress, vB, literalPoolLoc);
			result += "  }";

			break;
		}
		// opcode: 21 array-length
		case ARRAY_LENGTH: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

			result += "  {\n";
			result += String.format(
					"    if (v[%d] == 0) TRACE_EXCEPTION(%#x);\n", vB,
					codeAddress);
			result += String.format(
					"    v[%d] = *((int*) ( ((char*) v[%d]) + %d ));", vA, vB,
					offsetArrayObjectLength);
			result += "  }";
			break;
		}

		// opcode: 22 new-instance
		case NEW_INSTANCE: {
			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
			int classIndex = ((InstructionWithReference) instruction)
					.getReferencedItem().getIndex();

			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
					LiteralPoolType.CLASS_POINTER, classIndex);

			result += "  {\n";
			result += String
					.format("    v[%d] = new_instance_%#x(lit[%d], 1 /*ALLOC_DONT_TRACK*/, lit);\n",
							vA, codeAddress, literalPoolLoc);
			result += String.format(
					"    if (v[%d] == 0) TRACE_EXCEPTION(%#x);\n", vA,
					codeAddress);
			result += "  }";
			break;
		}

		// opcode: 23 new-array
		case NEW_ARRAY: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();
			int classIndex = ((InstructionWithReference) instruction)
					.getReferencedItem().getIndex();

			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
					LiteralPoolType.CLASS_POINTER, classIndex);

			result += "  {\n";
			result += String.format(
					"    if (v[%d] < 0) TRACE_EXCEPTION(%#x);\n", vB,
					codeAddress);
			result += String
					.format("    v[%d] = new_array(lit[%d], v[%d], 1 /*ALLOC_DONT_TRACK*/, lit);\n",
							vA, literalPoolLoc, vB);
			result += String.format(
					"    if (v[%d] == 0) TRACE_EXCEPTION(%#x);\n", vA,
					codeAddress);
			result += "  }";
			break;
		}

		// opcode: 24 filled-new-array
		case FILLED_NEW_ARRAY: {
			int classIndex = ((InstructionWithReference) instruction)
					.getReferencedItem().getIndex();

			String arrayType = ((InstructionWithReference) instruction)
					.getReferencedItem().getConciseIdentity().substring(15, 16);

			if (!arrayType.equals("I")) {
				System.out
						.println("Can only handle FILLED_NEW_ARRAY with [I right now. Fix.");
				System.exit(1);
			}

			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
					LiteralPoolType.CLASS_POINTER, classIndex);

			int length = ((FiveRegisterInstruction) instruction).getRegCount();

			result += "  {\n";
			result += String
					.format("    int *array_obj = new_array(lit[%d], %d, 1 /*ALLOC_DONT_TRACK*/, lit);\n",
							literalPoolLoc, length);
			result += String.format(
					"    if (array_obj == 0) TRACE_EXCEPTION(%#x);\n",
					codeAddress);
			result += String.format("    *((int*) (self+%d)) = array_obj;\n",
					offsetThreadReturn);
			if (length > 0) {
				result += String.format(
						"    *((int*)(((char*)array_obj) + 16)) = v[%d];\n",
						((FiveRegisterInstruction) instruction).getRegisterD());
			}
			if (length > 1) {
				result += String.format(
						"    *((int*)(((char*)array_obj) + 20)) = v[%d];\n",
						((FiveRegisterInstruction) instruction).getRegisterE());
			}
			if (length > 2) {
				result += String.format(
						"    *((int*)(((char*)array_obj) + 24)) = v[%d];\n",
						((FiveRegisterInstruction) instruction).getRegisterF());
			}
			if (length > 3) {
				result += String.format(
						"    *((int*)(((char*)array_obj) + 28)) = v[%d];\n",
						((FiveRegisterInstruction) instruction).getRegisterG());
			}
			if (length > 4) {
				result += String.format(
						"    *((int*)(((char*)array_obj) + 32)) = v[%d];\n",
						((FiveRegisterInstruction) instruction).getRegisterA());
			}
			// no need to mark card table if it's just ints
			result += "  }";
			break;
		}

		// opcode: 25 filled-new-array/range
		case FILLED_NEW_ARRAY_RANGE: {
			result += emitSingleStep(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 26 fill-array-data
		// opcode: 27 throw
		case THROW: {
			result += String.format("    TRACE_EXCEPTION(%#x);\n", codeAddress);
			break;
		}

		// opcode: 28 goto
		case GOTO: {
			result += emitGoto(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 29 goto/16
		case GOTO_16: {
			result += emitGoto(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 2a goto/32
		case GOTO_32: {
			result += emitGoto(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: 2b packed-switch
		case PACKED_SWITCH: {
			curTrace.meta.containsSwitch = true;

			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
			int dataOffset = ((OffsetInstruction) instruction)
					.getTargetAddressOffset();

			PackedSwitchDataPseudoInstruction dataInstruction = (PackedSwitchDataPseudoInstruction) context.currentRegion
					.getInstructionAtCodeAddress(codeAddress + dataOffset);

			result += String.format("  switch (v[%d]) {\n", vA);

			Iterator<PackedSwitchTarget> targetIterator = dataInstruction
					.iterateKeysAndTargets();
			while (targetIterator.hasNext()) {
				PackedSwitchTarget target = targetIterator.next();

				int targetAddress = codeAddress + target.targetAddressOffset;

				result += String.format("    case %d: %s;\n", target.value,
						getGotoLabel(curTrace, targetAddress));
			}

			int fallthroughAddress = codeAddress
					+ instruction.getSize(codeAddress);
			result += String.format("    default: %s;\n",
					getGotoLabel(curTrace, fallthroughAddress));

			result += "  }";
			break;
		}

		// opcode: 2c sparse-switch
		case SPARSE_SWITCH: {
			curTrace.meta.containsSwitch = true;

			int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
			int dataOffset = ((OffsetInstruction) instruction)
					.getTargetAddressOffset();

			SparseSwitchDataPseudoInstruction dataInstruction = (SparseSwitchDataPseudoInstruction) context.currentRegion
					.getInstructionAtCodeAddress(codeAddress + dataOffset);

			result += String.format("  switch (v[%d]) {\n", vA);

			Iterator<SparseSwitchTarget> targetIterator = dataInstruction
					.iterateKeysAndTargets();
			while (targetIterator.hasNext()) {
				SparseSwitchTarget target = targetIterator.next();

				int targetAddress = codeAddress + target.targetAddressOffset;

				result += String.format("    case %d: %s;\n", target.key,
						getGotoLabel(curTrace, targetAddress));
			}

			int fallthroughAddress = codeAddress
					+ instruction.getSize(codeAddress);
			result += String.format("    default: %s;\n",
					getGotoLabel(curTrace, fallthroughAddress));

			result += "  }";
			break;
		}

		// opcode: 2d cmpl-float
		case CMPL_FLOAT: {
			int vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction) instruction).getRegisterC();

			result += String.format("  {\n"
					+ "    float value1 = *((float*) (v + %1$d));\n"
					+ "    float value2 = *((float*) (v + %2$d));\n"
					+ "    if (value1 == value2) { v[%3$d] = 0; }\n"
					+ "    else if (value1 > value2) { v[%3$d] = 1; }\n"
					+ "    else { v[%3$d] = -1; }\n" + "  }", vB, vC, vA);

			break;
		}

		// opcode: 2e cmpg-float
		case CMPG_FLOAT: {
			int vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction) instruction).getRegisterC();

			result += String.format("  {\n"
					+ "    float value1 = *((float*) (v + %1$d));\n"
					+ "    float value2 = *((float*) (v + %2$d));\n"
					+ "    if (value1 == value2) { v[%3$d] = 0; }\n"
					+ "    else if (value1 < value2) { v[%3$d] = -1; }\n"
					+ "    else { v[%3$d] = 1; }\n" + "  }", vB, vC, vA);

			break;
		}

		// opcode: 2f cmpl-double
		case CMPL_DOUBLE: {
			int vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction) instruction).getRegisterC();

			result += String.format("  {\n"
					+ "    double value1 = *((double*) (v + %1$d));\n"
					+ "    double value2 = *((double*) (v + %2$d));\n"
					+ "    if (value1 == value2) { v[%3$d] = 0; }\n"
					+ "    else if (value1 > value2) { v[%3$d] = 1; }\n"
					+ "    else { v[%3$d] = -1; }\n" + "  }", vB, vC, vA);

			break;
		}

		// opcode: 30 cmpg-double
		case CMPG_DOUBLE: {
			int vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction) instruction).getRegisterC();

			result += String.format("  {\n"
					+ "    double value1 = *((double*) (v + %1$d));\n"
					+ "    double value2 = *((double*) (v + %2$d));\n"
					+ "    if (value1 == value2) { v[%3$d] = 0; }\n"
					+ "    else if (value1 < value2) { v[%3$d] = -1; }\n"
					+ "    else { v[%3$d] = 1; }\n" + "  }", vB, vC, vA);

			break;
		}

		// opcode: 31 cmp-long
		case CMP_LONG: {
			int vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction) instruction).getRegisterC();

			result += String.format("  {\n"
					+ "    long long value1 = *((long long*) (v + %1$d));\n"
					+ "    long long value2 = *((long long*) (v + %2$d));\n"
					+ "    if (value1 == value2) { v[%3$d] = 0; }\n"
					+ "    else if (value1 > value2) { v[%3$d] = 1; }\n"
					+ "    else { v[%3$d] = -1; }\n" + "  }", vB, vC, vA);

			break;
		}

		// opcode: 32 if-eq
		case IF_EQ: {
			result += emitIf(codeAddress, curTrace, instruction, "==");
			break;
		}

		// opcode: 33 if-ne
		case IF_NE: {
			result += emitIf(codeAddress, curTrace, instruction, "!=");
			break;
		}

		// opcode: 34 if-lt
		case IF_LT: {
			result += emitIf(codeAddress, curTrace, instruction, "<");
			break;
		}

		// opcode: 35 if-ge
		case IF_GE: {
			result += emitIf(codeAddress, curTrace, instruction, ">=");
			break;
		}

		// opcode: 36 if-gt
		case IF_GT: {
			result += emitIf(codeAddress, curTrace, instruction, ">");
			break;
		}

		// opcode: 37 if-le
		case IF_LE: {
			result += emitIf(codeAddress, curTrace, instruction, "<=");
			break;
		}

		// opcode: 38 if-eqz
		case IF_EQZ: {
			result += emitIfZero(codeAddress, curTrace, instruction, "==");
			break;
		}

		// opcode: 39 if-nez
		case IF_NEZ: {
			result += emitIfZero(codeAddress, curTrace, instruction, "!=");
			break;
		}

		// opcode: 3a if-ltz
		case IF_LTZ: {
			result += emitIfZero(codeAddress, curTrace, instruction, "<");
			break;
		}

		// opcode: 3b if-gez
		case IF_GEZ: {
			result += emitIfZero(codeAddress, curTrace, instruction, ">=");
			break;
		}

		// opcode: 3c if-gtz
		case IF_GTZ: {
			result += emitIfZero(codeAddress, curTrace, instruction, ">");
			break;
		}

		// opcode: 3d if-lez
		case IF_LEZ: {
			result += emitIfZero(codeAddress, curTrace, instruction, "<=");
			break;
		}

		// opcode: 44 aget
		case AGET: {
			result += emitArrayGet(codeAddress, curTrace, instruction, "int", 4);
			break;
		}

		// opcode: 45 aget-wide
		case AGET_WIDE: {
			result += emitArrayGet(codeAddress, curTrace, instruction,
					"long long", 8);
			break;
		}

		// opcode: 46 aget-object
		case AGET_OBJECT: {
			result += emitArrayGet(codeAddress, curTrace, instruction, "int", 4);
			break;
		}

		// opcode: 47 aget-boolean
		case AGET_BOOLEAN: {
			result += emitArrayGet(codeAddress, curTrace, instruction, "char",
					1);
			break;
		}

		// opcode: 48 aget-byte
		case AGET_BYTE: {
			result += emitArrayGet(codeAddress, curTrace, instruction, "char",
					1);
			break;
		}

		// opcode: 49 aget-char
		case AGET_CHAR: {
			result += emitArrayGet(codeAddress, curTrace, instruction, "char",
					1);
			break;
		}

		// opcode: 4a aget-short
		case AGET_SHORT: {
			result += emitArrayGet(codeAddress, curTrace, instruction, "short",
					2);
			break;
		}

		// opcode: 4b aput
		case APUT: {
			result += emitArrayPut(codeAddress, curTrace, instruction, "int", 4);
			break;
		}

		// opcode: 4c aput-wide
		case APUT_WIDE: {
			result += emitArrayPut(codeAddress, curTrace, instruction,
					"long long", 8);
			break;
		}

		// opcode: 4d aput-object
		case APUT_OBJECT: {
			result += emitArrayPut(codeAddress, curTrace, instruction, "int", 4);
			break;
		}

		// opcode: 4e aput-boolean
		case APUT_BOOLEAN: {
			result += emitArrayPut(codeAddress, curTrace, instruction, "char",
					1);
			break;
		}

		// opcode: 4f aput-byte
		case APUT_BYTE: {
			result += emitArrayPut(codeAddress, curTrace, instruction, "char",
					1);
			break;
		}

		// opcode: 50 aput-char
		case APUT_CHAR: {
			result += emitArrayPut(codeAddress, curTrace, instruction, "char",
					1);
			break;
		}

		// opcode: 51 aput-short
		case APUT_SHORT: {
			result += emitArrayPut(codeAddress, curTrace, instruction, "short",
					2);
			break;
		}

		// opcode: 52 iget
		// opcode: 53 iget-wide
		// opcode: 54 iget-object
		// opcode: 55 iget-boolean
		// opcode: 56 iget-byte
		// opcode: 57 iget-char
		// opcode: 58 iget-short
		// opcode: 59 iput
		// opcode: 5a iput-wide
		// opcode: 5b iput-object
		// opcode: 5c iput-boolean
		// opcode: 5d iput-byte
		// opcode: 5e iput-char
		// opcode: 5f iput-short
		// opcode: 60 sget
		case SGET: {
			result += emitStaticGet(codeAddress, curTrace, instruction, "int");
			break;
		}

		// opcode: 61 sget-wide
		case SGET_WIDE: {
			result += emitStaticGet(codeAddress, curTrace, instruction,
					"long long");
			break;
		}

		// opcode: 62 sget-object
		case SGET_OBJECT: {
			result += emitStaticGet(codeAddress, curTrace, instruction, "int");
			break;
		}

		// opcode: 63 sget-boolean
		case SGET_BOOLEAN: {
			result += emitStaticGet(codeAddress, curTrace, instruction, "char");
			break;
		}

		// opcode: 64 sget-byte
		case SGET_BYTE: {
			result += emitStaticGet(codeAddress, curTrace, instruction, "char");
			break;
		}

		// opcode: 65 sget-char
		case SGET_CHAR: {
			result += emitStaticGet(codeAddress, curTrace, instruction, "char");
			break;
		}

		// opcode: 66 sget-short
		case SGET_SHORT: {
			result += emitStaticGet(codeAddress, curTrace, instruction, "short");
			break;
		}

		// opcode: 67 sput
		case SPUT: {
			result += emitStaticPut(codeAddress, curTrace, instruction, "int");
			break;
		}

		// opcode: 68 sput-wide
		case SPUT_WIDE: {
			result += emitStaticPut(codeAddress, curTrace, instruction,
					"long long");
			break;
		}

		// opcode: 69 sput-object
		case SPUT_OBJECT: {
			result += emitStaticPut(codeAddress, curTrace, instruction, "int");
			break;
		}

		// opcode: 6a sput-boolean
		case SPUT_BOOLEAN: {
			result += emitStaticPut(codeAddress, curTrace, instruction, "char");
			break;
		}

		// opcode: 6b sput-byte
		case SPUT_BYTE: {
			result += emitStaticPut(codeAddress, curTrace, instruction, "char");
			break;
		}

		// opcode: 6c sput-char
		case SPUT_CHAR: {
			result += emitStaticPut(codeAddress, curTrace, instruction, "char");
			break;
		}

		// opcode: 6d sput-short
		case SPUT_SHORT: {
			result += emitStaticPut(codeAddress, curTrace, instruction, "short");
			break;
		}

		// opcode: 6e invoke-virtual
		// opcode: 6f invoke-super
		// opcode: 70 invoke-direct
		case INVOKE_DIRECT: {
			result += emitInvokeSingleton(codeAddress, true);
			break;
		}

		// opcode: 71 invoke-static
		case INVOKE_STATIC: {
			result += emitInvokeSingleton(codeAddress, false);
			break;
		}

		// opcode: 72 invoke-interface
		case INVOKE_INTERFACE: {
			result += emitInvokeInterface(codeAddress);
			break;
		}

		// opcode: 74 invoke-virtual/range
		// opcode: 75 invoke-super/range
		// opcode: 76 invoke-direct/range
		case INVOKE_DIRECT_RANGE: {
			result += emitInvokeSingleton(codeAddress, true);
			break;
		}

		// opcode: 77 invoke-static/range
		case INVOKE_STATIC_RANGE: {
			result += emitInvokeSingleton(codeAddress, false);
			break;
		}

		// opcode: 78 invoke-interface/range
		case INVOKE_INTERFACE_RANGE: {
			result += emitInvokeInterface(codeAddress);
			break;
		}

		// opcode: 7b neg-int
		case NEG_INT: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

			result += String.format("  v[%d] = -v[%d];", vA, vB);
			break;
		}

		// opcode: 7c not-int
		case NOT_INT: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

			result += String.format("  v[%d] = ~v[%d];", vA, vB);
			break;
		}

		// opcode: 7d neg-long
		case NEG_LONG: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

			result += String.format("  {\n"
					+ "    long long *write = (long long*) (v + %d);\n"
					+ "    long long *read = (long long*) (v + %d);\n"
					+ "    *write = -(*read);\n" + "  }", vA, vB);
			break;
		}

		// opcode: 7e not-long
		case NOT_LONG: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

			result += String.format("  {\n"
					+ "    long long *write = (long long*) (v + %d);\n"
					+ "    long long *read = (long long*) (v + %d);\n"
					+ "    *write = ~(*read);\n" + "  }", vA, vB);
			break;
		}

		// opcode: 7f neg-float
		case NEG_FLOAT: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

			result += String.format("  {\n"
					+ "    float *write = (float*) (v + %d);\n"
					+ "    float *read = (float*) (v + %d);\n"
					+ "    *write = -(*read);\n" + "  }", vA, vB);
			break;
		}

		// opcode: 80 neg-double
		case NEG_DOUBLE: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

			result += String.format("  {\n"
					+ "    double *write = (double*) (v + %d);\n"
					+ "    double *read = (double*) (v + %d);\n"
					+ "    *write = -(*read);\n" + "  }", vA, vB);
			break;
		}

		// opcode: 81 int-to-long
		case INT_TO_LONG: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"int", "long long");
			break;
		}

		// opcode: 82 int-to-float
		case INT_TO_FLOAT: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"int", "float");
			break;
		}

		// opcode: 83 int-to-double
		case INT_TO_DOUBLE: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"int", "double");
			break;
		}

		// opcode: 84 long-to-int
		case LONG_TO_INT: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"long long", "int");
			break;
		}

		// opcode: 85 long-to-float
		case LONG_TO_FLOAT: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"long long", "float");
			break;
		}

		// opcode: 86 long-to-double
		case LONG_TO_DOUBLE: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"long long", "double");
			break;
		}

		// opcode: 87 float-to-int
		case FLOAT_TO_INT: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"float", "int");
			break;
		}

		// opcode: 88 float-to-long
		case FLOAT_TO_LONG: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"float", "long long");
			break;
		}

		// opcode: 89 float-to-double
		case FLOAT_TO_DOUBLE: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"float", "double");
			break;
		}

		// opcode: 8a double-to-int
		case DOUBLE_TO_INT: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"double", "int");
			break;
		}

		// opcode: 8b double-to-long
		case DOUBLE_TO_LONG: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"double", "long long");
			break;
		}

		// opcode: 8c double-to-float
		case DOUBLE_TO_FLOAT: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"double", "float");
			break;
		}

		// opcode: 8d int-to-byte
		case INT_TO_BYTE: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"int", "char");
			break;
		}

		// opcode: 8e int-to-char
		case INT_TO_CHAR: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"int", "char");
			break;
		}

		// opcode: 8f int-to-short
		case INT_TO_SHORT: {
			result += emitTypeConversion(codeAddress, curTrace, instruction,
					"int", "short");
			break;
		}

		// opcode: 90 add-int
		case ADD_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, "+");
			break;
		}

		// opcode: 91 sub-int
		case SUB_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, "-");
			break;
		}

		// opcode: 92 mul-int
		case MUL_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, "*");
			break;
		}

		// opcode: 93 div-int
		case DIV_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, "*");
			break;
		}

		// opcode: 94 rem-int
		case REM_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, "%");
			break;
		}

		// opcode: 95 and-int
		case AND_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, "&");
			break;
		}

		// opcode: 96 or-int
		case OR_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, "|");
			break;
		}

		// opcode: 97 xor-int
		case XOR_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, "^");
			break;
		}

		// opcode: 98 shl-int
		case SHL_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, "<<");
			break;
		}

		// opcode: 99 shr-int
		case SHR_INT: {
			result += emitIntArith(codeAddress, curTrace, instruction, ">>");
			break;
		}

		// opcode: 9a ushr-int
		case USHR_INT: {
			result += emitIntArithUnsigned(codeAddress, curTrace, instruction,
					">>");
			break;
		}

		// opcode: 9b add-long
		case ADD_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, "+");
			break;
		}

		// opcode: 9c sub-long
		case SUB_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, "-");
			break;
		}

		// opcode: 9d mul-long
		case MUL_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, "*");
			break;
		}

		// opcode: 9e div-long
		case DIV_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, "/");
			break;
		}

		// opcode: 9f rem-long
		case REM_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, "%");
			break;
		}

		// opcode: a0 and-long
		case AND_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, "&");
			break;
		}

		// opcode: a1 or-long
		case OR_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, "|");
			break;
		}

		// opcode: a2 xor-long
		case XOR_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, "^");
			break;
		}

		// opcode: a3 shl-long
		case SHL_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, "<<");
			break;
		}

		// opcode: a4 shr-long
		case SHR_LONG: {
			result += emitLongArith(codeAddress, curTrace, instruction, ">>");
			break;
		}

		// opcode: a5 ushr-long
		case USHR_LONG: {
			result += emitLongArithUnsigned(codeAddress, curTrace, instruction,
					">>");
			break;
		}

		// opcode: a6 add-float
		case ADD_FLOAT: {
			result += emitFloatArith(codeAddress, curTrace, instruction, "+");
			break;
		}

		// opcode: a7 sub-float
		case SUB_FLOAT: {
			result += emitFloatArith(codeAddress, curTrace, instruction, "-");
			break;
		}

		// opcode: a8 mul-float
		case MUL_FLOAT: {
			result += emitFloatArith(codeAddress, curTrace, instruction, "*");
			break;
		}

		// opcode: a9 div-float
		case DIV_FLOAT: {
			result += emitFloatArith(codeAddress, curTrace, instruction, "/");
			break;
		}

		// opcode: aa rem-float
		case REM_FLOAT: {
			result += emitFloatArith(codeAddress, curTrace, instruction,
					"fmodf");
			break;
		}

		// opcode: ab add-double
		case ADD_DOUBLE: {
			result += emitDoubleArith(codeAddress, curTrace, instruction, "+");
			break;
		}

		// opcode: ac sub-double
		case SUB_DOUBLE: {
			result += emitDoubleArith(codeAddress, curTrace, instruction, "-");
			break;
		}

		// opcode: ad mul-double
		case MUL_DOUBLE: {
			result += emitDoubleArith(codeAddress, curTrace, instruction, "*");
			break;
		}

		// opcode: ae div-double
		case DIV_DOUBLE: {
			result += emitDoubleArith(codeAddress, curTrace, instruction, "/");
			break;
		}

		// opcode: af rem-double
		case REM_DOUBLE: {
			result += emitDoubleArith(codeAddress, curTrace, instruction,
					"fmod");
			break;
		}

		// opcode: b0 add-int/2addr
		case ADD_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction, "+");
			break;
		}

		// opcode: b1 sub-int/2addr
		case SUB_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction, "-");
			break;
		}

		// opcode: b2 mul-int/2addr
		case MUL_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction, "*");
			break;
		}

		// opcode: b3 div-int/2addr
		case DIV_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction, "/");
			break;
		}

		// opcode: b4 rem-int/2addr
		case REM_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction, "%");
			break;
		}

		// opcode: b5 and-int/2addr
		case AND_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction, "&");
			break;
		}

		// opcode: b6 or-int/2addr
		case OR_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction, "|");
			break;
		}

		// opcode: b7 xor-int/2addr
		case XOR_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction, "^");
			break;
		}

		// opcode: b8 shl-int/2addr
		case SHL_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction,
					"<<");
			break;
		}

		// opcode: b9 shr-int/2addr
		case SHR_INT_2ADDR: {
			result += emitIntArith2Addr(codeAddress, curTrace, instruction,
					">>");
			break;
		}

		// opcode: ba ushr-int/2addr
		case USHR_INT_2ADDR: {
			result += emitIntArithUnsigned2Addr(codeAddress, curTrace,
					instruction, ">>");
			break;
		}

		// opcode: bb add-long/2addr
		case ADD_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					"+");
			break;
		}

		// opcode: bc sub-long/2addr
		case SUB_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					"-");
			break;
		}

		// opcode: bd mul-long/2addr
		case MUL_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					"*");
			break;
		}

		// opcode: be div-long/2addr
		case DIV_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					"/");
			break;
		}

		// opcode: bf rem-long/2addr
		case REM_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					"%");
			break;
		}

		// opcode: c0 and-long/2addr
		case AND_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					"&");
			break;
		}

		// opcode: c1 or-long/2addr
		case OR_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					"|");
			break;
		}

		// opcode: c2 xor-long/2addr
		case XOR_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					"^");
			break;
		}

		// opcode: c3 shl-long/2addr
		case SHL_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					"<<");
			break;
		}

		// opcode: c4 shr-long/2addr
		case SHR_LONG_2ADDR: {
			result += emitLongArith2Addr(codeAddress, curTrace, instruction,
					">>");
			break;
		}

		// opcode: c5 ushr-long/2addr
		case USHR_LONG_2ADDR: {
			result += emitLongArithUnsigned2Addr(codeAddress, curTrace,
					instruction, ">>");
			break;
		}

		// opcode: c6 add-float/2addr
		case ADD_FLOAT_2ADDR: {
			result += emitFloatArith2Addr(codeAddress, curTrace, instruction,
					"+");
			break;
		}

		// opcode: c7 sub-float/2addr
		case SUB_FLOAT_2ADDR: {
			result += emitFloatArith2Addr(codeAddress, curTrace, instruction,
					"-");
			break;
		}

		// opcode: c8 mul-float/2addr
		case MUL_FLOAT_2ADDR: {
			result += emitFloatArith2Addr(codeAddress, curTrace, instruction,
					"*");
			break;
		}

		// opcode: c9 div-float/2addr
		case DIV_FLOAT_2ADDR: {
			result += emitFloatArith2Addr(codeAddress, curTrace, instruction,
					"/");
			break;
		}

		// opcode: ca rem-float/2addr
		case REM_FLOAT_2ADDR: {
			result += emitFloatArith2Addr(codeAddress, curTrace, instruction,
					"fmodf");
			break;
		}

		// opcode: cb add-double/2addr
		case ADD_DOUBLE_2ADDR: {
			result += emitDoubleArith2Addr(codeAddress, curTrace, instruction,
					"+");
			break;
		}

		// opcode: cc sub-double/2addr
		case SUB_DOUBLE_2ADDR: {
			result += emitDoubleArith2Addr(codeAddress, curTrace, instruction,
					"-");
			break;
		}

		// opcode: cd mul-double/2addr
		case MUL_DOUBLE_2ADDR: {
			result += emitDoubleArith2Addr(codeAddress, curTrace, instruction,
					"*");
			break;
		}

		// opcode: ce div-double/2addr
		case DIV_DOUBLE_2ADDR: {
			result += emitDoubleArith2Addr(codeAddress, curTrace, instruction,
					"/");
			break;
		}

		// opcode: cf rem-double/2addr
		case REM_DOUBLE_2ADDR: {
			result += emitDoubleArith2Addr(codeAddress, curTrace, instruction,
					"fmod");
			break;
		}

		// opcode: d0 add-int/lit16
		case ADD_INT_LIT16: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"+");
			break;
		}

		// opcode: d1 rsub-int
		case RSUB_INT: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"rsub");
			break;
		}

		// opcode: d2 mul-int/lit16
		case MUL_INT_LIT16: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"*");
			break;
		}

		// opcode: d3 div-int/lit16
		case DIV_INT_LIT16: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"/");
			break;
		}

		// opcode: d4 rem-int/lit16
		case REM_INT_LIT16: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"%");
			break;
		}

		// opcode: d5 and-int/lit16
		case AND_INT_LIT16: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"&");
			break;
		}

		// opcode: d6 or-int/lit16
		case OR_INT_LIT16: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"|");
			break;
		}

		// opcode: d7 xor-int/lit16
		case XOR_INT_LIT16: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"^");
			break;
		}

		// opcode: d8 add-int/lit8
		case ADD_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"+");
			break;
		}

		// opcode: d9 rsub-int/lit8
		case RSUB_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"rsub");
			break;
		}

		// opcode: da mul-int/lit8
		case MUL_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"*");
			break;
		}

		// opcode: db div-int/lit8
		case DIV_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"/");
			break;
		}

		// opcode: dc rem-int/lit8
		case REM_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"%");
			break;
		}

		// opcode: dd and-int/lit8
		case AND_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"&");
			break;
		}

		// opcode: de or-int/lit8
		case OR_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"|");
			break;
		}

		// opcode: df xor-int/lit8
		case XOR_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"^");
			break;
		}

		// opcode: e0 shl-int/lit8
		case SHL_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					"<<");
			break;
		}

		// opcode: e1 shr-int/lit8
		case SHR_INT_LIT8: {
			result += emitIntArithLiteral(codeAddress, curTrace, instruction,
					">>");
			break;
		}

		// opcode: e2 ushr-int/lit8
		case USHR_INT_LIT8: {
			result += emitIntArithUnsignedLiteral(codeAddress, curTrace,
					instruction, ">>");
			break;
		}

		// opcode: e3 +iget-volatile
		// opcode: e4 +iput-volatile
		// opcode: e5 +sget-volatile
		// opcode: e6 +sput-volatile
		// opcode: e7 +iget-object-volatile
		// opcode: e8 +iget-wide-volatile
		// opcode: e9 +iput-wide-volatile
		// opcode: ea +sget-wide-volatile
		// opcode: eb +sput-wide-volatile
		// opcode: ec ^breakpoint
		// opcode: ed ^throw-verification-error
		// opcode: ee +execute-inline
		case EXECUTE_INLINE: {
			result += emitExecuteInline(codeAddress, curTrace, instruction, false);

			break;
		}

		// opcode: ef +execute-inline/range
		case EXECUTE_INLINE_RANGE: {
			result += emitExecuteInline(codeAddress, curTrace, instruction, true);
			break;
		}

		// opcode: f0 +invoke-object-init/range
		case INVOKE_OBJECT_INIT_RANGE: {
			result += "  // NOP";
			break;
		}

		// opcode: f1 +return-void-barrier
		case RETURN_VOID_BARRIER: {
			curTrace.meta.containsReturn = true;

			result += "  barrier();\n";
			result += String.format("  TRACE_RETURN(%#x)", codeAddress);
			break;
		}

		// opcode: f2 +iget-quick
		case IGET_QUICK: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();
			int offset = ((OdexedFieldAccess) instruction).getFieldOffset();

			result += String
					.format("  if (v[%2$d] == 0) TRACE_EXCEPTION(%4$#x)\n"
							+ "  v[%1$d] = *((int*) (((char*)v[%2$d]) + %3$#x));",
							vA, vB, offset, codeAddress);
			break;
		}

		// opcode: f3 +iget-wide-quick
		case IGET_WIDE_QUICK: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();
			int offset = ((OdexedFieldAccess) instruction).getFieldOffset();

			result += String
					.format("  if (v[%3$d] == 0) TRACE_EXCEPTION(%6$#x)\n"
							+ "  v[%1$d] = *((int*) (((char*)v[%3$d]) + %4$#x));\n"
							+ "  v[%2$d] = *((int*) (((char*)v[%3$d]) + %5$#x));",
							vA, vA + 1, vB, offset, offset + 4, codeAddress);
			break;
		}

		// opcode: f4 +iget-object-quick
		case IGET_OBJECT_QUICK: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();
			int offset = ((OdexedFieldAccess) instruction).getFieldOffset();

			result += String
					.format("  if (v[%2$d] == 0) TRACE_EXCEPTION(%4$#x)\n"
							+ "  v[%1$d] = *((int*) (((char*)v[%2$d]) + %3$#x));",
							vA, vB, offset, codeAddress);
			break;
		}

		// opcode: f5 +iput-quick
		case IPUT_QUICK: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();
			int offset = ((OdexedFieldAccess) instruction).getFieldOffset();

			result += String
					.format("  if (v[%2$d] == 0) TRACE_EXCEPTION(%4$#x)\n"
							+ "  *((int*) (((char*)v[%2$d]) + %3$#x)) = v[%1$d];",
							vA, vB, offset, codeAddress);
			break;
		}

		// opcode: f6 +iput-wide-quick
		case IPUT_WIDE_QUICK: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();
			int offset = ((OdexedFieldAccess) instruction).getFieldOffset();

			result += String
					.format("  if (v[%3$d] == 0) TRACE_EXCEPTION(%6$#x)\n"
							+ "  *((int*) (((char*)v[%3$d]) + %4$#x)) = v[%1$d];\n"
							+ "  *((int*) (((char*)v[%3$d]) + %5$#x)) = v[%2$d];",
							vA, vA + 1, vB, offset, offset + 4, codeAddress);
			break;
		}

		// opcode: f7 +iput-object-quick
		case IPUT_OBJECT_QUICK: {
			int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction) instruction).getRegisterB();
			int offset = ((OdexedFieldAccess) instruction).getFieldOffset();

			result += String
					.format("  if (v[%2$d] == 0) TRACE_EXCEPTION(%4$#x)\n"
							+ "  *((int*) (((char*)v[%2$d]) + %3$#x)) = v[%1$d];",
							vA, vB, offset, codeAddress);
			break;
		}

		// opcode: f8 +invoke-virtual-quick
		case INVOKE_VIRTUAL_QUICK: {
			result += emitInvokeVirtualQuick(codeAddress);
			break;
		}

		// opcode: f9 +invoke-virtual-quick/range
		case INVOKE_VIRTUAL_QUICK_RANGE: {
			result += emitInvokeVirtualQuick(codeAddress);
			break;
		}

		// opcode: fa +invoke-super-quick
		case INVOKE_SUPER_QUICK: {
			result += emitSingleStep(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: fb +invoke-super-quick/range
		case INVOKE_SUPER_QUICK_RANGE: {
			result += emitSingleStep(codeAddress, curTrace, instruction);
			break;
		}

		// opcode: fc +iput-object-volatile
		// opcode: fd +sget-object-volatile
		// opcode: fe +sput-object-volatile

		default: {
			result += "  //\n  // Not implemented!!\n  //";
			throw new UnimplementedInstructionException(
					instruction.opcode.name, codeAddress);
		}

		}

		return result + "\n\n";
	}

	private String emitExecuteInline(int codeAddress, Trace curTrace, Instruction instruction, boolean range) {
		String result = "";
		int inlineIndex = ((OdexedInvokeInline) instruction)
				.getInlineIndex();

		switch (inlineIndex) {
		case INLINE_EMPTYINLINEMETHOD:
			result += "// NOP";
			break;
		case INLINE_STRING_CHARAT: {
			int vA = ((FiveRegisterInstruction) instruction).getRegisterD();
			int vB = ((FiveRegisterInstruction) instruction).getRegisterE();

			result += String
					.format("  {\n"
							+ "    // inlined: java.lang.String.charAt()\n"
							+ "    char *string = (char*) v[%1$d];\n"
							+ "    if (string == 0) TRACE_EXCEPTION(%3$#x)\n"
							+ "    int offset = *((int*) (string + 16));\n"
							+ "    int count = *((int*) (string + 20));\n"
							+ "    if (((unsigned int) v[%2$d]) >= count) TRACE_EXCEPTION(%3$#x)\n"
							+ "    char *char_array = *((char*) (string + 8));\n"
							+ "    short int *char_array_contents = (short int*) (char_array + 16 + (2 * v[%2$d]));\n"
							+ "    *((short int*)(self + %4$d)) = *char_array_contents;\n"
							+ "  }", vA, vB, codeAddress,
							offsetThreadReturn);
			break;
		}
		case INLINE_STRING_COMPARETO:
			break;
		case INLINE_STRING_EQUALS:
			break;
		case INLINE_STRING_FASTINDEXOF_II:
			break;
		case INLINE_STRING_IS_EMPTY:
			break;
		case INLINE_STRING_LENGTH: {
			int vA = ((FiveRegisterInstruction) instruction).getRegisterD();
			result += String.format("  {\n"
					+ "    // inlined: java.lang.String.length()\n"
					+ "    char *string = (char*) v[%1$d];\n"
					+ "    if (string == 0) TRACE_EXCEPTION(%2$#x)\n"
					+ "    int count = *((int*) (string + 20));\n"
					+ "    *((int*)(self + %3$d)) = count;\n" + "  }", vA,
					codeAddress, offsetThreadReturn);
			break;
		}
		case INLINE_MATH_ABS_INT: {
			int vA = ((FiveRegisterInstruction) instruction).getRegisterD();
			result += String.format("  {\n"
					+ "    // inlined: java.lang.math.abs(I)\n"
					+ "    if (v[%1$d] < 0) {\n"
					+ "      *((int*)(self + %2$d)) = -v[%1$d];\n"
					+ "    } else {\n"
					+ "      *((int*)(self + %2$d)) = v[%1$d];\n"
					+ "    }\n" + "  }", vA, offsetThreadReturn);
			break;
		}
		case INLINE_MATH_ABS_LONG:
			break;
		case INLINE_MATH_ABS_FLOAT:
			break;
		case INLINE_MATH_ABS_DOUBLE: {
			int vA = 0;
			if (range) {
				assert(((InvokeInstruction) instruction).getRegCount() == 2);
				vA = ((RegisterRangeInstruction) instruction).getStartRegister();
			} else {
				vA = ((FiveRegisterInstruction) instruction).getRegisterD();
			}
			result += String.format("  {\n"
					+ "    // inlined: java.lang.math.abs(D)\n"
					+ "    double value = *((double*)(v + %1$d));\n"
					+ "    if (value < 0.0) {\n"
					+ "      *((double*)(self + %2$d)) = -value;\n"
					+ "    } else {\n"
					+ "      *((double*)(self + %2$d)) = value;\n"
					+ "    }\n" + "  }", vA, offsetThreadReturn);
			break;
		}
		case INLINE_MATH_MIN_INT: {
			int vA = ((FiveRegisterInstruction) instruction).getRegisterD();
			int vB = ((FiveRegisterInstruction) instruction).getRegisterE();
			result += String.format("  {\n"
					+ "    // inlined: java.lang.math.min(I)\n"
					+ "    if (v[%1$d] < v[%2$d]) {\n"
					+ "      *((int*)(self + %3$d)) = v[%1$d];\n"
					+ "    } else {\n"
					+ "      *((int*)(self + %3$d)) = v[%2$d];\n"
					+ "    }\n" + "  }", vA, vB, offsetThreadReturn);
			break;
		}
		case INLINE_MATH_MAX_INT:
			break;
		case INLINE_MATH_SQRT: {
			// DEFGA
			int vA = ((FiveRegisterInstruction) instruction).getRegisterD();
			result += String
					.format("\t*((double*) (self+%d)) = __hiya_sqrt(*((double*)(v + %1$d)), lit);",
							offsetThreadReturn, vA);
			break;
		}
		case INLINE_MATH_COS: {
			int vA = 0;
			if (range) {
				assert(((InvokeInstruction) instruction).getRegCount() == 2);
				vA = ((RegisterRangeInstruction) instruction).getStartRegister();
			} else {
				vA = ((FiveRegisterInstruction) instruction).getRegisterD();
			}
			result += String
					.format("\t*((double*) (self+%1$d)) = __hiya_cos(*((double*)(v + %2$d)), lit);",
							offsetThreadReturn, vA);
			break;
		}
		case INLINE_MATH_SIN: {
			int vA = 0;
			if (range) {
				assert(((InvokeInstruction) instruction).getRegCount() == 2);
				vA = ((RegisterRangeInstruction) instruction).getStartRegister();
			} else {
				vA = ((FiveRegisterInstruction) instruction).getRegisterD();
			}
			result += String
					.format("\t*((double*) (self+%1$d)) = __hiya_sin(*((double*)(v + %2$d)), lit);",
							offsetThreadReturn, vA);
			break;
		}
		case INLINE_FLOAT_TO_INT_BITS:
			break;
		case INLINE_FLOAT_TO_RAW_INT_BITS:
			break;
		case INLINE_INT_BITS_TO_FLOAT:
			break;
		case INLINE_DOUBLE_TO_LONG_BITS:
			break;
		case INLINE_DOUBLE_TO_RAW_LONG_BITS:
			break;
		case INLINE_LONG_BITS_TO_DOUBLE:
			break;
		case INLINE_STRICT_MATH_ABS_INT:
			break;
		case INLINE_STRICT_MATH_ABS_LONG:
			break;
		case INLINE_STRICT_MATH_ABS_FLOAT:
			break;
		case INLINE_STRICT_MATH_ABS_DOUBLE:
			break;
		case INLINE_STRICT_MATH_MIN_INT:
			break;
		case INLINE_STRICT_MATH_MAX_INT:
			break;
		case INLINE_STRICT_MATH_SQRT:
			break;
		}
		return result;
	}
	
	private String emitSingleStep(int codeAddress, Trace curTrace,
			Instruction instruction) {
		System.out
				.println("Single step is now disabled. Offending instruction: "
						+ instruction.opcode.toString());
		System.exit(1);
		return String.format("  single_step_%1$#x_%2$#x(lit, v, self);",
				codeAddress, codeAddress + instruction.getSize(codeAddress));
	}
	
	private String emitLeaveRegion(int codeAddress, Trace curTrace, Instruction instruction) {
		return String.format("  // Leaving on purpose...\n  TRACE_EXCEPTION(%#x)", codeAddress);
	}

	private String emitPrintVregs(int codeAddress) {
		return String.format("  print_vregs(v, self, 0x%x, lit);", codeAddress);
	}
	
	private String emitTrail(int codeAddress) {
		return String.format("  print_trail(v, self, 0x%x, lit);", codeAddress);
	}

	private String emitMove(int codeAddress, Trace curTrace,
			Instruction instruction) {
		int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
		int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

		return String.format("  v[%d] = v[%d];", vA, vB);
	}

	private String emitMoveWide(int codeAddress, Trace curTrace,
			Instruction instruction) {
		int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
		int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

		return String.format(
				"  *((long long*)(v + %d)) = *((long long*)(v + %d));", vA, vB);
	}

	private String emitConst(int codeAddress, Trace curTrace,
			Instruction instruction) {
		int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
		long lit = ((LiteralInstruction) instruction).getLiteral();

		return String.format("  v[%d] = %d;", vA, lit);
	}

	private String emitConstWide(int codeAddress, Trace curTrace,
			Instruction instruction) {
		int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
		long lit = ((LiteralInstruction) instruction).getLiteral();

		return String.format("  *((long long*)(v + %d)) = %d;", vA, lit);
	}

	private String emitConstHigh(int codeAddress, Trace curTrace,
			Instruction instruction) {
		int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
		long lit = ((LiteralInstruction) instruction).getLiteral();

		return String.format("  v[%d] = %d;", vA, (lit << 16));
	}

	private String emitConstWideHigh(int codeAddress, Trace curTrace,
			Instruction instruction) {
		int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
		long lit = ((LiteralInstruction) instruction).getLiteral();

		return String.format("  *((long long*)(v + %d)) = %dLL;", vA,
				(lit << 48));
	}

	private String emitGoto(int codeAddress, Trace curTrace,
			Instruction instruction) {
		int targetAddressOffset = ((OffsetInstruction) instruction)
				.getTargetAddressOffset();
		int targetAddress = codeAddress + targetAddressOffset;

		return "  " + getGotoLabel(curTrace, targetAddress) + ";\n";
	}

	private String emitIf(int codeAddress, Trace curTrace,
			Instruction instruction, String comparison) {
		int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
		int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

		int targetAddressOffset = ((OffsetInstruction) instruction)
				.getTargetAddressOffset();
		int targetAddress = codeAddress + targetAddressOffset;

		return String.format("  if (v[%d] %s v[%d]) { %s; }", vA, comparison,
				vB, getGotoLabel(curTrace, targetAddress));

	}

	private String emitIfZero(int codeAddress, Trace curTrace,
			Instruction instruction, String comparison) {
		int vA = ((SingleRegisterInstruction) instruction).getRegisterA();

		int targetAddressOffset = ((OffsetInstruction) instruction)
				.getTargetAddressOffset();
		int targetAddress = codeAddress + targetAddressOffset;

		return String.format("  if (v[%d] %s 0) { %s; }", vA, comparison,
				getGotoLabel(curTrace, targetAddress));

	}

	private String emitArrayGet(int codeAddress, Trace curTrace,
			Instruction instruction, String type, int size) {
		int vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
		int vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
		int vC = ((ThreeRegisterInstruction) instruction).getRegisterC();

		return String
				.format("  {\n"
						+ "    char *array = (char*) v[%2$d];\n"
						+ "    int array_size = *((int*) (array + 8));\n"
						+ "    if (array == 0) TRACE_EXCEPTION(%4$#x)\n"
						+ "    if (((unsigned int) v[%3$d]) >= array_size) TRACE_EXCEPTION(%4$#x)\n"
						+ "    %5$s *array_contents = (%5$s*) (array + 16 + (%6$d * v[%3$d]));\n"
						+ "    %5$s *reg_location = (%5$s*) (v + %1$d);\n"
						+ "    *reg_location = *array_contents;\n" + "  }", vA,
						vB, vC, codeAddress, type, size);
	}

	private String emitArrayPut(int codeAddress, Trace curTrace,
			Instruction instruction, String type, int size) {
		int vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
		int vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
		int vC = ((ThreeRegisterInstruction) instruction).getRegisterC();

		return String
				.format("  {\n"
						+ "    char *array = (char*) v[%2$d];\n"
						+ "    int array_size = *((int*) (array + 8));\n"
						+ "    if (array == 0) TRACE_EXCEPTION(%4$#x)\n"
						+ "    if (((unsigned int) v[%3$d]) >= array_size) TRACE_EXCEPTION(%4$#x)\n"
						+ "    %5$s *array_contents = (%5$s*) (array + 16 + (%6$d * v[%3$d]));\n"
						+ "    %5$s *reg_location = (%5$s*) (v + %1$d);\n"
						+ "    *array_contents = *reg_location;\n" + "  }", vA,
						vB, vC, codeAddress, type, size);
	}

	private String emitStaticGet(int codeAddress, Trace curTrace,
			Instruction instruction, String type) {
		int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
		int field = ((InstructionWithReference) instruction)
				.getReferencedItem().getIndex();

		int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.STATIC_FIELD, field);

		if (type.equals("long long")) {
			return String.format(
					"  *((long long*) (v + %d)) = *((%s*) lit[%d]);", vA, type,
					literalPoolLoc);
		}
		return String.format("  v[%d] = (int) *((%s*) lit[%d]);", vA, type,
				literalPoolLoc);
	}

	private String emitStaticPut(int codeAddress, Trace curTrace,
			Instruction instruction, String type) {
		int vA = ((SingleRegisterInstruction) instruction).getRegisterA();
		int field = ((InstructionWithReference) instruction)
				.getReferencedItem().getIndex();

		int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(
				LiteralPoolType.STATIC_FIELD, field);

		if (type.equals("long long")) {
			return String.format(
					"  *((%s*) lit[%d]) = *((long long*) (v + %d));", type,
					literalPoolLoc, vA);
		}
		return String.format("  *((%1$s*) lit[%2$d]) = ((%1$s) v[%3$d]);",
				type, literalPoolLoc, vA);
	}

	private String emitIntArith(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitIntArithImpl(codeAddress, curTrace, instruction, op, false,
				false, false);
	}

	private String emitIntArithUnsigned(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitIntArithImpl(codeAddress, curTrace, instruction, op, true,
				false, false);
	}

	private String emitIntArith2Addr(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitIntArithImpl(codeAddress, curTrace, instruction, op, false,
				true, false);
	}

	private String emitIntArithUnsigned2Addr(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitIntArithImpl(codeAddress, curTrace, instruction, op, true,
				true, false);
	}

	private String emitIntArithLiteral(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitIntArithImpl(codeAddress, curTrace, instruction, op, false,
				false, true);
	}

	private String emitIntArithUnsignedLiteral(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitIntArithImpl(codeAddress, curTrace, instruction, op, true,
				false, true);
	}

	private String emitIntArithImpl(int codeAddress, Trace curTrace,
			Instruction instruction, String op, boolean unsigned,
			boolean twoaddr, boolean needsLiteral) {
		int vA = 0;
		int vB = 0;
		String vC = "";
		if (needsLiteral) {
			vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			vB = ((TwoRegisterInstruction) instruction).getRegisterB();
			vC = String.format("%d",
					((LiteralInstruction) instruction).getLiteral());
		} else {
			if (!twoaddr) {
				vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
				vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
				vC = String
						.format("v[%d]",
								((ThreeRegisterInstruction) instruction)
										.getRegisterC());
			} else {
				vA = ((TwoRegisterInstruction) instruction).getRegisterA();
				vB = ((TwoRegisterInstruction) instruction).getRegisterA();
				vC = String.format("v[%d]",
						((TwoRegisterInstruction) instruction).getRegisterB());
			}
		}

		if (op.equals(">>") || op.equals("<<")) {
			if (unsigned) {
				return String.format(
						"  v[%d] = ((unsigned int) v[%d]) %s (%s & 0x1f);", vA,
						vB, op, vC);
			} else {
				return String.format("  v[%d] = v[%d] %s (%s & 0x1f);", vA, vB,
						op, vC);
			}
		} else if (op.equals("rsub")) {
			return String.format("  v[%d] = %s - v[%d];", vA, vC, vB);
		}
		return String.format("  v[%d] = v[%d] %s %s;", vA, vB, op, vC);
	}

	/* FLOAT */

	private String emitFloatArith(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitFloatArithImpl(codeAddress, curTrace, instruction, op, false);
	}

	private String emitFloatArith2Addr(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitFloatArithImpl(codeAddress, curTrace, instruction, op, true);
	}

	private String emitFloatArithImpl(int codeAddress, Trace curTrace,
			Instruction instruction, String op, boolean twoaddr) {
		int vA = 0;
		int vB = 0;
		int vC = 0;
		if (!twoaddr) {
			vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
			vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
			vC = ((ThreeRegisterInstruction) instruction).getRegisterC();
		} else {
			vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			vB = ((TwoRegisterInstruction) instruction).getRegisterA();
			vC = ((TwoRegisterInstruction) instruction).getRegisterB();
		}

		if (op.equals("fmodf")) {
			return String.format("  {\n"
					+ "    float a = *((float*) (v + %2$d));\n"
					+ "    float b = *((float*) (v + %3$d));\n"
					+ "    *(((float*) (v + %1$d))) = %4$s(a, b);\n" + "  }",
					vA, vB, vC, op);
		}

		return String.format("  {\n"
				+ "    float a = *((float*) (v + %2$d));\n"
				+ "    float b = *((float*) (v + %3$d));\n"
				+ "    *(((float*) (v + %1$d))) = a %4$s b;\n" + "  }", vA, vB,
				vC, op);
	}

	/* DOUBLE */

	private String emitDoubleArith(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitDoubleArithImpl(codeAddress, curTrace, instruction, op,
				false);
	}

	private String emitDoubleArith2Addr(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitDoubleArithImpl(codeAddress, curTrace, instruction, op, true);
	}

	private String emitDoubleArithImpl(int codeAddress, Trace curTrace,
			Instruction instruction, String op, boolean twoaddr) {
		int vA = 0;
		int vB = 0;
		int vC = 0;
		if (!twoaddr) {
			vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
			vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
			vC = ((ThreeRegisterInstruction) instruction).getRegisterC();
		} else {
			vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			vB = ((TwoRegisterInstruction) instruction).getRegisterA();
			vC = ((TwoRegisterInstruction) instruction).getRegisterB();
		}

		if (op.equals("fmod")) {
			return String.format("  {\n"
					+ "    double a = *((double*) (v + %2$d));\n"
					+ "    double b = *((double*) (v + %3$d));\n"
					+ "    *(((double*) (v + %1$d))) = %4$s(a, b);\n" + "  }",
					vA, vB, vC, op);
		}
		return String.format("  {\n"
				+ "    double a = *((double*) (v + %2$d));\n"
				+ "    double b = *((double*) (v + %3$d));\n"
				+ "    *(((double*) (v + %1$d))) = a %4$s b;\n" + "  }", vA,
				vB, vC, op);
	}

	private String emitLongArith(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitLongArithImpl(codeAddress, curTrace, instruction, op, false,
				false);
	}

	private String emitLongArithUnsigned(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitLongArithImpl(codeAddress, curTrace, instruction, op, true,
				false);
	}

	private String emitLongArith2Addr(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitLongArithImpl(codeAddress, curTrace, instruction, op, false,
				true);
	}

	private String emitLongArithUnsigned2Addr(int codeAddress, Trace curTrace,
			Instruction instruction, String op) {
		return emitLongArithImpl(codeAddress, curTrace, instruction, op, true,
				true);
	}

	private String emitLongArithImpl(int codeAddress, Trace curTrace,
			Instruction instruction, String op, boolean unsigned,
			boolean twoaddr) {
		int vA = 0;
		int vB = 0;
		int vC = 0;
		if (!twoaddr) {
			vA = ((ThreeRegisterInstruction) instruction).getRegisterA();
			vB = ((ThreeRegisterInstruction) instruction).getRegisterB();
			vC = ((ThreeRegisterInstruction) instruction).getRegisterC();
		} else {
			vA = ((TwoRegisterInstruction) instruction).getRegisterA();
			vB = ((TwoRegisterInstruction) instruction).getRegisterA();
			vC = ((TwoRegisterInstruction) instruction).getRegisterB();
		}

		if (op.equals(">>") || op.equals("<<")) {
			if (unsigned) {
				return String
						.format("  {\n"
								+ "    long long a = *((unsigned long long*) (v + %2$d));\n"
								+ "    long long b = *((long long*) (v + %3$d)) & (0x3f);\n"
								+ "    *(((long long*) (v + %1$d))) = a %4$s b;\n"
								+ "  }", vA, vB, vC, op);
			} else {
				return String
						.format("  {\n"
								+ "    long long a = *((long long*) (v + %2$d));\n"
								+ "    long long b = *((long long*) (v + %3$d)) & (0x3f);\n"
								+ "    *(((long long*) (v + %1$d))) = a %4$s b;\n"
								+ "  }", vA, vB, vC, op);
			}
		}
		return String.format("  {\n"
				+ "    long long a = *((long long*) (v + %2$d));\n"
				+ "    long long b = *((long long*) (v + %3$d));\n"
				+ "    *(((long long*) (v + %1$d))) = a %4$s b;\n" + "  }", vA,
				vB, vC, op);
	}

	private String emitTypeConversion(int codeAddress, Trace curTrace,
			Instruction instruction, String from, String to) {
		int vA = ((TwoRegisterInstruction) instruction).getRegisterA();
		int vB = ((TwoRegisterInstruction) instruction).getRegisterB();

		return "  {\n"
				+ String.format("    %3$s value = *((%3$s*) (v + %2$d));\n"
						+ "    *(((%4$s*) (v + %1$d))) = (%4$s) value;\n", vA,
						vB, from, to) + "  }";
	}

	private String emitInvokeSingleton(int codeAddress, boolean nullCheck) {
		String nullCheckString = "nonullcheck";
		if (nullCheck) {
			nullCheckString = "nullcheck";
		}
		return String
				.format("  if (!invoke_singleton_%1$s_%2$#x(%2$#x, lit, v, self)) TRACE_EXCEPTION(%2$#x)\n"+
						"  *((int*)(self+40)) &= ~0x4000;\n"+
						"  if (*((int*)(self+40)) == 0x10000) *((int*)(self+40)) = 0x0;",
						nullCheckString, codeAddress);
	}

	private String emitInvokeInterface(int codeAddress) {
		return String
				.format("  if (!invoke_interface_%1$#x(lit, v, self)) TRACE_EXCEPTION(%1$#x)\n"+
						"  *((int*)(self+40)) &= ~0x4000;\n"+
						"  if (*((int*)(self+40)) == 0x10000) *((int*)(self+40)) = 0x0;",
						codeAddress);
	}

	private String emitInvokeVirtualQuick(int codeAddress) {
		return String
				.format("  if (!invoke_virtual_quick_%1$#x(lit, v, self)) TRACE_EXCEPTION(%1$#x)\n"+
						"  *((int*)(self+40)) &= ~0x4000;\n"+
						"  if (*((int*)(self+40)) == 0x10000) *((int*)(self+40)) = 0x0;",
						codeAddress);
	}

	public String getGotoLabel(Trace trace, int codeAddress) {
		if (!trace.containsCodeAddress(codeAddress)) {
			return String.format("TRACE_EXIT(%#x)", codeAddress);
		}
		return String.format("goto __L%#x", codeAddress);
	}
}
