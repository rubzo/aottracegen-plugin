package eu.whrl.aottracegen.converters;

import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.LiteralInstruction;
import org.jf.dexlib.Code.OdexedFieldAccess;
import org.jf.dexlib.Code.OdexedInvokeInline;
import org.jf.dexlib.Code.OdexedInvokeVirtual;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.Trace;

public class BytecodeToPrettyConverter {
	
	public boolean llvmMode = false;
	
	public BytecodeToPrettyConverter(boolean usingLLVM) {
		llvmMode = usingLLVM;
	}
	
	/*
	 * Return a string representing the instruction at codeAddress,
	 * as a human readable representation. MUST BE CALLED BEFORE THE
	 * BYTECODE TO C CONVERTER, AS THAT HAS SIDE EFFECTS.
	 */
	public String convert(CodeGenContext context, int codeAddress) {
		Instruction instruction = context.currentRegion.getInstructionAtCodeAddress(codeAddress);
		
		Trace curTrace = context.currentRegion.trace;
		
		String result = "";
		if (llvmMode) {
			result = String.format("  ; BYTECODE AT %#x: ", codeAddress);
		} else {
			result = String.format("  // BYTECODE AT %#x: ", codeAddress);
		}
		
		switch (instruction.opcode) {
		
		// opcode: 00 nop   
		case NOP:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += "nop";
			break;
		}
		
		// opcode: 01 move   
		case MOVE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 02 move/from16       
		case MOVE_FROM16:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move/from16 v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 03 move/16 
		case MOVE_16:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move/16 v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 04 move-wide   
		case MOVE_WIDE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move-wide v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 05 move-wide/from16    
		case MOVE_WIDE_FROM16:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move-wide/from16 v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 06 move-wide/16  
		case MOVE_WIDE_16:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move-wide/16 v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 07 move-object
		case MOVE_OBJECT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move-object v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 08 move-object/from16
		case MOVE_OBJECT_FROM16:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move-object/from16 v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 09 move-object/16
		case MOVE_OBJECT_16:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("move-object/16 v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 0a move-result    
		case MOVE_RESULT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("move-result v%d", vA);
			break;
		}
		
		// opcode: 0b move-result-wide  
		case MOVE_RESULT_WIDE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("move-result-wide v%d", vA);
			break;
		}
		
		// opcode: 0c move-result-object         
		case MOVE_RESULT_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("move-result-object v%d", vA);
			break;
		}
		
		// opcode: 0d move-exception
		case MOVE_EXCEPTION:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("move-exception v%d", vA);
			break;
		}
		
		// opcode: 0e return-void      
		case RETURN_VOID:
		{
			result += "return-void";
			break;
		}
		
		// opcode: 0f return   
		case RETURN:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("return v%d", vA);
			break;
		}
		
		// opcode: 10 return-wide      
		case RETURN_WIDE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("return-wide v%d", vA);
			break;
		}
		
		// opcode: 11 return-object 
		case RETURN_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("return-object v%d", vA);
			break;
		}
		
		// opcode: 12 const/4   
		case CONST_4:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			result += String.format("const/4 v%d, #%d", vA, lit);
			break;
		}
		
		// opcode: 13 const/16  
		case CONST_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			result += String.format("const/16 v%d, #%d", vA, lit);
			break;
		}
		
		// opcode: 14 const    
		case CONST:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			result += String.format("const v%d, #%d", vA, lit);
			break;
		}
		
		// opcode: 15 const/high16
		case CONST_HIGH16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			lit  = lit << 16;

			result += String.format("const/high16 v%d, #%d", vA, lit);
			break;
		}
		
		// opcode: 16 const-wide/16      
		case CONST_WIDE_16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result += String.format("const-wide/16 v%d, #%d", vA, lit);
			break;
		}
		
		// opcode: 17 const-wide/32     
		case CONST_WIDE_32:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result += String.format("const-wide/32 v%d, #%d", vA, lit);
			break;
		}
		
		// opcode: 18 const-wide
		case CONST_WIDE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();
			
			result += String.format("const-wide v%d, #%d", vA, lit);
			break;
		}
		
		// opcode: 19 const-wide/high16 
		case CONST_WIDE_HIGH16:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			long lit = ((LiteralInstruction)instruction).getLiteral();

			lit = lit << 48;
			
			result += String.format("const-wide/high16 v%d, #%d", vA, lit);
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
			
			result += String.format("goto +%#x;", targetAddress);
			break;
		}
		
		// opcode: 29 goto/16       
		case GOTO_16:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result += String.format("goto/16 +%#x;", targetAddress);
			break;
		}
		
		// opcode: 2a goto/32  
		case GOTO_32:
		{
			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();
			int targetAddress = codeAddress + targetAddressOffset;
			
			result += String.format("goto/32 +%#x;", targetAddress);
			break;
		}
		
		// opcode: 2b packed-switch   
		case PACKED_SWITCH:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			
			result += String.format("packed-switch v%d", vA);
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

			result += String.format("cmpl-double v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 30 cmpg-double                
		// opcode: 31 cmp-long    
		case CMP_LONG:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("cmp-long v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 32 if-eq 
		case IF_EQ:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-eq v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		// opcode: 33 if-ne 
		case IF_NE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-ne v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}
		
		// opcode: 34 if-lt
		case IF_LT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-lt v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		// opcode: 35 if-ge     
		case IF_GE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-ge v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		// opcode: 36 if-gt       
		case IF_GT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-gt v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		// opcode: 37 if-le      
		case IF_LE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-le v%d, v%d, +%#x",
					vA, vB, targetAddressOffset);
			break;
		}

		// opcode: 38 if-eqz  
		case IF_EQZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-eqz v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		// opcode: 39 if-nez     
		case IF_NEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-nez v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		// opcode: 3a if-ltz   
		case IF_LTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-ltz v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		// opcode: 3b if-gez     
		case IF_GEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-gez v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		// opcode: 3c if-gtz     
		case IF_GTZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-gtz v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}

		// opcode: 3d if-lez   
		case IF_LEZ:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();

			int targetAddressOffset = ((OffsetInstruction)instruction).getTargetAddressOffset();

			result += String.format("if-lez v%d, +%#x",
					vA, targetAddressOffset);
			break;
		}
		
		// opcode: 44 aget    
		case AGET:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aget v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 45 aget-wide
		case AGET_WIDE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aget-wide v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 46 aget-object  
		case AGET_OBJECT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aget-object v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 47 aget-boolean  
		case AGET_BOOLEAN:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aget-boolean v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 48 aget-byte           
		case AGET_BYTE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aget-byte v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 49 aget-char         
		case AGET_CHAR:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aget-char v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 4a aget-short
		case AGET_SHORT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aget-short v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 4b aput      
		case APUT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aput v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 4c aput-wide 
		case APUT_WIDE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aput-wide v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 4d aput-object
		case APUT_OBJECT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aput-object v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 4e aput-boolean      
		case APUT_BOOLEAN:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aput-boolean v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 4f aput-byte     
		case APUT_BYTE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aput-byte v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 50 aput-char           
		case APUT_CHAR:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aput-char v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 51 aput-short
		case APUT_SHORT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("aput-short v%d, v%d, v%d",
					vA, vB, vC);
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
		case SGET:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.literalPoolSize;
			
			result += String.format("sget v%d, field@%#x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		// opcode: 61 sget-wide      
		case SGET_WIDE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.literalPoolSize;
			
			result += String.format("sget-wide v%d, field@%#x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		// opcode: 62 sget-object   
		case SGET_OBJECT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.literalPoolSize;
			
			result += String.format("sget-object v%d, field@%#x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		// opcode: 63 sget-boolean   
		case SGET_BOOLEAN:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.literalPoolSize;
			
			result += String.format("sget-boolean v%d, field@%#x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		// opcode: 64 sget-byte   
		case SGET_BYTE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.literalPoolSize;
			
			result += String.format("sget-byte v%d, field@%#x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		// opcode: 65 sget-char      
		case SGET_CHAR:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.literalPoolSize;
			
			result += String.format("sget-char v%d, field@%#x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		// opcode: 66 sget-short    
		case SGET_SHORT:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.literalPoolSize;
			
			result += String.format("sget-short v%d, field@%#x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		// opcode: 67 sput                       
		// opcode: 68 sput-wide
		case SPUT_WIDE:
		{
			int vA = ((SingleRegisterInstruction)instruction).getRegisterA();
			int field = ((InstructionWithReference)instruction).getReferencedItem().getIndex();
			
			int literalPoolLoc = curTrace.meta.literalPoolSize;
			
			result += String.format("sput-wide v%d, field@%#x (lit pool idx: %d)",
					 vA, field, literalPoolLoc);
			break;
		}
		
		// opcode: 69 sput-object                
		// opcode: 6a sput-boolean               
		// opcode: 6b sput-byte                  
		// opcode: 6c sput-char                  
		// opcode: 6d sput-short                 
		// opcode: 6e invoke-virtual             
		// opcode: 6f invoke-super               
		// opcode: 70 invoke-direct    
		// opcode: 71 invoke-static 
		case INVOKE_STATIC:
		{
			int method = ((InstructionWithReference)instruction).getReferencedItem().getIndex(); 
			
			String argsString = getArgsString(instruction);
			
			result += String.format("invoke-static %s, method@%#x", argsString, method);
			break;
		}
		
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
			
			result += String.format("neg-int v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 7c not-int                    
		// opcode: 7d neg-long                   
		// opcode: 7e not-long                   
		// opcode: 7f neg-float                  
		// opcode: 80 neg-double     
		case NEG_DOUBLE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("neg-double v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 81 int-to-long    
		case INT_TO_LONG:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("int-to-long v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 82 int-to-float
		case INT_TO_FLOAT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("int-to-float v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 83 int-to-double         
		case INT_TO_DOUBLE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("int-to-double v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 84 long-to-int    
		case LONG_TO_INT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("long-to-int v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 85 long-to-float   
		case LONG_TO_FLOAT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("long-to-float v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 86 long-to-double    
		case LONG_TO_DOUBLE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("long-to-double v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 87 float-to-int    
		case FLOAT_TO_INT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("float-to-int v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 88 float-to-long    
		case FLOAT_TO_LONG:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("float-to-long v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 89 float-to-double       
		case FLOAT_TO_DOUBLE:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("float-to-double v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 8a double-to-int       
		case DOUBLE_TO_INT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("double-to-int v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 8b double-to-long            
		case DOUBLE_TO_LONG:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("double-to-long v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 8c double-to-float
		case DOUBLE_TO_FLOAT:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("double-to-float v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: 8d int-to-byte                
		// opcode: 8e int-to-char                
		// opcode: 8f int-to-short               
		// opcode: 90 add-int     
		case ADD_INT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("add-int v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 91 sub-int      
		case SUB_INT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("sub-int v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 92 mul-int    
		case MUL_INT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("mul-int v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 93 div-int      
		case DIV_INT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("div-int v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: 94 rem-int  
		case REM_INT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("rem-int v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// ... to be continued ...
		
		// opcode: 95 and-int                    
		// opcode: 96 or-int                     
		// opcode: 97 xor-int                    
		// opcode: 98 shl-int                    
		// opcode: 99 shr-int                    
		// opcode: 9a ushr-int                   
		// opcode: 9b add-long                   
		// opcode: 9c sub-long      
		case SUB_LONG:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("sub-long v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
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
		case ADD_FLOAT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("add-float v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: a7 sub-float    
		case SUB_FLOAT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("sub-float v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: a8 mul-float
		case MUL_FLOAT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("mul-float v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: a9 div-float
		case DIV_FLOAT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("div-float v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: aa rem-float
		case REM_FLOAT:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("rem-float v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: ab add-double 
		case ADD_DOUBLE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("add-double v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: ac sub-double                 
		// opcode: ad mul-double
		case MUL_DOUBLE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("mul-double v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: ae div-double  
		case DIV_DOUBLE:
		{
			int vA = ((ThreeRegisterInstruction)instruction).getRegisterA();
			int vB = ((ThreeRegisterInstruction)instruction).getRegisterB();
			int vC = ((ThreeRegisterInstruction)instruction).getRegisterC();

			result += String.format("div-double v%d, v%d, v%d",
					vA, vB, vC);
			break;
		}
		
		// opcode: af rem-double                 
		// opcode: b0 add-int/2addr    
		case ADD_INT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("add-int/2addr v%d, v%d", vA, vB);
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
		case ADD_FLOAT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("add-float/2addr v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: c7 sub-float/2addr
		case SUB_FLOAT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("sub-float/2addr v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: c8 mul-float/2addr
		case MUL_FLOAT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("mul-float/2addr v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: c9 div-float/2addr
		case DIV_FLOAT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("div-float/2addr v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: ca rem-float/2addr
		case REM_FLOAT_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("rem-float/2addr v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: cb add-double/2addr
		case ADD_DOUBLE_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("add-double/2addr v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: cc sub-double/2addr           
		// opcode: cd mul-double/2addr
		case MUL_DOUBLE_2ADDR:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			
			result += String.format("mul-double/2addr v%d, v%d", vA, vB);
			break;
		}
		
		// opcode: ce div-double/2addr           
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
			
			result += String.format("add-int/lit8 v%d, v%d, #%d", vA, vB, constant);
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
		case EXECUTE_INLINE:
		{
			int inlineIndex = ((OdexedInvokeInline)instruction).getInlineIndex();			
			String argsString = getArgsString(instruction);
			result += String.format("+execute-inline %s, [%#x]", argsString, inlineIndex);
			break;
		}
		
		// opcode: ef +execute-inline/range      
		// opcode: f0 +invoke-object-init/range  
		// opcode: f1 +return-void-barrier       
		// opcode: f2 +iget-quick     
		case IGET_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();

			result += String.format("+iget-quick v%d, v%d, [obj+%d]",
					vA, vB, offset);
			break;
		}
		
		// opcode: f3 +iget-wide-quick           
		// opcode: f4 +iget-object-quick     
		case IGET_OBJECT_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();

			result += String.format("+iget-object-quick v%d, v%d, [obj+%d]",
					vA, vB, offset);
			break;
		}
		
		// opcode: f5 +iput-quick      
		case IPUT_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();

			result += String.format("+iput-quick v%d, v%d, [obj+%d]",
					vA, vB, offset);
			break;
		}
		
		// opcode: f6 +iput-wide-quick           
		// opcode: f7 +iput-object-quick         
		// opcode: f8 +invoke-virtual-quick      
		case INVOKE_VIRTUAL_QUICK:
		{
			
			int vtableIndex = ((OdexedInvokeVirtual)instruction).getVtableIndex();
			
			String argsString = getArgsString(instruction);
			
			result += String.format("+invoke-virtual-quick %s, [%#x]", argsString, vtableIndex);
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
