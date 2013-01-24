package eu.whrl.aottracegen.converters;

import java.util.Iterator;

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
		
		// opcode: 00 nop                        
		// opcode: 01 move    
		case MOVE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %s = %s;", vrs(vA), vrs(vB));
			break;
		}
		
		// opcode: 02 move/from16 
		case MOVE_FROM16:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %s = %s;", vrs(vA), vrs(vB));
			break;
		}
		
		// opcode: 03 move/16                    
		// opcode: 04 move-wide   
		case MOVE_WIDE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %s = %s;\n" +
			                       "  %s = %s;", vrs(vA), vrs(vB), vrs(vA+1), vrs(vB+1));
			break;
		}
		
		// opcode: 05 move-wide/from16           
		// opcode: 06 move-wide/16               
		// opcode: 07 move-object                
		// opcode: 08 move-object/from16         
		// opcode: 09 move-object/16             
		// opcode: 0a move-result  
		case MOVE_RESULT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  %s = *((int*) (self+%d));", vrs(vA), offsetThreadRetValue);
			break;
		}
		
		// opcode: 0b move-result-wide           
		// opcode: 0c move-result-object         
		// opcode: 0d move-exception             
		// opcode: 0e return-void 
		case RETURN_VOID:
		{	
			result = String.format("  goto __return_L%#x;", codeAddress);
			break;
		}
		
		// opcode: 0f return    
		case RETURN:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  *((int*) (self+%d)) = %s;\n", offsetThreadRetValue, vrs(vA)) +
					String.format("  goto __return_L%#x;", codeAddress);
			break;
		}
		
		// opcode: 10 return-wide  
		case RETURN_WIDE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  *((int*) (self+%d)) = %s;\n", offsetThreadRetValue, vrs(vA)) +
					String.format("  *((int*) (self+%d)) = %s;\n", offsetThreadRetValue+4, vrs(vA+1)) +
					String.format("  goto __return_L%#x;", codeAddress);
			break;
		}
		
		// opcode: 11 return-object
		case RETURN_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result = String.format("  *((int*) (self+%d)) = %s;\n", offsetThreadRetValue, vrs(vA)) +
					String.format("  goto __return_L%#x;", codeAddress);
			break;
		}
		
		// opcode: 12 const/4  
		case CONST_4:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  %s = %d;", vrs(vA), lit);
			break;
		}
		
		// opcode: 13 const/16   
		case CONST_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  %s = %d;", vrs(vA), lit);
			break;
		}
		
		// opcode: 14 const     
		case CONST:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  %s = %d;", vrs(vA), lit);
			break;
		}
		
		// opcode: 15 const/high16               
		// opcode: 16 const-wide/16   
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
		
		// opcode: 17 const-wide/32              
		// opcode: 18 const-wide                 
		// opcode: 19 const-wide/high16
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
		
		// opcode: 1a const-string               
		// opcode: 1b const-string/jumbo         
		// opcode: 1c const-class                
		// opcode: 1d monitor-enter              
		// opcode: 1e monitor-exit               
		// opcode: 1f check-cast                 
		// opcode: 20 instance-of                
		// opcode: 21 array-length               
		// opcode: 22 new-instance               
		// opcode: 23 new-array                  
		// opcode: 24 filled-new-array           
		// opcode: 25 filled-new-array/range     
		// opcode: 26 fill-array-data            
		// opcode: 27 throw                      
		// opcode: 28 goto     
		case GOTO:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = "  " + getGotoLabel(curTrace, targetAddress) + ";\n";
			break;
		}
		
		// opcode: 29 goto/16     
		case GOTO_16:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = "  " + getGotoLabel(curTrace, targetAddress) + ";\n";
			break;
		}
		
		// opcode: 2a goto/32
		case GOTO_32:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = "  " + getGotoLabel(curTrace, targetAddress) + ";\n";
			break;
		}
		
		// opcode: 2b packed-switch 
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
		
		// opcode: 2c sparse-switch              
		// opcode: 2d cmpl-float                 
		// opcode: 2e cmpg-float                 
		// opcode: 2f cmpl-double  
		case CMPL_DOUBLE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
			
			result = String.format(
			"  {\n" +
			"    double value1 = *((double*) (((char*)v) + (4 * %1$d)));\n" +
			"    double value2 = *((double*) (((char*)v) + (4 * %2$d)));\n" +
			"    if (value1 == value2) { %3$s = 0; }\n" +
			"    else if (value1 > value2) { %3$s = 1; }\n" +
			"    else { %3$s = -1; }\n" +
			"  }", vB, vC, vrs(vA));
					
			break;
		}
		
		// opcode: 30 cmpg-double                
		// opcode: 31 cmp-long    
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
		
		// opcode: 32 if-eq   
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
		
		// opcode: 33 if-ne   
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
		
		// opcode: 34 if-lt   
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
		
		// opcode: 35 if-ge
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
		
		// opcode: 36 if-gt 
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
		
		// opcode: 37 if-le   
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
		
		// opcode: 38 if-eqz 
		case IF_EQZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s == 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		// opcode: 39 if-nez 
		case IF_NEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s != 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		// opcode: 3a if-ltz     
		case IF_LTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s < 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		// opcode: 3b if-gez     
		case IF_GEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s >= 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		// opcode: 3c if-gtz  
		case IF_GTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s > 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		// opcode: 3d if-lez   
		case IF_LEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result = String.format("  if (%s <= 0) { %s; }",
					vrs(vA), getGotoLabel(curTrace, targetAddress));
			break;
		}
		
		// opcode: 44 aget          
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
		
		// opcode: 45 aget-wide  
		case AGET_WIDE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  {\n" +
					 "    char *array = (char*) v[%2$d];\n" + 
					 "    int array_size = *((int*) (array + 8));\n" +
					 "    if (array == 0) goto __exception_L%4$#x;\n" +
					 "    if (((unsigned int) v[%3$d]) >= array_size) goto __exception_L%4$#x;\n" +
					 "    char *array_contents = array + 16 + (4 * v[%3$d]);\n" +
					 "    *((long long*) (((char*)v) + (4 * %1$d))) = *((long long*) array_contents);\n" +
					 "  }",
					 vA, vB, vC, codeAddress);
			break;
		}
		
		// opcode: 46 aget-object                
		// opcode: 47 aget-boolean               
		// opcode: 48 aget-byte 
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
		
		// opcode: 49 aget-char                  
		// opcode: 4a aget-short                 
		// opcode: 4b aput    
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
		
		// opcode: 4c aput-wide         
		case APUT_WIDE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  {\n" +
					 "    char *array = (char*) %3$s;\n" + 
					 "    int array_size = *((int*) (array + 8));\n" +
					 "    if (array == 0) goto __exception_L%5$#x;\n" +
					 "    if (((unsigned int) %4$s) >= array_size) goto __exception_L%5$#x;\n" +
					 "    int *array_contents = array + 16;\n" +
					 "    array_contents[%4$s] = %1$s;\n" +
					 "    array_contents[%4$s+1] = %2$s;\n" +
					 "  }",
					 vrs(vA), vrs(vA+1), vrs(vB), vrs(vC), codeAddress);
			break;
		}
		
		// opcode: 4d aput-object                
		// opcode: 4e aput-boolean               
		// opcode: 4f aput-byte                  
		// opcode: 50 aput-char                  
		// opcode: 51 aput-short                 
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
		// opcode: 61 sget-wide                  
		// opcode: 62 sget-object 
		case SGET_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.addLiteralPoolTypeAndValue(LiteralPoolType.STATIC_FIELD, field);
			
			result = String.format("  %s = *((int*) lit[%d]);", vrs(vA), literalPoolLoc);
			break;
		}
		
		// opcode: 63 sget-boolean               
		// opcode: 64 sget-byte                  
		// opcode: 65 sget-char                  
		// opcode: 66 sget-short                 
		// opcode: 67 sput                       
		// opcode: 68 sput-wide                  
		// opcode: 69 sput-object                
		// opcode: 6a sput-boolean               
		// opcode: 6b sput-byte                  
		// opcode: 6c sput-char                  
		// opcode: 6d sput-short                 
		// opcode: 6e invoke-virtual             
		// opcode: 6f invoke-super               
		// opcode: 70 invoke-direct              
		// opcode: 71 invoke-static              
		// opcode: 72 invoke-interface           
		// opcode: 74 invoke-virtual/range       
		// opcode: 75 invoke-super/range         
		// opcode: 76 invoke-direct/range        
		// opcode: 77 invoke-static/range        
		// opcode: 78 invoke-interface/range     
		// opcode: 7b neg-int  
		case NEG_INT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %s = -%s;", vrs(vA), vrs(vB));
			break;
		}
		
		// opcode: 7c not-int                    
		// opcode: 7d neg-long                   
		// opcode: 7e not-long                   
		// opcode: 7f neg-float                  
		// opcode: 80 neg-double                 
		// opcode: 81 int-to-long   
		case INT_TO_LONG:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %1$s = %3$s;\n" +
								   "  %2$s = (%3$s < 0) ? -1 : 0;", vrs(vA), vrs(vA+1), vrs(vB));
			break;
		}
		
		// opcode: 82 int-to-float               
		// opcode: 83 int-to-double
		case INT_TO_DOUBLE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %2$s = %3$s;\n" +
			                       "  %1$s = 0;\n", vrs(vA), vrs(vA+1), vrs(vB));
			break;
		}
		
		// opcode: 84 long-to-int                
		// opcode: 85 long-to-float              
		// opcode: 86 long-to-double 
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
		
		// opcode: 87 float-to-int               
		// opcode: 88 float-to-long              
		// opcode: 89 float-to-double            
		// opcode: 8a double-to-int              
		// opcode: 8b double-to-long             
		// opcode: 8c double-to-float            
		// opcode: 8d int-to-byte                
		// opcode: 8e int-to-char                
		// opcode: 8f int-to-short               
		// opcode: 90 add-int                   
		case ADD_INT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  %s = %s + %s;", vrs(vA), vrs(vB), vrs(vC));
			break;
		}
		
		// opcode: 91 sub-int                    
		// opcode: 92 mul-int  
		case MUL_INT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();
					
			result = String.format("  %s = %s * %s;", vrs(vA), vrs(vB), vrs(vC));
			break;
		}
		
		// opcode: 93 div-int                    
		// opcode: 94 rem-int                    
		// opcode: 95 and-int                    
		// opcode: 96 or-int                     
		// opcode: 97 xor-int                    
		// opcode: 98 shl-int                    
		// opcode: 99 shr-int                    
		// opcode: 9a ushr-int                   
		// opcode: 9b add-long                   
		// opcode: 9c sub-long                   
		// opcode: 9d mul-long                   
		// opcode: 9e div-long                   
		// opcode: 9f rem-long                   
		// opcode: a0 and-long                   
		// opcode: a1 or-long                    
		// opcode: a2 xor-long                   
		// opcode: a3 shl-long                   
		// opcode: a4 shr-long                   
		// opcode: a5 ushr-long                  
		// opcode: a6 add-float                  
		// opcode: a7 sub-float                  
		// opcode: a8 mul-float                  
		// opcode: a9 div-float                  
		// opcode: aa rem-float                  
		// opcode: ab add-double                 
		// opcode: ac sub-double                 
		// opcode: ad mul-double                 
		// opcode: ae div-double                 
		// opcode: af rem-double                 
		// opcode: b0 add-int/2addr 
		case ADD_INT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format("  %1$s = %1$s + %2$s;", vrs(vA), vrs(vB));
			break;
		}
		
		// opcode: b1 sub-int/2addr              
		// opcode: b2 mul-int/2addr              
		// opcode: b3 div-int/2addr              
		// opcode: b4 rem-int/2addr              
		// opcode: b5 and-int/2addr              
		// opcode: b6 or-int/2addr               
		// opcode: b7 xor-int/2addr              
		// opcode: b8 shl-int/2addr              
		// opcode: b9 shr-int/2addr              
		// opcode: ba ushr-int/2addr             
		// opcode: bb add-long/2addr 
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
		
		// opcode: bc sub-long/2addr             
		// opcode: bd mul-long/2addr             
		// opcode: be div-long/2addr             
		// opcode: bf rem-long/2addr             
		// opcode: c0 and-long/2addr             
		// opcode: c1 or-long/2addr              
		// opcode: c2 xor-long/2addr             
		// opcode: c3 shl-long/2addr             
		// opcode: c4 shr-long/2addr             
		// opcode: c5 ushr-long/2addr            
		// opcode: c6 add-float/2addr            
		// opcode: c7 sub-float/2addr            
		// opcode: c8 mul-float/2addr            
		// opcode: c9 div-float/2addr            
		// opcode: ca rem-float/2addr            
		// opcode: cb add-double/2addr  
		case ADD_DOUBLE_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format(
			"  {\n" +
			"    double a = *((double*) (((char*)v) + (4 * %1$d)));\n" +
			"    double b = *((double*) (((char*)v) + (4 * %2$d)));\n" +
			"    *(((double*) (((char*)v) + (4 * %1$d)))) = a+b;\n" +
			"  }", vA, vB);
			break;
		}
		
		// opcode: cc sub-double/2addr           
		// opcode: cd mul-double/2addr  
		case MUL_DOUBLE_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result = String.format(
			"  {\n" +
			"    double a = *((double*) (((char*)v) + (4 * %1$d)));\n" +
			"    double b = *((double*) (((char*)v) + (4 * %2$d)));\n" +
			"    *(((double*) (((char*)v) + (4 * %1$d)))) = a*b;\n" +
			"  }", vA, vB);
			break;
		}
		
		// opcode: ce div-double/2addr   
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
		
		// opcode: cf rem-double/2addr           
		// opcode: d0 add-int/lit16              
		// opcode: d1 rsub-int                   
		// opcode: d2 mul-int/lit16              
		// opcode: d3 div-int/lit16              
		// opcode: d4 rem-int/lit16              
		// opcode: d5 and-int/lit16              
		// opcode: d6 or-int/lit16               
		// opcode: d7 xor-int/lit16              
		// opcode: d8 add-int/lit8 
		case ADD_INT_LIT8:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			long constant = ((LiteralInstruction)instruction).getLiteral();
			
			result = String.format("  %s = %s + %d;", vrs(vA), vrs(vB), constant);
			break;
		}
		
		// opcode: d9 rsub-int/lit8              
		// opcode: da mul-int/lit8               
		// opcode: db div-int/lit8               
		// opcode: dc rem-int/lit8               
		// opcode: dd and-int/lit8               
		// opcode: de or-int/lit8                
		// opcode: df xor-int/lit8               
		// opcode: e0 shl-int/lit8               
		// opcode: e1 shr-int/lit8               
		// opcode: e2 ushr-int/lit8              
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
		// opcode: ef +execute-inline/range      
		// opcode: f0 +invoke-object-init/range  
		// opcode: f1 +return-void-barrier       
		// opcode: f2 +iget-quick       
		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  if (%2$s == 0) goto __exception_L%4$#x;\n" +
		               			   "  %1$s = *((int*) (((char*)%2$s) + %3$#x));", vrs(vA), vrs(vB), offset, codeAddress);
			break;
		}
		
		// opcode: f3 +iget-wide-quick        
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
		
		// opcode: f4 +iget-object-quick         
		// opcode: f5 +iput-quick         
		case IPUT_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();
			
			result = String.format("  if (%2$s == 0) goto __exception_L%4$#x;\n" +
					               "  *((int*) (((char*)%2$s) + %3$#x)) = %1$s;", vrs(vA), vrs(vB), offset, codeAddress);
			break;
		}
		
		// opcode: f6 +iput-wide-quick       
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
		
		// opcode: f7 +iput-object-quick         
		// opcode: f8 +invoke-virtual-quick     
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
		
		// opcode: f9 +invoke-virtual-quick/range
		// opcode: fa +invoke-super-quick        
		// opcode: fb +invoke-super-quick/range  
		// opcode: fc +iput-object-volatile      
		// opcode: fd +sget-object-volatile      
		// opcode: fe +sput-object-volatile 
		
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