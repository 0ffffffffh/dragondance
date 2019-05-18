package dragondance.eng;

public class InstructionInfo {
	
	private long addr;
	private int size;
	private int density;
	private CodeRange container;
	
	public static InstructionInfo cloneFor(InstructionInfo inst, CodeRange containerRange) {
		InstructionInfo cloneInst = new InstructionInfo(containerRange,inst.addr,inst.size, inst.density);
		return cloneInst;
	}
	
	public InstructionInfo(CodeRange containerRange, long a, int s, int d) {
		this.container = containerRange;
		this.addr=a;
		this.size=s;
		this.setDensity(d);
	}
	
	public long getAddr() {
		return this.addr;
	}
	
	public int getSize() {
		return this.size;
	}
	
	public int getDensity() {
		return this.density;
	}
	
	private void onDensityChanged()  {
		
		if (this.container != null)
			this.container.getContainerCoverage().setMaxDensity(this.density);
		
	}
	
	public void setDensity(int value) {
		this.density=value;
		onDensityChanged();
	}
	
	public void incrementDensityBy(int amount) {
		this.density += amount;
		onDensityChanged();
	}
	
	public void incrementDensity() {
		this.density++;
		onDensityChanged();
	}
	
	public final boolean hasOwnerRange() {
		return this.container != null;
	}
	
	public CodeRange getOwnerCodeRange() {
		return this.container;
	}
	
	@Override
	public String toString() {
		return String.format("(%x, %d [%x])",this.addr, this.size,this.addr + this.size);
	}
}