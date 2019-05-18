package dragondance.eng;

import ghidra.program.model.listing.CodeUnit;
import ghidra.program.model.listing.Instruction;

public class InstructionContext {
	@SuppressWarnings("unused")
	private Instruction instruction;
	private CodeUnit codeUnit;
	private InstructionInfo info;
	
	public InstructionContext(Instruction inst, CodeUnit cu, InstructionInfo info) {
		this.instruction = inst;
		this.codeUnit = cu;
		
		if (info == null) {
			info = new InstructionInfo(null,inst.getAddress().getOffset(),cu.getLength(),0);
		}
		
		this.info = info;
	}
	
	public InstructionContext(Instruction inst, CodeUnit cu) {
		this(inst,cu,null);
	}
	
	public final int getSize() {
		return this.codeUnit.getLength();
	}
	
	public final long getAddress() {
		return this.info.getAddr();
	}
}
