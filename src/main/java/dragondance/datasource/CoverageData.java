package dragondance.datasource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import dragondance.Globals;
import dragondance.Log;
import dragondance.eng.CodeRange;
import dragondance.eng.DragonHelper;
import dragondance.eng.InstructionInfo;
import dragondance.eng.Painter;
import dragondance.eng.session.Session;
import dragondance.eng.session.SessionManager;
import dragondance.exceptions.InvalidInstructionAddress;
import dragondance.exceptions.OperationAbortedException;

class CodeRangeComparator implements Comparator<CodeRange> {

	@Override
	public int compare(CodeRange o1, CodeRange o2) {
		return (int)(o1.getRangeStart() - o2.getRangeStart());
	}
	
}

public class CoverageData implements AutoCloseable {
	private static CodeRangeComparator rangeListComparator = new CodeRangeComparator();
	
	private List<CodeRange> rangeList = null;
	private HashMap<Long,List<InstructionInfo>> addressMap = null;
	
	private CoverageDataSource source = null;
	private int maxDensity=0;
	
	private int initialRangeCount=0;
	private int mergedRangeCount=0;
	
	private boolean visualized=false;
	private boolean sorted=false;
	private boolean inClose=false;
	
	private Session ownerSession=null;
	
	public CoverageData(CoverageDataSource source) {
		this.source = source;
		this.addressMap = new HashMap<Long,List<InstructionInfo>>();
		
		this.ownerSession = SessionManager.getActiveSession();
	}
	
	private static CoverageData newLogical() {
		CoverageData cov = new CoverageData(null);
		cov.rangeList = new ArrayList<CodeRange>();
		
		return cov;
	}
	
	public static CoverageData intersect(CoverageData covData1, CoverageData covData2) {
		
		CodeRange lastRange = null;
		CoverageData isectResult = CoverageData.newLogical();
		
		for (Long key : covData1.addressMap.keySet()) {
			if (covData2.addressMap.containsKey(key)) {
				InstructionInfo inst = covData1.lookupAddressMapSingle(key);
				
				lastRange = isectResult.pushRangeListNoThrow(lastRange,inst.getAddr(),inst.getSize(),false);
			
			}
		}
		
		isectResult.merge();
		
		return isectResult;
	}
	
	public static CoverageData difference(CoverageData covData1, CoverageData covData2) {
		CodeRange lastRange = null;
		CoverageData diffResult;
		InstructionInfo inst;
		
		HashSet<Long> rightKeyset = new HashSet<Long>();
		
		rightKeyset.addAll(covData2.addressMap.keySet());
		
		diffResult = CoverageData.newLogical();
		
		for (Long key : covData1.addressMap.keySet()) {
			if (!covData2.addressMap.containsKey(key)) {
				inst = covData1.lookupAddressMapSingle(key);
				
				lastRange = diffResult.pushRangeListNoThrow(lastRange, inst.getAddr(), inst.getSize(), false);
			}
		}
		
		
		diffResult.merge();
		
		return diffResult;
	}
	
	public static CoverageData distinct(CoverageData covData1, CoverageData covData2) {
		CodeRange lastRange = null;
		CoverageData distinctResult;
		InstructionInfo inst;
		
		HashSet<Long> rightKeyset = new HashSet<Long>();
		
		rightKeyset.addAll(covData2.addressMap.keySet());
		
		distinctResult = CoverageData.newLogical();
		
		for (Long key : covData1.addressMap.keySet()) {
			
			if (!covData2.addressMap.containsKey(key)) {
				
				inst = covData1.lookupAddressMapSingle(key);
				
				lastRange = distinctResult.pushRangeListNoThrow(lastRange, inst.getAddr(), inst.getSize(), false);
				
			}
			else
				rightKeyset.remove(key);
			
		}
		
		for (Long key : rightKeyset) {
			inst = covData2.lookupAddressMapSingle(key);
			lastRange = distinctResult.pushRangeListNoThrow(lastRange, inst.getAddr(), inst.getSize(), false);
		}
		
		distinctResult.merge();
		
		return distinctResult;
	}
	
	public static CoverageData sum(CoverageData covData1, CoverageData covData2) {
		CoverageData sumResult = CoverageData.newLogical();
		CodeRange lastRange=null;
		InstructionInfo inst=null;
		
		
		
		for (List<InstructionInfo> list : covData1.addressMap.values()) {
			inst = list.get(0);
			
			if (inst != null)
				lastRange = sumResult.pushRangeListNoThrow(lastRange, inst.getAddr(), inst.getSize(), false);
		}
		
		
		for (List<InstructionInfo> list : covData2.addressMap.values()) {
			inst = list.get(0);
			
			if (inst != null)
				lastRange = sumResult.pushRangeListNoThrow(lastRange, inst.getAddr(), inst.getSize(), false);
			
		}
		
		sumResult.merge();
		
		return sumResult;
	}
	
	public static CoverageData and(CoverageData ...covDataList) {
		return intersect(covDataList);
	}

	public static CoverageData or(CoverageData ...covDataList) {
		return sum(covDataList);
	}
	
	public static CoverageData xor(CoverageData ...covDataList) {
		return distinct(covDataList);
	}
	
	
	public static CoverageData intersect(CoverageData ...covDataList) {
		CoverageData result;
		
		if (covDataList.length < 2)
			return null;
		
		result = intersect(covDataList[0],covDataList[1]);
		
		for (int i=2;i<covDataList.length;i++) {
			result = intersect(result,covDataList[i]);
		}
		
		
		return result;
	}
	
	public static CoverageData difference(CoverageData ...covDataList) {
		CoverageData result;
		
		if (covDataList.length < 2)
			return null;
		
		result = difference(covDataList[0],covDataList[1]);
		
		for (int i=2;i<covDataList.length;i++) {
			result = difference(result,covDataList[i]);
		}
		
		
		return result;
	}
	
	public static CoverageData sum(CoverageData ...covDataList) {
		CoverageData result;
		
		if (covDataList.length < 2)
			return null;
		
		result = sum(covDataList[0],covDataList[1]);
		
		for (int i=2;i<covDataList.length;i++) {
			result = sum(result,covDataList[i]);
		}
		
		
		return result;
	}
	
	public static CoverageData distinct(CoverageData ...covDataList) {
		CoverageData result;
		
		if (covDataList.length < 2)
			return null;
		
		result = distinct(covDataList[0],covDataList[1]);
		
		for (int i=2;i<covDataList.length;i++) {
			result = distinct(result,covDataList[i]);
		}
		
		
		return result;
	}
	
	private void merge() {
		CodeRange range;
		
		if (this.isLogicalCoverageData()) {
			//Merging operation only needed after raw coverage data read from the coverage file.
			return;
		}
		
		Log.info("rangeList: %d", this.rangeList.size());
		
		for (int i=0;i<this.rangeList.size();i++) {
			for (int j=0;j<this.rangeList.size();j++) {
				if (i != j) {
					if (this.rangeList.get(i).mergeFrom(
							this.rangeList.get(j) )) {
						
						this.mergedRangeCount++;
						
						range = this.rangeList.remove(j);
						range.unmapFromAddressMap();
						
						if (i >= j)
							i--;
						
						if (j > 0)
							j--;
						
					}
				}
			}
		}
	}
	
	private InstructionInfo lookupAddressMapSingle(long addrKey) {
		
		List<InstructionInfo> list = null;
		
		if (!this.addressMap.containsKey(addrKey))
			return null;
		
		list = this.addressMap.get(addrKey);
		
		if (list.size()>0)
			return list.get(0);
		
		return null;
	}
	
	private CodeRange pushRangeListNoThrow(CodeRange codeRange, long addr, int size, boolean isSequence) {
		try {
			return pushRangeList(codeRange, addr, size,isSequence);
		} catch (InvalidInstructionAddress | OperationAbortedException e) {
			Log.println("pushRangeList (%s)", e.getMessage());
		}
		
		return null;
	}
	
	private CodeRange pushRangeList(CodeRange codeRange, long addr, int size, boolean isSequence) throws InvalidInstructionAddress, OperationAbortedException {
		final boolean singleInstruction = !isSequence;
		
		if (this.rangeList.isEmpty()) {
			codeRange = new CodeRange(this,addr,size,this.addressMap,singleInstruction);
			this.rangeList.add(codeRange);
		}
		else {
			if (!codeRange.tryApply(addr, size,singleInstruction)) {
				codeRange = new CodeRange(this, addr, size,this.addressMap, singleInstruction);
				this.rangeList.add(codeRange);
			}
		}
		
		return codeRange;
	}
	
	private void buildRanges() throws InvalidInstructionAddress, OperationAbortedException {
		long imgBase,addr;
		CodeRange codeRange = null;
		
		this.rangeList = new ArrayList<CodeRange>();
		
		imgBase = DragonHelper.getImageBase().getOffset();
		
		Log.info("Generating initial code ranges. Total block entry: %d",source.entries.size());
		
		for (BlockEntry be : source.entries) {
			
			addr = imgBase + be.getOffset();
			codeRange = pushRangeList(codeRange, addr,be.getSize(),true);
		}
		
		this.initialRangeCount = this.rangeList.size();
		
		Log.info("%d ranges generated.", this.rangeList.size());
		Log.info("trying to merge ranges");
		
		merge();
		
		Log.info("final code range size: %d, %d range merged", this.rangeList.size(),this.mergedRangeCount);
		
	}
	
	public boolean build() throws InvalidInstructionAddress, OperationAbortedException {
		
		if (this.rangeList != null)
			return true;
		
		if (this.source == null)
			return false;
		
		if (!this.source.isProcessed())
			return false;
		
		this.buildRanges();
		
		if (Globals.DumpInstructions) {
			boolean pv,pd;
			pv = Log.enableVerbose(true);
			pd = Log.enableDebug(true);
			this.dump();
			this.dumpHashMap();
			Log.enableVerbose(pv);
			Log.enableDebug(pd);
		}
		
		return true;
	}
	
	public void dump() {
		for (CodeRange range : this.rangeList) {
			range.dumpInstructionDensityList();
		}
	}
	
	public void dumpHashMap() {
		List<InstructionInfo> listValue;
		
		for (Long key : this.addressMap.keySet())
		{
			listValue = this.addressMap.get(key);
			
			Log.debug("Key: %x | ",key);
			Log.debug("Value(s): ");
			
			for (InstructionInfo ii : listValue) {
				if (ii.getOwnerCodeRange().getContainerCoverage() == this) {
					Log.debug("%x (density: %d)," , ii.hashCode(), ii.getDensity());
				}
			}
			
			Log.println("");
		}
	}
	
	public void paint(Painter painter) {
		
		if (this.visualized)
			return;
		
		boolean failed=false;
		
		int transId = DragonHelper.startTransaction("BgPaint");
		
		for (CodeRange range : this.rangeList) {
			if (!range.paintRange(painter)) {
				failed=true;
				break;
			}
		}
		
		DragonHelper.finishTransaction(transId,!failed);
		
		if (!failed)
			this.visualized=true;
	}
	
	public void clearPaint() {
		
		if (!this.visualized)
			return;
		
		int transId = DragonHelper.startTransaction("ClearBgPaint");
		
		for (CodeRange range : this.rangeList) {
			range.clearPaint();
		}
		
		DragonHelper.finishTransaction(transId, true);
		
		this.visualized=false;
	}
	
	public CoverageDataSource getSource() {
		return this.source;
	}
	
	public int getSourceId() {
		
		if (this.source==null)
			return 0;
		
		return this.source.getId();
	}
	
	public void setMaxDensity(int density) {
		if (density > this.maxDensity) {
			Log.debug("new max density: %d", density);
			this.maxDensity = density;
		}
	}
	
	public final int getMaxDensity() {
		return this.maxDensity;
	}

	@Override
	public void close() throws Exception {
		
		if (this.inClose)
			return;
		
		this.inClose = true;
		
		if (this.ownerSession.isActiveCoverage(this)) {
			this.ownerSession.setActiveCoverage(null);
		}
		
		this.clearPaint();
		
		if (this.rangeList != null)
			this.rangeList.clear();
		
		if (this.addressMap != null)
			this.addressMap.clear();
		
		if (!isLogicalCoverageData())
			this.source.close();
		
		this.ownerSession=null;
		
		this.inClose = false;
		
	}
	
	public void closeNothrow() {
		try {
			close();
		} catch (Exception e) {
		}
	}
	
	public final int getRangeCount() {
		
		if (this.rangeList==null)
			return 0;
		
		return this.rangeList.size();
	}
	
	public final int getInitialRangeCount() {
		return this.initialRangeCount;
	}
	
	public final int getMergedRangeCount() {
		return this.mergedRangeCount;
	}
	
	public void sort() {
		if (!this.sorted) {
			this.rangeList.sort(rangeListComparator);
			this.sorted=true;
		}
	}
	
	public final String getName() {
		if (this.source == null) {
			return "";
		}
		
		return this.source.getName();
	}
	
	public final String getSourceFilePath() {
		if (this.source == null) {
			return "";
		}
		
		return this.source.getFilePath();
	}
	
	public final boolean isLogicalCoverageData() {
		return this.source == null;
	}
}
