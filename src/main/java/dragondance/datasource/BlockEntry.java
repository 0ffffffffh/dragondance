package dragondance.datasource;

public class BlockEntry {
	private int offset;
	private int size;
	private int mid;
	private int instCount;
	
	public BlockEntry(int entryOffset, int entrySize, int moduleId, int instructionCount) {
		this.offset=entryOffset;
		this.size=entrySize;
		this.mid=moduleId;
		this.instCount=instructionCount;
	}
	
	public final int getOffset() {
		return this.offset;
	}
	
	public final int getSize() {
		return this.size;
	}
	
	public final int getModuleId() {
		return this.mid;
	}
	
	public final int getInstructionCount() {
		return this.instCount;
	}
}
