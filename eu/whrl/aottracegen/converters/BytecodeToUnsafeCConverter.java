package eu.whrl.aottracegen.converters;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OdexedFieldAccess;
import org.jf.dexlib.Code.ThreeRegisterInstruction;
import org.jf.dexlib.Code.TwoRegisterInstruction;

import eu.whrl.aottracegen.CodeGenContext;
import eu.whrl.aottracegen.exceptions.UnimplementedInstructionException;

public class BytecodeToUnsafeCConverter extends BytecodeToCConverter {
	
	public BytecodeToUnsafeCConverter(CodeGenContext context) {
		super(context);
	}

	public String convert(CodeGenContext context, int codeAddress) throws UnimplementedInstructionException {
		
		Instruction instruction = context.getInstructionAtCodeAddress(codeAddress);
		
		String result = "";
		
		switch (instruction.opcode) {

		// opcode: 00 nop                        
		// opcode: 01 move                       
		// opcode: 02 move/from16                
		// opcode: 03 move/16                    
		// opcode: 04 move-wide                  
		// opcode: 05 move-wide/from16           
		// opcode: 06 move-wide/16               
		// opcode: 07 move-object                
		// opcode: 08 move-object/from16         
		// opcode: 09 move-object/16             
		// opcode: 0a move-result                
		// opcode: 0b move-result-wide           
		// opcode: 0c move-result-object         
		// opcode: 0d move-exception             
		// opcode: 0e return-void                
		// opcode: 0f return                     
		// opcode: 10 return-wide                
		// opcode: 11 return-object              
		// opcode: 12 const/4                    
		// opcode: 13 const/16                   
		// opcode: 14 const                      
		// opcode: 15 const/high16               
		// opcode: 16 const-wide/16              
		// opcode: 17 const-wide/32              
		// opcode: 18 const-wide                 
		// opcode: 19 const-wide/high16          
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
		// opcode: 29 goto/16                    
		// opcode: 2a goto/32                    
		// opcode: 2b packed-switch              
		// opcode: 2c sparse-switch              
		// opcode: 2d cmpl-float                 
		// opcode: 2e cmpg-float                 
		// opcode: 2f cmpl-double                
		// opcode: 30 cmpg-double                
		// opcode: 31 cmp-long                   
		// opcode: 32 if-eq                      
		// opcode: 33 if-ne                      
		// opcode: 34 if-lt                      
		// opcode: 35 if-ge                      
		// opcode: 36 if-gt                      
		// opcode: 37 if-le                      
		// opcode: 38 if-eqz                     
		// opcode: 39 if-nez                     
		// opcode: 3a if-ltz                     
		// opcode: 3b if-gez                     
		// opcode: 3c if-gtz                     
		// opcode: 3d if-lez                     
		// opcode: 44 aget     
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
		
		// opcode: 45 aget-wide                  
		// opcode: 46 aget-object                
		// opcode: 47 aget-boolean               
		// opcode: 48 aget-byte     
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
		
		// opcode: 49 aget-char                  
		// opcode: 4a aget-short                 
		// opcode: 4b aput            
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
		
		// opcode: 4c aput-wide                  
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
		// opcode: 7c not-int                    
		// opcode: 7d neg-long                   
		// opcode: 7e not-long                   
		// opcode: 7f neg-float                  
		// opcode: 80 neg-double                 
		// opcode: 81 int-to-long                
		// opcode: 82 int-to-float               
		// opcode: 83 int-to-double              
		// opcode: 84 long-to-int                
		// opcode: 85 long-to-float              
		// opcode: 86 long-to-double             
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
		// opcode: 91 sub-int                    
		// opcode: 92 mul-int                    
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
		// opcode: c7 sub-float/2addr            
		// opcode: c8 mul-float/2addr            
		// opcode: c9 div-float/2addr            
		// opcode: ca rem-float/2addr            
		// opcode: cb add-double/2addr           
		// opcode: cc sub-double/2addr           
		// opcode: cd mul-double/2addr           
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

			result = String.format("  v[%1$d] = *((int*) (((char*)v[%2$d]) + %3$#x));", vA, vB, offset);
			break;
		}
		
		// opcode: f3 +iget-wide-quick           
		// opcode: f4 +iget-object-quick         
		// opcode: f5 +iput-quick           
		case IPUT_QUICK:
		{
			int vA = ((TwoRegisterInstruction)instruction).getRegisterA();
			int vB = ((TwoRegisterInstruction)instruction).getRegisterB();
			int offset = ((OdexedFieldAccess)instruction).getFieldOffset();

			result = String.format("  *((int*) (((char*)v[%2$d]) + %3$#x)) = v[%1$d];", vA, vB, offset);
			break;
		}
		
		// opcode: f6 +iput-wide-quick           
		// opcode: f7 +iput-object-quick         
		// opcode: f8 +invoke-virtual-quick      
		// opcode: f9 +invoke-virtual-quick/range
		// opcode: fa +invoke-super-quick        
		// opcode: fb +invoke-super-quick/range  
		// opcode: fc +iput-object-volatile      
		// opcode: fd +sget-object-volatile      
		// opcode: fe +sput-object-volatile 
		
		default:
		{
			return super.convert(context, codeAddress);
		}
		
		}
		
		return result + "\n\n";
	}
}
