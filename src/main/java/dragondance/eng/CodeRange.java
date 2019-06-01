package dragondance.eng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dragondance.Globals;
import dragondance.Log;
import dragondance.datasource.CoverageData;
import dragondance.exceptions.InvalidInstructionAddress;
import dragondance.exceptions.OperationAbortedException;

public class CodeRange implements AutoCloseable {
	
	private static int gs_RangeIndex=1;
	
	private long rangeStart,rangeEnd,rangeSize;
	
	private long avgInstSize=0;
	private long totalInstSize=0;
	private List<InstructionInfo> densityList;
	private HashMap<Long, List<InstructionInfo>> map;
	
	private String name;
	private CoverageData container;
	
	private class IntersectionBound
	{
		public long begin,end;
		
		public final boolean areEqual() {
			return begin==end;
		}
	}
	
	public CodeRange(CoverageData container, long start, int size, HashMap<Long, List<InstructionInfo>> addressMap, boolean singleInstruction) throws InvalidInstructionAddress, OperationAbortedException {
		this.rangeStart = start;
		this.rangeEnd = start + size;
		this.rangeSize = size;
		this.container = container;
		this.map = addressMap;
		
		this.densityList = new ArrayList<InstructionInfo>();
		
		this.name = "Range " + String.valueOf(gs_RangeIndex);
		
		CodeRange.gs_RangeIndex++;
		
		this.add(start, size,singleInstruction);
	}
	
	public CodeRange(CoverageData container, long start, int size, boolean singleInstruction) throws InvalidInstructionAddress, OperationAbortedException {
		this(container,start,size,null,singleInstruction);
	}
	
	public CodeRange(CoverageData container, long start, int size) throws InvalidInstructionAddress, OperationAbortedException {
		this(container,start,size,false);
	}
	
	private void printRangeInfo() {
		Log.debug("Range start: %p, Range end: %p, size: %d",
				this.rangeStart,this.rangeEnd,this.rangeSize);
		
	}
	
	public void dumpInstructionDensityList() {
		for (InstructionInfo ii : this.densityList)
		{
			Log.verbose("addr: %p, size: %d, density: %d", ii.getAddr(),ii.getSize(),ii.getDensity());
		}
	}
	
	private boolean copyList(List<InstructionInfo> dest, CodeRange destRange, List<InstructionInfo> src, int destCopyIndex, int srcCopyIndex, int size) {
		
		int dindex = destCopyIndex < 0 ? dest.size() : destCopyIndex;
		int sindex = srcCopyIndex < 0 ? src.size() : srcCopyIndex;
		
		if (dindex > dest.size())
			dindex = dest.size();
		
		if (sindex + size > src.size())
			return false;
		
		
		while (size-- > 0)
		{
			InstructionInfo inst = InstructionInfo.cloneFor(src.get(sindex), destRange);
			
			dest.add(dindex, inst);
			this.putMapIfAvailable(inst);
			
			dindex++;
			sindex++;
		}
		
		return true;
	}
	
	private final boolean isInRange(long addr) {
		return addr >= this.rangeStart && addr <= this.rangeEnd;
	}
	
	private IntersectionBound getIntersectionBound(CodeRange source) {
		
		IntersectionBound isectBound=new IntersectionBound();
		
		isectBound.begin = source.rangeStart > this.rangeStart ? 
				source.rangeStart : this.rangeStart;
		
		isectBound.end = source.rangeEnd < this.rangeEnd ?
				source.rangeEnd : this.rangeEnd;
		
		return isectBound;
	}
	
	private void updateAvgInstSize(int size) {
		this.totalInstSize += size;
		this.avgInstSize = this.totalInstSize / this.densityList.size();
	}
	
	private int getInstructionSize(long addr) throws InvalidInstructionAddress, OperationAbortedException {
		InstructionContext ictx = null;
		
		if (Globals.WithoutGhidra)
			return 4;
		
		ictx = DragonHelper.getInstruction(addr,true);
		
		if (ictx == null) {
			throw new OperationAbortedException(String.format("There is no valid instruction at %x",addr));
		}
		
		return ictx.getSize();
	}
	
	private int getIndexFromAddr(long addr) {
		
		InstructionInfo instInfo;
		
		int stepLimit=15,ri;
		
		if (!isInRange(addr))
			return -1;
		
		if (this.densityList.isEmpty())
			return -1;
		
		assert(this.avgInstSize != 0);
		
		//guess the index. most of the time our guess will hit for first time
		ri = (int)((addr - this.rangeStart) / this.avgInstSize);
		
		//reduce the guess if its out of the list
		if (ri >= this.densityList.size())
			ri = this.densityList.size()/2;
		
		instInfo = this.densityList.get(ri);
		
		//test addresses 
		if (instInfo.getAddr() == addr)
			return ri;
		
		if (instInfo.getAddr() > addr)
		{
			/*
			 * Guessed address of the index greater than we looking for
			 * So we have step back few times.
			 * (That would not be nothing more than 15 times. 
			 * cuz of the x86's maximum instruction size)
			 * 
			 * That would not be issue on the RISC instruction sets.
			 */
			
			while (ri > 0 && stepLimit > 0)
			{
				ri--;
				
				instInfo = this.densityList.get(ri);
				
				if (instInfo.getAddr() == addr)
					return ri;
				
				stepLimit--;
			}
		}
		else
		{
			//in this case we have step forward
			
			while (ri != this.densityList.size()-1 && stepLimit > 0)
			{
				ri++;
				
				instInfo = this.densityList.get(ri);
				
				if (instInfo.getAddr() == addr)
					return ri;
				
				stepLimit--;
				
			}
			
		}
		
		return -1;
	}
	
	private boolean incrementRangeDensity(long addr, int size) {
		int ri = getIndexFromAddr(addr);
		
		if (ri == -1)
			return false;
		
		while (size > 0)
		{
			InstructionInfo ictx = this.densityList.get(ri);
			
			ictx.incrementDensity();
			size -= ictx.getSize();
			
			ri++;
		}
		
		return true;
	}
	
	private boolean mergeFromInternal(CodeRange source) {
		
		int ri,sri,tmp;
		
		IntersectionBound ibound = getIntersectionBound(source);
		
		Log.debug("--------src range--------");
		source.printRangeInfo();
		Log.debug("--------self range--------");
		this.printRangeInfo();
		
		Log.debug("intersection start: %p, end: %p",ibound.begin,ibound.end);
		
		if (ibound.areEqual())
		{
			Log.verbose("intersection points are same");
		}
		
		//make sure the intersection start point is valid for the source range
		ri = source.getIndexFromAddr(ibound.begin);
		
		if (ri == -1)
		{
			if (ibound.areEqual())
				ri = source.densityList.size();
			else
				return false; //no they are not really intersected.
		}
		
		tmp = ri;
		
		sri = this.getIndexFromAddr(ibound.begin);
		
		if (sri == -1)
		{
			if (!ibound.areEqual())
				return false; // they are not really intersected either.
		}
		
		if (!ibound.areEqual())
		{
			//merge intersected range densities first.
			
			assert(this.densityList.get(sri).getAddr() == ibound.begin);
			
			while (sri != this.densityList.size() &&
					this.densityList.get(sri).getAddr() < ibound.end)
			{
				this.densityList.get(sri).incrementDensityBy(
						source.densityList.get(ri).getDensity());
				
				sri++;
				ri++;
			}
		}
		else
		{
			//intersection areas are same. there is no item to merge
		}
		
		//prepend lower address range part if exists
		if (source.rangeStart < this.rangeStart)
		{
			ri = tmp;
			copyList(this.densityList,this, source.densityList,0,0,ri);
			
			tmp = (int)this.rangeSize;
			
			this.rangeStart = source.rangeStart;
			this.rangeSize = this.rangeEnd - this.rangeStart;
			
			tmp = (int)this.rangeSize - tmp;
			
			updateAvgInstSize(tmp);
			
		}
		
		//append higher address range part if exists
		if (source.rangeEnd > this.rangeEnd)
		{
			sri = source.getIndexFromAddr(ibound.end);
			copyList(this.densityList,this, source.densityList,-1,sri,source.densityList.size()-sri);
			tmp = (int)this.rangeSize;
			
			this.rangeEnd = source.rangeEnd;
			this.rangeSize = this.rangeEnd - this.rangeStart;
			
			tmp = (int)this.rangeSize - tmp;
			updateAvgInstSize(tmp);
		}
		
		Log.debug("new range info");
		this.printRangeInfo();
		
		return true;
	}
	
	private void putMapIfAvailable(InstructionInfo inst) {
		List<InstructionInfo> instList;
		
		if (this.map == null)
			return;
		
		if (this.map.containsKey(inst.getAddr())) {
			instList = this.map.get(inst.getAddr());
			
			if (!instList.contains(inst)) {
				instList.add(inst);
			}
			else {
				Log.warning("%p already exists",inst.getAddr());
			}
			
		}
		else {
			instList = new ArrayList<InstructionInfo>();
			instList.add(inst);
			
			this.map.put(inst.getAddr(), instList);
		}
	}
	
	private void removeMapIfAvailable(InstructionInfo inst) {
		List<InstructionInfo> instList;
		
		if (this.map == null)
			return;
		
		Log.debug("Removing for inst: %x", inst.getAddr());
		
		if (this.map.containsKey(inst.getAddr())) {
			instList = this.map.get(inst.getAddr());
			
			for (int i=0;i<instList.size();i++) {
				if (instList.get(i).getOwnerCodeRange() == this) {
					instList.remove(i);
					i--;
				}
			}
		}
	}
	
	private boolean add(long start, int size, boolean singleInstruction) throws InvalidInstructionAddress, OperationAbortedException {
		InstructionInfo instCtx;
		int insSize=0;
		long addr = start;
		long eaddr = addr + size;
		
		while (addr < eaddr)
		{
			if (singleInstruction)
				insSize = size;
			else
				insSize = getInstructionSize(addr);
			
			if (insSize == 0) {
				//TODO: maybe raise an abort event?
				return false;
			}
			
			if (addr == start && size < insSize)
				return false;
			
			instCtx = new InstructionInfo(this, addr,insSize,1);
			
			addr += insSize;
			
			this.densityList.add(instCtx);
			
			this.putMapIfAvailable(instCtx);
			updateAvgInstSize(insSize);
			
		}
		
		this.rangeEnd = addr;
		this.rangeSize = this.rangeEnd - this.rangeStart;
		
		//fix end if neccessary due to instruction size
		
		if (addr > eaddr)
		{
			/*
			 * #hmm. the last instruction has overflowed the expected range size
			 * so the last instruction must be discarded from the list.
			 * this is violates our strict range bound
			 */
			
			InstructionInfo lastInst;
			
			lastInst = this.densityList.remove(this.densityList.size()-1);
			this.removeMapIfAvailable(lastInst);
			
			this.rangeEnd -= lastInst.getSize();
			this.rangeSize = this.rangeEnd - this.rangeStart;
			
		}
		
		return true;
	}
	
	private boolean tryApplyOverlappedFromHead(long addr, int size) throws InvalidInstructionAddress, OperationAbortedException {
		List<InstructionInfo> headPartList = new ArrayList<InstructionInfo>();
		InstructionInfo inst;
		
		int instSize=0;
		long currAddr = addr;
		
		while (true)
		{
			instSize = getInstructionSize(currAddr);
			
			if (instSize == 0)
			{
				headPartList.clear();
				return false;
			}
			
			inst = new InstructionInfo(this,currAddr,instSize,1);
			
			headPartList.add(inst);
			
			currAddr += instSize;
			
			//stop if we are at the this range's start point
			if (currAddr == this.rangeStart)
				break;
			else if (currAddr > this.rangeEnd)
			{
				//hmm we are working on wrong place. so cancel operation
				headPartList.clear();
				return false;
			}
		}
		
		//prepend head part to the current density list
		
		int nx=0;
		
		for (InstructionInfo ii : headPartList) {
			this.putMapIfAvailable(ii);
			this.densityList.add(nx++, ii);
		}
		
		//this.densityList.addAll(0, headPartList);
		headPartList.clear();
		
		Log.debug("old range (start: %p, size: %d)", this.rangeStart,this.rangeSize);
		
		//set new range bound
		
		this.rangeStart = addr;
		this.rangeSize = this.rangeEnd - this.rangeStart;
		
		Log.debug("new range (start: %p, size: %d)", this.rangeStart,this.rangeSize);
		
		
		int remainSize = size - (int)(currAddr - addr);
		
		if (remainSize > 0)
		{
			//just increase the density of the intersected part.
			incrementRangeDensity(currAddr, remainSize);
			//TODO: beware its status
		}
		
		return true;
	}
	
	public boolean tryApply(long addr, int size, boolean singleInstruction) throws InvalidInstructionAddress, OperationAbortedException {
		
		if (!isInRange(addr))
		{
			if (isInRange(addr+size))
			{
				return tryApplyOverlappedFromHead(addr,size);
			}
			
			return false;
		}
		
		
		if (addr + size > this.rangeEnd)
		{
			//addr + size goes out of the range. but is the starting address
			//mapped in the range?
			int ri = getIndexFromAddr(addr);
			
			if (ri == -1) // the address wasn't mapped in the range.
			{
				//so is it at range's upper bound?
				if (this.rangeEnd == addr)
				{
					//yep. we can append the block
					return add(addr, size, singleInstruction);
				}
				
				//otherwise the address is completely invalid.
				return false;
			}
			
			//ok. first we have to increment density of the intersected range part
			incrementRangeDensity(addr,(int)(this.rangeEnd-addr));
			
			//then append remaining part
			int excess = (int)((addr + size) - this.rangeEnd);
			
			Log.verbose("%d bytes exceeded. expanding range",excess);
			
			add(this.rangeEnd,excess,singleInstruction);
		}
		else //otherwise its completely overlapped.
			incrementRangeDensity(addr,size);
		
		return true;
	}
	
	public boolean tryApply(long addr, int size) throws InvalidInstructionAddress, OperationAbortedException {
		return tryApply(addr,size,false);
	}
	
	public boolean tryPutInstruction(long instAddr, int size) throws InvalidInstructionAddress, OperationAbortedException {
		return tryApply(instAddr,size,true);
	}
	
	public boolean mergeFrom(CodeRange sourceRange) {
		
		boolean canBeMerge=false;
		
		if (sourceRange.rangeStart <= this.rangeStart &&
				sourceRange.rangeEnd >= this.rangeStart)
		{
			canBeMerge=true;
		}
		else if (sourceRange.rangeStart >= this.rangeStart &&
				sourceRange.rangeStart <= this.rangeEnd)
		{
			canBeMerge=true;
		}
		
		if (canBeMerge != intersectable(sourceRange)) {
			Log.info("WARNING! canBeMerge=%s, intersectable=%s", 
					Boolean.toString(canBeMerge), Boolean.toString(intersectable(sourceRange)));
		}
		
		if (canBeMerge)
			return mergeFromInternal(sourceRange);
		
		
		return false;
	}
	
	
	public boolean intersectable(CodeRange range) {
		if (this.rangeEnd < range.rangeStart) {
			return false;
		}
		
		if (this.rangeStart > range.rangeEnd) {
			return false;
		}
		
		return true;
	}
	
	public void unmapFromAddressMap() {
		
		for (InstructionInfo ii : this.densityList) {
			this.removeMapIfAvailable(ii);
		}
	}
	
	public boolean paintRange(Painter painter) {
		
		for (InstructionInfo inst : this.densityList) {
			
			if (!painter.paint(inst))
				return false;
		}
		
		return true;
	}
	
	public void clearPaint() {
		for (InstructionInfo inst : this.densityList) {
			DragonHelper.clearInstructionBackgroundColor(inst.getAddr());
		}
	}
	
	public void setName(String rangeName) {
		this.name = rangeName;
	}
	
	public final long getRangeStart() {
		return this.rangeStart;
	}
	
	public final int getRangeSize() {
		return (int)this.rangeSize;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public CoverageData getContainerCoverage() {
		return this.container;
	}

	@Override
	public void close() throws Exception {
		this.densityList.clear();
	}
	
	@Override
	public String toString() {
		return String.format("(Start: %x, End: %x, Size: %d)", this.rangeStart, this.rangeEnd, this.rangeSize);
	}

}
